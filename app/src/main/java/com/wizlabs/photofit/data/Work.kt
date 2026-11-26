package com.wizlabs.photofit.data

import android.graphics.Bitmap
import android.net.Uri
import com.wizlabs.photofit.engine.Render

/** In-memory hand-off between Editor and Result (bitmaps don't belong in navigation state). */
object Work {
    var preset: Preset? = null
    var partIndex: Int = 0
    var sourceUri: Uri? = null
    var source: Bitmap? = null
    /** Exact-size image after crop / background / strip, before encoding (re-encoded by Smaller/Bigger). */
    var rendered: Bitmap? = null
    var output: Render.Output? = null
    var savedUri: Uri? = null
    /** Something the Result screen should tell the user (e.g. background not changed). */
    var notice: String? = null
    /** Face box in source-bitmap pixels when FaceFinder ran, and the head/crop-height share at export (for the Face-size check). */
    var face: com.wizlabs.photofit.engine.RectF4? = null
    var headRatio: Float? = null

    val part: Part? get() = preset?.parts?.getOrNull(partIndex)

    fun clearImages() { face = null; headRatio = null; source?.recycle(); source = null; rendered?.recycle(); rendered = null; output = null; savedUri = null }
}
