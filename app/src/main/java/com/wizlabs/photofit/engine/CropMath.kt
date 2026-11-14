package com.wizlabs.photofit.engine

import kotlin.math.max
import kotlin.math.min

/**
 * Crop state in image pixels: a rectangle with the target aspect, described by its centre and a zoom
 * (1 = the largest crop of that aspect that fits the image). Pure math, unit-tested.
 */
data class CropState(val cx: Float, val cy: Float, val zoom: Float) {
    companion object {
        const val MAX_ZOOM = 8f
        fun centered(imgW: Int, imgH: Int) = CropState(imgW / 2f, imgH / 2f, 1f)
    }
}

data class RectF4(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right get() = left + width
    val bottom get() = top + height
}

object CropMath {
    /** Size of the crop at zoom 1: the biggest [aspect] (w/h) rectangle inside the image. */
    fun baseSize(imgW: Int, imgH: Int, aspect: Float): Pair<Float, Float> {
        val w = min(imgW.toFloat(), imgH * aspect)
        return w to w / aspect
    }

    fun rect(s: CropState, imgW: Int, imgH: Int, aspect: Float): RectF4 {
        val (bw, bh) = baseSize(imgW, imgH, aspect)
        val z = s.zoom.coerceIn(1f, CropState.MAX_ZOOM)
        val w = bw / z; val h = bh / z
        val l = (s.cx - w / 2).coerceIn(0f, imgW - w)
        val t = (s.cy - h / 2).coerceIn(0f, imgH - h)
        return RectF4(l, t, w, h)
    }

    /** Clamp the centre so the crop stays inside the image. */
    fun clamp(s: CropState, imgW: Int, imgH: Int, aspect: Float): CropState {
        val z = s.zoom.coerceIn(1f, CropState.MAX_ZOOM)
        val r = rect(s.copy(zoom = z), imgW, imgH, aspect)
        return CropState(r.left + r.width / 2, r.top + r.height / 2, z)
    }

    /** Pan by a screen drag; [framePx] is the on-screen frame width the crop is shown in. */
    fun pan(s: CropState, dxScreen: Float, dyScreen: Float, framePx: Float, imgW: Int, imgH: Int, aspect: Float): CropState {
        val r = rect(s, imgW, imgH, aspect)
        val k = r.width / framePx
        return clamp(s.copy(cx = s.cx - dxScreen * k, cy = s.cy - dyScreen * k), imgW, imgH, aspect)
    }

    /** Zoom around the current centre. */
    fun zoom(s: CropState, factor: Float, imgW: Int, imgH: Int, aspect: Float): CropState =
        clamp(s.copy(zoom = s.zoom * factor), imgW, imgH, aspect)

    /**
     * Fit a face: head height = [headRatio] of the crop height, eyes [eyeLine] from the top.
     * Face box in image px (from face detection).
     */
    fun fitFace(face: RectF4, imgW: Int, imgH: Int, aspect: Float, headRatio: Float = 0.6f, eyeLine: Float = 0.42f): CropState {
        val (_, bh) = baseSize(imgW, imgH, aspect)
        val headH = face.height * 1.35f // detector box is forehead-to-chin; add hair
        val cropH = headH / headRatio
        val zoom = (bh / cropH).coerceIn(1f, CropState.MAX_ZOOM)
        val h = bh / zoom
        val eyesY = face.top + face.height * 0.38f
        val cy = eyesY - h * eyeLine + h / 2
        return clamp(CropState(face.left + face.width / 2, cy, zoom), imgW, imgH, aspect)
    }

    fun aspectOf(w: Int, h: Int): Float = max(1, w).toFloat() / max(1, h)
}
