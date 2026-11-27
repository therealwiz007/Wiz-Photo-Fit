package com.wizlabs.photofit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.engine.ModelStatus
import com.wizlabs.photofit.ui.motion.rememberMotionAllowed
import com.wizlabs.photofit.ui.motion.rememberThrottledClock

/** A step of a long job, shown with a pop-in check when done. */
data class Step(val label: String, val detail: String? = null, val failed: Boolean = false)

/**
 * Full-screen "working" sheet: the photo with a scan beam sweeping over it, a list of live steps (current one
 * pulses, finished ones pop a check), and a big live number line. Transform/draw-phase only, 30 fps clock.
 */
@Composable
fun ProcessingOverlay(visible: Boolean, title: String, steps: List<Step>, current: Int, image: ImageBitmap?, aspect: Float, live: String? = null) {
    AnimatedVisibility(visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(220))) {
        val motion = rememberMotionAllowed()
        val clock = rememberThrottledClock(running = motion && visible, fps = 30, )
        Box(Modifier.fillMaxSize().background(Wiz.Bg.copy(alpha = 0.94f))
            .clickable(remember { MutableInteractionSource() }, null) {}, contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth(if (aspect < 1f) 0.5f else 0.75f).aspectRatio(aspect).clip(RoundedCornerShape(14.dp))
                    .background(Wiz.Panel).border(1.dp, Wiz.AccentInk.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .drawWithCache {
                        val beamH = size.height * 0.22f
                        val beam = Brush.verticalGradient(listOf(Color.Transparent, Wiz.AccentInk.copy(alpha = 0.45f), Color.Transparent), 0f, beamH)
                        val line = Wiz.AccentInk
                        onDrawWithContent {
                            drawContent()
                            // beam sweeps down and back every 1.6 s
                            val p = (clock.value % 1.6f) / 1.6f
                            val y = (if (p < 0.5f) p * 2 else 2 - p * 2) * (size.height + beamH) - beamH
                            translateBeam(y, beamH, beam)
                            drawLine(line, Offset(0f, y + beamH / 2), Offset(size.width, y + beamH / 2), 2.dp.toPx(), StrokeCap.Round)
                        }
                    }) {
                    image?.let { Image(it, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                }
                Spacer(Modifier.height(22.dp))
                Text(title, color = Wiz.Text, style = MaterialTheme.typography.headlineMedium)
                live?.let { Text(it, color = Wiz.AccentInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    steps.forEachIndexed { i, s -> StepRow(s, done = i < current, active = i == current, clockValue = { clock.value }) }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.translateBeam(y: Float, h: Float, b: Brush) {
    drawContext.canvas.save(); drawContext.transform.translate(0f, y)
    drawRect(b, size = Size(size.width, h))
    drawContext.transform.translate(0f, -y); drawContext.canvas.restore()
}

@Composable
private fun StepRow(s: Step, done: Boolean, active: Boolean, clockValue: () -> Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            // ring (pulses while active), check pops in when done
            Box(Modifier.size(22.dp).graphicsLayer {
                if (active) { val k = 1f + 0.12f * kotlin.math.sin(clockValue() * 6f); scaleX = k; scaleY = k }
            }.clip(CircleShape).border(2.dp, if (done) Wiz.Good else if (active) Wiz.AccentInk else Wiz.Line, CircleShape))
            androidx.compose.animation.AnimatedVisibility(done, enter = scaleIn(spring(0.45f, 600f)) + fadeIn()) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(if (s.failed) Wiz.Warn else Wiz.Good), contentAlignment = Alignment.Center) {
                    if (s.failed) Text("!", color = Wiz.Bg, fontWeight = FontWeight.Black)
                    else androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Check, null, tint = Wiz.Bg, modifier = Modifier.size(15.dp))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(s.label, color = if (done || active) Wiz.Text else Wiz.Muted, style = MaterialTheme.typography.bodyLarge, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
            if ((active || s.failed) && s.detail != null) Text(s.detail, color = Wiz.Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Shimmering placeholder (one shared gradient, translated in the draw phase). */
fun Modifier.shimmer(): Modifier = composed {
    val motion = rememberMotionAllowed()
    val clock = rememberThrottledClock(running = motion, fps = 24)
    drawWithCache {
        val band = size.width * 0.6f
        val brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent), 0f, band)
        onDrawWithContent {
            drawContent()
            val x = ((clock.value % 1.4f) / 1.4f) * (size.width + band) - band
            drawContext.canvas.save(); drawContext.transform.translate(x, 0f)
            drawRect(brush, size = Size(band, size.height))
            drawContext.transform.translate(-x, 0f); drawContext.canvas.restore()
        }
    }
}

/** Card that explains and shows the one-time model download by Google Play services. */
@Composable
fun ModelReadyCard(state: ModelStatus.State, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val (title, sub, col, prog) = when (state) {
        ModelStatus.State.Ready -> listOf("Face + background tools ready", "Ready to use on your phone", Wiz.Good, 1f)
        ModelStatus.State.Unknown, ModelStatus.State.Checking -> listOf("Checking face + background tools…", "One moment", Wiz.AccentInk, null)
        is ModelStatus.State.Downloading -> listOf("Getting face + background tools",
            if (state.total > 0) "Google Play services · ${state.bytes / 1024 / 1024}.${(state.bytes / 1024 % 1024) / 103} of ${state.total / 1024 / 1024} MB · one time only"
            else "Google Play services is downloading them · one time only", Wiz.AccentInk, state.progress)
        is ModelStatus.State.Installing -> listOf("Almost ready", state.note, Wiz.AccentInk, 0.98f)
        is ModelStatus.State.Failed -> listOf("Face + background tools not ready", state.reason, Wiz.Warn, null)
    }
    val target = (prog as Float?) ?: 0f
    val p by animateFloatAsState(target, spring(0.9f, 200f), label = "model")
    WizCard(modifier.fillMaxWidth(), corner = 16.dp) {
        Box {
            if (state !is ModelStatus.State.Ready && state !is ModelStatus.State.Failed && prog == null) Box(Modifier.matchParentSize().shimmer())
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val mood = when (state) {
                        is ModelStatus.State.Downloading -> com.wizlabs.photofit.engine.SnipBrain.react(com.wizlabs.photofit.engine.SnipEvent.ModelDownloading(((state.progress ?: 0f) * 100).toInt()))
                        is ModelStatus.State.Failed -> com.wizlabs.photofit.engine.SnipBrain.react(com.wizlabs.photofit.engine.SnipEvent.ModelUnavailable)
                        else -> null
                    }
                    if (mood != null) com.wizlabs.photofit.ui.mascot.Snip(mood.expression, mood.action, 48.dp)
                    else Box(Modifier.size(10.dp).clip(CircleShape).background(col as Color))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title as String, color = Wiz.Text, style = MaterialTheme.typography.titleMedium)
                        Text(sub as String, color = Wiz.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    if (state is ModelStatus.State.Failed) Text("Retry", color = Wiz.AccentInk, style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable(onClick = onRetry).padding(8.dp))
                    if (prog != null && state !is ModelStatus.State.Ready) Text("${(p * 100).toInt()}%", color = Wiz.AccentInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (state !is ModelStatus.State.Ready && state !is ModelStatus.State.Failed) {
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Wiz.PanelHi)) {
                        Box(Modifier.matchParentSize().graphicsLayer { scaleX = if (prog == null) 0.25f + 0.15f * kotlin.math.sin(p * 0f) else p; transformOrigin = TransformOrigin(0f, 0.5f) }
                            .background(Wiz.Accent))
                    }
                }
            }
        }
    }
}

/** Namespaced access for screens that also import other shimmer helpers. */
object Loading { fun Modifier.shim(): Modifier = this.shimmer() }
