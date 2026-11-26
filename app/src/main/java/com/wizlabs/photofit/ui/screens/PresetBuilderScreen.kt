package com.wizlabs.photofit.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.data.Bg
import com.wizlabs.photofit.data.Confidence
import com.wizlabs.photofit.data.ImgFormat
import com.wizlabs.photofit.data.IntSz
import com.wizlabs.photofit.data.LenUnit
import com.wizlabs.photofit.data.Part
import com.wizlabs.photofit.data.PartKind
import com.wizlabs.photofit.data.Preset
import com.wizlabs.photofit.data.PresetRepo
import com.wizlabs.photofit.data.SizeSpec
import com.wizlabs.photofit.engine.Units
import com.wizlabs.photofit.ui.components.GhostButton
import com.wizlabs.photofit.ui.components.PrimaryButton
import com.wizlabs.photofit.ui.components.RoundIconButton
import com.wizlabs.photofit.ui.components.SectionLabel
import com.wizlabs.photofit.ui.components.TopBar
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.components.WizCard
import com.wizlabs.photofit.ui.motion.WizBackHandler
import com.wizlabs.photofit.ui.motion.rememberHaptics

/** Editable form state of one part (strings, so half-typed numbers are allowed). */
private class PartForm(p: Part) {
    var kind by mutableStateOf(p.kind)
    var label by mutableStateOf(p.label)
    var unit by mutableStateOf(if (p.size.px != null && p.size.phys == null) LenUnit.PX else p.size.unit)
    var w by mutableStateOf(p.size.phys?.first?.let(Units::fmt) ?: p.size.px?.w?.toString() ?: "")
    var h by mutableStateOf(p.size.phys?.second?.let(Units::fmt) ?: p.size.px?.h?.toString() ?: "")
    var dpi by mutableStateOf(p.size.dpi.toString())
    var kbMin by mutableStateOf(p.kbMin?.toString() ?: "")
    var kbMax by mutableStateOf(p.kbMax?.toString() ?: "")
    var format by mutableStateOf(p.format)
    var bg by mutableStateOf(p.background)
    var nameDate by mutableStateOf(p.nameDate)
    var hint by mutableStateOf(p.hint ?: "")
    var shake by mutableIntStateOf(0)

    init { if (p.size.px != null && p.size.phys != null) { unit = p.size.unit } }

    fun num(s: String) = s.replace(',', '.').toFloatOrNull()

    /** Null + message when invalid. */
    fun build(): Pair<Part?, String?> {
        val wv = num(w); val hv = num(h); val d = dpi.toIntOrNull() ?: 0
        if (wv == null || hv == null || wv <= 0 || hv <= 0) return null to "Enter width and height"
        if (unit != LenUnit.PX && d !in 50..1200) return null to "DPI must be 50–1200"
        val size = if (unit == LenUnit.PX) SizeSpec(px = IntSz(wv.toInt(), hv.toInt()), dpi = d.coerceIn(50, 1200).takeIf { it > 0 } ?: 200)
        else SizeSpec(phys = wv to hv, unit = unit, dpi = d)
        val t = size.targetPx()
        if (t.w > 6000 || t.h > 6000) return null to "Too big: ${t.w} × ${t.h} px (max 6000)"
        val mn = kbMin.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        val mx = kbMax.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
        if (kbMin.isNotBlank() && mn == null || kbMax.isNotBlank() && mx == null) return null to "KB must be a whole number"
        if (mn != null && mx != null && mn > mx) return null to "Min KB is bigger than max KB"
        return Part(kind, label.ifBlank { kind.label }, size, mn, mx, format, bg, nameDate = nameDate, hint = hint.ifBlank { null }) to null
    }

    fun summary(): String {
        val (p, err) = build()
        if (p == null) return err ?: ""
        val t = p.size.targetPx()
        return "= ${t.w} × ${t.h} px  ·  ${Units.ratio(t.w, t.h)}" + (if (unit != LenUnit.PX) "  ·  at ${p.size.dpi} dpi" else "")
    }
}

