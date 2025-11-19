package cc.worldmandia.kwebconverter.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import cc.worldmandia.kwebconverter.FileEditorRoute
import cc.worldmandia.kwebconverter.viewmodel.FilesViewModel
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitPickerState
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.launch

val MainFont = FontFamily.Monospace

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(filesViewModel: FilesViewModel, changePage: (NavKey) -> Unit) {
    val scope = rememberCoroutineScope()
    val files by filesViewModel.files.collectAsStateWithLifecycle()

    val launcher = rememberFilePickerLauncher(
        mode = FileKitMode.MultipleWithState(maxItems = 5),
        type = FileKitType.File(extensions = listOf("yml", "yaml", "json", "json5")),
        title = "Open config files"
    ) { state ->
        when (state) {
            is FileKitPickerState.Started -> println("Loading...")
            is FileKitPickerState.Completed -> {
                filesViewModel.loadFile(state.result)
                filesViewModel.loadFilesContent()
            }

            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { launcher.launch() },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Open File", fontFamily = MainFont) },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                "Projects",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = MainFont,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(16.dp))

            if (files.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No open files.\nClick + to start.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = MainFont,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(files, key = { it.originalFile.name }) { fileData ->
                        Card(
                            onClick = { changePage(FileEditorRoute(fileData)) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )

                                Spacer(Modifier.width(16.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        fileData.originalFile.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = MainFont,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        fileData.parserType.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = MainFont,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        scope.launch { fileData.saveToUser() }
                                    }) {
                                        Icon(
                                            Icons.Rounded.Download,
                                            "Download",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = { changePage(FileEditorRoute(fileData)) }
                                    ) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Edit", fontFamily = MainFont)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}