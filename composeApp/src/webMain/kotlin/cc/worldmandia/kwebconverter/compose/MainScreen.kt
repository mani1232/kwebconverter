package cc.worldmandia.kwebconverter.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(filesViewModel: FilesViewModel, changePage: (NavKey) -> Unit) {
    val scope = rememberCoroutineScope()
    val files by filesViewModel.files.collectAsStateWithLifecycle()
    val launcher = rememberFilePickerLauncher(
        mode = FileKitMode.MultipleWithState(maxItems = 3),
        type = FileKitType.File(extensions = listOf("yml", "yaml", "json")),
        title = "Only config files"
    ) { state ->
        when (state) {
            is FileKitPickerState.Started -> {
                println("Selection started with ${state.total} files") // Loading animation
            }

            is FileKitPickerState.Progress -> {
                filesViewModel.loadFile(state.processed.last())
                println("Processing: ${state.processed.size} / ${state.total}")
            }

            is FileKitPickerState.Completed -> {
                filesViewModel.loadFilesContent()
                filesViewModel.restoreDraftsIfAny()
                println("Completed: ${state.result.size} files selected")
            }

            is FileKitPickerState.Cancelled -> {
                println("Selection cancelled")
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Row {
                Button(onClick = { launcher.launch() }) {
                    Text("Pick a file")
                }
            }
            LazyColumn {
                if (files.isNotEmpty()) {
                    items(files.mapIndexed { index, string -> index to string }) { (id, fileData) ->
                        Column(Modifier.animateItem()) {
                            Text("Id: $id.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Text("Selected file: ${fileData.originalFile.name}")
                                Button(onClick = {
                                    scope.launch {
                                        fileData.saveToUser()
                                    }
                                }) {
                                    Text("Download")
                                }
                                Button(onClick = {
                                    changePage(FileEditorRoute(fileData))
                                }) {
                                    Text("Web editor")
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}