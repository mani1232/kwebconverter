package cc.worldmandia.kwebutils.presentation.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.worldmandia.kwebutils.domain.model.ProjectFile
import cc.worldmandia.kwebutils.domain.repository.IFileRepository
import cc.worldmandia.kwebutils.domain.repository.saveAsFile
import cc.worldmandia.kwebutils.domain.usecase.ParseContentUseCase
import cc.worldmandia.kwebutils.presentation.feature.editor.logic.Command
import cc.worldmandia.kwebutils.presentation.feature.editor.logic.CommandManager
import cc.worldmandia.kwebutils.presentation.feature.editor.mapper.NodeSerializer
import cc.worldmandia.kwebutils.presentation.feature.editor.mapper.toEditableNode
import cc.worldmandia.kwebutils.presentation.model.EditableRoot
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

sealed interface EditorUiState {
    data object Loading : EditorUiState
    data class Error(val message: String) : EditorUiState
    data class Content(
        val root: EditableRoot,
        val fileInfo: ProjectFile,
        val isDirty: Boolean = false
    ) : EditorUiState
}

class EditorViewModel(
    private val file: ProjectFile,
    private val parseContentUseCase: ParseContentUseCase,
    private val repository: IFileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditorUiState>(EditorUiState.Loading)
    val uiState = _uiState.asStateFlow()

    val commandManager = CommandManager()

    private val _contentUpdates = MutableSharedFlow<String>()

    init {
        loadFile()
        setupAutoSave()
    }

    private fun loadFile() {
        viewModelScope.launch {
            val draft = repository.getDraft(file.id)
            val contentToParse = draft ?: file.content

            parseContentUseCase(contentToParse, file.format)
                .onSuccess { abstractTree ->
                    val editableNode = abstractTree.toEditableNode(null) // UI Mapper
                    _uiState.value = EditorUiState.Content(
                        root = EditableRoot(editableNode),
                        fileInfo = file,
                        isDirty = draft != null
                    )
                }
                .onFailure {
                    _uiState.value = EditorUiState.Error("Failed to parse: ${it.message}")
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupAutoSave() {
        viewModelScope.launch {
            _contentUpdates
                .debounce(1000)
                .collect { content ->
                    repository.saveDraft(file.id, content)
                }
        }
    }

    fun onContentChanged() {
        val state = _uiState.value as? EditorUiState.Content ?: return
        viewModelScope.launch {
            val content = NodeSerializer.serialize(state.root.rootNode, state.fileInfo.format)
            _contentUpdates.emit(content)
        }
    }

    fun saveFile() {
        val state = _uiState.value as? EditorUiState.Content ?: return
        viewModelScope.launch {
            val content = NodeSerializer.serialize(state.root.rootNode, state.fileInfo.format)
            repository.saveAsFile(state.fileInfo.copy(content = content))
            repository.clearDraft(state.fileInfo.id)
        }
    }

    fun resetFile() {
        viewModelScope.launch {
            repository.clearDraft(file.id)
            loadFile()
            commandManager.clear()
        }
    }

    fun executeCommand(command: Command) {
        commandManager.execute(command)
        onContentChanged()
    }

    fun undo() {
        commandManager.undo()
        onContentChanged()
    }

    fun redo() {
        commandManager.redo()
        onContentChanged()
    }
}