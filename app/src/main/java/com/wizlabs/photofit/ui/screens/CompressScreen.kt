package com.wizlabs.photofit.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.wizlabs.photofit.data.Exporter
import com.wizlabs.photofit.data.ImgFormat
import com.wizlabs.photofit.engine.BatchCompressor
import com.wizlabs.photofit.engine.TargetEncoder
import com.wizlabs.photofit.ui.components.GhostButton
import com.wizlabs.photofit.ui.components.GlyphCompress
import com.wizlabs.photofit.ui.components.PrimaryButton
import com.wizlabs.photofit.ui.components.SectionLabel
import com.wizlabs.photofit.ui.components.TopBar
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.components.WizCard
import com.wizlabs.photofit.ui.motion.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CompressScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val batch = remember { BatchCompressor(ctx.contentResolver) }
    val items by batch.items.collectAsState()
    var kb by remember { mutableStateOf<Int?>(100) }
    var maxSide by remember { mutableStateOf<Int?>(null) }
    var fmt by remember { mutableStateOf(ImgFormat.JPG) }
    var job by remember { mutableStateOf<Job?>(null) }
    var savedCount by remember { mutableStateOf(-1) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(100)) { uris -> if (uris.isNotEmpty()) { batch.set(uris); savedCount = -1 } }
    val running = job?.isActive == true
    val done = items.filterIsInstance<BatchCompressor.Item.Done>()

    Column(Modifier.fillMaxSize().background(Wiz.Bg).navigationBarsPadding()) {
        TopBar("Compress", if (items.isEmpty()) "Many photos to a target size" else "${items.size} photos", onBack = { job?.cancel(); onBack() })
        if (items.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                GlyphCompress(Wiz.Accent, size = 64.dp)
                Spacer(Modifier.height(16.dp))
                Text("Make photos smaller", color = Wiz.Text, style = MaterialTheme.typography.headlineMedium)
                Text("Pick up to 100 photos. Everything stays on your phone.", color = Wiz.Muted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Pick photos", { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, icon = Icons.Default.Add)
            }
            return@Column
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            SectionLabel("Each photo under")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(20, 50, 100, 200, 500, 1000, null)) { v -> Chip(if (v == null) "Best quality" else if (v >= 1000) "${v / 1000} MB" else "$v KB", kb == v) { kb = v } }
            }
            SectionLabel("Longest side")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(null, 2048, 1600, 1280, 1024, 800)) { v -> Chip(v?.let { "$it px" } ?: "Keep", maxSide == v) { maxSide = v } }
            }
            SectionLabel("Format")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ImgFormat.JPG, ImgFormat.WEBP, ImgFormat.PNG).forEach { f -> Chip(f.ext.uppercase(), fmt == f) { fmt = f } }
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(items, key = { i, it -> it.uri.toString() + i }, contentType = { _, _ -> "row" }) { i, it ->
                Box(Modifier.animateItem(fadeInSpec = tween(180), placementSpec = spring(1f, 400f, IntOffset.VisibilityThreshold), fadeOutSpec = tween(120))) { BatchRow(i, it) }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (done.size == items.size && !running) {
                Text("${TargetEncoder.kb(done.sumOf { it.beforeBytes.toInt() })} KB → ${TargetEncoder.kb(done.sumOf { it.bytes.size })} KB" + if (savedCount >= 0) "  ·  saved $savedCount" else "",
                    color = Wiz.Good, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("Save all", {
                        scope.launch {
                            var n = 0
                            withContext(Dispatchers.IO) { done.forEachIndexed { i, d -> if (Exporter.save(ctx, d.bytes, "compressed_${System.currentTimeMillis()}_$i.${fmt.ext}", fmt) != null) n++ } }
                            savedCount = n; haptics.confirm()
                        }
                    }, Modifier.weight(1f), icon = Icons.Default.Check)
                    GhostButton("Share all", {
                        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
                        val uris = ArrayList<Uri>(done.mapIndexed { i, d -> FileProvider.getUriForFile(ctx, ctx.packageName + ".files", File(dir, "compressed_$i.${fmt.ext}").apply { writeBytes(d.bytes) }) })
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            type = fmt.mime; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }, "Share photos"))
                    }, Modifier.weight(1f), icon = Icons.Default.Share)
                }
            } else {
                PrimaryButton(if (running) "Working… (tap to stop)" else "Compress all", {
                    if (running) { job?.cancel(); return@PrimaryButton }
                    batch.set(items.map { it.uri }); savedCount = -1
                    job = scope.launch { batch.run(BatchCompressor.Settings(kb, 88, maxSide, fmt)); haptics.confirm() }
                }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BatchRow(i: Int, it: BatchCompressor.Item) {
    val progress by animateFloatAsState(when (it) { is BatchCompressor.Item.Done, is BatchCompressor.Item.Failed -> 1f; is BatchCompressor.Item.Working -> 0.6f; else -> 0f }, tween(300), label = "p")
    WizCard(Modifier.fillMaxWidth(), corner = 14.dp) {
        Box {
            // progress bar as a scaleX transform (no layout animation)
            Box(Modifier.matchParentSize().graphicsLayer { scaleX = progress; transformOrigin = TransformOrigin(0f, 0.5f) }.background(Wiz.Accent.copy(alpha = 0.08f)))
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Photo ${i + 1}", color = Wiz.Text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                when (it) {
                    is BatchCompressor.Item.Queued -> Text("Waiting", color = Wiz.Muted, style = MaterialTheme.typography.bodyMedium)
                    is BatchCompressor.Item.Working -> Text("Working…", color = Wiz.Accent, style = MaterialTheme.typography.bodyMedium)
                    is BatchCompressor.Item.Failed -> Text(it.reason, color = Wiz.Bad, style = MaterialTheme.typography.bodyMedium)
                    is BatchCompressor.Item.Done -> Text("${TargetEncoder.kb(it.beforeBytes.toInt())} → ${TargetEncoder.kb(it.bytes.size)} KB  ·  ${it.w}×${it.h}",
                        color = if (it.ok) Wiz.Good else Wiz.Warn, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

