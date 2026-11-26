package com.wizlabs.photofit.ui.screens

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wizlabs.photofit.Screen
import com.wizlabs.photofit.data.Preset
import com.wizlabs.photofit.data.PresetRepo
import com.wizlabs.photofit.ui.components.ConfidenceDot
import com.wizlabs.photofit.ui.components.GhostButton
import com.wizlabs.photofit.ui.components.InitialBadge
import com.wizlabs.photofit.ui.components.PrimaryButton
import com.wizlabs.photofit.ui.components.SpecText
import com.wizlabs.photofit.ui.components.TopBar
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.motion.pressScale

@Composable
fun ExamListScreen(repo: PresetRepo, open: (Screen) -> Unit, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf("all") }
    val list = remember(query, group, repo.mine) {
        repo.search(query).filter { group == "all" || (group == "mine" && it.mine) || it.group == group }
    }
    Box(Modifier.fillMaxSize().background(Wiz.Bg)) {
        Column(Modifier.fillMaxSize()) {
            TopBar("Exam Photo", "${repo.all().size} forms · pick yours", onBack = onBack)
            SearchBox(query, { query = it }, Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(10.dp))
            val chips = buildList { add("all" to "All"); if (repo.mine.isNotEmpty()) add("mine" to "Mine"); repo.groups.forEach { add(it.id to it.label) } }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chips, key = { it.first }) { (id, label) -> Chip(label, id == group) { group = id } }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (list.isEmpty()) item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No exam found", color = Wiz.Text, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text("Make your own size: enter the width, height and KB from your form.", color = Wiz.Muted, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        GhostButton("Make your own size", { open(Screen.Builder()) }, icon = Icons.Default.Add)
                    }
                }
                items(list, key = { it.id }, contentType = { "preset" }) { p ->
                    Box(Modifier.animateItem(fadeInSpec = tween(180), placementSpec = spring(1f, 400f, IntOffset.VisibilityThreshold), fadeOutSpec = tween(120))) {
                        PresetRow(p, repo.groups.firstOrNull { it.id == p.group }?.label ?: if (p.mine) "Mine" else "") { open(Screen.ExamDetail(p.id)) }
                    }
                }
            }
        }
        PrimaryButton("Custom size", { open(Screen.Builder()) }, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(18.dp), icon = Icons.Default.Add)
    }
}

@Composable
fun SearchBox(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, hint: String = "Search exam, e.g. SSC, NEET, PAN") {
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(16.dp)).background(Wiz.Panel)
            .border(1.dp, Wiz.Line, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, null, tint = Wiz.AccentInk)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, color = Wiz.Muted, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            BasicTextField(value, onChange, singleLine = true, textStyle = TextStyle(color = Wiz.Text, fontSize = 16.sp),
                cursorBrush = SolidColor(Wiz.Text), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), modifier = Modifier.fillMaxWidth())
        }
        if (value.isNotEmpty()) Icon(Icons.Default.Close, "Clear", tint = Wiz.Muted, modifier = Modifier.size(22.dp).clickable { onChange("") })
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier.heightIn(min = 40.dp).pressScale(src, 0.94f).clip(RoundedCornerShape(14.dp))
            .background(if (selected) Wiz.Accent else Wiz.Panel)
            .border(1.dp, if (selected) Wiz.Accent else Wiz.Line, RoundedCornerShape(14.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (selected) Wiz.OnAccent else Wiz.Text, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) }
}

@Composable
private fun PresetRow(p: Preset, groupLabel: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val photo = p.parts.first()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 76.dp).pressScale(src, 0.97f).clip(RoundedCornerShape(18.dp)).background(Wiz.Panel)
            .border(1.dp, if (p.mine) Wiz.AccentInk.copy(alpha = 0.4f) else Wiz.Line, RoundedCornerShape(18.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialBadge(p.name)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, color = Wiz.Text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConfidenceDot(p.confidence); Spacer(Modifier.width(6.dp))
                Text("$groupLabel · ${p.parts.joinToString(", ") { it.label.substringBefore(' ') }}", color = Wiz.Muted,
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(SpecText.kb(photo), color = Wiz.AccentInk, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}
