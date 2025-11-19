package cc.worldmandia.kwebconverter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.worldmandia.kwebconverter.model.FileItemModel
import cc.worldmandia.kwebconverter.parserType
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import web.storage.localStorage

class FilesViewModel : ViewModel() {

    private val _files = MutableStateFlow<List<FileItemModel>>(emptyList())
    val files = _files.asStateFlow()

    fun loadFile(file: PlatformFile) {
        _files.update { currentList ->
            currentList + FileItemModel(file, parserType = file.parserType())
        }
    }

    fun loadFile(files: List<PlatformFile>) {
        _files.update { currentList ->
            currentList + files.map { FileItemModel(it, parserType = it.parserType()) }
        }
    }

    fun removeString(index: Int) {
        _files.update { currentList ->
            if (index in currentList.indices) {
                currentList.toMutableList().apply { removeAt(index) }
            } else {
                currentList
            }
        }
    }

    fun loadFilesContent() {
        viewModelScope.launch {
            val currentFiles = _files.value

            val updatedFiles = currentFiles.map { file ->
                if (file.cachedOriginalContent == null) {
                    try {
                        val content = file.originalFile.readString()
                        file.copy(cachedOriginalContent = content)
                    } catch (e: Exception) {
                        println("Error reading file ${file.originalFile.name}: $e")
                        file
                    }
                } else {
                    file
                }
            }

            _files.update { _ -> updatedFiles }
        }
    }

    companion object {
        fun saveDraft(fileName: String, content: String) {
            localStorage.setItem("draft_$fileName", content)
        }

        fun checkDraft(fileName: String): String? {
            return localStorage.getItem("draft_$fileName")
        }

        fun clearDraft(fileName: String) {
            localStorage.removeItem("draft_$fileName")
        }
    }

    fun restoreDraftsIfAny() {
        _files.update { currentList ->
            currentList.map { fileModel ->
                val draft = checkDraft(fileModel.originalFile.name)
                if (draft != null) {
                    fileModel.copy(cachedEditedContent = draft)
                } else {
                    fileModel
                }
            }
        }
    }
}