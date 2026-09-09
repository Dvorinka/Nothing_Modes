package com.tdvorak.nothingmodes.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingPrimaryButton
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing

/**
 * Editor for [Action.SetWallpaper] that lets the user pick an image from the
 * system gallery/file picker, persists the read URI permission, and previews
 * the selected image. The URI can still be entered manually as a fallback.
 */
@Composable
fun WallpaperActionEditor(
    action: Action.SetWallpaper,
    onActionChange: (Action.SetWallpaper) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

    val imagePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            // Persist the grant so the executor can read the URI later.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onActionChange(action.copy(uri = uri.toString()))
        }

    LaunchedEffect(action.uri) {
        thumbnail = loadWallpaperThumbnail(context, action.uri)
    }

    Column(modifier = modifier) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = "Selected wallpaper",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
        } else if (action.uri.isNotBlank()) {
            Text(
                text = "Selected: ${Uri.parse(action.uri).lastPathSegment ?: action.uri}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
        }

        NothingPrimaryButton(
            text = if (action.uri.isBlank()) "Pick from gallery" else "Change image",
            onClick = { imagePicker.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = action.uri,
            onValueChange = { onActionChange(action.copy(uri = it)) },
            label = "Image URI (optional)",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingEnumSelector(
            label = "Which",
            value = action.which,
            options = listOf("home", "lock"),
            onSelect = { onActionChange(action.copy(which = it)) },
        )
    }
}

private fun loadWallpaperThumbnail(
    context: Context,
    uriString: String,
): Bitmap? {
    if (uriString.isBlank()) return null
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null

    return runCatching {
        context.contentResolver
            .openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it) }
            ?.scaledToMax(512)
    }.getOrNull()
        ?: runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source).scaledToMax(512)
            } else {
                null
            }
        }.getOrNull()
}

private fun Bitmap.scaledToMax(maxDim: Int): Bitmap {
    if (width <= maxDim && height <= maxDim) return this
    val scale = (maxDim.toFloat() / maxOf(width, height)).coerceAtMost(1f)
    return Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt(),
        (height * scale).toInt(),
        true,
    )
}
