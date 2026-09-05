package com.tdvorak.nothingmodes.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("versionName") private val currentVersionName: String,
) {

    private val downloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    /** The ID of the in-flight update download, if any. Shared with [UpdateDownloadReceiver]. */
    @Volatile
    var activeDownloadId: Long? = null
        private set

    /** Emits the download ID when the active update download completes. */
    private val _downloadCompleted = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val downloadCompleted: SharedFlow<Long> = _downloadCompleted.asSharedFlow()

    private val json = Json { ignoreUnknownKeys = true }

    private val authority = "${context.packageName}.fileprovider"

    /** GitHub repository used for release checks. */
    private val repoOwner = "Dvorinka"
    private val repoName = "Nothing_Modes"

    /** Fetch the latest GitHub release and return update info if it is newer than the current version. */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val latest = fetchLatestRelease() ?: return@withContext null
        val newVersion = parseVersion(latest.tagName)
        val currentVersion = parseVersion(currentVersionName)
        if (currentVersion != null && newVersion != null && !isNewer(currentVersion, newVersion)) {
            return@withContext null
        }

        val apkAsset = latest.assets.find { it.name.endsWith(".apk", ignoreCase = true) }
            ?: return@withContext null

        return@withContext UpdateInfo(
            versionName = latest.tagName,
            releaseName = latest.name,
            releaseNotes = latest.body,
            apkUrl = apkAsset.browserDownloadUrl,
        )
    }

    /** Start a DownloadManager download for the update APK. Returns the download ID. */
    fun startDownload(update: UpdateInfo): Long {
        clearPreviousDownload()
        val request = DownloadManager.Request(Uri.parse(update.apkUrl)).apply {
            setTitle("Nothing Modes ${update.displayVersion}")
            setDescription("Downloading update...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        return downloadManager.enqueue(request).also { activeDownloadId = it }
    }

    /** Called by [UpdateDownloadReceiver] when a download finishes. Only acts on our own download. */
    fun onDownloadComplete(downloadId: Long) {
        if (downloadId != activeDownloadId) return
        _downloadCompleted.tryEmit(downloadId)
        installUpdate(downloadId)
    }

    /** Install the downloaded APK using a FileProvider content URI. */
    fun installUpdate(downloadId: Long) {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        val localUri = cursor.use {
            if (!it.moveToFirst()) return
            val index = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            if (index < 0) return
            Uri.parse(it.getString(index))
        }
        val file = File(localUri.path ?: return)
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = contentUri
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }
        } else {
            @Suppress("DEPRECATION")
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Open system settings so the user can grant the install-unknown-apps permission. */
    fun openInstallPermissionSettings() {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Whether the app can request package installs. */
    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    private suspend fun fetchLatestRelease(): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Nothing-Modes/$currentVersionName")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            val response = connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            json.decodeFromString(GitHubRelease.serializer(), response)
        } catch (e: Exception) {
            null
        }
    }

    private fun clearPreviousDownload() {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, "update.apk")
        if (file.exists()) file.delete()
    }

    private fun parseVersion(version: String): List<Int>? {
        val cleaned = version.removePrefix("v").removePrefix("V")
        val parts = cleaned.split(".")
            .take(3)
            .mapNotNull { it.toIntOrNull() }
        return if (parts.isNotEmpty()) parts else null
    }

    private fun isNewer(current: List<Int>, latest: List<Int>): Boolean {
        for (i in 0 until maxOf(current.size, latest.size)) {
            val c = current.getOrElse(i) { 0 }
            val l = latest.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
