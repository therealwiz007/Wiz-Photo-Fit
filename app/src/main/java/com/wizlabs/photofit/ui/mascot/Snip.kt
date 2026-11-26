package com.wizlabs.photofit.ui.mascot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.engine.SnipAction
import com.wizlabs.photofit.engine.SnipExpression
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.motion.rememberMotionAllowed
import com.wizlabs.photofit.ui.motion.rememberThrottledClock

/**
 * Snip, code-drawn. Decorative (no content description); any line it says must also be real Text next to it.
 * The shared clock is read only inside the draw lambda; with reduce-motion, Battery Saver, 9 s idle or an
 * unfocused window the clock stops and Snip shows a still frame.
 */
@Composable
fun Snip(
    expression: SnipExpression,
    action: SnipAction = SnipAction.NONE,
    size: Dp = 96.dp,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    val art = remember { SnipArt() }
    val pose = remember { SnipPose() }
    val allowed = rememberMotionAllowed()
    val clock = rememberThrottledClock(running = allowed && (action != SnipAction.NONE || expression != SnipExpression.NEUTRAL), fps = if (action == SnipAction.TAPE_SWAY) 24 else 30)
    var m = modifier.size(size).semantics { contentDescription = "" }
    if (onTap != null) m = m.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTap() }
    Canvas(m) {
        pose.update(expression, action, clock.value, still = !allowed)
        art.draw(this, pose, Wiz.dark)
    }
}
