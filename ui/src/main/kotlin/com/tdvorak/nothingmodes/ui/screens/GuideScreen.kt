package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingCardLarge
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar

/**
 * First-run "how to use" guide: what a mode is, the 4-step first automation,
 * what works without any permissions vs what needs Shizuku, and starter
 * recipes. Reachable from setup onboarding, Settings, and the empty home state.
 */
@Composable
fun GuideScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = NothingSpacing.md),
            ) {
                Spacer(modifier = Modifier.height(NothingSpacing.xl))

                NothingSectionHeader(text = "How modes work")
                NothingCardLarge {
                    GuideRow(
                        label = "TRIGGER",
                        description = "What starts the mode — time, Wi-Fi, Bluetooth, a notification, charging, or an app opening. No trigger means the mode runs only when you start it by hand.",
                    )
                    GuideRow(
                        label = "CONDITION",
                        description = "Optional extra checks. The mode runs only when the trigger fires AND every condition agrees.",
                    )
                    GuideRow(
                        label = "ACTION",
                        description = "What happens — DND, ringer, brightness, screen timeout, flashlight, open an app, Glyph animation, and more.",
                    )
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xxl))

                NothingSectionHeader(text = "Your first mode in 4 steps")
                NothingCardLarge {
                    GuideStep(
                        step = 1,
                        text = "Tap + on the home screen and name the mode.",
                    )
                    GuideStep(
                        step = 2,
                        text = "Add a trigger — or leave it empty to run the mode yourself.",
                    )
                    GuideStep(
                        step = 3,
                        text = "Add one simple action. DND Total is a good first try.",
                    )
                    GuideStep(
                        step = 4,
                        text = "Save, then run it from the mode toggle or a Quick Settings tile.",
                    )
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xxl))

                NothingSectionHeader(text = "What works on your phone")
                NothingCard {
                    NothingListRow(
                        title = "Never needs setup",
                        subtitle = "Modes you run by hand and Quick Settings tiles. Flip the toggle, done.",
                    )
                    NothingListRow(
                        title = "Needs a one-time permission",
                        subtitle = "DND, brightness, screen timeout, ringer volume. Granted in Settings > Setup Guide.",
                    )
                    NothingListRow(
                        title = "Needs Shizuku (optional)",
                        subtitle = "Dark mode, mobile data, Wi-Fi and Bluetooth toggles — things Android blocks for normal apps. Shizuku is a free companion app; skip it if you don't need those.",
                    )
                    NothingListRow(
                        title = "Glyph — Nothing phones only",
                        subtitle = "Glyph actions and the editor do nothing on phones without Glyph lights.",
                    )
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xxl))

                NothingSectionHeader(text = "Starter modes — no Shizuku needed")
                NothingCard {
                    NothingListRow(
                        title = "Quick silence",
                        subtitle = "No trigger. Actions: DND Total, ringer volume 0. Run it from the Quick Settings tile when you enter a meeting.",
                    )
                    NothingListRow(
                        title = "Work focus",
                        subtitle = "Trigger: Wi-Fi named your work network. Actions: DND, launch your calendar.",
                    )
                    NothingListRow(
                        title = "Bedtime",
                        subtitle = "Trigger: time every night. Actions: DND Total, brightness 20%, screen timeout 30 s.",
                    )
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xxl))

                NothingSectionHeader(text = "Where things live")
                NothingCard {
                    NothingListRow(
                        title = "Templates",
                        subtitle = "Prebuilt modes to install with one tap — Dashboard icon in the home screen top bar.",
                    )
                    NothingListRow(
                        title = "Execution log",
                        subtitle = "Every trigger fire and action the engine ran, with timestamps.",
                    )
                    NothingListRow(
                        title = "Setup Guide",
                        subtitle = "Permissions and Shizuku status — Settings > More > Setup Guide.",
                    )
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xxxl))
            }

            NothingTopBar(
                title = "Guide",
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun GuideRow(
    label: String,
    description: String,
) {
    NothingListRow(
        title = label,
        subtitle = description,
        leading = {
            NothingIconCircle(size = 40f, accent = true) {
                Text(
                    text = label.take(1),
                    style = MaterialTheme.typography.labelMedium,
                    color = NothingColors.accent,
                    fontFamily = NothingFonts.mono(),
                )
            }
        },
    )
}

@Composable
private fun GuideStep(
    step: Int,
    text: String,
) {
    NothingListRow(
        title = text,
        leading = {
            NothingIconCircle(size = 40f) {
                Text(
                    text = String.format("%02d", step),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
            }
        },
    )
}
