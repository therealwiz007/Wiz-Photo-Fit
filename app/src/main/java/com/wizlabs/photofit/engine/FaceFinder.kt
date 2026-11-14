package com.wizlabs.photofit.engine

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

/** Finds the biggest face (ML Kit via Play services, on-device). Null when none, or when the model isn't ready. */
object FaceFinder {
    private val client by lazy {
        FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).setMinFaceSize(0.1f).build())
    }

    suspend fun biggest(src: Bitmap): RectF4? = runCatching {
        // detect on a small copy: faster and less memory; scale the box back
        val k = (640f / maxOf(src.width, src.height)).coerceAtMost(1f)
        val small = if (k < 1f) Bitmap.createScaledBitmap(src, (src.width * k).toInt(), (src.height * k).toInt(), true) else src
        val faces = client.process(InputImage.fromBitmap(small, 0)).await()
        if (small !== src) small.recycle()
        faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }?.boundingBox?.let {
            RectF4(it.left / k, it.top / k, it.width() / k, it.height() / k)
        }
    }.getOrNull()
}
