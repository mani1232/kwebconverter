package cc.worldmandia.kwebconverter.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import cc.worldmandia.kwebconverter.FileParser
import cc.worldmandia.kwebconverter.NodeSerializer
import cc.worldmandia.kwebconverter.ParseResult
import cc.worldmandia.kwebconverter.logic.CommandManager
import cc.worldmandia.kwebconverter.logic.ReorderItemCommand
import cc.worldmandia.kwebconverter.logic.ReorderMapEntryCommand
import cc.worldmandia.kwebconverter.model.*
import cc.worldmandia.kwebconverter.ui.*
import cc.worldmandia.kwebconverter.viewmodel.FilesViewModel
import com.mohamedrejeb.compose.dnd.annotation.ExperimentalDndApi
import com.mohamedrejeb.compose.dnd.drag.DropStrategy
import com.mohamedrejeb.compose.dnd.reorder.ReorderContainer
import com.mohamedrejeb.compose.dnd.reorder.ReorderableItem
import com.mohamedrejeb.compose.dnd.reorder.rememberReorderState
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalDndApi::class)
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
            // TODO: показать ошибку или вернуть
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

    val reorderState = rememberReorderState<UiNode>()

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
        ReorderContainer(
            state = reorderState,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(items = displayItems, key = { it.id }) { item ->
                    when (item) {
                        is UiNode -> {
                            ReorderableItem(
                                state = reorderState,
                                key = item.id,
                                data = item,
                                dropStrategy = DropStrategy.CenterDistance, // Стратегия: вставляем, когда курсор ближе к центру элемента
                                onDrop = {}, // Оставляем пустым, все делается в onDragEnter
                                onDragEnter = { dropState ->
                                    val fromItem = dropState.data
                                    val toItem = item

                                    // ПРОВЕРКА: Разрешаем перестановку только внутри одного родителя (List или Map)
                                    val fromNode = fromItem.node
                                    val toNode = toItem.node
                                    val parent = toNode.parent

                                    // 1. Если родитель - Список
                                    if (parent is EditableList && fromNode.parent === parent) {
                                        val fromIndex = parent.items.indexOf(fromNode)
                                        val toIndex = parent.items.indexOf(toNode)
                                        if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                                            cmdManager.execute(ReorderItemCommand(parent, fromIndex, toIndex))
                                        }
                                    }
                                    // 2. Если родитель - Карта (Map)
                                    if (parent is EditableMapEntry && fromNode.parent is EditableMapEntry) {
                                        val targetMap = parent.parentMap
                                        val draggedMap = (fromNode.parent as EditableMapEntry).parentMap
                                        if (targetMap === draggedMap) {
                                            val fromIndex = targetMap.entries.indexOf(fromNode.parent)
                                            val toIndex = targetMap.entries.indexOf(parent)
                                            if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                                                cmdManager.execute(ReorderMapEntryCommand(targetMap, fromIndex, toIndex))
                                            }
                                        }
                                    }
                                },

                                draggableContent = {
                                    val isContainer = item.node is EditableList || item.node is EditableMap

                                    Surface(
                                        shadowElevation = 12.dp,
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest, // Контрастный фон
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight(unbounded = true, align = Alignment.Top)
                                    ) {
                                        Column(Modifier.padding(8.dp)) {
                                            // Сама строка заголовка
                                            NodeRow(
                                                item = item,
                                                isDuplicate = false,
                                                cmdManager = cmdManager,
                                                isDragging = true, // Чтобы подсветить иконку
                                                onFocus = {}
                                            )

                                            // Визуализация скрытого контента
                                            if (isContainer) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 4.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant
                                                )
                                                Text(
                                                    text = "... nested content ...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(start = 32.dp, bottom = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            ) {
                                NodeRow(
                                    item = item,
                                    isDuplicate = (item.node.parent as? EditableMap)?.duplicateKeys?.contains(
                                        (item.keyInfo as? MapKey)?.state?.text?.toString()
                                    ) == true,
                                    cmdManager = cmdManager,
                                    isDragging = isDragging,
                                    onFocus = { focusedNode = it }
                                )
                            }
                        }

                        is UiAddAction -> {
                            AddActionRow(item, file.parserType)
                        }
                    }
                }
            }
        }
    }
}