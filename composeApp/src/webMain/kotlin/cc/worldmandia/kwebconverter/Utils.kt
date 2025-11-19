package cc.worldmandia.kwebconverter

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension

fun PlatformFile.parserType(): ParserType = when (this.extension) {
    "json" -> ParserType.JSON5
    "yaml", "yml" -> ParserType.YAML
    else -> ParserType.UNSUPPORTED
}