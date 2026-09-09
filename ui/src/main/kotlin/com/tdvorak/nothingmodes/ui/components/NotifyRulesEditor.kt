package com.tdvorak.nothingmodes.ui.components

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tdvorak.nothingmodes.automation.notification.ModeNotificationHelper
import com.tdvorak.nothingmodes.engine.model.NotifyRule
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing

@Composable
fun NotifyRulesEditor(
    rules: List<NotifyRule>,
    onChange: (List<NotifyRule>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var blocked by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val check = {
            val appEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            val channelBlocked =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context
                        .getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                } else {
                    null
                }?.getNotificationChannel(ModeNotificationHelper.CHANNEL_ID)
                    ?.let { it.importance == android.app.NotificationManager.IMPORTANCE_NONE } == true
            blocked = !appEnabled || channelBlocked
        }
        check()
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) check()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = modifier) {
        if (rules.isNotEmpty() && blocked) {
            Text(
                text = "Notifications are blocked — these heads-ups cannot reach you. Tap to open settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontFamily = NothingFonts.mono(),
                modifier =
                    Modifier
                        .padding(bottom = NothingSpacing.sm)
                        .clickableNoRipple {
                            val intent =
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            context.startActivity(intent)
                        },
            )
        }

        if (rules.isEmpty()) {
            Text(
                text = "Off — mode runs silently.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
        } else {
            rules.forEachIndexed { index, rule ->
                NotifyRuleRow(
                    rule = rule,
                    onUpdate = { updated ->
                        onChange(rules.toMutableList().apply { set(index, updated) })
                    },
                    onRemove = { onChange(rules.filterIndexed { i, _ -> i != index }) },
                )
            }
        }

        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
        ) {
            // Only one BEFORE rule for now; replace if it already exists.
            val hasBefore = rules.any { it is NotifyRule.Before }
            val hasOnTrigger = rules.contains(NotifyRule.OnTrigger)
            val hasOnEnd = rules.contains(NotifyRule.OnEnd)

            NothingPillButton(
                text = "+ Before",
                onClick = {
                    val next = rules.toMutableList()
                    if (!hasBefore) next.add(NotifyRule.Before(15))
                    onChange(next)
                },
                modifier = Modifier.weight(1f),
            )
            NothingPillButton(
                text = "+ On trigger",
                onClick = { onChange(rules + NotifyRule.OnTrigger) },
                modifier = Modifier.weight(1f),
                enabled = !hasOnTrigger,
            )
            NothingPillButton(
                text = "+ On end",
                onClick = { onChange(rules + NotifyRule.OnEnd) },
                modifier = Modifier.weight(1f),
                enabled = !hasOnEnd,
            )
        }
    }
}

@Composable
private fun NotifyRuleRow(
    rule: NotifyRule,
    onUpdate: (NotifyRule) -> Unit,
    onRemove: () -> Unit,
) {
    NothingListRow(
        title =
            when (rule) {
                is NotifyRule.Before -> "Before it fires"
                is NotifyRule.OnTrigger -> "When it triggers"
                is NotifyRule.OnEnd -> "When it ends"
            },
        subtitle =
            when (rule) {
                is NotifyRule.Before -> "${rule.minutes} minutes before"
                else -> "One heads-up"
            },
        onClick = {},
        trailing = {
            if (rule is NotifyRule.Before) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var custom by remember(rule.minutes) { mutableStateOf(rule.minutes.toString()) }
                    NothingInput(
                        value = custom,
                        onValueChange = {
                            custom = it
                            it.toIntOrNull()?.coerceIn(1, 1440)?.let { m -> onUpdate(NotifyRule.Before(m)) }
                        },
                        label = "min",
                        modifier = Modifier.width(80.dp),
                    )
                    Spacer(modifier = Modifier.width(NothingSpacing.sm))
                    Text(
                        text = "[X]",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontFamily = NothingFonts.mono(),
                        modifier = Modifier.padding(start = NothingSpacing.sm).clickableNoRipple(onClick = { onRemove() }),
                    )
                }
            } else {
                Text(
                    text = "[X]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = NothingFonts.mono(),
                    modifier = Modifier.clickableNoRipple(onClick = { onRemove() }),
                )
            }
        },
    )
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            interactionSource = MutableInteractionSource(),
            indication = null,
            onClick = onClick,
        ),
    )
