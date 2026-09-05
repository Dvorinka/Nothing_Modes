package com.tdvorak.nothingmodes.update

data class UpdateInfo(
    val versionName: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkUrl: String,
) {
    val displayVersion: String
        get() = versionName.removePrefix("v")
}
