package cc.worldmandia.kwebconverter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.byValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.worldmandia.kwebconverter.NodeSerializer
import cc.worldmandia.kwebconverter.ParserType
import cc.worldmandia.kwebconverter.logic.CommandManager
import cc.worldmandia.kwebconverter.logic.MoveItemCommand
import cc.worldmandia.kwebconverter.logic.ReplaceNodeCommand
import cc.worldmandia.kwebconverter.model.*
import cc.worldmandia.kwebconverter.setPlainText
import kotlinx.coroutines.launch

// --- Colors for IDE look ---
val KeyColor = Color(0xFFCFD8DC) // Light Grey for Keys
val StringColor = Color(0xFFA5D6A7) // Soft Green
val NumberColor = Color(0xFF90CAF9) // Soft Blue
val BooleanColor = Color(0xFFFFCC80) // Soft Orange
val NullColor = Color(0xFFEF9A9A)   // Soft Red
val CommentColor = Color.Gray

val CodeFont = FontFamily.Monospace

@Composable
fun NodeRow(
    item: UiNode,
    isDuplicate: Boolean,
    cmdManager: CommandManager,
    onFocus: (EditableNode) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isDuplicate -> MaterialTheme.colorScheme.errorContainer.copy(0.3f)
        isHovered -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .hoverable(interactionSource)
            .padding(vertical = 1.dp, horizontal = 4.dp), // Compact padding
        verticalAlignment = Alignment.Top // Align top for multiline strings
    ) {
        // Indentation
        IndentationGuides(item.level)

        // Expand icon / Spacer
        ExpandControl(item.node)

        // Key (Map Key or List Index)
        KeyField(
            keyInfo = item.keyInfo,
            isError = isDuplicate,
            onFocus = { onFocus(item.node) }
        )

        // Separator for Map
        if (item.keyInfo is MapKey) {
            Text(":", color = CommentColor, fontFamily = CodeFont, modifier = Modifier.padding(end = 8.dp))
        }

        // Value Area
        Box(Modifier.weight(1f).align(Alignment.CenterVertically)) {
            when (val n = item.node) {
                is EditableScalar -> ScalarEditor(n, onFocus)
                is EditableList -> ContainerBadge("List", n.items.size, "[", "]")
                is EditableMap -> ContainerBadge("Map", n.entries.size, "{", "}")
                is EditableNull -> Text("null", color = NullColor, fontFamily = CodeFont, fontSize = 14.sp)
            }
        }

        // Actions (Show only on hover or if menu is open)
        // Note: For touch devices, you might want to always show this or use long press
        Box(Modifier.align(Alignment.CenterVertically)) {
            NodeActionsMenu(item, cmdManager, visible = isHovered)
        }
    }
}

