package cc.worldmandia.kwebconverter.domain.repository

import cc.worldmandia.kwebconverter.domain.model.ProjectFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

actual suspend fun IFileRepository.saveAsFile(file: ProjectFile) {
    val newFile = FileKit.openFileSaver(
        suggestedName = file.name,
        extension = file.extension
    )

    newFile?.write(file.content.encodeToByteArray())
}