package cc.worldmandia.kwebconverter.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import cc.worldmandia.kwebconverter.FileParser
import cc.worldmandia.kwebconverter.NodeSerializer
import cc.worldmandia.kwebconverter.ParseResult
import cc.worldmandia.kwebconverter.logic.CommandManager
import cc.worldmandia.kwebconverter.model.EditableMap
import cc.worldmandia.kwebconverter.model.EditableNode
import cc.worldmandia.kwebconverter.model.FileItemModel
import cc.worldmandia.kwebconverter.ui.*
import cc.worldmandia.kwebconverter.viewmodel.FilesViewModel
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileEditorScreen(
    file: FileItemModel,
    filesViewModel: FilesViewModel,
    back: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val cmdManager = remember { CommandManager() }

    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var focusedNode by remember { mutableStateOf<EditableNode?>(null) }

    LaunchedEffect(searchQuery) {
        delay(300)
        debouncedQuery = searchQuery
    }

    val parsedRoot = when (val parsed = FileParser.parseFile(file)) {
        is ParseResult.Error -> {
            // TODO
            return
        }

        is ParseResult.Success -> {
            parsed.root
        }
    }

    val editableRoot = remember(file) { parsedRoot }

    val uiItems by remember(editableRoot.rootNode, cmdManager.canUndo, cmdManager.canRedo) {
        derivedStateOf {
            flattenTree(editableRoot.rootNode, cmd = cmdManager)
        }
    }

    val displayItems by remember(uiItems, debouncedQuery) {
        derivedStateOf {
            if (debouncedQuery.isEmpty()) {
                uiItems
            } else {
                uiItems.filter { item ->
                    when (item) {
                        is UiNode -> item.matches(debouncedQuery)
                        is UiAddAction -> false
                    }
                }
            }
        }
    }

    val generateContent: () -> String? = remember(editableRoot, file.parserType) {
        {
            NodeSerializer.serialize(editableRoot.rootNode, file.parserType)
        }
    }

    val currentContent = generateContent()

    LaunchedEffect(currentContent) {
        if (currentContent != null) {
            delay(1000)

            FilesViewModel.saveDraft(file.originalFile.name, currentContent)
        }
    }

    val handleReset = {
        when (val freshRoot = FileParser.parseFile(file.also { f -> f.resetCache() })) {
            is ParseResult.Error -> TODO()
            is ParseResult.Success -> {
                editableRoot.rootNode = freshRoot.root.rootNode
                cmdManager.clear()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.isCtrlPressed) {
                if (event.key == Key.Z) {
                    if (cmdManager.canUndo) cmdManager.undo()
                    return@onPreviewKeyEvent true
                } else if (event.key == Key.Y ||
                    (event.isShiftPressed && event.key == Key.Z)
                ) {
                    if (cmdManager.canRedo) cmdManager.redo()
                    return@onPreviewKeyEvent true
                } else if (event.key == Key.S) {
                    scope.launch {
                        file.saveToUser()
                    }
                    return@onPreviewKeyEvent true
                }
            }
            false
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EditorTopBar(
                title = file.originalFile.name,
                type = file.parserType,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                cmdManager = cmdManager,
                onBack = back,
                onSave = {
                    scope.launch {
                        file.saveToUser()
                    }
                },
                onGenerateContent = generateContent,
                rootNode = editableRoot.rootNode,
                onReset = handleReset
            )
        },
        bottomBar = {
            BottomAppBar(modifier = Modifier.height(48.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                Breadcrumbs(focusedNode) { targetNode ->
                    focusedNode = targetNode
                    targetNode.requestFocus = true
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(items = displayItems, key = { it.id }) { item ->
                when (item) {
                    is UiNode -> NodeRow(
                        item = item,
                        isDuplicate = (item.node.parent as? EditableMap)?.duplicateKeys?.contains(
                            (item.keyInfo as? MapKey)?.state?.text?.toString()
                        ) == true,
                        cmdManager = cmdManager,
                        onFocus = {
                            focusedNode = it
                        }
                    )

                    is UiAddAction -> AddActionRow(item, file.parserType)
                }
            }
        }

    }
}