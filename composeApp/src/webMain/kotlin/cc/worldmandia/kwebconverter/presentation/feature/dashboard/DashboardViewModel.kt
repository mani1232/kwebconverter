package cc.worldmandia.kwebconverter.presentation.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.worldmandia.kwebconverter.domain.model.FileFormat
import cc.worldmandia.kwebconverter.domain.model.ProjectFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.nameWithoutExtension
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

class DashboardViewModel : ViewModel() {

    private val _files = MutableStateFlow<List<ProjectFile>>(emptyList())
    val files = _files.asStateFlow()

    @OptIn(ExperimentalUuidApi::class)
    fun onFilesSelected(platformFiles: List<PlatformFile>) {
        viewModelScope.launch {
            val newFiles = platformFiles.mapNotNull { file ->
                try {
                    val content = file.readString()
                    val ext = file.extension.lowercase()
                    val format = when (ext) {
                        "json" -> FileFormat.JSON5
                        "yaml", "yml" -> FileFormat.YAML
                        else -> FileFormat.UNSUPPORTED
                    }

                    ProjectFile(
                        name = file.nameWithoutExtension,
                        extension = ext,
                        content = content,
                        format = format
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            _files.update { it + newFiles }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun onFilesSelected(projectFile: ProjectFile) {
        viewModelScope.launch {
            try {
                val ext = projectFile.extension.lowercase()
                val format = when (ext) {
                    "json" -> FileFormat.JSON5
                    "yaml", "yml" -> FileFormat.YAML
                    else -> FileFormat.UNSUPPORTED
                }

                _files.update { it + projectFile.copy(format = format) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeFile(id: String) {
        _files.update { list -> list.filter { it.id != id } }
    }
}