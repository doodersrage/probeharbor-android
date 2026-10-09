package dev.thermaltrace.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/** probeharbor.dev wordmark: sky "Probe" + signal-teal "Harbor" (harbor palette). */
val BrandThermal = Color(0xFFE6F4FB)
val BrandTrace = Color(0xFF22B8D4)
val BrandAccent = Color(0xFF67D3E6)
val BrandNavy = Color(0xFF07111A)
val BrandText = Color(0xFFF3F8FB)
val BrandMuted = Color(0xFF8AA0AF)
/** Dark ink on teal: 7.1:1. White on teal is under 3:1. */
val BrandInk = Color(0xFF03202E)
val BrandSuccess = Color(0xFF22C55E)
val BrandDanger = Color(0xFFF87171)
val BrandSurface = Color(0xFF0F1F2C)

private val DarkColors = darkColorScheme(
    primary = BrandTrace,
    onPrimary = BrandInk,
    secondary = BrandAccent,
    onSecondary = BrandNavy,
    background = BrandNavy,
    onBackground = BrandText,
    surface = BrandSurface,
    onSurface = BrandText,
    onSurfaceVariant = BrandMuted,
    error = BrandDanger,
    onError = BrandText,
    tertiary = BrandSuccess,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF08708A),
    onPrimary = Color.White,
    secondary = Color(0xFF0A7C93),
    background = Color(0xFFF3F8FB),
    onBackground = Color(0xFF0B2433),
    surface = Color.White,
    onSurface = Color(0xFF0B2433),
    onSurfaceVariant = Color(0xFF4A6070),
    error = Color(0xFFDC2626),
    tertiary = BrandSuccess,
)

fun brandTitle(): androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = BrandThermal)) { append("Probe") }
    withStyle(SpanStyle(color = BrandTrace)) { append("Harbor") }
}

@Composable
fun ThermalTraceTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme || isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
