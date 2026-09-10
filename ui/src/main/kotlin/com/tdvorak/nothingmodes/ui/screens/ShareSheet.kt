package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tdvorak.nothingmodes.data.community.CommunityApi
import com.tdvorak.nothingmodes.engine.model.CreatorProfile
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingPrimaryButton
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing

/**
 * Publish-to-community sheet shared by templates and glyph designs.
 * Submissions go to admin review; the handle is public, email stays private.
 */
@Composable
fun ShareSheet(
    initialTitle: String,
    initialDescription: String,
    profile: CreatorProfile,
    sharing: Boolean,
    result: CommunityApi.SubmitResult?,
    onPublish: (title: String, description: String, handle: String, email: String, github: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription) }
    var displayName by remember { mutableStateOf(profile.displayName) }
    var handle by remember { mutableStateOf(profile.handle) }
    var email by remember { mutableStateOf(profile.email) }
    var github by remember { mutableStateOf(profile.github) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(NothingSpacing.lg)
                .navigationBarsPadding(),
    ) {
        NothingLabel(text = "Publish to community library")
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        Text(
            text = "Goes to review before it's public. Your handle is shown as author; email stays private.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(NothingSpacing.md))

        NothingInput(value = title, onValueChange = { title = it }, label = "Title")
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = description,
            onValueChange = { description = it },
            label = "Description (optional)",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = displayName,
            onValueChange = { displayName = it },
            label = "Display name",
            placeholder = "Your name or alias",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = handle,
            onValueChange = { handle = it },
            label = "Handle",
            placeholder = "@you",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = email,
            onValueChange = { email = it },
            label = "Email (private — decision notice)",
            placeholder = "optional",
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = github,
            onValueChange = { github = it },
            label = "GitHub (optional)",
            placeholder = "username or URL",
        )

        when (result) {
            is CommunityApi.SubmitResult.Queued -> {
                Spacer(modifier = Modifier.height(NothingSpacing.md))
                Text(
                    text = "Submitted — pending review.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                result.findings.filter { it.severity != "info" }.forEach { f ->
                    Text(
                        text = "• ${f.detail}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                    )
                }
            }
            is CommunityApi.SubmitResult.Rejected -> {
                Spacer(modifier = Modifier.height(NothingSpacing.md))
                NothingLabel(text = "Rejected by safety check")
                result.findings.forEach { f ->
                    Text(
                        text = "• ${f.detail}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingColors.accent,
                        fontFamily = NothingFonts.mono(),
                    )
                }
            }
            is CommunityApi.SubmitResult.Duplicate -> {
                Spacer(modifier = Modifier.height(NothingSpacing.md))
                Text(
                    text = "Identical content already submitted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is CommunityApi.SubmitResult.Failed -> {
                Spacer(modifier = Modifier.height(NothingSpacing.md))
                Text(
                    text = result.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = NothingColors.accent,
                )
            }
            null -> Unit
        }

        Spacer(modifier = Modifier.height(NothingSpacing.lg))
        NothingPrimaryButton(
            text =
                when {
                    sharing -> "Submitting…"
                    result is CommunityApi.SubmitResult.Queued -> "Done"
                    else -> "Submit for review"
                },
            onClick = {
                if (result is CommunityApi.SubmitResult.Queued) {
                    onDismiss()
                } else {
                    // Keep the local creator profile in sync with the publish form.
                    com.tdvorak.nothingmodes.ui.prefs
                        .CreatorPreferences(context)
                        .save(
                            profile.copy(
                                displayName = displayName,
                                handle = handle,
                                email = email,
                                github = github,
                            ),
                        )
                    onPublish(title, description, handle, email, github)
                }
            },
            enabled = !sharing && title.isNotBlank() && handle.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.md))
    }
}
