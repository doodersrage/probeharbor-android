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

/** probeharbor.dev wordmark: gray "Thermal" + orange "Trace" */
val BrandThermal = Color(0xFFC5CBD3)
val BrandTrace = Color(0xFFE85500)
val BrandAccent = Color(0xFFFF9E4A)
val BrandNavy = Color(0xFF090B0F)
val BrandText = Color(0xFFF8FAFC)
val BrandMuted = Color(0xFF94A3B8)
val BrandSuccess = Color(0xFF22C55E)
val BrandDanger = Color(0xFFF87171)
val BrandSurface = Color(0xFF111827)

private val DarkColors = darkColorScheme(
    primary = BrandTrace,
    onPrimary = BrandText,
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
    primary = BrandTrace,
    onPrimary = BrandText,
    secondary = Color(0xFFE85500),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    error = Color(0xFFDC2626),
    tertiary = BrandSuccess,
)

fun brandTitle(): androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = BrandThermal)) { append("Thermal") }
    withStyle(SpanStyle(color = BrandTrace)) { append("Trace") }
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
