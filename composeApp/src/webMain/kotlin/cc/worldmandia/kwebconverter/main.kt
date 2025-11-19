package cc.worldmandia.kwebconverter

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import cc.worldmandia.kwebconverter.compose.FileEditorScreen
import cc.worldmandia.kwebconverter.compose.MainScreen
import cc.worldmandia.kwebconverter.model.FileItemModel
import cc.worldmandia.kwebconverter.viewmodel.FilesViewModel
import com.charleskorn.kaml.AnchorsAndAliases
import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.KoinApplication
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@Serializable
object HomeRoute : NavKey

@Serializable
data class FileEditorRoute(val file: FileItemModel) : NavKey

val viewModule = module {
    viewModelOf(::FilesViewModel)
}

val YAMLConfigured = Yaml(
    configuration = YamlConfiguration(
        anchorsAndAliases = AnchorsAndAliases.Permitted(),
        singleLineStringStyle = SingleLineStringStyle.Plain,
    )
)

@OptIn(ExperimentalComposeUiApi::class, KoinExperimentalAPI::class, ExperimentalMaterial3ExpressiveApi::class)
fun main() = ComposeViewport {
    KoinApplication(application = {
        modules(viewModule)
    }) {
        MaterialExpressiveTheme(colorScheme = darkColorScheme()) {
            val backStack = rememberNavBackStack(SavedStateConfiguration {
                serializersModule = SerializersModule {
                    polymorphic(NavKey::class) {
                        subclass(HomeRoute::class)
                        subclass(FileEditorRoute::class)
                    }
                }
            }, HomeRoute)

            rememberKoinModules {
                listOf(module {
                    navigation<HomeRoute> { _ ->
                        MainScreen(koinViewModel()) {
                            backStack.add(it)
                        }
                    }

                    navigation<FileEditorRoute> { route ->
                        FileEditorScreen(file = route.file, koinViewModel()) {
                            backStack.removeLastOrNull()
                        }
                    }
                })
            }

            val entryProvider = koinEntryProvider()

            NavDisplay(
                backStack = backStack, onBack = { backStack.removeLastOrNull() }, entryProvider = entryProvider
            )
        }
    }
}