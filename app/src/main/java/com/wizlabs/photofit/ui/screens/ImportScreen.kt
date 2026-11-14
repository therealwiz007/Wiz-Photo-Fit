package com.wizlabs.photofit.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.data.PresetRepo
import com.wizlabs.photofit.data.ShareCode
import com.wizlabs.photofit.ui.components.GhostButton
import com.wizlabs.photofit.ui.components.PrimaryButton
import com.wizlabs.photofit.ui.components.SectionLabel
import com.wizlabs.photofit.ui.components.SpecText
import com.wizlabs.photofit.ui.components.TopBar
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.components.WizCard

/** Paste a shared size code (or open a wizphotofit:// link) and add it to "Mine". */
@Composable
fun ImportScreen(repo: PresetRepo, initial: String?, onAdded: (String) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf(initial ?: "") }
    val parsed = remember(text) { if (text.isBlank()) null else runCatching { ShareCode.decode(text, PresetRepo.newId()) } }
    Column(Modifier.fillMaxSize().background(Wiz.Bg).navigationBarsPadding().imePadding()) {
        TopBar("Add a shared size", "Paste the code a friend or cyber café sent", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Field("Code (starts with WPF1.)", text) { text = it }
            Spacer(Modifier.height(10.dp))
            GhostButton("Paste", {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString()?.let { text = it }
            })
            Spacer(Modifier.height(16.dp))
            parsed?.onSuccess { p ->
                SectionLabel("Preview")
                WizCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(p.name, color = Wiz.Text, style = MaterialTheme.typography.titleLarge)
                        p.parts.forEach { Spacer(Modifier.height(6.dp)); Text("${it.label}: ${SpecText.line(it)}", color = Wiz.Muted, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }?.onFailure { Text(it.message ?: "Not a valid code", color = Wiz.Bad, style = MaterialTheme.typography.bodyMedium) }
        }
        PrimaryButton("Add to Mine", { parsed?.getOrNull()?.let { repo.saveMine(it); onAdded(it.id) } },
            Modifier.fillMaxWidth().padding(16.dp), icon = Icons.Default.Add, enabled = parsed?.isSuccess == true)
    }
}
