package cc.worldmandia.kwebconverter.domain.repository

import cc.worldmandia.kwebconverter.domain.model.ProjectFile
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

class FileRepositoryImpl : IFileRepository {
    private val settings = Settings()

    override suspend fun saveDraft(fileId: String, content: String) {
        settings[fileId] = content
    }
    override suspend fun getDraft(fileId: String): String? {
        return settings.get<String>(fileId)
    }
    override suspend fun clearDraft(fileId: String) {
        settings.remove(fileId)
    }
}

expect suspend fun IFileRepository.saveAsFile(file: ProjectFile)