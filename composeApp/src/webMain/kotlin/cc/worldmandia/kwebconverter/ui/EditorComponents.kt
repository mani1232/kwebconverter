package cc.worldmandia.kwebconverter.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.byValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cc.worldmandia.kwebconverter.*
import cc.worldmandia.kwebconverter.logic.CommandManager
import cc.worldmandia.kwebconverter.logic.MoveItemCommand
import cc.worldmandia.kwebconverter.logic.ReplaceNodeCommand
import cc.worldmandia.kwebconverter.model.*
import kotlinx.coroutines.launch

@Composable
fun NodeRow(
    item: UiNode,
    isDuplicate: Boolean,
    cmdManager: CommandManager,
    onFocus: (EditableNode) -> Unit
) {
    val background = if (isDuplicate) MaterialTheme.colorScheme.errorContainer.copy(0.3f) else Color.Transparent

    Row(
        Modifier
            .fillMaxWidth()
            .background(background)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IndentationLines(item.level)

        ExpandControl(item.node)

        KeyField(
            keyInfo = item.keyInfo,
            isError = isDuplicate,
            onFocus = { onFocus(item.node) }
        )

        Box(Modifier.weight(1f)) {
            when (val n = item.node) {
                is EditableScalar -> ScalarEditor(n, onFocus)
                is EditableList -> ContainerBadge("List", n.items.size)
                is EditableMap -> ContainerBadge("Map", n.entries.size)
                is EditableNull -> Text("null", color = MaterialTheme.colorScheme.error)
            }
        }

        NodeActionsMenu(item, cmdManager)
    }
}

@Composable
fun Breadcrumbs(
    node: EditableNode?,
    onNodeClick: (EditableNode) -> Unit
) {
    val path = remember(node) {
        val list = mutableListOf<Pair<String, EditableNode>>()
        var current = node
        while (current != null) {
            val parent = current.parent
            val name = when (parent) {
                is EditableMapEntry -> parent.keyState.text.toString().ifEmpty { "key" }
                is EditableList -> {
                    val index = parent.items.indexOf(current)
                    "[$index]"
                }

                else -> "root"
            }
            list.add(0, name to current)

            current = if (parent is EditableMapEntry) parent.parentMap else parent as? EditableNode
        }
        if (list.isEmpty()) listOf("root" to (node ?: return@remember emptyList())) else list
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        path.forEachIndexed { index, (name, pathNode) ->
            if (index > 0) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
            }
            TextButton(
                onClick = { onNodeClick(pathNode) },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(name, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ExpandControl(node: EditableNode) {
    val isContainer = node is EditableList || node is EditableMap
    if (isContainer) {
        val expanded = (node as? EditableList)?.isExpanded ?: (node as? EditableMap)?.isExpanded ?: false
        val rotation by animateFloatAsState(if (expanded) 0f else -90f)

        IconButton(onClick = {
            if (node is EditableList) node.isExpanded = !node.isExpanded
            if (node is EditableMap) node.isExpanded = !node.isExpanded
        }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Rounded.KeyboardArrowDown, null, modifier = Modifier.rotate(rotation))
        }
    } else {
        Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun KeyField(
    keyInfo: NodeKeyInfo?,
    isError: Boolean,
    onFocus: () -> Unit
) {
    when (keyInfo) {
        is MapKey -> {
            OutlinedTextField(
                state = keyInfo.state,
                modifier = Modifier
                    .width(140.dp)
                    .padding(horizontal = 4.dp)
                    .onFocusChanged { if (it.isFocused) onFocus() },
                isError = isError,
                lineLimits = TextFieldLineLimits.SingleLine,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }

        is ListIndex -> {
            Text(
                text = "${keyInfo.index}.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        null -> Unit
    }
}

@Composable
fun AddActionRow(item: UiAddAction, rootType: ParserType) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = (item.level * 16).dp)
            .clickable { expanded = true }
            .padding(8.dp)
    ) {
        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
        Text("Add Item...", Modifier.padding(start = 8.dp))

        DropdownMenu(expanded, { expanded = false }) {
            NodeType.entries.forEach { type ->
                val isAllowed = if (rootType == ParserType.UNSUPPORTED) { // TODO for PROPERTIES
                    type == NodeType.String || type == NodeType.Number || type == NodeType.Boolean
                } else true

                if (isAllowed) {
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = { item.onAdd(type); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
fun ScalarEditor(node: EditableScalar, onFocus: (EditableNode) -> Unit) {
    val typeColor = when (node.explicitType) {
        ScalarType.String -> ColorString
        ScalarType.Number -> ColorNumber
        ScalarType.Boolean -> ColorBoolean
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            when (node.explicitType) {
                ScalarType.String -> SimpleTextField(node, typeColor, onFocus)
                ScalarType.Number -> NumberTextField(node, typeColor, onFocus)
                ScalarType.Boolean -> BooleanSelector(node)
            }
        }
        TypeSelector(node, typeColor)
    }
}

@Composable
fun SimpleTextField(node: EditableScalar, color: Color, onFocus: (EditableNode) -> Unit) {
    OutlinedTextField(
        state = node.state,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocus(node) }, // <-- Убедитесь, что это здесь
        textStyle = MaterialTheme.typography.bodyMedium,
        lineLimits = if (node.isMultiLine) TextFieldLineLimits.MultiLine(3, 10) else TextFieldLineLimits.SingleLine,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = color,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(0.5f)
        )
    )
}

@Composable
fun NumberTextField(node: EditableScalar, color: Color, onFocus: (EditableNode) -> Unit) {
    val numberFilter = remember {
        InputTransformation.byValue { old, new ->
            if (new.toString().matches(Regex("^-?\\d*\\.?\\d*$")) || new.isEmpty()) new else old
        }
    }
    OutlinedTextField(
        state = node.state,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocus(node) },
        inputTransformation = numberFilter,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        lineLimits = TextFieldLineLimits.SingleLine,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = color)
    )
}

@Composable
fun BooleanSelector(node: EditableScalar) {
    val isTrue = node.state.text.toString().toBooleanStrictOrNull() ?: false
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = isTrue,
            onClick = { node.state.edit { replace(0, length, "true") } },
            label = { Text("True") }
        )
        Spacer(Modifier.width(8.dp))
        FilterChip(
            selected = !isTrue,
            onClick = { node.state.edit { replace(0, length, "false") } },
            label = { Text("False") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.errorContainer)
        )
    }
}

@Composable
fun TypeSelector(node: EditableScalar, color: Color) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .size(24.dp)
            .background(color, shape = MaterialTheme.shapes.extraSmall)
            .clickable { expanded = true },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (node.explicitType) {
                ScalarType.String -> "S"
                ScalarType.Number -> "N"
                ScalarType.Boolean -> "B"
            },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary // Белый текст на цветном фоне
        )

        DropdownMenu(expanded, { expanded = false }) {
            ScalarType.entries.filter { it != node.explicitType }.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        val text = node.state.text.toString()
                        if (type == ScalarType.Boolean && text != "true") {
                            node.state.edit { replace(0, length, "false") }
                        } else if (type == ScalarType.Number && text.toDoubleOrNull() == null) {
                            node.state.edit { replace(0, length, "0") }
                        }
                        node.explicitType = type
                        expanded = false
                    }
                )
            }
            if (node.explicitType == ScalarType.String) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(if (node.isMultiLine) "Single Line" else "Multi Line") },
                    onClick = { node.isMultiLine = !node.isMultiLine; expanded = false }
                )
            }
        }
    }
}

