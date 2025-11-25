package cc.worldmandia.kwebconverter.presentation.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@OptIn(markerClass = [ExperimentalMaterial3ExpressiveApi::class])
@Composable
actual fun AppTheme(
    useDarkTheme: Boolean,
    content: @Composable (() -> Unit)
) {
    val colors = if (useDarkTheme) darkColorScheme() else lightColorScheme()

    MaterialExpressiveTheme(
        colorScheme = colors,
        content = content
    )
}