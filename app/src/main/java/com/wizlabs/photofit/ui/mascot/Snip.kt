package com.wizlabs.photofit.ui.mascot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.engine.SnipAction
import com.wizlabs.photofit.engine.SnipExpression
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.motion.rememberHaptics
import com.wizlabs.photofit.ui.motion.rememberMotionAllowed
import kotlinx.coroutines.delay

/** Snip's clock: ~[fps] frames a second for as long as [running] (visible, focused, no Battery Saver, animations on). Never settles on its own. */
@Composable
private fun rememberAliveClock(running: Boolean, fps: Int): State<Float> {
    val t = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running, fps) {
        if (!running) return@LaunchedEffect
        val pause = (1000L / fps - 8L).coerceAtLeast(8L)
        var last = -1L
        while (true) {
            withFrameNanos { now ->
                if (last >= 0) t.floatValue += ((now - last) / 1e9f).coerceAtMost(0.1f)
                last = now
            }
            delay(pause)
        }
    }
    return t
}

/**
 * Snip, code-drawn and always alive while visible (idle loop at 15 fps: blink, breathe, glances, an idle beat every 8-15 s).
 * Decorative: no content description; any line it says is real Text next to it. The clock is read only inside the draw
 * lambda. Reduce-motion, Battery Saver or an unfocused window freeze it on a still frame of [expression].
 * [oneShot] plays for ≤600 ms whenever [shotKey] changes, then Snip settles into [expression]. A tap plays one of four
 * delight reactions with a haptic tick.
 */
@Composable
fun Snip(
    expression: SnipExpression,
    action: SnipAction = SnipAction.NONE,
    size: Dp = 96.dp,
    modifier: Modifier = Modifier,
    oneShot: SnipExpression? = null,
    shotMs: Int = 450,
    shotKey: Int = 0,
    lookAtSearch: Boolean = false,
    tappable: Boolean = true,
) {
    val art = remember { SnipArt() }
    val pose = remember { SnipPose() }
    val driver = remember { SnipDriver() }
    val haptics = rememberHaptics()
    val allowed = rememberMotionAllowed()
    val clock = rememberAliveClock(running = allowed, fps = 15)
    LaunchedEffect(shotKey) { if (shotKey != 0) driver.requestShot(oneShot, shotMs) }
    var m = modifier.size(size).semantics { contentDescription = "" }
    if (tappable) m = m.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { driver.tap(); haptics.tick() }
    Canvas(m) {
        if (allowed) driver.frame(pose, clock.value, expression, action, lookAtSearch) else driver.still(pose, expression)
        art.draw(this, pose, Wiz.dark)
    }
}
