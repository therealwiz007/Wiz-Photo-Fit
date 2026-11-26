package com.wizlabs.photofit.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wizlabs.photofit.ui.components.TopBar
import com.wizlabs.photofit.ui.components.TrustStrip
import com.wizlabs.photofit.ui.components.Wiz

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AboutScreen(onBack: () -> Unit, onShowIntro: () -> Unit = {}) {
    val ctx = LocalContext.current
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }
    Column(Modifier.fillMaxSize().background(Wiz.Bg).navigationBarsPadding()) {
        TopBar("Wiz Photo Fit", "Version $version · Wiz Labs", onBack = onBack)
        com.wizlabs.photofit.ui.components.GhostButton("Show the intro again", onShowIntro, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        TrustStrip(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        // The bundled privacy policy (same file as the website copy). No network is used.
        AndroidView(
            factory = { c ->
                WebView(c).apply {
                    setBackgroundColor(if (Wiz.dark) 0xFF0F0F12.toInt() else 0xFFFAFAF7.toInt())
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = false
                    settings.blockNetworkLoads = true
                    loadUrl("file:///android_asset/privacy-policy.html")
                }
            },
            modifier = Modifier.fillMaxWidth().fillMaxSize(),
        )
    }
}
