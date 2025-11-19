package cc.worldmandia.kwebconverter

import androidx.compose.ui.graphics.Color
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension

fun PlatformFile.parserType(): ParserType = when (this.extension) {
    "json" -> ParserType.JSON5
    "yaml", "yml" -> ParserType.YAML
    else -> ParserType.UNSUPPORTED
}

val ColorString = Color(0xFF66BB6A) // Green
val ColorNumber = Color(0xFF42A5F5) // Blue
val ColorBoolean = Color(0xFFFFA726) // Orange
val ColorNull = Color(0xFFEF5350)   // Red