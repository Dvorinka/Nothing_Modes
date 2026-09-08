package com.tdvorak.nothingmodes.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tdvorak.nothingmodes.nothing.GlyphToysBridge
import com.tdvorak.nothingmodes.nothing.NothingGlyphMatrixProvider
import com.tdvorak.nothingmodes.nothing.NothingGlyphProvider
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GlyphPreviewViewModel
    @Inject
    constructor(
        private val stripeProvider: NothingGlyphProvider,
        private val matrixProvider: NothingGlyphMatrixProvider,
    ) : ViewModel() {
        val glyphAvailable: Boolean
            get() = stripeProvider.isAvailable() || matrixProvider.isAvailable()
    }

@Composable
fun GlyphPreviewScreen(
    onBack: () -> Unit,
    onOpenEditor: () -> Unit = {},
    viewModel: GlyphPreviewViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val toysBridge = remember { GlyphToysBridge(context) }
    var systemInstalled by remember { mutableStateOf(toysBridge.isGlyphSystemInstalled()) }
    var systemToys by remember { mutableStateOf(toysBridge.listSystemToys()) }
    var activeAodToy by remember { mutableStateOf(toysBridge.activeAodToy()) }

    // Refresh when the user returns from the system Glyph settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer =
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    systemInstalled = toysBridge.isGlyphSystemInstalled()
                    systemToys = toysBridge.listSystemToys()
                    activeAodToy = toysBridge.activeAodToy()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NothingTopBar(title = "Glyph Preview", onBack = onBack)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding =
                PaddingValues(
                    start = NothingSpacing.md,
                    end = NothingSpacing.md,
                    top = NothingSpacing.lg,
                    bottom = NothingSpacing.xxxl,
                ),
        ) {
            // ── System Glyph Toys integration ──────────────────────────────
            item {
                NothingSectionHeader(text = "Glyph Toys (system)")
                NothingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NothingLabel(text = "System app")
                        Text(
                            text = if (systemInstalled) "Found" else "Not found",
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                if (systemInstalled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            fontFamily = NothingFonts.mono(),
                        )
                    }

                    // Ownership verdict — Nothing arbitrates the matrix:
                    // exactly one toy can drive the lights at a time.
                    val ours = systemToys.firstOrNull { it.packageName == context.packageName }
                    val weOwnMatrix = ours?.let { it.isActive || it.isAodActive } == true
                    NothingDivider(modifier = Modifier.padding(top = NothingSpacing.md))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = NothingSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        NothingLabel(text = "Matrix owner")
                        Text(
                            text = if (weOwnMatrix) "NOTHING MODES" else "ANOTHER TOY",
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                if (weOwnMatrix) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    NothingColors.accent
                                },
                            fontFamily = NothingFonts.mono(),
                        )
                    }
                    Text(
                        text =
                            if (weOwnMatrix) {
                                "This app currently controls the lights. Every Glyph action will be visible."
                            } else {
                                "Nothing reserves the matrix for one selected toy. Open system Glyph settings and pick Nothing Modes. " +
                                    "We cannot start other apps' toys or show output while another toy owns the lights."
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    activeAodToy?.let {
                        Text(
                            text = "Always-on toy: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
                            modifier = Modifier.padding(top = NothingSpacing.sm),
                        )
                    }

                    NothingDivider(modifier = Modifier.padding(vertical = NothingSpacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                    ) {
                        GlyphLinkButton(
                            "Open system Glyph settings",
                            Modifier.weight(1f),
                        ) {
                            toysBridge.openToysManager() || toysBridge.openAodToyPicker()
                        }
                    }
                    if (!systemInstalled) {
                        Text(
                            text = "System settings open only on Nothing phones with the Glyph Toys manager.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
                            modifier = Modifier.padding(top = NothingSpacing.sm),
                        )
                    }
                    NothingDivider(modifier = Modifier.padding(vertical = NothingSpacing.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                    ) {
                        GlyphLinkButton(
                            "Glyph Studio",
                            Modifier.weight(1f),
                        ) {
                            onOpenEditor()
                            true
                        }
                        GlyphLinkButton(
                            "Glyph Museum",
                            Modifier.weight(1f),
                        ) {
                            openGlyphMuseum(context)
                            true
                        }
                    }
                }
            }

            // ── Device capability summary ───────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(NothingSpacing.md))
                NothingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NothingLabel(text = "Glyph output")
                        Text(
                            text = if (viewModel.glyphAvailable) "Hardware detected" else "No Glyph hardware on this device",
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                if (viewModel.glyphAvailable) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            fontFamily = NothingFonts.mono(),
                        )
                    }
                    Text(
                        text = "Real Glyph output happens inside a routine via the Glyph preset action. This screen only shows status and deep links.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = NothingSpacing.sm),
                    )
                }
            }
        }
    }
}

private const val GLYPH_MUSEUM_PKG = "com.pauwma.glyphmuseum"

private fun openGlyphMuseum(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(GLYPH_MUSEUM_PKG)
    if (launch != null) {
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } else {
        val store =
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$GLYPH_MUSEUM_PKG")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        runCatching { context.startActivity(store) }
    }
}

@Composable
private fun GlyphLinkButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Boolean,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier =
            modifier
                .clip(NothingShapes.input)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = NothingSpacing.sm)
                .clickable {
                    if (!onClick()) {
                        Toast.makeText(
                            context,
                            "Not available on this device",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = NothingFonts.mono(),
        )
    }
}