@Composable
fun Breadcrumbs(
    node: EditableNode?,
    onNodeClick: (EditableNode) -> Unit
) {
    // Улучшенные хлебные крошки
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
        if (list.isEmpty()) emptyList() else list
    }

    if (path.isEmpty()) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        path.forEachIndexed { index, (name, pathNode) ->
            if (index > 0) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.Gray
                )
            }

            val isLast = index == path.lastIndex
            Text(
                text = name,
                color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = TextStyle(fontFamily = CodeFont, fontSize = 12.sp),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onNodeClick(pathNode) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun ExpandControl(node: EditableNode) {
    val isContainer = node is EditableList || node is EditableMap
    if (isContainer) {
        val expanded = (node as? EditableList)?.isExpanded ?: (node as? EditableMap)?.isExpanded ?: false
        val rotation by animateFloatAsState(if (expanded) 0f else -90f)

        Icon(
            Icons.Rounded.KeyboardArrowDown,
            null,
            modifier = Modifier
                .size(20.dp) // Smaller icon
                .rotate(rotation)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable {
                    if (node is EditableList) node.isExpanded = !node.isExpanded
                    if (node is EditableMap) node.isExpanded = !node.isExpanded
                },
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Spacer(Modifier.size(20.dp))
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
            // Minimalist TextField
            BasicTextField(
                state = keyInfo.state,
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .defaultMinSize(minWidth = 40.dp)
                    .onFocusChanged { if (it.isFocused) onFocus() },
                textStyle = TextStyle(
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    fontFamily = CodeFont,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorator = { innerTextField ->
                    Box(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        innerTextField()
                    }
                }
            )
        }

        is ListIndex -> {
            Text(
                text = "${keyInfo.index}",
                style = TextStyle(fontFamily = CodeFont, fontSize = 14.sp),
                color = CommentColor,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        null -> Unit
    }
}

@Composable
fun AddActionRow(item: UiAddAction, rootType: ParserType) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    var expanded by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable { expanded = true }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IndentationGuides(item.level)
        Spacer(Modifier.width(24.dp)) // Offset for expand icon

        // Dotted line or subtle look
        Box(
            Modifier
                .weight(1f)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AddCircleOutline,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Add Item",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }

        DropdownMenu(expanded, { expanded = false }) {
            NodeType.entries.forEach { type ->
                // Filter logic...
                val isAllowed = if (rootType == ParserType.UNSUPPORTED) {
                    type == NodeType.String || type == NodeType.Number || type == NodeType.Boolean
                } else true

                if (isAllowed) {
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = { item.onAdd(type); expanded = false },
                        leadingIcon = { TypeIcon(type) }
                    )
                }
            }
        }
    }
}

@Composable
fun TypeIcon(type: NodeType) {
    val (label, color) = when (type) {
        NodeType.String -> "S" to StringColor
        NodeType.Number -> "N" to NumberColor
        NodeType.Boolean -> "B" to BooleanColor
        NodeType.List -> "[]" to Color.Gray
        NodeType.Map -> "{}" to Color.Gray
        NodeType.Null -> "Ø" to NullColor
    }

    Box(
        modifier = Modifier
            .size(20.dp)
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}


@Composable
fun ScalarEditor(node: EditableScalar, onFocus: (EditableNode) -> Unit) {
    val typeColor = when (node.explicitType) {
        ScalarType.String -> StringColor
        ScalarType.Number -> NumberColor
        ScalarType.Boolean -> BooleanColor
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            when (node.explicitType) {
                ScalarType.String -> CodeTextField(node, typeColor, onFocus)
                ScalarType.Number -> NumberTextField(node, typeColor, onFocus)
                ScalarType.Boolean -> BooleanSelector(node)
            }
        }
        Spacer(Modifier.width(8.dp))
        TypeSelector(node, typeColor)
    }
}

@Composable
fun CodeTextField(node: EditableScalar, color: Color, onFocus: (EditableNode) -> Unit) {
    BasicTextField(
        state = node.state,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocus(node) },
        textStyle = TextStyle(
            color = color,
            fontFamily = CodeFont,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        lineLimits = if (node.isMultiLine) TextFieldLineLimits.MultiLine(1, 10) else TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(color),
        decorator = { innerTextField ->
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                if (node.state.text.isEmpty()) {
                    Text(
                        "empty",
                        color = Color.Gray.copy(0.5f),
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun NumberTextField(node: EditableScalar, color: Color, onFocus: (EditableNode) -> Unit) {
    val numberFilter = remember {
        InputTransformation.byValue { old, new ->
            if (new.toString().matches(Regex("^-?\\d*\\.?\\d*$")) || new.isEmpty()) new else old
        }
    }
    BasicTextField(
        state = node.state,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocus(node) },
        inputTransformation = numberFilter,
        textStyle = TextStyle(color = color, fontFamily = CodeFont, fontSize = 14.sp),
        cursorBrush = SolidColor(color),
        decorator = { innerTextField ->
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                innerTextField()
            }
        }
    )
}

@Composable
fun BooleanSelector(node: EditableScalar) {
    val isTrue = node.state.text.toString().toBooleanStrictOrNull() ?: false
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(BooleanColor.copy(alpha = 0.1f))
            .clickable {
                val newValue = (!isTrue).toString()
                node.state.edit { replace(0, length, newValue) }
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isTrue) "true" else "false",
            color = BooleanColor,
            fontFamily = CodeFont,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TypeSelector(node: EditableScalar, color: Color) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.2f))
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (node.explicitType) {
                    ScalarType.String -> "S"
                    ScalarType.Number -> "N"
                    ScalarType.Boolean -> "B"
                },
                style = TextStyle(fontFamily = CodeFont, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                color = color
            )
        }

        DropdownMenu(expanded, { expanded = false }) {
            ScalarType.entries.filter { it != node.explicitType }.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        // Logic remains the same
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
                    text = { Text(if (node.isMultiLine) "Switch to Single Line" else "Switch to Multi Line") },
                    onClick = { node.isMultiLine = !node.isMultiLine; expanded = false }
                )
            }
        }
    }
}

@Composable
fun ContainerBadge(type: String, size: Int, openChar: String, closeChar: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$type $openChar", style = TextStyle(fontFamily = CodeFont, color = Color.Gray))

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp)
        ) {
            Text("$size", style = MaterialTheme.typography.labelSmall)
        }

        Text(closeChar, style = TextStyle(fontFamily = CodeFont, color = Color.Gray))
    }
}

@Composable
fun IndentationGuides(level: Int) {
    // Используем более чистую линию, а не много Box-ов
    if (level > 0) {
        Row(Modifier.width((level * 20).dp)) { // Reduced width multiplier
            repeat(level) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    VerticalDivider(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun NodeActionsMenu(
    item: UiNode,
    cmdManager: CommandManager,
    visible: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val parent = item.node.parent
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Box {
        // Показываем иконку только при наведении или если меню открыто
        AnimatedVisibility(visible || expanded) {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.MoreVert, "Actions", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }

        if (!visible && !expanded) {
            Spacer(Modifier.size(24.dp))
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // ... (Logic logic remains exactly the same as in your original code) ...
            if (parent is EditableList) {
                val index = parent.items.indexOf(item.node)
                DropdownMenuItem(
                    text = { Text("Move Up") },
                    enabled = index > 0,
                    leadingIcon = { Icon(Icons.Default.ArrowUpward, null) },
                    onClick = {
                        cmdManager.execute(MoveItemCommand(parent, item.node, true))
                        expanded = false
                    }
                )
                // ... Move Down logic ...
                DropdownMenuItem(
                    text = { Text("Move Down") },
                    enabled = index < parent.items.lastIndex,
                    leadingIcon = { Icon(Icons.Default.ArrowDownward, null) },
                    onClick = {
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
                text = { Text("Copy JSON/YAML") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                onClick = {
                    val text = NodeSerializer.serializeForClipboard(item.node)
                    scope.launch { clipboard.setPlainText(text) }
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