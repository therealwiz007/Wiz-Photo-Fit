package com.wizlabs.photofit.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Non-blocking Play In-App Updates card (flexible). Shown on every launch until the user updates.
 * Never blocks the app: the user can tap "Later". No ads, no network calls from the app itself.
 */
private const val UPDATE_REQUEST_CODE = 4107

@Composable
fun UpdatePrompt(modifier: Modifier = Modifier) {
    val activity = LocalContext.current as? Activity ?: return
    val manager = remember { AppUpdateManagerFactory.create(activity) }
    var available by remember { mutableStateOf(false) }
    var downloaded by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(false) }
    var pendingInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    DisposableEffect(manager) {
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) downloaded = true
        }
        manager.registerListener(listener)
        onDispose { manager.unregisterListener(listener) }
    }

    LaunchedEffect(Unit) {
        manager.appUpdateInfo.addOnSuccessListener { info: AppUpdateInfo ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) downloaded = true
            else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                available = true
                pendingInfo = info
            }
        }
    }

    if (dismissed || (!available && !downloaded)) return

    Surface(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        color = Wiz.Panel,
        tonalElevation = 2.dp,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {

            Text(
                if (downloaded) "Update ready. Restart to finish." else "A new version of Wiz Photo Fit is available.",
                modifier = Modifier.weight(1f),
                color = Wiz.Text,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = { dismissed = true }) { Text("Later", color = Wiz.Muted) }
            TextButton(onClick = {
                if (downloaded) manager.completeUpdate()
                else pendingInfo?.let { manager.startUpdateFlowForResult(it, AppUpdateType.FLEXIBLE, activity, UPDATE_REQUEST_CODE) }
            }) { Text(if (downloaded) "Restart" else "Update", color = Wiz.AccentInk) }
        }
    }
}
