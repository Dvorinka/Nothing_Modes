package com.dvoranka.nothingmodes.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dvoranka.nothingmodes.ui.screens.AutomationDetailScreen
import com.dvoranka.nothingmodes.ui.screens.AutomationListScreen
import com.dvoranka.nothingmodes.ui.screens.ExecutionLogScreen
import com.dvoranka.nothingmodes.ui.screens.SettingsScreen

object Routes {
    const val AUTOMATION_LIST = "automations"
    const val AUTOMATION_DETAIL = "automation/{id}"
    const val EXECUTION_LOG = "log"
    const val SETTINGS = "settings"

    fun automationDetail(id: String) = "automation/$id"
}

@Composable
fun NothingModesNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.AUTOMATION_LIST,
    ) {
        composable(Routes.AUTOMATION_LIST) {
            AutomationListScreen(
                onAutomationClick = { id -> navController.navigate(Routes.automationDetail(id)) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onLogClick = { navController.navigate(Routes.EXECUTION_LOG) },
            )
        }

        composable(
            route = Routes.AUTOMATION_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            AutomationDetailScreen(
                automationId = id,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.EXECUTION_LOG) {
            ExecutionLogScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
