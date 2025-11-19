package cc.worldmandia.kwebconverter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.worldmandia.kwebconverter.ParserType
import cc.worldmandia.kwebconverter.logic.CommandManager
import cc.worldmandia.kwebconverter.logic.setAllExpanded
import cc.worldmandia.kwebconverter.model.EditableNode
import cc.worldmandia.kwebconverter.setPlainText
import kotlinx.coroutines.launch

// Используем тот же шрифт, что и в редакторе
val BarFont = FontFamily.Monospace

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

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Discard changes?", fontFamily = BarFont) },
            text = {
                Text(
                    "This will revert the file to its original state. All unsaved edits will be lost.",
                    fontFamily = BarFont
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReset()
                        showResetDialog = false
                    }
                ) {
                    Text(
                        "Reset",
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = BarFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", fontFamily = BarFont)
                }
            }
        )
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        title = {
            if (isSearchActive) {
                // Minimalist Search Field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = BarFont,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search node...",
                                        color = MaterialTheme.colorScheme.outline,
                                        fontFamily = BarFont
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            } else {
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = BarFont,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        type.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = BarFont,
                        color = MaterialTheme.colorScheme.primary
                    )
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
            // Search Toggle
            IconButton(onClick = {
                isSearchActive = !isSearchActive
                if (!isSearchActive) onSearchChange("")
            }) {
                Icon(if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search, "Search")
            }

            if (!isSearchActive) {
                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))

                // Tree Controls
                IconButton(onClick = { setAllExpanded(rootNode, false) }) {
                    Icon(Icons.Default.UnfoldLess, "Collapse All")
                }
                IconButton(onClick = { setAllExpanded(rootNode, true) }) {
                    Icon(Icons.Default.UnfoldMore, "Expand All")
                }

                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))

                // History Controls
                IconButton(onClick = { cmdManager.undo() }, enabled = cmdManager.canUndo) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        "Undo",
                        tint = if (cmdManager.canUndo) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(0.3f)
                    )
                }
                IconButton(onClick = { cmdManager.redo() }, enabled = cmdManager.canRedo) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        "Redo",
                        tint = if (cmdManager.canRedo) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(0.3f)
                    )
                }

                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))

                // File Actions
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(Icons.Default.Refresh, "Reset File")
                }

                IconButton(onClick = {
                    val content = onGenerateContent()
                    if (content != null) {
                        scope.launch { clipboard.setPlainText(content) }
                    }
                }) {
                    Icon(Icons.Default.ContentCopy, "Copy All")
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text("Save", fontFamily = BarFont)
                }
            }
        }
    )
}