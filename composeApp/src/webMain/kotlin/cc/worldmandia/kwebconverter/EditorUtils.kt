package cc.worldmandia.kwebconverter

import cc.worldmandia.kwebconverter.model.*
import cc.worldmandia.kwebconverter.ui.UiNode
import com.charleskorn.kaml.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import li.songe.json5.Json5

sealed class ParseResult {
    data class Success(val root: EditableRoot) : ParseResult()
    data class Error(val message: String, val throwable: Throwable? = null) : ParseResult()
}

object FileParser {
    fun parseFile(file: FileItemModel): ParseResult {
        val content = file.cachedEditedContent ?: file.cachedOriginalContent
        ?: return ParseResult.Error("File not found or cant read")
        return try {
            val rootNode = when (file.parserType) {
                ParserType.YAML -> YAMLConfigured.parseToYamlNode(content).toEditableNode(null)
                ParserType.JSON5 -> Json5.parseToJson5Element(content).toEditableNode(null)
                else -> return ParseResult.Error("Cant parse file")
            }
            ParseResult.Success(EditableRoot(rootNode))
        } catch (e: Exception) {
            ParseResult.Error("Failed to parse ${file.parserType}: ${e.message}", e)
        }
    }
}

// --- Mappers: Library Node -> Editable Node ---

fun YamlNode.toEditableNode(parent: ParentContainer?): EditableNode {
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
            list.items.addAll(this.items.map { it.toEditableNode(list) })
            list
        }

        is YamlMap -> {
            val map = EditableMap(emptyList(), parent)
            map.entries.addAll(this.entries.map { (k, v) ->
                val entry = EditableMapEntry(k.content, EditableNull(null), map)
                entry.value = v.toEditableNode(entry)
                entry
            })
            map
        }

        is YamlNull -> EditableNull(parent)
        is YamlTaggedNode -> innerNode.toEditableNode(parent)
    }
}

fun JsonElement.toEditableNode(parent: ParentContainer?): EditableNode {
    return when (this) {
        is JsonPrimitive -> {
            if (this is JsonNull) EditableNull(parent)
            else {
                val type = when {
                    isString -> ScalarType.String
                    content == "true" || content == "false" -> ScalarType.Boolean
                    else -> ScalarType.Number
                }
                EditableScalar(if (isString) content else toString(), type, parent)
            }
        }

        is JsonArray -> {
            val list = EditableList(emptyList(), parent)
            list.items.addAll(this.map { it.toEditableNode(list) })
            list
        }

        is JsonObject -> {
            val map = EditableMap(emptyList(), parent)
            map.entries.addAll(this.entries.map { (k, v) ->
                val entry = EditableMapEntry(k, EditableNull(null), map)
                entry.value = v.toEditableNode(entry)
                entry
            })
            map
        }
    }
}

fun getNodePath(item: Any?): String {
    if (item !is UiNode) return "root"
    val path = StringBuilder()
    var current: EditableNode? = item.node

    while (current != null) {
        val parent = current.parent
        when (parent) {
            is EditableMapEntry -> {
                val key = parent.keyState.text.toString()
                path.insert(0, if (path.isNotEmpty()) " > $key" else key)
            }

            is EditableList -> {
                val index = parent.items.indexOf(current)
                path.insert(0, if (path.isNotEmpty()) "[$index]" else "[$index]")
            }

            else -> {}
        }
        current = if (parent is EditableMapEntry) parent.parentMap else parent as? EditableNode
    }
    return if (path.isEmpty()) "root" else "root > $path"
}

object NodeSerializer {
    fun serialize(node: EditableNode, type: ParserType): String {
        return when (type) {
            ParserType.YAML -> {
                val yamlNode = node.toYamlNode()
                YAMLConfigured.encodeToString(yamlNode)
            }

            ParserType.JSON5 -> {
                Json5.encodeToString(node.toJsonElement())
            }

            ParserType.JSON -> {
                val json = Json { prettyPrint = true }
                json.encodeToString(node.toJsonElement())
            }

            else -> {
                "Unknown"
            }
        }
    }

    fun serializeForClipboard(node: EditableNode): String {
        return Json5.encodeToString(node.toJsonElement())
    }
}

fun EditableNode.toYamlNode(): YamlNode {
    return when (this) {
        is EditableScalar -> YamlScalar(state.text.toString(), YamlPath.root)
        is EditableList -> YamlList(items.map { it.toYamlNode() }, YamlPath.root)
        is EditableMap -> YamlMap(entries.associate {
            YamlScalar(it.key, YamlPath.root) to it.value.toYamlNode()
        }, YamlPath.root)

        is EditableNull -> YamlNull(YamlPath.root)
    }
}

fun EditableNode.toJsonElement(): JsonElement {
    return when (this) {
        is EditableScalar -> {
            val text = this.state.text.toString()
            when (this.explicitType) {
                ScalarType.Boolean -> JsonPrimitive(text.toBooleanStrictOrNull() ?: false)
                ScalarType.Number -> {
                    val long = text.toLongOrNull()
                    if (long != null) JsonPrimitive(long)
                    else JsonPrimitive(text.toDoubleOrNull() ?: 0.0)
                }

                ScalarType.String -> JsonPrimitive(text)
            }
        }

        is EditableList -> JsonArray(this.items.map { it.toJsonElement() })
        is EditableMap -> JsonObject(this.entries.associate {
            it.keyState.text.toString() to it.value.toJsonElement()
        })

        is EditableNull -> JsonNull
    }
}