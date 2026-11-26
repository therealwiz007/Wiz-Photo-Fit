package com.wizlabs.photofit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wizlabs.photofit.Screen
import com.wizlabs.photofit.data.PresetRepo
import com.wizlabs.photofit.data.Recents
import com.wizlabs.photofit.data.SearchEntry
import com.wizlabs.photofit.data.SearchIndex
import com.wizlabs.photofit.engine.SnipAction
import com.wizlabs.photofit.engine.SnipExpression
import com.wizlabs.photofit.ui.components.GlyphCompress
import com.wizlabs.photofit.ui.components.GlyphExam
import com.wizlabs.photofit.ui.components.GlyphJoin
import com.wizlabs.photofit.ui.components.GlyphPassport
import com.wizlabs.photofit.ui.components.GlyphSaved
import com.wizlabs.photofit.ui.components.GlyphSign
import com.wizlabs.photofit.ui.components.RoundIconButton
import com.wizlabs.photofit.ui.components.SectionLabel
import com.wizlabs.photofit.ui.components.SpecText
import com.wizlabs.photofit.ui.components.TrustStrip
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.mascot.Snip
import com.wizlabs.photofit.ui.motion.enterRise
import com.wizlabs.photofit.ui.motion.pressScale

@Composable
fun HomeScreen(repo: PresetRepo, open: (Screen) -> Unit) {
    val ctx = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val recent = remember { Recents.load(ctx).mapNotNull { repo.find(it) } }
    val index = remember(repo.mine) { repo.all().map { SearchEntry(it.id, it.name, repo.groups.firstOrNull { g -> g.id == it.group }?.label ?: "") } +
        com.wizlabs.photofit.data.IdPresets.all.map { SearchEntry(it.id, it.name, "ID Passport") } }
    val hits = remember(query, index) { if (query.isBlank()) emptyList() else SearchIndex.search(index, query).take(6) }

    Column(
        Modifier.fillMaxSize().background(Wiz.Bg).verticalScroll(rememberScrollState())
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Wordmark(Modifier.weight(1f))
            RoundIconButton(Icons.Outlined.Info, "About", { open(Screen.About) }, tint = Wiz.Muted, size = 48.dp)
        }
        if (query.isBlank()) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp).enterRise(0), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your exam photo and signature, accepted the first time.", color = Wiz.Text, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Exact pixels. Exact KB. Checked before you save.", color = Wiz.Muted, style = MaterialTheme.typography.bodyMedium)
                }
                Snip(SnipExpression.HAPPY, SnipAction.NONE, 96.dp)
            }
        } else Spacer(Modifier.height(12.dp))
        SearchBox(query, { query = it }, hint = "Search exam or document…", modifier = Modifier.enterRise(1))
        if (query.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            if (hits.isEmpty()) {
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Snip(SnipExpression.SEARCHING, SnipAction.LOOK_SIDE, 72.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Can't find it?", color = Wiz.Text, style = MaterialTheme.typography.titleMedium)
                        Text("Try a custom size.", color = Wiz.Muted, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.heightIn(min = 48.dp).clickable { open(Screen.Builder()) }.padding(top = 4.dp))
                    }
                }
            } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (h in hits) repo.find(h.id)?.let { p ->
                    ResultRow(p.name, p.parts.first().let { SpecText.px(it) + "  ·  " + SpecText.kb(it) }) { Recents.push(ctx, p.id); open(Screen.ExamDetail(p.id)) }
                }
            }
            return@Column
        }
        if (recent.isNotEmpty()) {
            SectionLabel("Recent")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (p in recent.take(3)) ResultRow(p.name, null, Modifier.weight(1f)) { open(Screen.ExamDetail(p.id)) }
            }
        }
        SectionLabel("Tools")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("Exam photo & sign", { GlyphExam(Wiz.AccentInk) }, { open(Screen.ExamList) }, Modifier.weight(1f).enterRise(2))
                Tile("ID / Passport", { GlyphPassport(Wiz.AccentInk) }, { open(Screen.IdList) }, Modifier.weight(1f).enterRise(2))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("Signature only", { GlyphSign(Wiz.AccentInk) }, { open(Screen.Signature) }, Modifier.weight(1f).enterRise(3))
                Tile("Custom size", { Icon(Icons.Default.Add, null, tint = Wiz.AccentInk, modifier = Modifier.size(28.dp)) }, { open(Screen.Builder()) }, Modifier.weight(1f).enterRise(3))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("Compress", { GlyphCompress(Wiz.AccentInk) }, { open(Screen.Compress) }, Modifier.weight(1f).enterRise(4))
                Tile("Join photo + sign", { GlyphJoin(Wiz.AccentInk) }, { open(Screen.Joiner) }, Modifier.weight(1f).enterRise(4))
            }
            Tile("Saved photos", { GlyphSaved(Wiz.AccentInk) }, { open(Screen.Saved) }, Modifier.fillMaxWidth().enterRise(5), wide = true)
        }
        Spacer(Modifier.height(16.dp))
        TrustStrip(Modifier.enterRise(6))
        Spacer(Modifier.height(24.dp))
    }
}

/** "Wiz" 700 then the product word 500; marigold-ink "Fit". Never used on the launcher icon. */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Text(buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Wiz ") }
        withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append("Photo ") }
        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = Wiz.AccentInk)) { append("Fit") }
    }, modifier, color = Wiz.Text, style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun Tile(title: String, icon: @Composable () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier, wide: Boolean = false) {
    val src = remember { MutableInteractionSource() }
    Row(
        modifier.heightIn(min = if (wide) 64.dp else 88.dp).pressScale(src, 0.96f).clip(RoundedCornerShape(18.dp)).background(Wiz.Panel)
            .border(1.dp, Wiz.Line, RoundedCornerShape(18.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Wiz.Accent.copy(alpha = 0.28f)), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(12.dp))
        Text(title, color = Wiz.Text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ResultRow(title: String, sub: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Column(
        modifier.fillMaxWidth().heightIn(min = 48.dp).pressScale(src, 0.97f).clip(RoundedCornerShape(14.dp)).background(Wiz.Panel)
            .border(1.dp, Wiz.Line, RoundedCornerShape(14.dp)).clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(title, color = Wiz.Text, style = MaterialTheme.typography.labelLarge, maxLines = if (sub == null) 2 else 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        if (sub != null) Text(sub, color = Wiz.Muted, style = com.wizlabs.photofit.ui.components.MonoStyle.copy(fontSize = 12.sp, lineHeight = 16.sp), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}
