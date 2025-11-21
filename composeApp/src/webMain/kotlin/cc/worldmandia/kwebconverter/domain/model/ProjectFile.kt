package cc.worldmandia.kwebconverter.domain.model

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
enum class FileFormat {
    JSON, JSON5, YAML, UNSUPPORTED
}

@Serializable
data class ProjectFile @OptIn(ExperimentalUuidApi::class) constructor(
    val id: String = Uuid.random().toHexString(),
    val name: String,
    val extension: String,
    val content: String,
    val nameWithExtension: String = "$name.$extension",
    val format: FileFormat = FileFormat.UNSUPPORTED
)