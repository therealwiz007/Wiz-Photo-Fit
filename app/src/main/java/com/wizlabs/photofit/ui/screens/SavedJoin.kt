package com.wizlabs.photofit.ui.screens

import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.data.Exporter
import com.wizlabs.photofit.data.ImgFormat
import com.wizlabs.photofit.data.IntSz
import com.wizlabs.photofit.data.Part
import com.wizlabs.photofit.data.PartKind
import com.wizlabs.photofit.data.Confidence
import com.wizlabs.photofit.data.Preset
import com.wizlabs.photofit.data.SizeSpec
import com.wizlabs.photofit.data.Work
import com.wizlabs.photofit.engine.Decode
import com.wizlabs.photofit.engine.Render
import com.wizlabs.photofit.engine.TargetEncoder
import com.wizlabs.photofit.ui.components.GhostButton
import com.wizlabs.photofit.ui.components.GlyphSaved
import com.wizlabs.photofit.ui.components.PrimaryButton
import com.wizlabs.photofit.ui.components.SectionLabel
import com.wizlabs.photofit.ui.components.TopBar
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.components.WizCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A generic signature size for the Home shortcut (most forms: 140 × 60 px, 10–20 KB). */
val GenericSignature = Preset("gen-signature", "docs", "Signature (common size)", listOf(
    Part(PartKind.SIGNATURE, "Signature", SizeSpec(px = IntSz(140, 60)), 10, 20, hint = "Black ink on white paper. Edit the size if your form is different."),
), Confidence.USER)

private data class SavedFile(val uri: Uri, val name: String, val bytes: Long, val date: Long)

/** Files this app saved to Pictures/Wiz Photo Fit (readable without permission on Android 10+). */
@Composable
fun SavedScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val files by produceState<List<SavedFile>?>(null) {
        value = withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < 29) return@withContext emptyList()
            val out = ArrayList<SavedFile>()
            val col = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ctx.contentResolver.query(col, arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_ADDED),
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?", arrayOf("%${Exporter.FOLDER}%"), "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { c ->
                while (c.moveToNext()) out += SavedFile(ContentUris.withAppendedId(col, c.getLong(0)), c.getString(1), c.getLong(2), c.getLong(3))
            }
            out
        }
    }
    Column(Modifier.fillMaxSize().background(Wiz.Bg).navigationBarsPadding()) {
        TopBar("Saved", "Pictures/${Exporter.FOLDER}", onBack = onBack)
        val list = files
        if (list != null && list.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                GlyphSaved(Wiz.Muted, size = 56.dp); Spacer(Modifier.height(12.dp))
                Text("Your photos will show here", color = Wiz.Muted, style = MaterialTheme.typography.titleMedium)
            }
            return@Column
        }
        LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list ?: emptyList(), key = { it.uri.toString() }) { f ->
                val thumb by produceState<ImageBitmap?>(null, f.uri) {
                    value = withContext(Dispatchers.IO) { runCatching { Decode.load(ctx.contentResolver, f.uri, 320)?.asImageBitmap() }.getOrNull() }
                }
                Column(Modifier.clickable {
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/*"; putExtra(Intent.EXTRA_STREAM, f.uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share"))
                }) {
                    Box(Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(10.dp)).background(Wiz.Panel), contentAlignment = Alignment.Center) {
                        thumb?.let { Image(it, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
                    }
                    Text("${TargetEncoder.kb(f.bytes.toInt())} KB", color = Wiz.Accent, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

/** Joins a photo and a signature into one image (some forms want both in one upload). */
@Composable
fun JoinerScreen(onResult: () -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var sign by remember { mutableStateOf<Bitmap?>(null) }
    var pickingPhoto by remember { mutableStateOf(true) }
    var kbMax by remember { mutableStateOf<Int?>(50) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val b = withContext(Dispatchers.IO) { Decode.load(ctx.contentResolver, uri, 1200) }
            if (pickingPhoto) photo = b else sign = b
        }
    }
    Column(Modifier.fillMaxSize().background(Wiz.Bg).navigationBarsPadding()) {
        TopBar("Join photo + signature", "One image with the photo on top and the signature below", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            for ((isPhoto, b) in listOf(true to photo, false to sign)) {
                WizCard(Modifier.fillMaxWidth().clickable { pickingPhoto = isPhoto; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isPhoto) "1. Photo" else "2. Signature", color = Wiz.Text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Text(if (b == null) "Tap to pick" else "${b.width}×${b.height} ✓", color = if (b == null) Wiz.Muted else Wiz.Good, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            SectionLabel("File size under")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(20, 50, 100, 200).forEach { v -> Chip("$v KB", kbMax == v) { kbMax = v } } }
        }
        PrimaryButton(if (busy) "Joining…" else "Join", {
            val p = photo ?: return@PrimaryButton; val s = sign ?: return@PrimaryButton
            busy = true
            scope.launch {
                val out = withContext(Dispatchers.Default) {
                    // photo 300 px wide on top, signature scaled to the same width below, white gap between
                    val w = 300; val ph = (p.height * w / p.width.toFloat()).toInt(); val sh = (s.height * w / s.width.toFloat()).toInt()
                    val gap = 12
                    val img = Bitmap.createBitmap(w, ph + gap + sh, Bitmap.Config.ARGB_8888)
                    android.graphics.Canvas(img).apply {
                        drawColor(android.graphics.Color.WHITE)
                        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
                        drawBitmap(p, null, android.graphics.RectF(0f, 0f, w.toFloat(), ph.toFloat()), paint)
                        drawBitmap(s, null, android.graphics.RectF(0f, (ph + gap).toFloat(), w.toFloat(), (ph + gap + sh).toFloat()), paint)
                    }
                    val part = Part(PartKind.EXTRA, "Photo + signature", SizeSpec(px = IntSz(img.width, img.height)), null, kbMax, ImgFormat.JPG)
                    Work.clearImages(); Work.preset = Preset("joined", "docs", "Joined photo + signature", listOf(part), Confidence.USER)
                    Work.partIndex = 0; Work.rendered = img; Work.notice = null
                    Render.encode(img, part)
                }
                Work.output = out; busy = false; onResult()
            }
        }, Modifier.fillMaxWidth().padding(16.dp), icon = Icons.Default.Check, enabled = photo != null && sign != null && !busy)
    }
}
