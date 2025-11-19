package cc.worldmandia.kwebconverter.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import cc.worldmandia.kwebconverter.ParserType
import cc.worldmandia.kwebconverter.logic.CommandManager
import cc.worldmandia.kwebconverter.logic.setAllExpanded
import cc.worldmandia.kwebconverter.model.EditableNode
import cc.worldmandia.kwebconverter.setPlainText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    title: String,
    type: ParserType,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    cmdManager: CommandManager,
    rootNode: EditableNode,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onGenerateContent: () -> String?
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.Refresh, null) },
            title = { Text("Reset File?") },
            text = { Text("This will discard all unsaved changes and revert to the original file. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    TopAppBar(
        title = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search...") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(type.name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        },
        navigationIcon = {
            if (!isSearchActive) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            }
        },
        actions = {
            if (!isSearchActive) {
                IconButton(onClick = { setAllExpanded(rootNode, false) }) {
                    Icon(Icons.Default.UnfoldLess, "Collapse All")
                }
                IconButton(onClick = { setAllExpanded(rootNode, true) }) {
                    Icon(Icons.Default.UnfoldMore, "Expand All")
                }
                IconButton(onClick = { cmdManager.undo() }, enabled = cmdManager.canUndo) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
                }
                IconButton(onClick = { cmdManager.redo() }, enabled = cmdManager.canRedo) {
                    Icon(Icons.AutoMirrored.Filled.Redo, "Redo")
                }
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(Icons.Default.Refresh, "Reset")
                }
            }

            IconButton(onClick = {
                isSearchActive = !isSearchActive
                if (!isSearchActive) onSearchChange("")
            }) {
                Icon(if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search, "Search")
            }

            if (!isSearchActive) {
                IconButton(onClick = {
                    val content = onGenerateContent()
                    if (content != null) {
                        scope.launch {
                            clipboard.setPlainText(content)
                        }
                    }
                }) {
                    Icon(Icons.Default.ContentCopy, "Copy All")
                }

                Button(onClick = onSave, modifier = Modifier.padding(start = 8.dp)) {
                    Text("Save")
                }
            }
        }
    )
}