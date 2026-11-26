package com.wizlabs.photofit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Before / after: [after] fills the box, [before] shows left of a draggable divider.
 * The drag handle is 48 dp wide. A separate toggle button (in the Result screen) is the TalkBack alternative.
 * [split] is 0..1 and is hoisted so the toggle can drive it.
 */
@Composable
fun CompareSlider(before: ImageBitmap, after: ImageBitmap, split: Float, onSplit: (Float) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().aspectRatio(after.width.toFloat() / after.height).clipToBounds()
            .semantics { contentDescription = "Before and after comparison. Use the Original and Final buttons below." }
            .pointerInput(Unit) { detectHorizontalDragGestures { change, _ -> change.consume(); onSplit((change.position.x / size.width).coerceIn(0f, 1f)) } }
            .drawWithContent { drawContent() },
    ) {
        Image(after, null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val x = size.width * split
            clipRect(0f, 0f, x, size.height) {
                // centre-crop the original to the output's shape so both sides line up
                val want = size.width / size.height
                val cw = if (before.width.toFloat() / before.height > want) (before.height * want).toInt() else before.width
                val ch = if (before.width.toFloat() / before.height > want) before.height else (before.width / want).toInt()
                drawImage(before, srcOffset = androidx.compose.ui.unit.IntOffset((before.width - cw) / 2, (before.height - ch) / 2),
                    srcSize = androidx.compose.ui.unit.IntSize(cw, ch), dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
            }
            drawLine(Wiz.Accent, Offset(x, 0f), Offset(x, size.height), 3.dp.toPx())
            drawCircle(Wiz.Accent, 14.dp.toPx(), Offset(x, size.height / 2))
            drawLine(Wiz.OnAccent, Offset(x - 5.dp.toPx(), size.height / 2), Offset(x + 5.dp.toPx(), size.height / 2), 2.dp.toPx())
        }
    }
}
