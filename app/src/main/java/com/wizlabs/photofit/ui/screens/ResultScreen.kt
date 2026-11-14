package com.wizlabs.photofit.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.data.Exporter
import com.wizlabs.photofit.data.Work
import com.wizlabs.photofit.engine.Render
import com.wizlabs.photofit.engine.TargetEncoder
import com.wizlabs.photofit.engine.Units
import com.wizlabs.photofit.ui.components.GhostButton
import com.wizlabs.photofit.ui.components.PrimaryButton
import com.wizlabs.photofit.ui.components.SpecText
import com.wizlabs.photofit.ui.components.TopBar
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.components.WizCard
import com.wizlabs.photofit.ui.motion.enterRise
import com.wizlabs.photofit.ui.motion.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ResultScreen(onNextPart: (() -> Unit)?, onPrint: (() -> Unit)? = null, onBack: () -> Unit) {
    val part = Work.part; var out by remember { mutableStateOf(Work.output) }
    val img = Work.rendered
    if (part == null || out == null || img == null) { StubScreen("Nothing here", onBack); return }
    val o = out!!
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val name = remember(o) { Exporter.fileName(Work.preset?.name ?: "photo", part, o.format) }
    var saved by remember { mutableStateOf(Work.savedUri != null) }
    var working by remember { mutableStateOf(false) }
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(o.format.mime)) { uri ->
        if (uri != null && Exporter.writeTo(ctx, uri, o.bytes)) { Work.savedUri = uri; saved = true; haptics.confirm() }
    }
    // Auto-save on Android 10+ (no permission needed)
    LaunchedEffect(o) {
        if (o.ok && Exporter.canAutoSave() && Work.savedUri == null) {
            val u = withContext(Dispatchers.IO) { Exporter.save(ctx, o.bytes, name, o.format) }
            if (u != null) { Work.savedUri = u; saved = true }
        }
    }
    fun reencode(maxB: Int?, minB: Int?) {
        working = true
        scope.launch {
            val r = withContext(Dispatchers.Default) { Render.encode(img, part, maxB, minB) }
            Work.output = r; Work.savedUri = null; saved = false; out = r; working = false
        }
    }
    val preview = remember(img) { img.asImageBitmap() }

    Column(Modifier.fillMaxSize().background(Wiz.Bg).navigationBarsPadding()) {
        TopBar(if (o.ok) "Ready" else "Almost", Work.preset?.name, onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(6.dp))
            // show the small file big enough to judge (pixels are scaled up for display only)
            Image(preview, null, contentScale = ContentScale.Fit, filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium,
                modifier = Modifier.fillMaxWidth(if (img.width < img.height) 0.5f else 0.85f)
                    .aspectRatio(img.width.toFloat() / img.height).enterRise(0).clip(RoundedCornerShape(10.dp)).border(1.dp, Wiz.Line, RoundedCornerShape(10.dp)))
            Spacer(Modifier.height(16.dp))
            WizCard(Modifier.fillMaxWidth().enterRise(1)) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    KbGauge(o.bytes.size, part.kbMin, part.kbMax, o.ok)
                    Text("${TargetEncoder.kb(o.bytes.size)} KB", color = if (o.ok) Wiz.Text else Wiz.Bad, style = MaterialTheme.typography.displayLarge)
                    Text("Needed: ${SpecText.kb(part)}", color = Wiz.Muted, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (o.ok) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (o.ok) Wiz.Good else Wiz.Bad)
                        Spacer(Modifier.width(8.dp))
                        Text(if (o.ok) "Fits the form" else (o.message ?: "Does not fit"), color = if (o.ok) Wiz.Good else Wiz.Bad, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("${o.widthPx} × ${o.heightPx} px  ·  ${Units.fmt(Units.pxToCm(o.widthPx, o.dpi))} × ${Units.fmt(Units.pxToCm(o.heightPx, o.dpi))} cm @ ${o.dpi} dpi  ·  ${o.format.ext.uppercase()}",
                        color = Wiz.Muted, style = MaterialTheme.typography.bodySmall)
                    if (o.padded) Text("Padded to reach the minimum size (image unchanged).", color = Wiz.Muted, style = MaterialTheme.typography.bodySmall)
                    if (saved) Text(if (Exporter.canAutoSave()) "Saved to ${if (o.format.ext == "pdf") "Download" else "Pictures"}/${Exporter.FOLDER}" else "Saved",
                        color = Wiz.Good, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Smaller", { reencode((o.bytes.size * 0.85).toInt().let { m -> part.kbMin?.let { maxOf(it * 1024 + 512, m) } ?: m }, null) }, Modifier.weight(1f), icon = Icons.Default.KeyboardArrowDown)
                GhostButton("Bigger", { reencode(part.kbMax?.let { it * 1000 }, (o.bytes.size * 1.15).toInt().let { m -> part.kbMax?.let { minOf(m, it * 1000 - 256) } ?: m }) }, Modifier.weight(1f), icon = Icons.Default.KeyboardArrowUp)
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("Share", { haptics.tap(); ctx.startActivity(Exporter.shareIntent(ctx, o.bytes, name, o.format)) }, Modifier.fillMaxWidth(), icon = Icons.Default.Share, enabled = !working)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!Exporter.canAutoSave() || !saved) GhostButton("Save", {
                    if (Exporter.canAutoSave()) scope.launch {
                        val u = withContext(Dispatchers.IO) { Exporter.save(ctx, o.bytes, name, o.format) }
                        if (u != null) { Work.savedUri = u; saved = true; haptics.confirm() }
                    } else createDoc.launch(name)
                }, Modifier.weight(1f))
                if (onPrint != null) GhostButton("Print sheet", onPrint, Modifier.weight(1f))
                if (onNextPart != null) GhostButton("Next: ${Work.preset?.parts?.getOrNull(Work.partIndex + 1)?.label ?: ""}", onNextPart, Modifier.weight(1f))
            }
        }
    }
}

/** Half-dial: green band = allowed KB range, needle = this file. Needle springs into place on landing. */
@Composable
private fun KbGauge(bytes: Int, kbMin: Int?, kbMax: Int?, ok: Boolean) {
    val maxKb = (kbMax ?: (bytes / 1024 * 2).coerceAtLeast(20)).toFloat()
    val span = maxKb * 1.3f
    val v = (bytes / 1024f / span).coerceIn(0f, 1f)
    val a = remember { Animatable(0f) }
    LaunchedEffect(bytes) { a.animateTo(v, spring(dampingRatio = 0.6f, stiffness = 120f)) }
    Canvas(Modifier.size(width = 200.dp, height = 104.dp)) {
        val r = size.width * 0.42f; val c = Offset(size.width / 2, size.height * 0.95f)
        val tl = Offset(c.x - r, c.y - r); val sz = Size(r * 2, r * 2); val st = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
        drawArc(Wiz.PanelHi, 180f, 180f, false, tl, sz, style = st)
        val lo = (kbMin ?: 0) / span; val hi = maxKb / span
        drawArc(Wiz.Good.copy(alpha = 0.8f), 180f + 180f * lo, 180f * (hi - lo), false, tl, sz, style = st)
        val ang = (180f + 180f * a.value) * (PI.toFloat() / 180f)
        drawLine(if (ok) Wiz.Text else Wiz.Bad, c, Offset(c.x + cos(ang) * r * 0.82f, c.y + sin(ang) * r * 0.82f), 4.dp.toPx(), StrokeCap.Round)
        drawCircle(Wiz.Accent, 7.dp.toPx(), c)
    }
}
