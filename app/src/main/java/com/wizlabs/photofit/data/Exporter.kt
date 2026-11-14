package com.wizlabs.photofit.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/** Saving and sharing without any storage permission. */
object Exporter {
    const val FOLDER = "Wiz Photo Fit"

    /** Android 10+: write straight to Pictures/Wiz Photo Fit (images) or Download/Wiz Photo Fit (PDF). */
    fun canAutoSave(): Boolean = Build.VERSION.SDK_INT >= 29

    fun save(ctx: Context, bytes: ByteArray, name: String, fmt: ImgFormat): Uri? {
        if (Build.VERSION.SDK_INT < 29) return null
        val cr = ctx.contentResolver
        val pdf = fmt == ImgFormat.PDF
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, fmt.mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, (if (pdf) Environment.DIRECTORY_DOWNLOADS else Environment.DIRECTORY_PICTURES) + "/" + FOLDER)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val col = if (pdf) MediaStore.Downloads.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri = cr.insert(col, values) ?: return null
        return try {
            cr.openOutputStream(uri)?.use { it.write(bytes) } ?: error("no stream")
            cr.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            uri
        } catch (e: Exception) { cr.delete(uri, null, null); null }
    }

    /** Writes to a SAF document the user picked (Android 8-9 save path). */
    fun writeTo(ctx: Context, uri: Uri, bytes: ByteArray): Boolean =
        runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null }.getOrDefault(false)

    fun shareIntent(ctx: Context, bytes: ByteArray, name: String, fmt: ImgFormat): Intent {
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 3_600_000 }?.forEach { it.delete() }
        val f = File(dir, name).apply { writeBytes(bytes) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = fmt.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, "Share $name")
    }

    /** A fresh FileProvider uri for the system camera app to write into (no CAMERA permission needed). */
    fun cameraUri(ctx: Context): Uri {
        val dir = File(ctx.cacheDir, "shots").apply { mkdirs() }
        dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000 }?.forEach { it.delete() }
        val f = File(dir, "shot_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
    }

    fun fileName(base: String, part: Part, fmt: ImgFormat): String {
        val safe = base.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').take(24).ifEmpty { "photo" }
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        return "${safe}_${part.kind.name.lowercase()}_$stamp.${fmt.ext}"
    }
}
