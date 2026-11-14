package com.wizlabs.photofit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wizlabs.photofit.ui.components.TopBar
import com.wizlabs.photofit.ui.components.Wiz

/** Placeholder for screens built in later steps. */
@Composable
fun StubScreen(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Wiz.Bg)) {
        TopBar(title, onBack = onBack)
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Coming in the next build", color = Wiz.Muted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
