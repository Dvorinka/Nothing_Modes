package com.tdvorak.nothingmodes.update

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Play Store in-app update flow (play flavor only).
 * Renders Google's flexible update sheet; the download then continues in the
 * background, so [installListener] watches for DOWNLOADED and calls
 * completeUpdate() — that is what shows the install overlay and restarts the
 * app. Without it a finished download sat unnoticed until the next cold start.
 */
class PlatformInAppUpdate(
    activity: ComponentActivity,
) {
    private val manager = AppUpdateManagerFactory.create(activity)

    private val installListener = InstallStateUpdatedListener(::onInstallState)

    // Must be registered before the activity is STARTED; constructing this
    // class as an activity field satisfies that.
    private val launcher: ActivityResultLauncher<IntentSenderRequest> =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            // Sheet declined or failed: no download is running, nothing left
            // for the listener to observe.
            if (result.resultCode != Activity.RESULT_OK) {
                manager.unregisterListener(installListener)
            }
        }

    fun check() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            when (info.installStatus()) {
                InstallStatus.DOWNLOADED -> manager.completeUpdate()
                InstallStatus.DOWNLOADING -> manager.registerListener(installListener)
                else -> {
                    if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                    ) {
                        manager.registerListener(installListener)
                        manager.startUpdateFlowForResult(
                            info,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        )
                    }
                }
            }
        }
    }

    /** Applies a previously downloaded flexible update when the app resumes. */
    fun onResume() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            when (info.installStatus()) {
                InstallStatus.DOWNLOADED -> manager.completeUpdate()
                InstallStatus.DOWNLOADING -> manager.registerListener(installListener)
                else -> Unit
            }
        }
    }

    private fun onInstallState(state: InstallState) {
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                manager.unregisterListener(installListener)
                manager.completeUpdate()
            }
            // Play paused for user consent (e.g. a large download on metered
            // data); relaunch the sheet so it can resume instead of stalling.
            InstallStatus.REQUIRES_UI_INTENT ->
                manager.appUpdateInfo.addOnSuccessListener { info ->
                    manager.startUpdateFlowForResult(
                        info,
                        launcher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    )
                }
            InstallStatus.FAILED,
            InstallStatus.CANCELED,
            InstallStatus.INSTALLED,
            -> manager.unregisterListener(installListener)
            else -> Unit
        }
    }
}
