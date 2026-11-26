package com.wizlabs.photofit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.Screen
import com.wizlabs.photofit.ui.components.GlyphCompress
import com.wizlabs.photofit.ui.components.GlyphExam
import com.wizlabs.photofit.ui.components.GlyphJoin
import com.wizlabs.photofit.ui.components.GlyphPassport
import com.wizlabs.photofit.ui.components.GlyphSaved
import com.wizlabs.photofit.ui.components.GlyphSign
import com.wizlabs.photofit.ui.components.HeroArt
import com.wizlabs.photofit.ui.components.MiniTile
import com.wizlabs.photofit.ui.components.RoundIconButton
import com.wizlabs.photofit.ui.components.SectionLabel
import com.wizlabs.photofit.ui.components.ToolTile
import com.wizlabs.photofit.ui.components.TrustStrip
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.components.WizCard
import com.wizlabs.photofit.ui.motion.enterRise

@Composable
fun HomeScreen(open: (Screen) -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Wiz.Bg).verticalScroll(rememberScrollState())
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        WizCard(Modifier.fillMaxWidth().enterRise(0), corner = 28.dp) {
            Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("WIZ LABS", color = Wiz.AccentInk, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                    Text("Photo Fit", color = Wiz.Text, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Exact size, exact KB.\nFor exam forms, ID and passport.", color = Wiz.Muted, style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    RoundIconButton(Icons.Outlined.Info, "About", { open(Screen.About) }, tint = Wiz.Muted, size = 40.dp)
                    HeroArt()
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        TrustStrip(Modifier.enterRise(1))
        Spacer(Modifier.height(14.dp))
        SectionLabel("Tools")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile("Exam Photo", "Photo + signature in exact KB", { GlyphExam(Wiz.AccentInk) }, { open(Screen.ExamList) }, Modifier.enterRise(2))
            ToolTile("ID / Passport", "Passport, visa and PAN photos + print sheet", { GlyphPassport(Wiz.AccentInk) }, { open(Screen.IdList) }, Modifier.enterRise(3))
            ToolTile("Compress", "Many photos to a target KB at once", { GlyphCompress(Wiz.AccentInk) }, { open(Screen.Compress) }, Modifier.enterRise(4))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().enterRise(5), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniTile("Signature", { GlyphSign(Wiz.AccentInk) }, { open(Screen.Signature) }, Modifier.weight(1f))
            MiniTile("Join", { GlyphJoin(Wiz.AccentInk) }, { open(Screen.Joiner) }, Modifier.weight(1f))
            MiniTile("Saved", { GlyphSaved(Wiz.AccentInk) }, { open(Screen.Saved) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
    }
}
