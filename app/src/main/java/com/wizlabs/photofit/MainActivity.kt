package com.wizlabs.photofit

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.wizlabs.photofit.ui.components.Wiz
import com.wizlabs.photofit.ui.components.WizTheme
import com.wizlabs.photofit.ui.motion.LocalBackPeek
import com.wizlabs.photofit.ui.motion.MotionIdle
import com.wizlabs.photofit.ui.motion.ProvideWizMotion
import com.wizlabs.photofit.ui.motion.WizBackHandler
import com.wizlabs.photofit.ui.motion.WizMotion
import com.wizlabs.photofit.ui.motion.backPeek
import com.wizlabs.photofit.ui.screens.AboutScreen
import com.wizlabs.photofit.ui.screens.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        val link = intent?.data?.toString()
        setContent { WizTheme { ProvideWizMotion { App(link) } } }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // singleTask: a share link tapped while the app is open restarts the UI at the Import screen
        if (intent.data != null) { setIntent(intent); recreate() }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) MotionIdle.onTouch()
        return super.dispatchTouchEvent(ev)
    }
}

@Composable
private fun App(link: String?) {
    val nav = remember { Nav() }
    LaunchedEffect(link) { if (link != null && link.startsWith(com.wizlabs.photofit.data.ShareCode.LINK)) nav.push(Screen.Import(link)) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { com.wizlabs.photofit.data.PresetRepo(ctx.applicationContext) }
    val peek = LocalBackPeek.current
    // Once the back transition has played, drop the peek so the next visit starts full size.
    LaunchedEffect(nav.top) { kotlinx.coroutines.delay(650); if (peek != null && !peek.dragging) peek.reset() }
    WizBackHandler(nav.top.route, enabled = nav.canPop) { nav.pop() }

    AnimatedContent(
        targetState = nav.top,
        transitionSpec = {
            val dir = if (nav.forward) 1 else -1
            (slideInHorizontally(tween(320, easing = WizMotion.Emphasized)) { dir * it * 12 / 100 } + fadeIn(tween(240))) togetherWith
                (slideOutHorizontally(tween(200)) { -dir * it / 12 } + fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.97f))
        },
        contentKey = { it.route },
        label = "screen",
        modifier = Modifier.fillMaxSize().background(Wiz.Bg),
    ) { s ->
        Box(Modifier.fillMaxSize().backPeek(s.route)) {
            when (s) {
                Screen.Home -> HomeScreen(open = nav::push)
                Screen.About -> AboutScreen(onBack = nav::pop)
                Screen.ExamList -> com.wizlabs.photofit.ui.screens.ExamListScreen(repo, nav::push, nav::pop)
                is Screen.ExamDetail -> com.wizlabs.photofit.ui.screens.ExamDetailScreen(repo, s.presetId, nav::push, nav::pop)
                is Screen.Editor -> com.wizlabs.photofit.ui.screens.EditorScreen(onDone = { nav.replace(Screen.Result) }, onBack = nav::pop)
                Screen.Result -> {
                    val next = com.wizlabs.photofit.data.Work.preset?.parts?.getOrNull(com.wizlabs.photofit.data.Work.partIndex + 1)
                    com.wizlabs.photofit.ui.screens.ResultScreen(
                        onNextPart = if (next != null) ({ nav.pop() }) else null,
                        onPrint = if (com.wizlabs.photofit.data.Work.preset?.group == com.wizlabs.photofit.data.IdPresets.GROUP || com.wizlabs.photofit.data.Work.part?.size?.phys != null) ({ nav.push(Screen.PrintSheet) }) else null,
                        onBack = nav::pop,
                    )
                }
                Screen.IdList -> com.wizlabs.photofit.ui.screens.IdListScreen(nav::push, nav::pop)
                Screen.PrintSheet -> com.wizlabs.photofit.ui.screens.PrintSheetScreen(nav::pop)
                Screen.Compress -> com.wizlabs.photofit.ui.screens.CompressScreen(onBack = nav::pop)
                Screen.Signature -> com.wizlabs.photofit.ui.screens.ExamDetailScreen(repo, com.wizlabs.photofit.ui.screens.GenericSignature.id, nav::push, nav::pop)
                Screen.Joiner -> com.wizlabs.photofit.ui.screens.JoinerScreen(onResult = { nav.push(Screen.Result) }, onBack = nav::pop)
                Screen.Saved -> com.wizlabs.photofit.ui.screens.SavedScreen(nav::pop)
                is Screen.Builder -> com.wizlabs.photofit.ui.screens.PresetBuilderScreen(repo, s.editId, s.copyFrom,
                    onSaved = { id -> nav.pop(); if (s.editId == null) nav.push(Screen.ExamDetail(id)) }, onBack = nav::pop)
                is Screen.Import -> com.wizlabs.photofit.ui.screens.ImportScreen(repo, s.code,
                    onAdded = { id -> nav.pop(); nav.push(Screen.ExamDetail(id)) }, onBack = nav::pop)
            }
        }
    }
}
