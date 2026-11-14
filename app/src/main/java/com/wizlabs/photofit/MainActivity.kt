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
import com.wizlabs.photofit.ui.screens.StubScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent { WizTheme { ProvideWizMotion { App() } } }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) MotionIdle.onTouch()
        return super.dispatchTouchEvent(ev)
    }
}

@Composable
private fun App() {
    val nav = remember { Nav() }
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
                        onBack = nav::pop,
                    )
                }
                Screen.IdList -> StubScreen("ID / Passport", nav::pop)
                Screen.Compress -> StubScreen("Compress", nav::pop)
                Screen.Signature -> StubScreen("Signature", nav::pop)
                Screen.Joiner -> StubScreen("Join photo + signature", nav::pop)
                Screen.Saved -> StubScreen("Saved", nav::pop)
                is Screen.Builder -> StubScreen("Custom size", nav::pop)
                is Screen.Import -> StubScreen("Import preset", nav::pop)
            }
        }
    }
}
