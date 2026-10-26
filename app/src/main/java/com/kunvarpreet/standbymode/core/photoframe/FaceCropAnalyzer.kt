package com.kunvarpreet.standbymode.core.photoframe

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * On-device face detection analyzer powered by Google ML Kit.
 * Extracts face bounding boxes for smart crop alignment without sending images or telemetry off-device.
 */
object FaceCropAnalyzer {
    private const val TAG = "FaceCropAnalyzer"
    private const val MAX_FACE_CACHE_ENTRIES = 50

    private val faceCache = object : LruCache<String, List<FaceBounds>>(MAX_FACE_CACHE_ENTRIES) {}

    @Volatile
    private var detectorInstance: FaceDetector? = null

    private fun getOrCreateDetector(): FaceDetector {
        return detectorInstance ?: synchronized(this) {
            detectorInstance ?: FaceDetection.getClient(
                FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .setMinFaceSize(0.1f)
                    .build()
            ).also { detectorInstance = it }
        }
    }

    /**
     * Retrieves cached face bounding boxes for a URI if already analyzed in the current session.
     */
    fun getCachedFaces(uriString: String): List<FaceBounds>? {
        return faceCache.get(uriString)
    }

    /**
     * Clears in-memory face cache entries. Called when photos are removed or replaced.
     */
    fun clearCache() {
        faceCache.evictAll()
    }

    /**
     * Closes and releases detector resources when no longer needed.
     */
    fun releaseDetector() {
        synchronized(this) {
            try {
                detectorInstance?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Exception closing FaceDetector", e)
            } finally {
                detectorInstance = null
            }
        }
    }

    /**
     * Analyzes an already-decoded [Bitmap] to detect face bounding boxes on-device.
     * Caches results in memory keyed by [uriString] to ensure each image is analyzed at most once.
     */
    suspend fun analyzeFaces(
        uriString: String,
        bitmap: Bitmap
    ): Result<List<FaceBounds>> = withContext(Dispatchers.Default) {
        val cached = faceCache.get(uriString)
        if (cached != null) {
            return@withContext Result.success(cached)
        }

        runCatching {
            val detector = getOrCreateDetector()
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            val detectedBounds: List<FaceBounds> = suspendCancellableCoroutine { continuation ->
                detector.process(inputImage)
                    .addOnSuccessListener { faces ->
                        if (continuation.isActive) {
                            val bounds = faces.map { face ->
                                val box = face.boundingBox
                                FaceBounds(
                                    left = box.left.toFloat(),
                                    top = box.top.toFloat(),
                                    right = box.right.toFloat(),
                                    bottom = box.bottom.toFloat()
                                )
                            }
                            continuation.resume(bounds)
                        }
                    }
                    .addOnFailureListener { exception ->
                        if (continuation.isActive) {
                            Log.w(TAG, "ML Kit Face Detection failed for $uriString", exception)
                            continuation.resume(emptyList())
                        }
                    }
            }

            faceCache.put(uriString, detectedBounds)
            detectedBounds
        }.onFailure { e ->
            Log.w(TAG, "Unexpected error analyzing faces for $uriString", e)
        }
    }
}
