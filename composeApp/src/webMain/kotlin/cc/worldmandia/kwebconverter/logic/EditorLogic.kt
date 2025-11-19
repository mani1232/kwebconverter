package cc.worldmandia.kwebconverter.logic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import cc.worldmandia.kwebconverter.model.*

interface Command {
    fun execute()
    fun undo()
}

class CommandManager(private val maxHistory: Int = 25) {
    private val _undoStack = mutableListOf<Command>()
    private val _redoStack = mutableListOf<Command>()

    // Use State to trigger UI updates on button availability
    var canUndo by mutableStateOf(false)
    var canRedo by mutableStateOf(false)

    fun execute(command: Command) {
        command.execute()
        _undoStack.add(command)
        if (_undoStack.size > maxHistory) _undoStack.removeFirst()
        _redoStack.clear()
        updateState()
    }

    fun undo() {
        if (_undoStack.isNotEmpty()) {
            val command = _undoStack.removeLast()
            command.undo()
            _redoStack.add(command)
            updateState()
        }
    }

    fun clear() {
        _undoStack.clear()
        _redoStack.clear()
        updateState()
    }

    fun redo() {
        if (_redoStack.isNotEmpty()) {
            val command = _redoStack.removeLast()
            command.execute()
            _undoStack.add(command)
            updateState()
        }
    }

    private fun updateState() {
        canUndo = _undoStack.isNotEmpty()
        canRedo = _redoStack.isNotEmpty()
    }
}

// --- Specific Commands ---

class AddItemCommand(
    private val parent: EditableNode,
    private val type: NodeType
) : Command {
    private var addedNode: Any? = null // Can be Node or MapEntry

    override fun execute() {
        when (parent) {
            is EditableList -> {
                val newNode = NodeFactory.create(type, parent)
                newNode.requestFocus = true
                parent.items.add(newNode)
                parent.isExpanded = true
                addedNode = newNode
            }

            is EditableMap -> {
                val newKey = UniqueKeyGenerator.generate(parent, "key")
                val entry = EditableMapEntry(newKey, EditableNull(null), parent)
                val value = NodeFactory.create(type, entry)
                value.requestFocus = true
                entry.value = value
                entry.keyState.edit { selection = TextRange(0, length) }
                entry.requestKeyFocus = true
                parent.entries.add(entry)
                parent.isExpanded = true
                addedNode = entry
            }

            else -> {}
        }
    }

    override fun undo() {
        when (parent) {
            is EditableList -> parent.items.remove(addedNode)
            is EditableMap -> parent.entries.remove(addedNode)
            else -> {}
        }
    }
}

class RemoveNodeCommand(private val node: EditableNode) : Command {
    private var index: Int = -1
    private var container: ParentContainer? = null
    private var mapEntry: EditableMapEntry? = null

    override fun execute() {
        when (val p = node.parent) {
            is EditableList -> {
                container = p
                index = p.items.indexOf(node)
                p.items.remove(node)
            }

            is EditableMapEntry -> {
                // Removing a value from a map means removing the entry
                val map = p.parentMap
                container = map
                mapEntry = p
                index = map.entries.indexOf(p)
                map.entries.remove(p)
            }

            else -> {}
        }
    }

    override fun undo() {
        when (container) {
            is EditableList -> (container as EditableList).items.add(index, node)
            is EditableMap -> mapEntry?.let { (container as EditableMap).entries.add(index, it) }
            else -> {}
        }
    }
}

class InsertListCommand(
    private val list: EditableList,
    private val index: Int,
) : Command {
    private var inserted: EditableNode? = null
    override fun execute() {
        val item = EditableScalar("", ScalarType.String, list)
        item.requestFocus = true
        list.items.add(index.coerceIn(0, list.items.size), item)
        inserted = item
    }

    override fun undo() {
        inserted?.let { list.items.remove(it) }
    }
}

class MoveItemCommand(
    private val list: EditableList,
    private val node: EditableNode,
    private val up: Boolean
) : Command {
    private var moved = false
    override fun execute() = move(up)
    override fun undo() = move(!up) // Reverse direction

    private fun move(directionUp: Boolean) {
        val idx = list.items.indexOf(node)
        if (idx == -1) return

        val targetIdx = if (directionUp) idx - 1 else idx + 1
        if (targetIdx in list.items.indices) {
            list.items.removeAt(idx)
            list.items.add(targetIdx, node)
            moved = true
        }
    }
}

class ReplaceNodeCommand(
    private val oldNode: EditableNode,
    private val factory: () -> EditableNode
) : Command {
    private var newNode: EditableNode? = null
    override fun execute() {
        newNode = factory()
        oldNode.parent?.replaceChild(oldNode, newNode!!)
    }

    override fun undo() {
        newNode?.let { oldNode.parent?.replaceChild(it, oldNode) }
    }
}

// --- Utils ---

object NodeFactory {
    fun create(type: NodeType, parent: ParentContainer?): EditableNode {
        return when (type) {
            NodeType.String -> EditableScalar("", ScalarType.String, parent)
            NodeType.Number -> EditableScalar("0", ScalarType.Number, parent)
            NodeType.Boolean -> EditableScalar("true", ScalarType.Boolean, parent)
            NodeType.Null -> EditableNull(parent)
            NodeType.List -> EditableList(emptyList(), parent)
            NodeType.Map -> EditableMap(emptyList(), parent)
        }
    }
}

object UniqueKeyGenerator {
    fun generate(map: EditableMap, base: String): String {
        var key = base
        var counter = 0
        val existing = map.entries.map { it.key }.toSet()
        while (existing.contains(key)) {
            counter++
            key = "${base}_$counter"
        }
        return key
    }
}

fun setAllExpanded(node: EditableNode, expanded: Boolean) {
    when (node) {
        is EditableList -> {
            node.isExpanded = expanded
            node.items.forEach { setAllExpanded(it, expanded) }
        }

        is EditableMap -> {
            node.isExpanded = expanded
            node.entries.forEach { setAllExpanded(it.value, expanded) }
        }

        else -> {}
    }
}