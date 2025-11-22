package cc.worldmandia.kwebconverter.domain.repository

import cc.worldmandia.kwebconverter.domain.model.ProjectFile
import io.github.vinceglb.filekit.FileKit

actual suspend fun IFileRepository.saveAsFile(file: ProjectFile) {
    val file = FileKit.openFileSaver(
        suggestedName = file.name,
        extension = file.extension
    )

    file.write(file.content.encodeToByteArray())
}