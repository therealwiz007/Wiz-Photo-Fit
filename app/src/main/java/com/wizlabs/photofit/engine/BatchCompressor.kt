package com.wizlabs.photofit.engine

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import com.wizlabs.photofit.data.ImgFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Compresses many images one at a time (low RAM), reporting each item's state. */
class BatchCompressor(private val cr: ContentResolver) {
    sealed interface Item {
        val uri: Uri
        data class Queued(override val uri: Uri) : Item
        data class Working(override val uri: Uri) : Item
        data class Done(override val uri: Uri, val beforeBytes: Long, val bytes: ByteArray, val w: Int, val h: Int, val ok: Boolean) : Item
        data class Failed(override val uri: Uri, val reason: String) : Item
    }

    data class Settings(val targetKb: Int?, val quality: Int, val maxSide: Int?, val format: ImgFormat)

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items

    fun set(uris: List<Uri>) { _items.value = uris.map { Item.Queued(it) } }

    private fun update(i: Int, item: Item) { _items.value = _items.value.toMutableList().also { it[i] = item } }

    suspend fun run(s: Settings) = withContext(Dispatchers.Default) {
        for (i in _items.value.indices) {
            coroutineContext.ensureActive()
            val uri = _items.value[i].uri
            update(i, Item.Working(uri))
            val res = runCatching { one(uri, s) }.getOrElse { Item.Failed(uri, if (it is OutOfMemoryError) "Photo too large" else "Could not read this file") }
            update(i, res)
        }
    }

    private fun one(uri: Uri, s: Settings): Item {
        val before = cr.openAssetFileDescriptor(uri, "r")?.use { it.length }?.takeIf { it > 0 }
            ?: cr.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
        var bmp = Decode.load(cr, uri, maxEdge = minOf(s.maxSide ?: 4096, 4096)) ?: return Item.Failed(uri, "Could not read this file")
        s.maxSide?.let { m ->
            val long = maxOf(bmp.width, bmp.height)
            if (long > m) { val f = m.toFloat() / long; val r = Bitmap.createScaledBitmap(bmp, (bmp.width * f).toInt(), (bmp.height * f).toInt(), true); if (r !== bmp) bmp.recycle(); bmp = r }
        }
        if (s.format != ImgFormat.PNG && bmp.hasAlpha()) { // JPEG/WEBP: flatten transparency onto white
            val f = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(f).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(bmp, 0f, 0f, null) }
            bmp.recycle(); bmp = f
        }
        val codec = BitmapCodec(bmp, s.format)
        val out = if (s.targetKb == null) {
            Item.Done(uri, before, codec.encode(1f, s.quality, true), bmp.width, bmp.height, true)
        } else when (val r = TargetEncoder.run(codec, null, s.targetKb * 1000, allowFewerPixels = true, pad = null)) {
            is EncodeResult.Ok -> { val (w, h) = codec.sizeAt(r.scale, r.keepSize); Item.Done(uri, before, r.bytes, w, h, true) }
            is EncodeResult.Fail -> Item.Done(uri, before, r.bestBytes, bmp.width, bmp.height, false)
        }
        codec.release(); bmp.recycle()
        return out
    }
}
