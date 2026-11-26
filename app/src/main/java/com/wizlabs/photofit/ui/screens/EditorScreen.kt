package com.wizlabs.photofit.ui.screens

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wizlabs.photofit.data.PartKind
import com.wizlabs.photofit.data.Work
import com.wizlabs.photofit.engine.CropMath
import com.wizlabs.photofit.engine.CropState
import com.wizlabs.photofit.engine.Decode
import com.wizlabs.photofit.engine.NameDateStrip
import com.wizlabs.photofit.engine.Render
import com.wizlabs.photofit.engine.SignatureCleaner
import com.wizlabs.photofit.ui.components.GhostButton
import com.wizlabs.photofit.ui.components.PrimaryButton
import com.wizlabs.photofit.ui.components.SpecText
import com.wizlabs.photofit.ui.components.TopBar
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.motion.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun EditorScreen(onDone: () -> Unit, onBack: () -> Unit) {
    val part = Work.part ?: run { StubScreen("Nothing to edit", onBack); return }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    var bmp by remember { mutableStateOf<Bitmap?>(Work.source) }
    var img by remember { mutableStateOf<ImageBitmap?>(Work.source?.asImageBitmap()) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val target = part.size.targetPx()
    var nameDate by rememberSaveable { mutableStateOf(part.nameDate) }
    var name by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(NameDateStrip.today()) }
    // aspect of the photo area (the strip, when on, takes the bottom of the image)
    val photoAspect = if (nameDate && part.kind == PartKind.PHOTO) target.w / (target.h * (1 - NameDateStrip.BAND)) else target.w.toFloat() / target.h
    var crop by remember { mutableStateOf<CropState?>(null) }
    val inkPart = part.kind == PartKind.SIGNATURE || part.kind == PartKind.PARENT_SIGNATURE || part.kind == PartKind.DECLARATION || part.kind == PartKind.THUMB
    val photoPart = part.kind == PartKind.PHOTO || part.kind == PartKind.POSTCARD
    var bg by rememberSaveable { mutableStateOf(part.background) }
    var faceLocked by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    val model by com.wizlabs.photofit.engine.ModelStatus.state.collectAsState()
    var step by remember { mutableStateOf(-1) }
    var steps by remember { mutableStateOf(emptyList<com.wizlabs.photofit.ui.components.Step>()) }
    var live by remember { mutableStateOf<String?>(null) }
    var clean by rememberSaveable { mutableStateOf(inkPart) }
    var strength by rememberSaveable { mutableStateOf(0.5f) }
    var ink by rememberSaveable { mutableStateOf(if (part.kind == PartKind.THUMB) SignatureCleaner.Ink.KEEP else SignatureCleaner.Ink.BLACK) }

    LaunchedEffect(Unit) {
        if (photoPart) com.wizlabs.photofit.engine.ModelStatus.ensure(ctx.applicationContext)
        if (bmp == null) {
            val b = withContext(Dispatchers.IO) {
                val u = Work.sourceUri
                runCatching { u?.let { Decode.load(ctx.contentResolver, it) } }
                    .onFailure { android.util.Log.w("PhotoFit", "decode failed for $u", it) }
                    .getOrNull().also { if (it == null) android.util.Log.w("PhotoFit", "decode null for $u") }
            }
            if (b == null) failed = true else { Work.source = b; bmp = b; img = b.asImageBitmap() }
        }
    }
    val b = bmp
    LaunchedEffect(b, photoAspect, model is com.wizlabs.photofit.engine.ModelStatus.State.Ready) {
        if (b == null) return@LaunchedEffect
        crop = CropMath.clamp(crop ?: CropState.centered(b.width, b.height), b.width, b.height, photoAspect)
        if (photoPart && !faceLocked) {
            val face = com.wizlabs.photofit.engine.FaceFinder.biggest(b)
            if (face != null) {
                com.wizlabs.photofit.engine.ModelStatus.markReady()
                val ratio = com.wizlabs.photofit.data.IdPresets.headRatio[Work.preset?.id] ?: 0.6f
                Work.face = face
                crop = CropMath.fitFace(face, b.width, b.height, photoAspect, headRatio = ratio)
                faceLocked = true; haptics.tick()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(Wiz.Bg).navigationBarsPadding().imePadding()) {
        TopBar(part.label, SpecText.line(part), onBack = onBack)
        if (failed) { Text("Could not open this photo. Try another one.", color = Wiz.Bad, modifier = Modifier.padding(24.dp)); return@Column }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            val cs = crop; val im = img
            Box(
                Modifier.fillMaxWidth(if (photoAspect < 1f) 0.72f else 1f).aspectRatio(photoAspect)
                    .clip(RoundedCornerShape(12.dp)).background(Wiz.Panel).border(1.dp, Wiz.Line, RoundedCornerShape(12.dp))
                    .pointerInput(b, photoAspect) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val bb = b ?: return@detectTransformGestures
                            var s = crop ?: return@detectTransformGestures
                            if (zoom != 1f) s = CropMath.zoom(s, zoom, bb.width, bb.height, photoAspect)
                            s = CropMath.pan(s, pan.x, pan.y, size.width.toFloat(), bb.width, bb.height, photoAspect)
                            crop = s
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (cs == null || im == null || b == null) {
                    Box(Modifier.fillMaxSize().background(Wiz.PanelHi).then(Modifier.shimmerSafe()))
                    Text("Opening your photo…", color = Wiz.Muted, style = MaterialTheme.typography.bodyMedium)
                }
                else Canvas(Modifier.fillMaxSize()) {
                    val r = CropMath.rect(cs, b.width, b.height, photoAspect)
                    drawImage(im, srcOffset = IntOffset(r.left.roundToInt(), r.top.roundToInt()),
                        srcSize = IntSize(r.width.roundToInt().coerceAtLeast(1), r.height.roundToInt().coerceAtLeast(1)),
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium)
                    // corner brackets
                    val arm = size.minDimension * 0.14f; val w = 4.dp.toPx(); val c = Wiz.Accent
                    val e = w / 2
                    for ((x, y, dx, dy) in listOf(listOf(e, e, 1f, 1f), listOf(size.width - e, e, -1f, 1f), listOf(e, size.height - e, 1f, -1f), listOf(size.width - e, size.height - e, -1f, -1f))) {
                        drawLine(c, Offset(x, y), Offset(x + dx * arm, y), w, StrokeCap.Round); drawLine(c, Offset(x, y), Offset(x, y + dy * arm), w, StrokeCap.Round)
                    }
                    if (part.kind == PartKind.PHOTO || part.kind == PartKind.POSTCARD) {
                        // head guide: face should fill this oval
                        val ow = size.width * 0.52f; val oh = size.height * 0.5f
                        drawOval(Wiz.Accent.copy(alpha = 0.7f), Offset((size.width - ow) / 2, size.height * 0.14f), Size(ow, oh), style = Stroke(2.dp.toPx()))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Pinch to zoom · drag to move", color = Wiz.Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Rotate", {
                    val bb = b ?: return@GhostButton
                    val r = Bitmap.createBitmap(bb, 0, 0, bb.width, bb.height, Matrix().apply { postRotate(90f) }, true)
                    if (r !== bb) bb.recycle(); Work.source = r; bmp = r; img = r.asImageBitmap(); crop = CropState.centered(r.width, r.height)
                }, icon = Icons.Default.Refresh)
                GhostButton("Reset", { b?.let { crop = CropMath.clamp(CropState.centered(it.width, it.height), it.width, it.height, photoAspect) } })
            }
            if (photoPart) {
                Spacer(Modifier.height(14.dp))
                Text(if (faceLocked) "Face found and centred" else "Move the face into the oval", color = if (faceLocked) Wiz.Good else Wiz.Muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                Text("Background", color = Wiz.Muted, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((v, l) in listOf(com.wizlabs.photofit.data.Bg.KEEP to "Keep", com.wizlabs.photofit.data.Bg.WHITE to "White", com.wizlabs.photofit.data.Bg.LIGHT to "Light blue")) Chip(l, bg == v) { bg = v }
                }
                if ((bg != com.wizlabs.photofit.data.Bg.KEEP && model !is com.wizlabs.photofit.engine.ModelStatus.State.Ready) || note != null) {
                    Spacer(Modifier.height(10.dp))
                    com.wizlabs.photofit.ui.components.ModelReadyCard(model, onRetry = { com.wizlabs.photofit.engine.ModelStatus.retry(ctx.applicationContext) })
                }
                note?.let { Spacer(Modifier.height(6.dp)); Text(it, color = Wiz.Warn, style = MaterialTheme.typography.bodySmall) }
            }
            if (part.kind == PartKind.PHOTO) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Name + date on photo", color = Wiz.Text, style = MaterialTheme.typography.titleMedium)
                        Text(if (part.nameDate) "This form asks for it" else "Some forms ask for it", color = Wiz.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(nameDate, { nameDate = it }, colors = SwitchDefaults.colors(checkedTrackColor = Wiz.Accent, checkedThumbColor = Wiz.OnAccent))
                }
                if (nameDate) {
                    Spacer(Modifier.height(8.dp))
                    Field("Your name", name) { name = it }
                    Spacer(Modifier.height(8.dp))
                    Field("Date (dd-mm-yyyy)", date) { date = it }
                }
            }
            if (inkPart) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Clean paper", color = Wiz.Text, style = MaterialTheme.typography.titleMedium)
                        Text("White background, sharp ink", color = Wiz.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(clean, { clean = it }, colors = SwitchDefaults.colors(checkedTrackColor = Wiz.Accent, checkedThumbColor = Wiz.OnAccent))
                }
                if (clean) {
                    Text("Strength", color = Wiz.Muted, style = MaterialTheme.typography.labelMedium)
                    androidx.compose.material3.Slider(strength, { strength = it },
                        colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Wiz.Accent, activeTrackColor = Wiz.Accent))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((v, l) in listOf(SignatureCleaner.Ink.BLACK to "Black ink", SignatureCleaner.Ink.BLUE to "Blue ink", SignatureCleaner.Ink.KEEP to "Keep colour"))
                            Chip(l, ink == v) { ink = v }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(if (busy) "Making…" else "Make ${part.label.lowercase()}", {
            val bb = b ?: return@PrimaryButton; val cs = crop ?: return@PrimaryButton
            busy = true; haptics.tap()
            val willBg = photoPart && bg != com.wizlabs.photofit.data.Bg.KEEP
            steps = buildList {
                add(com.wizlabs.photofit.ui.components.Step("Cropping to ${target.w} × ${target.h} px"))
                if (willBg) add(com.wizlabs.photofit.ui.components.Step("Changing the background", "Finding you and smoothing the hair edge"))
                if (inkPart && clean) add(com.wizlabs.photofit.ui.components.Step("Cleaning the paper", "White paper, sharp ink"))
                if (nameDate && part.kind == PartKind.PHOTO) add(com.wizlabs.photofit.ui.components.Step("Adding name and date"))
                add(com.wizlabs.photofit.ui.components.Step("Fitting to ${SpecText.kb(part)}", "Trying the best quality that fits"))
                add(com.wizlabs.photofit.ui.components.Step("Done"))
            }
            step = 0; live = null
            scope.launch {
                val started = System.currentTimeMillis()
                val out = withContext(Dispatchers.Default) {
                    fun next() { step++ }
                    val r = CropMath.rect(cs, bb.width, bb.height, photoAspect)
                    val withStrip = nameDate && part.kind == PartKind.PHOTO
                    val photoH = if (withStrip) (target.h * (1 - NameDateStrip.BAND)).roundToInt() else target.h
                    Work.headRatio = Work.face?.let { (it.height / r.height).coerceIn(0f, 1f) }
                    var cropped = Render.crop(bb, r, target.w, photoH)
                    next()
                    val bgColor = if (photoPart) com.wizlabs.photofit.engine.BackgroundSwap.colorFor(bg, part.bgColor) else null
                    Work.notice = null
                    if (bgColor != null) {
                        // segment at up to 2x the output size for cleaner hair edges, then scale down
                        val k = minOf(2f, r.width / target.w, 1024f / maxOf(target.w, photoH)).coerceAtLeast(1f)
                        val big = Render.crop(bb, r, (target.w * k).roundToInt(), (photoH * k).roundToInt())
                        try {
                            val swapped = com.wizlabs.photofit.engine.BackgroundSwap.apply(big, bgColor)
                            cropped.recycle(); cropped = run { val sp = IntArray(swapped.width * swapped.height); swapped.getPixels(sp, 0, swapped.width, 0, 0, swapped.width, swapped.height); Bitmap.createBitmap(com.wizlabs.photofit.engine.Resample.scale(sp, swapped.width, swapped.height, target.w, photoH), target.w, photoH, Bitmap.Config.ARGB_8888) }; swapped.recycle()
                            note = null
                        } catch (e: com.wizlabs.photofit.engine.BackgroundSwap.NotReady) {
                            note = e.message; Work.notice = "Background not changed: " + e.message
                            steps = steps.toMutableList().also { l -> l[step] = com.wizlabs.photofit.ui.components.Step("Background kept as it is", "Tool not ready yet", failed = true) }
                        } catch (e: Exception) {
                            note = "Could not change the background on this photo."; Work.notice = note
                            steps = steps.toMutableList().also { l -> l[step] = com.wizlabs.photofit.ui.components.Step("Background kept as it is", null, failed = true) }
                        }
                        big.recycle(); next()
                    }
                    if (inkPart && clean) { cropped.recycle(); cropped = cleanedCrop(bb, r, target.w, photoH, strength, ink); next() }
                    cropped = Render.sharpen(cropped, if (inkPart) 0.5f else 0.35f).also { cropped.recycle() }
                    val full = if (withStrip) {
                        val f = Bitmap.createBitmap(target.w, target.h, Bitmap.Config.ARGB_8888)
                        android.graphics.Canvas(f).apply { drawColor(android.graphics.Color.WHITE); drawBitmap(cropped, 0f, 0f, null) }
                        cropped.recycle(); NameDateStrip.draw(f, name, date); next(); f
                    } else cropped
                    Work.rendered?.recycle(); Work.rendered = full; Work.savedUri = null
                    val o = Render.encode(full, part)
                    live = "${com.wizlabs.photofit.engine.TargetEncoder.kb(o.bytes.size)} KB"
                    step = steps.size; o
                }
                // keep the steps on screen long enough to read (never adds more than ~0.8 s)
                kotlinx.coroutines.delay((900 - (System.currentTimeMillis() - started)).coerceIn(0, 900))
                Work.output = out; busy = false; step = -1
                if (out.ok) haptics.confirm() else haptics.reject()
                onDone()
            }
        }, Modifier.fillMaxWidth().padding(16.dp), icon = Icons.Default.Check, enabled = !busy && b != null)
    }
    com.wizlabs.photofit.ui.components.ProcessingOverlay(
        visible = step >= 0, title = if (step >= steps.lastIndex && steps.isNotEmpty()) "Ready!" else "Making your ${part.label.lowercase()}",
        steps = steps, current = step, image = img, aspect = photoAspect, live = live,
    )
    }
}

private fun Modifier.shimmerSafe(): Modifier = with(com.wizlabs.photofit.ui.components.Loading) { this@shimmerSafe.shim() }

@Composable
fun Field(label: String, value: String, modifier: Modifier = Modifier, keyboard: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default, onChange: (String) -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Text(label, color = Wiz.Muted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().heightIn(min = 50.dp).clip(RoundedCornerShape(14.dp)).background(Wiz.Panel)
            .border(1.dp, Wiz.Line, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
            BasicTextField(value, onChange, singleLine = true, textStyle = TextStyle(color = Wiz.Text, fontSize = 17.sp),
                cursorBrush = SolidColor(Wiz.Text), keyboardOptions = keyboard, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Clean at up to 2x the target resolution (sharper strokes), then scale to the exact size. */
private fun cleanedCrop(src: Bitmap, r: com.wizlabs.photofit.engine.RectF4, w: Int, h: Int, strength: Float, ink: SignatureCleaner.Ink): Bitmap {
    // Clean at up to 4x the output so thin pen strokes survive, then Lanczos down: smooth, crisp ink instead of blur.
    val k = minOf(4f, r.width / w).coerceAtLeast(1f)
    val ww = (w * k).roundToInt(); val hh = (h * k).roundToInt()
    val big = Render.crop(src, r, ww, hh)
    val px = IntArray(ww * hh); big.getPixels(px, 0, ww, 0, 0, ww, hh); big.recycle()
    val out = SignatureCleaner.clean(px, ww, hh, strength, ink)
    val scaled = if (ww == w && hh == h) out else com.wizlabs.photofit.engine.Resample.scale(out, ww, hh, w, h)
    return Bitmap.createBitmap(scaled, w, h, Bitmap.Config.ARGB_8888)
}
