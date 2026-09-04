package com.tdvorak.nothingmodes.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tdvorak.nothingmodes.ui.screens.AutomationDetailScreen
import com.tdvorak.nothingmodes.ui.screens.AutomationListScreen
import com.tdvorak.nothingmodes.ui.screens.CustomAutomationBuilderScreen
import com.tdvorak.nothingmodes.ui.screens.ExecutionLogScreen
import com.tdvorak.nothingmodes.ui.screens.GlyphPreviewScreen
import com.tdvorak.nothingmodes.ui.screens.OnboardingScreen
import com.tdvorak.nothingmodes.ui.screens.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val AUTOMATION_LIST = "automations"
    const val AUTOMATION_DETAIL = "automation/{id}"
    const val CREATE_AUTOMATION = "create"
    const val EDIT_AUTOMATION = "edit/{id}"
    const val CUSTOM_BUILDER = "builder"
    const val CUSTOM_BUILDER_EDIT = "builder/edit/{id}"
    const val EXECUTION_LOG = "log"
    const val GLYPH_PREVIEW = "glyph_preview"
    const val SETTINGS = "settings"

    fun automationDetail(id: String) = "automation/$id"
    fun editAutomation(id: String) = "edit/$id"
    fun builderEdit(id: String) = "builder/edit/$id"
}

@Composable
fun NothingModesNavHost() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("nothing_modes", android.content.Context.MODE_PRIVATE) }
    val onboardingCompleted = remember { prefs.getBoolean("onboarding_completed", false) }
    val startDestination = if (onboardingCompleted) Routes.AUTOMATION_LIST else Routes.ONBOARDING

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    prefs.edit().putBoolean("onboarding_completed", true).apply()
                    navController.popBackStack(Routes.AUTOMATION_LIST, inclusive = false)
                },
            )
        }

        composable(Routes.AUTOMATION_LIST) {
            AutomationListScreen(
                onAutomationClick = { id -> navController.navigate(Routes.automationDetail(id)) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onLogClick = { navController.navigate(Routes.EXECUTION_LOG) },
                onCreateClick = { navController.navigate(Routes.CREATE_AUTOMATION) },
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
                onEdit = { navController.navigate(Routes.builderEdit(id)) },
            )
        }

        composable(Routes.CREATE_AUTOMATION) {
            CustomAutomationBuilderScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack(Routes.AUTOMATION_LIST, inclusive = false)
                },
            )
        }

        composable(Routes.CUSTOM_BUILDER) {
            CustomAutomationBuilderScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack(Routes.AUTOMATION_LIST, inclusive = false)
                },
            )
        }

        composable(
            route = Routes.CUSTOM_BUILDER_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            CustomAutomationBuilderScreen(
                automationId = id,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack(Routes.AUTOMATION_LIST, inclusive = false)
                },
            )
        }

        composable(
            route = Routes.EDIT_AUTOMATION,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            CustomAutomationBuilderScreen(
                automationId = id,
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack(Routes.AUTOMATION_LIST, inclusive = false)
                },
            )
        }

        composable(Routes.EXECUTION_LOG) {
            ExecutionLogScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.GLYPH_PREVIEW) {
            GlyphPreviewScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOnboarding = { navController.navigate(Routes.ONBOARDING) },
                onGlyphPreview = { navController.navigate(Routes.GLYPH_PREVIEW) },
            )
        }
    }
}
