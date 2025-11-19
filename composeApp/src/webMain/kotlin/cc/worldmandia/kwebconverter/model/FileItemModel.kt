package cc.worldmandia.kwebconverter.model

import cc.worldmandia.kwebconverter.ParserType
import cc.worldmandia.kwebconverter.viewmodel.FilesViewModel
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.download
import io.github.vinceglb.filekit.name
import kotlinx.serialization.Serializable

@Serializable
data class FileItemModel(
    val originalFile: PlatformFile,
    val cachedOriginalContent: String? = null,
    var cachedEditedContent: String? = cachedOriginalContent,
    val parserType: ParserType
) {
    fun resetCache() {
        cachedEditedContent = cachedOriginalContent
        FilesViewModel.clearDraft(originalFile.name)
    }

    suspend fun saveToUser() {
        FileKit.download(
            (cachedEditedContent
                ?: cachedOriginalContent ?: "Content is null").encodeToByteArray(),
            originalFile.name
        )
    }

}