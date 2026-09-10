package com.tdvorak.nothingmodes.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Open the system settings page for a specific runtime permission.
 *
 * Tries the permission-specific PermissionController page first, then the
 * app permissions list, then falls back to the app details page.
 */
fun openAppPermissionPage(
    context: Context,
    permission: String,
) {
    runCatching {
        context.startActivity(
            Intent("android.intent.action.MANAGE_APP_PERMISSION").apply {
                putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra("android.intent.extra.PERMISSION_NAME", permission)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent("android.intent.action.MANAGE_APP_PERMISSIONS").apply {
                    putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }.onFailure {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }
}
