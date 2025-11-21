package cc.worldmandia.kwebconverter

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

actual suspend fun Clipboard.setPlainText(content: String) {
    setClipEntry(ClipEntry(ClipData.newPlainText("copy_content",content)))
}