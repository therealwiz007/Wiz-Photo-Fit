package com.wizlabs.photofit.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Wiz colour tokens (shared with the other Wiz apps). */
object Wiz {
    val Bg = Color(0xFF0B0D10)
    val Bar = Color(0xFF07080A)
    val Panel = Color(0xFF14171C)
    val PanelHi = Color(0xFF1F242C)
    val Line = Color(0x1FFFFFFF)
    val Accent = Color(0xFFFFC400)
    val AccentDeep = Color(0xFFE0A800)
    val OnAccent = Color(0xFF0B0D10)
    val Text = Color(0xFFF4F5F7)
    val Muted = Color(0xFF8B93A1)
    val Good = Color(0xFF2BD97C)
    val Warn = Color(0xFFFF9F1C)
    val Bad = Color(0xFFFF5A4E)
}

/** Numbers always use tabular figures so KB / px values don't jitter while they roll. */
const val Tabular = "tnum"

private val type = Typography(
    displayLarge = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, fontFeatureSettings = Tabular, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    bodySmall = TextStyle(fontSize = 12.sp),
)

@Composable
fun WizTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Wiz.Accent, onPrimary = Wiz.OnAccent, background = Wiz.Bg, onBackground = Wiz.Text,
            surface = Wiz.Panel, onSurface = Wiz.Text, surfaceVariant = Wiz.PanelHi, onSurfaceVariant = Wiz.Muted,
            outline = Wiz.Line, error = Wiz.Bad,
        ),
        typography = type,
        content = content,
    )
}