@Composable
fun ContainerBadge(label: String, size: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(Modifier.width(4.dp))
        Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) { Text("$size") }
    }
}

@Composable
fun IndentationLines(level: Int) {
    Row(Modifier.width((level * 16).dp)) {
        repeat(level) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.Gray.copy(alpha = 0.3f))
                )
            }
        }
    }
}

@Composable
fun NodeActionsMenu(
    item: UiNode,
    cmdManager: CommandManager
) {
    var expanded by remember { mutableStateOf(false) }
    val parent = item.node.parent

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, "Actions", tint = Color.Gray)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (parent is EditableList) {
                val index = parent.items.indexOf(item.node)
                DropdownMenuItem(
                    text = { Text("Move Up") },
                    enabled = index > 0,
                    leadingIcon = { Icon(Icons.Default.ArrowUpward, null) },
                    onClick = {
                        // Вот здесь мы используем cmdManager!
                        cmdManager.execute(MoveItemCommand(parent, item.node, true))
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Move Down") },
                    enabled = index < parent.items.lastIndex,
                    leadingIcon = { Icon(Icons.Default.ArrowDownward, null) },
                    onClick = {
                        // И здесь тоже
                        cmdManager.execute(MoveItemCommand(parent, item.node, false))
                        expanded = false
                    }
                )
                HorizontalDivider()
            }

            if (item.node !is EditableList) {
                DropdownMenuItem(
                    text = { Text("Wrap in List") },
                    onClick = {
                        // И здесь
                        cmdManager.execute(ReplaceNodeCommand(item.node) {
                            val list = EditableList(emptyList(), parent)
                            list.items.add(item.node.clone(list))
                            list
                        })
                        expanded = false
                    }
                )
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("Copy Subtree") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                onClick = {
                    val text = NodeSerializer.serializeForClipboard(item.node)

                    scope.launch {
                        clipboard.setPlainText(text)
                    }
                    expanded = false
                }
            )

            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    item.onDelete()
                    expanded = false
                }
            )
        }
    }
}