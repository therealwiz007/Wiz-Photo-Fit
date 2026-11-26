package com.wizlabs.photofit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.engine.SnipAction
import com.wizlabs.photofit.engine.SnipExpression
import com.wizlabs.photofit.ui.components.GhostButton
import com.wizlabs.photofit.ui.components.PrimaryButton
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.mascot.Snip

private data class Card(val title: String, val body: String, val face: SnipExpression, val action: SnipAction)

private val cards = listOf(
    Card("Pick your exam", "Search SSC, NEET, UPSC, bank forms, passport and more. Sizes come from public notices.", SnipExpression.HAPPY, SnipAction.WAVE),
    Card("Crop and check", "Drag the guides to frame your face or signature. Photo Fit checks every rule for you.", SnipExpression.NEUTRAL, SnipAction.NONE),
    Card("Save exactly in KB", "Your file lands on the exact pixels and inside the KB range. Everything stays on your phone.", SnipExpression.PROUD, SnipAction.FLOURISH),
)

/** First-run guide: three skippable cards with Snip. Shown once; reachable again from About. */
@Composable
fun IntroGuide(onDone: () -> Unit) {
    var i by rememberSaveable { mutableIntStateOf(0) }
    val c = cards[i]
    Column(
        Modifier.fillMaxSize().background(Wiz.Bg).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { GhostButton("Skip", onDone) }
        Spacer(Modifier.weight(1f))
        Snip(c.face, c.action, 140.dp)
        Spacer(Modifier.height(24.dp))
        Text(c.title, color = Wiz.Text, style = MaterialTheme.typography.displayLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(c.body, color = Wiz.Muted, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(cards.size) { n -> Box(Modifier.size(8.dp).clip(CircleShape).background(if (n == i) Wiz.AccentInk else Wiz.Line)) }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(if (i == cards.lastIndex) "Start" else "Next", { if (i == cards.lastIndex) onDone() else i++ }, Modifier.fillMaxWidth())
    }
}
