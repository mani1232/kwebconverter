package cc.worldmandia.kwebutils.presentation.feature.editor.mapper

import cc.worldmandia.kwebutils.presentation.model.*
import com.charleskorn.kaml.*
import kotlinx.serialization.json.*

fun Any.toEditableNode(parent: ParentContainer?): EditableNode {
    return when (this) {
        is YamlNode -> this.toEditableNodeYaml(parent)
        is JsonElement -> this.toEditableNodeJson(parent)
        else -> EditableNull(parent)
    }
}

private fun YamlNode.toEditableNodeYaml(parent: ParentContainer?): EditableNode {
    return when (this) {
        is YamlScalar -> {
            val type = when {
                content.equals("true", true) || content.equals("false", true) -> ScalarType.Boolean
                content.toDoubleOrNull() != null -> ScalarType.Number
                else -> ScalarType.String
            }
            EditableScalar(content, type, parent)
        }

        is YamlList -> {
            val list = EditableList(emptyList(), parent)
            list.items.addAll(this.items.map { it.toEditableNodeYaml(list) })
            list
        }

        is YamlMap -> {
            val map = EditableMap(emptyList(), parent)
            map.entries.addAll(this.entries.map { (k, v) ->
                val entry = EditableMapEntry(k.content, EditableNull(null), map)
                entry.value = v.toEditableNodeYaml(entry)
                entry
            })
            map
        }

        is YamlNull -> EditableNull(parent)
        is YamlTaggedNode -> innerNode.toEditableNodeYaml(parent)
    }
}

private fun JsonElement.toEditableNodeJson(parent: ParentContainer?): EditableNode {
    return when (this) {
        is JsonPrimitive -> {
            if (this is JsonNull) return EditableNull(parent)
            val contentStr = contentOrNull ?: toString()
            val type = when {
                isString -> ScalarType.String
                booleanOrNull != null -> ScalarType.Boolean
                doubleOrNull != null || longOrNull != null -> ScalarType.Number
                else -> ScalarType.String
            }
            EditableScalar(contentStr, type, parent)
        }

        is JsonArray -> {
            val list = EditableList(emptyList(), parent)
            list.items.addAll(this.map { it.toEditableNodeJson(list) })
            list
        }

        is JsonObject -> {
            val map = EditableMap(emptyList(), parent)
            map.entries.addAll(this.entries.map { (k, v) ->
                val entry = EditableMapEntry(k, EditableNull(null), map)
                entry.value = v.toEditableNodeJson(entry)
                entry
            })
            map
        }
    }
}