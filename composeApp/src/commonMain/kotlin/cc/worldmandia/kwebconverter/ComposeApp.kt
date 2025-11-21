package cc.worldmandia.kwebconverter

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import cc.worldmandia.kwebconverter.di.appModule
import cc.worldmandia.kwebconverter.domain.model.ProjectFile
import cc.worldmandia.kwebconverter.presentation.feature.dashboard.DashboardScreen
import cc.worldmandia.kwebconverter.presentation.feature.editor.FileEditorScreen
import cc.worldmandia.kwebconverter.presentation.theme.AppTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.KoinApplication
import org.koin.compose.module.rememberKoinModules
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@Serializable
object DashboardRoute : NavKey

@Serializable
data class EditorRoute(val file: ProjectFile) : NavKey

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3ExpressiveApi::class, KoinExperimentalAPI::class)
@Composable
@Preview
fun StartComposeApp() {
    KoinApplication(application = { modules(appModule) }) {
        AppTheme {
            val backStack = rememberNavBackStack(SavedStateConfiguration {
                serializersModule = SerializersModule {
                    polymorphic(NavKey::class) {
                        subclass(DashboardRoute::class)
                        subclass(EditorRoute::class)
                    }
                }
            }, DashboardRoute)

            rememberKoinModules {
                listOf(module {
                    navigation<DashboardRoute> {
                        DashboardScreen(
                            viewModel = koinViewModel(),
                            onFileOpen = { file ->
                                backStack.add(EditorRoute(file))
                            }
                        )
                    }

                    navigation<EditorRoute> { route ->
                        FileEditorScreen(
                            viewModel = koinViewModel(parameters = { parametersOf(route.file) }),
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                })
            }

            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = koinEntryProvider()
            )
        }
    }
}