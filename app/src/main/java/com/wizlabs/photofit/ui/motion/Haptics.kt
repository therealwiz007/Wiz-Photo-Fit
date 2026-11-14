package com.wizlabs.photofit.ui.motion

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/** Haptics through the View system: needs no VIBRATE permission and respects the user's touch-feedback setting. */
class Haptics(private val view: View) {
    fun tap() { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
    fun tick() { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    fun confirm() {
        view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
    }
    fun reject() {
        view.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)
    }
}

@Composable
fun rememberHaptics(): Haptics { val v = LocalView.current; return remember(v) { Haptics(v) } }
