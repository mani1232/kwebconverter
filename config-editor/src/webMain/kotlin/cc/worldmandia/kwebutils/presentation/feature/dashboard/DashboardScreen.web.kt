package cc.worldmandia.kwebutils.presentation.feature.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.domDataTransferOrNull
import cc.worldmandia.kwebutils.domain.model.ProjectFile
import js.core.JsPrimitives.toKotlinString
import kotlinx.browser.window
import org.w3c.dom.MediaQueryList
import org.w3c.dom.events.Event
import web.events.EventHandler
import web.file.FileReader

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
actual fun onDropDragAndDropEvent(viewModel: DashboardViewModel): (DragAndDropEvent) -> Boolean = { event ->
    event.transferData?.domDataTransferOrNull?.let { clipData ->
        if (clipData.files.length > 0) {
            for (i in 0..clipData.items.length) {
                clipData.files.item(i)?.let { file ->
                    FileReader().apply {
                        onloadend = EventHandler { event ->
                            file.name.split(".").let {
                                viewModel.onFilesSelected(
                                    ProjectFile(
                                        name = it[0],
                                        extension = it[1],
                                        content = (event.currentTarget.result as JsString?)?.toKotlinString() ?: "[]",
                                    )
                                )
                            }
                        }
                        onerror = EventHandler { event ->
                            println(event.type)
                        }
                    }.readAsText(file.unsafeCast())
                } ?: println("Warning: Failed to load file ${clipData.files.item(i)?.name ?: "$i not found"}")
            }
        }
        true
    } ?: false
}

@OptIn(ExperimentalComposeUiApi::class)
actual fun onDragAndDropEvent(): (DragAndDropEvent) -> Boolean = { event ->
    (event.transferData?.domDataTransferOrNull)?.also {
        it.dropEffect = "copy"
        it.effectAllowed = "all"
    } != null
}

@Composable
actual fun WebBackButton() {
    val isPwa by rememberIsPwaState()

    if (!isPwa) {
        IconButton(onClick = {
            window.history.back()
        }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
        }
    }
}

@Composable
private fun rememberIsPwaState(): State<Boolean> {
    return produceState(initialValue = false) {
        val mediaQueryList: MediaQueryList = window.matchMedia("(display-mode: standalone)")

        value = mediaQueryList.matches

        val callback: (Event) -> Unit = {
            value = mediaQueryList.matches
        }

        mediaQueryList.addEventListener("change", callback)

        awaitDispose {
            mediaQueryList.removeEventListener("change", callback)
        }
    }
}