package com.wizlabs.photofit.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wizlabs.photofit.R

/**
 * "Studio Marigold" colour tokens, light-first. Each getter reads [dark] (Compose state), so every screen
 * that reads `Wiz.X` in composition or draw re-themes when the system theme flips.
 * Accent = marigold fill (buttons, chips). AccentInk = marigold-family colour that is legible as text/icons.
 */
object Wiz {
    var dark by mutableStateOf(false)
    private fun p(l: Long, d: Long) = Color(if (dark) d else l)
    val Bg get() = p(0xFFFAFAF7, 0xFF0F0F12)
    val Bar get() = p(0xFFFFFFFF, 0xFF18181D)
    val Panel get() = p(0xFFFFFFFF, 0xFF18181D)
    val PanelHi get() = p(0xFFF1F0EA, 0xFF222229)
    val Line get() = p(0xFFE6E4DC, 0xFF2D2D36)
    val Accent = Color(0xFFFFC400)
    val AccentDeep = Color(0xFFE0A800)
    val OnAccent = Color(0xFF1A1400)
    val AccentInk get() = p(0xFF8F6A00, 0xFFFFD54D)
    val Guide get() = p(0xFF0EA5E9, 0xFF38BDF8)
    val Text get() = p(0xFF15151A, 0xFFF3F3F6)
    val Muted get() = p(0xFF62626E, 0xFF9A9AA8)
    val Good get() = p(0xFF16A34A, 0xFF34D399)
    val Warn get() = p(0xFFC2410C, 0xFFFB923C)
    val Bad get() = p(0xFFDC2626, 0xFFF87171)
    /** Editor canvas: dark in both themes so photos read true. */
    val Canvas = Color(0xFF2A2A30)
    val CanvasCheck = Color(0xFF33333A)
}

/** Numbers always use tabular figures so KB / px values don't jitter while they roll. */
const val Tabular = "tnum"

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun jakarta(w: Int) = Font(R.font.plus_jakarta_sans, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
val Jakarta = FontFamily(jakarta(500), jakarta(600), jakarta(700), jakarta(800))
val DmMono = FontFamily(Font(R.font.dm_mono_medium, FontWeight.Medium))

/** Monospace style for px / cm / KB / DPI values. */
val MonoStyle = TextStyle(fontFamily = DmMono, fontSize = 14.sp, lineHeight = 18.sp, fontFeatureSettings = Tabular)

private val type = Typography(
    displayLarge = TextStyle(fontFamily = Jakarta, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = Tabular),
    headlineMedium = TextStyle(fontFamily = Jakarta, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold),
    titleLarge = TextStyle(fontFamily = Jakarta, fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = Jakarta, fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = Jakarta, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontFamily = Jakarta, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
)

@Composable
fun WizTheme(content: @Composable () -> Unit) {
    val d = isSystemInDarkTheme()
    if (Wiz.dark != d) Wiz.dark = d
    val scheme = if (d) darkColorScheme(
        primary = Wiz.Accent, onPrimary = Wiz.OnAccent, background = Wiz.Bg, onBackground = Wiz.Text,
        surface = Wiz.Panel, onSurface = Wiz.Text, surfaceVariant = Wiz.PanelHi, onSurfaceVariant = Wiz.Muted, outline = Wiz.Line, error = Wiz.Bad,
    ) else lightColorScheme(
        primary = Wiz.Accent, onPrimary = Wiz.OnAccent, background = Wiz.Bg, onBackground = Wiz.Text,
        surface = Wiz.Panel, onSurface = Wiz.Text, surfaceVariant = Wiz.PanelHi, onSurfaceVariant = Wiz.Muted, outline = Wiz.Line, error = Wiz.Bad,
    )
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
