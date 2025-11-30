package cc.worldmandia.kwebutils.presentation.feature.editor.model

import androidx.compose.foundation.text.input.TextFieldState
import cc.worldmandia.kwebutils.presentation.feature.editor.logic.AddItemCommand
import cc.worldmandia.kwebutils.presentation.feature.editor.logic.CommandManager
import cc.worldmandia.kwebutils.presentation.feature.editor.logic.RemoveNodeCommand
import cc.worldmandia.kwebutils.presentation.model.*

// --- Key Info Models ---
sealed interface NodeKeyInfo
data class MapKey(val state: TextFieldState, val entry: EditableMapEntry) : NodeKeyInfo
data class ListIndex(val index: Int) : NodeKeyInfo

sealed interface NodeUiItem {
    val id: String
    val level: Int
}

data class UiNode(
    override val id: String,
    val node: EditableNode,
    val keyInfo: NodeKeyInfo?,
    override val level: Int,
    val mapId: String? = null,
    val onDelete: () -> Unit
) : NodeUiItem {
    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        if (keyInfo is MapKey && keyInfo.state.text.toString().contains(query, true)) return true
        if (node is EditableScalar && node.state.text.toString().contains(query, true)) return true
        return false
    }
}

data class UiAddAction(
    override val id: String,
    override val level: Int,
    val onAdd: (NodeType) -> Unit
) : NodeUiItem

fun flattenTree(
    node: EditableNode,
    keyInfo: NodeKeyInfo? = null,
    level: Int = 0,
    cmd: CommandManager
): List<NodeUiItem> {
    val result = ArrayList<NodeUiItem>()
    val parent = node.parent
    val parentMapId = (parent as? EditableMapEntry)?.parentMap?.id

    result.add(
        UiNode(
            id = if (parent is EditableMapEntry) parent.id else node.id,
            node = node,
            keyInfo = keyInfo,
            level = level,
            mapId = parentMapId,
            onDelete = { cmd.execute(RemoveNodeCommand(node)) }
        )
    )

    if (node is EditableList && node.isExpanded) {
        node.items.forEachIndexed { index, child ->
            result.addAll(flattenTree(child, ListIndex(index), level + 1, cmd))
        }
        result.add(UiAddAction("${node.id}_add", level + 1) { type ->
            cmd.execute(AddItemCommand(node, type))
        })
    } else if (node is EditableMap && node.isExpanded) {
        node.entries.forEach { entry ->
            result.addAll(flattenTree(entry.value, MapKey(entry.keyState, entry), level + 1, cmd))
        }
        result.add(UiAddAction("${node.id}_add", level + 1) { type ->
            cmd.execute(AddItemCommand(node, type))
        })
    }
    return result
}