package com.wizlabs.photofit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.Screen
import com.wizlabs.photofit.data.Exporter
import com.wizlabs.photofit.data.IdPresets
import com.wizlabs.photofit.data.ImgFormat
import com.wizlabs.photofit.data.Work
import com.wizlabs.photofit.engine.PrintSheet
import com.wizlabs.photofit.engine.Units
import com.wizlabs.photofit.ui.components.GhostButton
import com.wizlabs.photofit.ui.components.GlyphPassport
import com.wizlabs.photofit.ui.components.PrimaryButton
import com.wizlabs.photofit.ui.components.SectionLabel
import com.wizlabs.photofit.ui.components.SpecText
import com.wizlabs.photofit.ui.components.TopBar
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.components.WizCard
import com.wizlabs.photofit.ui.motion.enterRise
import com.wizlabs.photofit.ui.motion.pressScale
import com.wizlabs.photofit.ui.motion.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun IdListScreen(open: (Screen) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Wiz.Bg).navigationBarsPadding()) {
        TopBar("ID / Passport", "Pick a size, then print many on one sheet", onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(IdPresets.all, key = { it.id }) { p ->
                val src = remember { MutableInteractionSource() }
                val part = p.parts.first()
                Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).pressScale(src, 0.97f).clip(RoundedCornerShape(18.dp)).background(Wiz.Panel)
                    .clickable(interactionSource = src, indication = null) { open(Screen.ExamDetail(p.id)) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    GlyphPassport(Wiz.Accent)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, color = Wiz.Text, style = MaterialTheme.typography.titleMedium)
                        Text(SpecText.px(part) + " at ${part.size.dpi} dpi · " + part.background.name.lowercase().replace("light", "light blue") + " background",
                            color = Wiz.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Spacer(Modifier.height(6.dp))
                GhostButton("Other size (mm)", { open(Screen.Builder()) }, Modifier.fillMaxWidth(), icon = Icons.Default.Add)
                Spacer(Modifier.height(8.dp))
                Text("Not an official tool. Embassies and passport offices change their rules: check them before printing.",
                    color = Wiz.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun PrintSheetScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val tile = Work.rendered; val part = Work.part
    if (tile == null || part == null) { StubScreen("Nothing to print", onBack); return }
    val ph = part.size.phys ?: (Units.fromPx(tile.width, com.wizlabs.photofit.data.LenUnit.MM, part.size.dpi) to Units.fromPx(tile.height, com.wizlabs.photofit.data.LenUnit.MM, part.size.dpi))
    val wMm = if (part.size.unit == com.wizlabs.photofit.data.LenUnit.MM) ph.first else Units.fromPx(tile.width, com.wizlabs.photofit.data.LenUnit.MM, part.size.dpi)
    val hMm = if (part.size.unit == com.wizlabs.photofit.data.LenUnit.MM) ph.second else Units.fromPx(tile.height, com.wizlabs.photofit.data.LenUnit.MM, part.size.dpi)
    var paper by remember { mutableStateOf(PrintSheet.Paper.P4X6) }
    val layout = remember(paper) { PrintSheet.layout(paper, wMm, hMm) }
    var copies by remember(layout) { mutableIntStateOf(layout.count) }
    var cut by remember { mutableStateOf(true) }
    var pdf by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(layout, copies, cut) {
        preview = withContext(Dispatchers.Default) {
            val small = PrintSheet.renderJpeg(layout.copy(), tile, copies, cut)
            android.graphics.BitmapFactory.decodeByteArray(small, 0, small.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap()
        }
    }
    Column(Modifier.fillMaxSize().background(Wiz.Bg).navigationBarsPadding()) {
        TopBar("Print sheet", "${Units.fmt(wMm)} × ${Units.fmt(hMm)} mm photos", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            preview?.let { Image(it, null, Modifier.fillMaxWidth(if (paper == PrintSheet.Paper.A4) 0.7f else 0.6f).aspectRatio(paper.wMm / paper.hMm).enterRise(0).clip(RoundedCornerShape(6.dp))) }
            Spacer(Modifier.height(12.dp))
            SectionLabel("Paper")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PrintSheet.Paper.entries.forEach { Chip(it.label, paper == it) { paper = it } } }
            SectionLabel("Copies (max ${layout.count})")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GhostButton("−", { copies = (copies - 1).coerceAtLeast(1) })
                Text("$copies", color = Wiz.Text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                GhostButton("+", { copies = (copies + 1).coerceAtMost(layout.count) })
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Cut lines", color = Wiz.Text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(cut, { cut = it }, colors = SwitchDefaults.colors(checkedTrackColor = Wiz.Accent, checkedThumbColor = Wiz.OnAccent))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("PDF (prints at exact size)", color = Wiz.Text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(pdf, { pdf = it }, colors = SwitchDefaults.colors(checkedTrackColor = Wiz.Accent, checkedThumbColor = Wiz.OnAccent))
            }
            Spacer(Modifier.height(8.dp))
            Text("Print at 100 % / \"actual size\" (no fit to page) so each photo is exactly ${Units.fmt(wMm)} × ${Units.fmt(hMm)} mm.", color = Wiz.Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val fmt = if (pdf) ImgFormat.PDF else ImgFormat.JPG
            val name = "print_sheet_${paper.name.lowercase()}_${System.currentTimeMillis() / 1000}.${fmt.ext}"
            suspend fun make(): ByteArray = withContext(Dispatchers.Default) {
                if (pdf) PrintSheet.renderPdf(paper, layout, tile, copies, cut) else PrintSheet.renderJpeg(layout, tile, copies, cut)
            }
            PrimaryButton("Save", { scope.launch { val b = make(); bytes = b
                if (Exporter.canAutoSave() && Exporter.save(ctx, b, name, fmt) != null) haptics.confirm() } }, Modifier.weight(1f), icon = Icons.Default.Check)
            GhostButton("Share / print", { scope.launch { val b = make(); ctx.startActivity(Exporter.shareIntent(ctx, b, name, fmt)) } }, Modifier.weight(1f), icon = Icons.Default.Share)
        }
    }
}
