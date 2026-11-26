package com.wizlabs.photofit.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.Screen
import com.wizlabs.photofit.data.Exporter
import com.wizlabs.photofit.data.Part
import com.wizlabs.photofit.data.PartKind
import com.wizlabs.photofit.data.Preset
import com.wizlabs.photofit.data.PresetRepo
import com.wizlabs.photofit.data.Work
import com.wizlabs.photofit.ui.components.ConfidenceDot
import com.wizlabs.photofit.ui.components.GhostButton
import com.wizlabs.photofit.ui.components.GlyphExam
import com.wizlabs.photofit.ui.components.GlyphSign
import com.wizlabs.photofit.ui.components.Pill
import com.wizlabs.photofit.ui.components.SectionLabel
import com.wizlabs.photofit.ui.components.SpecText
import com.wizlabs.photofit.ui.components.TopBar
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.components.WizCard
import com.wizlabs.photofit.ui.components.confidenceLabel
import com.wizlabs.photofit.ui.motion.enterRise
import com.wizlabs.photofit.ui.motion.pressScale

@Composable
fun ExamDetailScreen(repo: PresetRepo, presetId: String, open: (Screen) -> Unit, onBack: () -> Unit) {
    val p = repo.find(presetId) ?: run { StubScreen("Not found", onBack); return }
    val ctx = LocalContext.current
    var target by rememberSaveable { mutableIntStateOf(0) }
    var camUri by rememberSaveable { mutableStateOf<String?>(null) }
    fun start(uri: Uri) {
        Work.clearImages(); Work.preset = p; Work.partIndex = target; Work.sourceUri = uri
        open(Screen.Editor(p.id, target))
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) start(uri) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) camUri?.let { start(Uri.parse(it)) } }

    // Live-capture forms: the signature is what people actually need, so it goes first.
    val order = remember(p) { p.parts.indices.sortedBy { if (p.parts.any { it.liveCapture } && p.parts[it].kind == PartKind.PHOTO) 1 else 0 } }

    Column(Modifier.fillMaxSize().background(Wiz.Bg).navigationBarsPadding()) {
        TopBar(p.name, if (p.mine) "Your own size" else repo.groups.firstOrNull { it.id == p.group }?.label, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().enterRise(0).clip(RoundedCornerShape(14.dp)).background(Wiz.Warn.copy(alpha = 0.12f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Wiz.Warn)
                Spacer(Modifier.width(10.dp))
                Text("Sizes change every year. Check your latest notification and edit the size if it is different.",
                    color = Wiz.Text, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.enterRise(1), verticalAlignment = Alignment.CenterVertically) {
                ConfidenceDot(p.confidence); Spacer(Modifier.width(8.dp))
                Text(confidenceLabel(p.confidence), color = Wiz.Muted, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            SectionLabel("Tap what you need")
            order.forEachIndexed { i, idx ->
                PartCard(p.parts[idx], Modifier.enterRise(2 + i), onPick = {
                    target = idx; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, onCamera = {
                    target = idx; val u = Exporter.cameraUri(ctx); camUri = u.toString(); camera.launch(u)
                })
                Spacer(Modifier.height(12.dp))
            }
            p.remarks?.let {
                SectionLabel("Notes")
                Text(it, color = Wiz.Muted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton(if (p.mine) "Edit size" else "Copy and edit", {
                    open(if (p.mine) Screen.Builder(editId = p.id) else Screen.Builder(copyFrom = p.id))
                }, Modifier.weight(1f), icon = Icons.Default.Edit)
                GhostButton("Share size", {
                    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, com.wizlabs.photofit.data.ShareCode.shareText(p)) }
                    ctx.startActivity(Intent.createChooser(send, "Share size"))
                }, Modifier.weight(1f), icon = Icons.Default.Share)
            }
            if (p.sources.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel("Sources (public notices)")
                p.sources.forEach { s ->
                    Text(s.removePrefix("https://").removePrefix("www.").substringBefore('?').take(70), color = Wiz.Muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 3.dp).clickable {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s))) }
                        })
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PartCard(part: Part, modifier: Modifier, onPick: () -> Unit, onCamera: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    WizCard(modifier.fillMaxWidth().pressScale(src, 0.97f).clickable(interactionSource = src, indication = null, onClick = onPick)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (part.kind == PartKind.SIGNATURE || part.kind == PartKind.PARENT_SIGNATURE || part.kind == PartKind.DECLARATION) GlyphSign(Wiz.AccentInk) else GlyphExam(Wiz.AccentInk, size = 26.dp)
                Spacer(Modifier.width(12.dp))
                Text(part.label, color = Wiz.Text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(SpecText.kb(part), color = Wiz.AccentInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(SpecText.line(part), color = Wiz.Muted, style = MaterialTheme.typography.bodyMedium)
            part.hint?.let { Spacer(Modifier.height(4.dp)); Text(it, color = Wiz.Muted, style = MaterialTheme.typography.bodySmall) }
            if (part.liveCapture) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Wiz.Warn)
                    Spacer(Modifier.width(6.dp))
                    Text("This form now takes your photo live with the camera. You may only need the signature.",
                        color = Wiz.Warn, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill("Pick from gallery", Wiz.AccentInk)
                Text("or", color = Wiz.Muted, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.heightIn(min = 32.dp).clip(RoundedCornerShape(50)).border(1.dp, Wiz.Line, RoundedCornerShape(50))
                    .clickable(onClick = onCamera).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("Take photo", color = Wiz.Text, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