@Composable
fun PresetBuilderScreen(repo: PresetRepo, editId: String?, copyFrom: String?, onSaved: (String) -> Unit, onBack: () -> Unit) {
    val haptics = rememberHaptics()
    val base = remember { (editId ?: copyFrom)?.let { repo.find(it) } }
    var name by remember { mutableStateOf(when { editId != null -> base?.name ?: ""; base != null -> base.name + " (mine)"; else -> "" }) }
    val forms = remember {
        mutableStateListOf<PartForm>().apply {
            (base?.parts ?: listOf(Part(PartKind.PHOTO, "Photo", SizeSpec(px = IntSz(200, 230)), 20, 50))).forEach { add(PartForm(it)) }
        }
    }
    var confirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    WizBackHandler("builder-discard") { confirm = true }

    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, containerColor = Wiz.Panel,
        title = { Text("Discard this size?", color = Wiz.Text) },
        confirmButton = { TextButton({ confirm = false; onBack() }) { Text("Discard", color = Wiz.Bad) } },
        dismissButton = { TextButton({ confirm = false }) { Text("Keep editing", color = Wiz.AccentInk) } })

    Column(Modifier.fillMaxSize().background(Wiz.Bg).navigationBarsPadding().imePadding()) {
        TopBar(if (editId != null) "Edit size" else "Make your own size", "Copy the numbers from your form", onBack = { confirm = true })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Field("Name (e.g. My college form)", name) { name = it.take(60) }
            Spacer(Modifier.height(14.dp))
            forms.forEachIndexed { i, f ->
                PartEditor(f, canDelete = forms.size > 1, onDelete = { forms.removeAt(i) })
                Spacer(Modifier.height(12.dp))
            }
            if (forms.size < 8) {
                SectionLabel("Add another item")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PartKind.entries.filter { k -> k == PartKind.EXTRA || forms.none { it.kind == k } }) { k ->
                        Chip("+ ${k.label}", false) {
                            val def = when (k) {
                                PartKind.SIGNATURE, PartKind.PARENT_SIGNATURE -> Part(k, k.label, SizeSpec(px = IntSz(140, 60)), 10, 20)
                                PartKind.THUMB -> Part(k, k.label, SizeSpec(px = IntSz(240, 240)), 20, 50)
                                PartKind.DECLARATION -> Part(k, k.label, SizeSpec(px = IntSz(800, 400)), 50, 100)
                                PartKind.POSTCARD -> Part(k, k.label, SizeSpec(phys = 10.2f to 15.2f, unit = LenUnit.CM, dpi = 150), 10, 200)
                                PartKind.EXTRA -> Part(k, "Extra document", SizeSpec(px = IntSz(600, 800)), null, 200)
                                PartKind.PHOTO -> Part(k, k.label, SizeSpec(px = IntSz(200, 230)), 20, 50)
                            }
                            forms.add(PartForm(def))
                        }
                    }
                }
            }
            error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = Wiz.Bad, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton("Save size", {
            val built = forms.map { it.build() }
            val bad = built.indexOfFirst { it.first == null }
            when {
                name.isBlank() -> { error = "Give it a name"; haptics.reject() }
                bad >= 0 -> { error = "${forms[bad].label}: ${built[bad].second}"; forms[bad].shake++; haptics.reject() }
                else -> {
                    val id = editId ?: PresetRepo.newId()
                    repo.saveMine(Preset(id, "mine", name.trim(), built.map { it.first!! }, Confidence.USER, mine = true, copiedFrom = copyFrom ?: base?.copiedFrom))
                    haptics.confirm(); onSaved(id)
                }
            }
        }, Modifier.fillMaxWidth().padding(16.dp), icon = Icons.Default.Check)
    }
}

@Composable
private fun PartEditor(f: PartForm, canDelete: Boolean, onDelete: () -> Unit) {
    val dx = remember { Animatable(0f) }
    LaunchedEffect(f.shake) { if (f.shake > 0) for (v in listOf(1f, -0.8f, 0.6f, -0.4f, 0.2f, 0f)) dx.animateTo(v, tween(55)) }
    val num = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    val int = KeyboardOptions(keyboardType = KeyboardType.Number)
    WizCard(Modifier.fillMaxWidth().graphicsLayer { translationX = dx.value * 10.dp.toPx() }) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f.kind.label.uppercase(), color = Wiz.AccentInk, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                if (canDelete) RoundIconButton(Icons.Default.Delete, "Remove", onDelete, tint = Wiz.Muted, size = 40.dp)
            }
            if (f.kind == PartKind.EXTRA) { Field("Label", f.label) { f.label = it.take(40) }; Spacer(Modifier.height(8.dp)) }
            Text("Unit", color = Wiz.Muted, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LenUnit.entries.forEach { u -> Chip(u.label, f.unit == u) {
                    // convert the current numbers so the size stays the same when switching units
                    val (p, _) = f.build()
                    if (p != null) { val t = p.size.targetPx(); val d = f.dpi.toIntOrNull() ?: 200
                        f.w = Units.fmt(Units.fromPx(t.w, u, d)); f.h = Units.fmt(Units.fromPx(t.h, u, d)) }
                    f.unit = u
                } }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Width (${f.unit.label})", f.w, Modifier.weight(1f), num) { f.w = it.take(8) }
                Field("Height (${f.unit.label})", f.h, Modifier.weight(1f), num) { f.h = it.take(8) }
                if (f.unit != LenUnit.PX) Field("DPI", f.dpi, Modifier.weight(0.8f), int) { f.dpi = it.take(4) }
            }
            Spacer(Modifier.height(6.dp))
            Text(f.summary(), color = if (f.build().first != null) Wiz.Good else Wiz.Warn, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Min KB (optional)", f.kbMin, Modifier.weight(1f), int) { f.kbMin = it.take(6) }
                Field("Max KB (blank = no limit)", f.kbMax, Modifier.weight(1f), int) { f.kbMax = it.take(6) }
            }
            Spacer(Modifier.height(10.dp))
            Text("Format", color = Wiz.Muted, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ImgFormat.JPG, ImgFormat.PNG, ImgFormat.PDF).forEach { x -> Chip(x.ext.uppercase(), f.format == x) { f.format = x } }
            }
            if (f.kind == PartKind.PHOTO || f.kind == PartKind.POSTCARD) {
                Spacer(Modifier.height(10.dp))
                Text("Background", color = Wiz.Muted, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Bg.KEEP to "Keep", Bg.WHITE to "White", Bg.LIGHT to "Light blue").forEach { (b, l) -> Chip(l, f.bg == b) { f.bg = b } }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Name + date on photo", color = Wiz.Text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(f.nameDate, { f.nameDate = it }, colors = SwitchDefaults.colors(checkedTrackColor = Wiz.Accent, checkedThumbColor = Wiz.OnAccent))
                }
            }
            Spacer(Modifier.height(8.dp))
            Field("Note (optional)", f.hint) { f.hint = it.take(80) }
        }
    }
}
