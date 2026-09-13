package com.tdvorak.nothingmodes.ui.util

import androidx.navigation.NavController

/**
 * Pop the back stack; if there is nothing to pop (cold start on a deep
 * destination, restored task, single-entry stack), land on [fallbackRoute]
 * instead of finishing the activity and dropping to the launcher.
 */
fun NavController.popBackStackOr(fallbackRoute: String) {
    if (!popBackStack()) {
        navigate(fallbackRoute) {
            popUpTo(0) { inclusive = true }
        }
    }
}
