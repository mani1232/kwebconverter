package cc.worldmandia.kwebconverter.model

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun generateId(): String = Uuid.generateV7().toHexDashString()

enum class ScalarType { String, Number, Boolean }
enum class NodeType { String, Number, Boolean, List, Map, Null }

sealed interface ParentContainer {
    fun replaceChild(oldNode: EditableNode, newNode: EditableNode)
    fun isRoot(): Boolean = false
}

sealed interface EditableNode {
    val id: String
    val parent: ParentContainer?
    var requestFocus: Boolean
    fun clone(newParent: ParentContainer?): EditableNode
}

class EditableScalar(
    initialContent: String,
    initialType: ScalarType,
    override val parent: ParentContainer?,
    override val id: String = generateId()
) : EditableNode {
    val state = TextFieldState(initialContent)
    var explicitType by mutableStateOf(initialType)
    var isMultiLine by mutableStateOf(initialContent.contains("\n"))
    override var requestFocus by mutableStateOf(false)

    override fun clone(newParent: ParentContainer?) =
        EditableScalar(state.text.toString(), explicitType, newParent)
}

class EditableNull(
    override val parent: ParentContainer?,
    override val id: String = generateId()
) : EditableNode {
    override var requestFocus by mutableStateOf(false)
    override fun clone(newParent: ParentContainer?) = EditableNull(newParent)
}

class EditableList(
    initialItems: List<EditableNode>,
    override val parent: ParentContainer? = null,
    override val id: String = generateId()
) : EditableNode, ParentContainer {
    val items = mutableStateListOf<EditableNode>().apply { addAll(initialItems) }
    var isExpanded by mutableStateOf(true)
    override var requestFocus by mutableStateOf(false)

    override fun replaceChild(oldNode: EditableNode, newNode: EditableNode) {
        val index = items.indexOf(oldNode)
        if (index != -1) items[index] = newNode
    }

    override fun clone(newParent: ParentContainer?): EditableNode {
        val newList = EditableList(emptyList(), newParent)
        newList.items.addAll(items.map { it.clone(newList) })
        return newList
    }
}

class EditableMapEntry(
    initialKey: String,
    initialValue: EditableNode,
    val parentMap: EditableMap,
    val id: String = generateId()
) : ParentContainer {
    val keyState = TextFieldState(initialKey)
    var value: EditableNode by mutableStateOf(initialValue)
    var requestKeyFocus by mutableStateOf(false)

    val key: String get() = keyState.text.toString()

    override fun replaceChild(oldNode: EditableNode, newNode: EditableNode) {
        if (value === oldNode) value = newNode
    }

    fun clone(newParentMap: EditableMap): EditableMapEntry {
        val newEntry = EditableMapEntry(keyState.text.toString(), EditableNull(null), newParentMap)
        newEntry.value = value.clone(newEntry)
        return newEntry
    }
}

class EditableMap(
    initialEntries: List<EditableMapEntry>,
    override val parent: ParentContainer? = null,
    override val id: String = generateId()
) : EditableNode, ParentContainer {
    val entries = mutableStateListOf<EditableMapEntry>().apply { addAll(initialEntries) }
    var isExpanded by mutableStateOf(true)
    override var requestFocus by mutableStateOf(false)

    val duplicateKeys by derivedStateOf {
        entries.groupingBy { it.keyState.text.toString() }
            .eachCount()
            .filter { it.value > 1 }
            .keys
    }

    override fun replaceChild(oldNode: EditableNode, newNode: EditableNode) {}

    override fun clone(newParent: ParentContainer?): EditableNode {
        val newMap = EditableMap(emptyList(), newParent)
        newMap.entries.addAll(entries.map { it.clone(newMap) })
        return newMap
    }
}

class EditableRoot(initialNode: EditableNode) : ParentContainer {
    var rootNode: EditableNode by mutableStateOf(initialNode)
    override fun replaceChild(oldNode: EditableNode, newNode: EditableNode) {
        if (rootNode == oldNode) rootNode = newNode
    }

    override fun isRoot() = true
}