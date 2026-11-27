package com.wizlabs.photofit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.ui.motion.pressScale

/** Screen top bar: back arrow (when given), title, optional subtitle and trailing actions. */
@Composable
fun TopBar(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp).heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack) else Spacer(Modifier.width(8.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Wiz.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Wiz.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = Wiz.Text, bg: Color = Wiz.PanelHi, size: Dp = 48.dp) {
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier.size(size).pressScale(src, 0.9f).clip(CircleShape).background(bg)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = tint, modifier = Modifier.size(22.dp)) }
}

/** Card surface used everywhere. */
@Composable
fun WizCard(modifier: Modifier = Modifier, corner: Dp = 20.dp, color: Color = Wiz.Panel, content: @Composable () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(corner)).background(color).border(1.dp, Wiz.Line, RoundedCornerShape(corner))) { content() }
}

/** Big primary button: at least 56 dp tall, icon + word. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val src = remember { MutableInteractionSource() }
    Row(
        modifier.heightIn(min = 56.dp).pressScale(src, 0.96f).clip(RoundedCornerShape(18.dp))
            .background(if (enabled) Wiz.Accent else Wiz.PanelHi)
            .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = if (enabled) Wiz.OnAccent else Wiz.Muted, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(10.dp)) }
        Text(text, color = if (enabled) Wiz.OnAccent else Wiz.Muted, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/** Secondary outlined button. */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val src = remember { MutableInteractionSource() }
    Row(
        modifier.heightIn(min = 52.dp).pressScale(src, 0.96f).clip(RoundedCornerShape(16.dp))
            .border(1.dp, Wiz.AccentInk.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = Wiz.AccentInk, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = Wiz.AccentInk, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

/** Full-width tool tile on Home: icon tile left, title, subtitle, chevron. */
@Composable
fun ToolTile(title: String, subtitle: String, icon: @Composable () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    Row(
        modifier.fillMaxWidth().heightIn(min = 96.dp).pressScale(src, 0.96f).clip(RoundedCornerShape(22.dp))
            .background(Wiz.Panel).border(1.dp, Wiz.Line, RoundedCornerShape(22.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(Wiz.Accent.copy(alpha = 0.28f)), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Wiz.Text)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Wiz.Muted)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Wiz.Muted)
    }
}

/** Small square shortcut (Signature, Join, Saved). */
@Composable
fun MiniTile(label: String, icon: @Composable () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    Column(
        modifier.heightIn(min = 88.dp).pressScale(src, 0.94f).clip(RoundedCornerShape(18.dp)).background(Wiz.Panel)
            .border(1.dp, Wiz.Line, RoundedCornerShape(18.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = Wiz.Text, maxLines = 1)
    }
}

/** Rounded pill label. */
@Composable
fun Pill(text: String, color: Color = Wiz.AccentInk, modifier: Modifier = Modifier, icon: (@Composable () -> Unit)? = null) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { icon(); Spacer(Modifier.width(5.dp)) }
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/** Trust row: photos are processed on the phone, no account. (No ad / offline / permission-count claims: INTERNET is declared.) */
@Composable
fun TrustStrip(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        Pill("On-device", Wiz.Good, icon = { GlyphShield(Wiz.Good) })
        Pill("No login", Wiz.Good)
    }
}

/** Section header text. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier = modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp), color = Wiz.Muted,
        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
}
