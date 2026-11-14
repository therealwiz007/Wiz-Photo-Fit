package com.wizlabs.photofit.engine

import android.content.Context
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * State of the on-device face + background models that Google Play services installs once.
 * Drives the "getting ready" card with real download progress.
 */
object ModelStatus {
    sealed interface State {
        data object Unknown : State
        data object Checking : State
        data object Ready : State
        data class Downloading(val progress: Float?, val bytes: Long, val total: Long) : State
        data class Installing(val note: String = "Setting up…") : State
        data class Failed(val reason: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Unknown)
    val state: StateFlow<State> = _state
    private var started = false

    private val apis: List<com.google.android.gms.common.api.OptionalModuleApi> by lazy {
        listOf<com.google.android.gms.common.api.OptionalModuleApi>(
            SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundConfidenceMask().build()),
            FaceDetection.getClient(),
        )
    }

    /** Checks the models and, if missing, asks Play services to install them, reporting progress. Safe to call often. */
    fun ensure(ctx: Context) {
        if (_state.value is State.Ready || (started && _state.value !is State.Failed)) return
        started = true
        _state.value = State.Checking
        val client = ModuleInstall.getClient(ctx.applicationContext)
        runCatching {
            client.areModulesAvailable(*apis.toTypedArray()).addOnSuccessListener { r ->
                if (r.areModulesAvailable()) { _state.value = State.Ready; return@addOnSuccessListener }
                val listener = object : InstallStatusListener {
                    override fun onInstallStatusUpdated(u: ModuleInstallStatusUpdate) {
                        val p = u.progressInfo
                        _state.value = when (u.installState) {
                            InstallState.STATE_COMPLETED -> State.Ready.also { client.unregisterListener(this) }
                            InstallState.STATE_FAILED, InstallState.STATE_CANCELED -> State.Failed("Google Play services could not get it. Check the internet once and try again.").also { client.unregisterListener(this) }
                            InstallState.STATE_DOWNLOADING, InstallState.STATE_DOWNLOAD_PAUSED, InstallState.STATE_PENDING ->
                                State.Downloading(p?.let { if (it.totalBytesToDownload > 0) it.bytesDownloaded.toFloat() / it.totalBytesToDownload else null }, p?.bytesDownloaded ?: 0, p?.totalBytesToDownload ?: 0)
                            InstallState.STATE_INSTALLING -> State.Installing()
                            else -> _state.value
                        }
                    }
                }
                client.installModules(ModuleInstallRequest.newBuilder().apply { apis.forEach { addApi(it) } }.setListener(listener).build())
                    .addOnSuccessListener { if (it.areModulesAlreadyInstalled()) _state.value = State.Ready else if (_state.value is State.Checking) _state.value = State.Downloading(null, 0, 0) }
                    .addOnFailureListener { _state.value = State.Failed("Needs Google Play services and the internet once to get the face and background tools.") }
            }.addOnFailureListener { _state.value = State.Failed("Needs Google Play services and the internet once to get the face and background tools.") }
        }.onFailure { _state.value = State.Failed("Google Play services is not available on this phone.") }
    }

    fun markReady() { _state.value = State.Ready }
    fun retry(ctx: Context) { started = false; ensure(ctx) }
}
