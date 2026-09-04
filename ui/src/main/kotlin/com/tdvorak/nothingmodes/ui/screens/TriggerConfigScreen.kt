package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.ui.components.CustomTimePicker
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.ui.util.defaultTimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Composable
fun TriggerConfigScreen(
    triggerJson: String,
    navController: NavController,
) {
    val initial = remember(triggerJson) {
        runCatching { Json.decodeFromString<Trigger>(triggerJson) }.getOrNull()
            ?: Trigger.Time(cron = "0 12 * * *", tz = defaultTimeZone())
    }
    var trigger by remember { mutableStateOf(initial) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(
                title = "Configure Trigger",
                onBack = { navController.popBackStack() },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(NothingSpacing.md),
        ) {
            Spacer(modifier = Modifier.height(NothingSpacing.lg))

            if (trigger is Trigger.Time) {
                CustomTimePicker(
                    trigger = trigger as Trigger.Time,
                    onUpdate = { trigger = it },
                )
            } else {
                Text(
                    text = triggerDescription(trigger),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                )
                Spacer(modifier = Modifier.height(NothingSpacing.md))
                Text(
                    text = "This trigger type does not have additional configuration yet.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                )
            }

            Spacer(modifier = Modifier.height(NothingSpacing.xxxl))
            NothingPillButton(
                text = "Done",
                onClick = {
                    val result = Json.encodeToString(trigger)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("trigger_result", result)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
