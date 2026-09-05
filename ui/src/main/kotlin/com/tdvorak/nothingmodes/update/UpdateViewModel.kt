package com.tdvorak.nothingmodes.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdvorak.nothingmodes.engine.runtime.FeatureFlags
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val updateManager: UpdateManager,
    ) : ViewModel() {
        private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
        val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

        private val _updateStatus = MutableStateFlow(UpdateStatus.IDLE)
        val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

        init {
            viewModelScope.launch {
                updateManager.downloadCompleted.collect { _updateStatus.value = UpdateStatus.DOWNLOADED }
            }
        }

        fun checkForUpdate() {
            if (!FeatureFlags.enableInAppUpdates) {
                _updateStatus.value = UpdateStatus.UP_TO_DATE
                return
            }
            viewModelScope.launch {
                _updateStatus.value = UpdateStatus.CHECKING
                val info = updateManager.checkForUpdate()
                _updateInfo.value = info
                _updateStatus.value = if (info != null) UpdateStatus.AVAILABLE else UpdateStatus.UP_TO_DATE
            }
        }

        fun startDownload(info: UpdateInfo) {
            if (!FeatureFlags.enableInAppUpdates) return
            if (!updateManager.canInstallPackages()) {
                _updateStatus.value = UpdateStatus.NEEDS_PERMISSION
                return
            }
            _updateStatus.value = UpdateStatus.DOWNLOADING
            updateManager.startDownload(info)
        }

        fun openInstallPermissionSettings() {
            if (!FeatureFlags.enableInAppUpdates) return
            updateManager.openInstallPermissionSettings()
        }

        fun installIfReady() {
            if (!FeatureFlags.enableInAppUpdates) return
            val downloadId = updateManager.activeDownloadId ?: return
            updateManager.installUpdate(downloadId)
        }

        fun clearUpdate() {
            _updateInfo.value = null
            _updateStatus.value = UpdateStatus.IDLE
        }

        fun currentVersionName(): String {
            val packageInfo =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        android.content.pm.PackageManager.PackageInfoFlags
                            .of(0),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            return packageInfo?.versionName ?: ""
        }

        fun hasInstallPermission(): Boolean = updateManager.canInstallPackages()
    }
