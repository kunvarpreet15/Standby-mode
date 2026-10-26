package com.kunvarpreet.standbymode.core.photoframe

import android.content.ContentResolver
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asynchronous, lifecycle-safe image loader for local content URIs.
 * Uses Android's native ImageDecoder with downsampling and bounded in-memory caching.
 */
object AsyncPhotoLoader {
    private const val TAG = "AsyncPhotoLoader"
    private const val MAX_CACHE_ENTRIES = 24
    private const val MAX_DECODE_DIMENSION = 1200

    private val memoryCache = object : LruCache<String, DecodedPhoto>(MAX_CACHE_ENTRIES) {}

    fun getCachedPhoto(uriString: String): DecodedPhoto? {
        return memoryCache.get(uriString)
    }

    fun getCachedBitmap(uriString: String): ImageBitmap? {
        return memoryCache.get(uriString)?.imageBitmap
    }

    fun clearCache() {
        memoryCache.evictAll()
        FaceCropAnalyzer.clearCache()
    }

    /**
     * Decodes and downsamples a photo from a content URI off the main thread,
     * returning both native Bitmap (for ML Kit) and Compose ImageBitmap.
     */
    suspend fun loadPhoto(
        contentResolver: ContentResolver,
        uriString: String,
        targetWidth: Int = MAX_DECODE_DIMENSION,
        targetHeight: Int = MAX_DECODE_DIMENSION
    ): Result<DecodedPhoto> = withContext(Dispatchers.IO) {
        val cached = memoryCache.get(uriString)
        if (cached != null) {
            return@withContext Result.success(cached)
        }

        runCatching {
            val uri = Uri.parse(uriString)
            val source = ImageDecoder.createSource(contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val origW = info.size.width
                val origH = info.size.height
                if (targetWidth > 0 && targetHeight > 0 && (origW > targetWidth || origH > targetHeight)) {
                    val sample = calculateSampleSize(origW, origH, targetWidth, targetHeight)
                    if (sample > 1) {
                        decoder.setTargetSampleSize(sample)
                    }
                }
            }
            val imageBitmap = bitmap.asImageBitmap()
            val decoded = DecodedPhoto(imageBitmap = imageBitmap, bitmap = bitmap)
            memoryCache.put(uriString, decoded)
            decoded
        }.onFailure { e ->
            Log.w(TAG, "Failed to decode photo from URI: $uriString", e)
        }
    }

    /**
     * Legacy convenience wrapper returning ImageBitmap for callers that do not require raw Bitmap.
     */
    suspend fun loadPhotoBitmap(
        contentResolver: ContentResolver,
        uriString: String,
        targetWidth: Int = MAX_DECODE_DIMENSION,
        targetHeight: Int = MAX_DECODE_DIMENSION
    ): Result<ImageBitmap> {
        return loadPhoto(contentResolver, uriString, targetWidth, targetHeight).map { it.imageBitmap }
    }

    private fun calculateSampleSize(origW: Int, origH: Int, targetW: Int, targetH: Int): Int {
        var sample = 1
        var currentW = origW
        var currentH = origH
        while (currentW / 2 >= targetW && currentH / 2 >= targetH) {
            currentW /= 2
            currentH /= 2
            sample *= 2
        }
        return sample
    }
}
