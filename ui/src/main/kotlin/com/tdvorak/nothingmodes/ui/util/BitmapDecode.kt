package com.tdvorak.nothingmodes.ui.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Decodes [uri] with `inSampleSize` so the result's long edge lands near
 * [maxDim]. `BitmapFactory.decodeStream` alone materializes the full image —
 * a modern photo or video frame can exceed the whole heap on low-RAM
 * devices (OutOfMemoryError reports on a 192MB-heap Samsung A30).
 */
fun decodeSampledBitmap(
    resolver: ContentResolver,
    uri: Uri,
    maxDim: Int,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }
}
