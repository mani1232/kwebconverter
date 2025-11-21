package cc.worldmandia.kwebconverter.domain.repository

import cc.worldmandia.kwebconverter.domain.model.ProjectFile

// TODO
actual class FileRepositoryImpl : IFileRepository {
    actual override suspend fun saveFile(file: ProjectFile) {
    }

    actual override suspend fun saveDraft(fileId: String, content: String) {
    }

    actual override suspend fun getDraft(fileId: String): String? {
        return null
    }

    actual override suspend fun clearDraft(fileId: String) {
    }
}