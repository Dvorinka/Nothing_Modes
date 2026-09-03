package com.tdvorak.nothingmodes.automation.lifecycle

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit

/**
 * Polls UsageStatsManager to detect foreground app changes.
 * Dispatches AppForegroundChanged trigger events to AutomationService.
 *
 * Requires PACKAGE_USAGE_STATS permission (granted via Settings > Usage Access).
 * Does NOT require a foreground service — polling runs via Handler postDelayed.
 */
class UsageStatsMonitor(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private var lastForegroundPackage: String? = null
    private var polling = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!polling) return
            checkForegroundApp()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    fun start() {
        if (polling) return
        if (usageStatsManager == null) {
            Log.w(TAG, "UsageStatsManager not available")
            return
        }
        polling = true
        handler.post(pollRunnable)
        Log.i(TAG, "Usage stats monitoring started")
    }

    fun stop() {
        polling = false
        handler.removeCallbacks(pollRunnable)
        Log.i(TAG, "Usage stats monitoring stopped")
    }

    private fun checkForegroundApp() {
        val now = System.currentTimeMillis()
        val stats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            now - TimeUnit.SECONDS.toMillis(5),
            now,
        ) ?: return

        val foreground = stats
            .filter { it.lastTimeStamp > now - TimeUnit.SECONDS.toMillis(5) }
            .maxByOrNull { it.lastTimeStamp }
            ?: return

        val pkg = foreground.packageName
        if (pkg != lastForegroundPackage) {
            Log.d(TAG, "Foreground app changed: $lastForegroundPackage -> $pkg")
            lastForegroundPackage = pkg

            val intent = Intent(context, AutomationService::class.java).apply {
                action = AutomationService.ACTION_APP_FOREGROUND
                putExtra(EXTRA_PACKAGE, pkg)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    companion object {
        private const val TAG = "UsageStatsMonitor"
        private const val POLL_INTERVAL_MS = 5000L
        const val EXTRA_PACKAGE = "foreground_pkg"
    }
}
