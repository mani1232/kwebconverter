package cc.worldmandia.kwebconverter.domain.repository

import cc.worldmandia.kwebconverter.domain.model.ProjectFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.download

actual suspend fun IFileRepository.saveAsFile(file: ProjectFile) {
    FileKit.download(
        bytes = file.content.encodeToByteArray(),
        fileName = "${file.name}.${file.extension}"
    )
}