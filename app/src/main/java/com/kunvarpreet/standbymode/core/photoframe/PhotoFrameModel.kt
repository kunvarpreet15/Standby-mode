package com.kunvarpreet.standbymode.core.photoframe

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Normalized bounding box for a detected face within source image coordinates [0, width] x [0, height].
 */
data class FaceBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = left + width / 2f
    val centerY: Float get() = top + height / 2f
}

/**
 * Crop alignment biases compatible with Compose Alignment(biasX, biasY).
 * Range is strictly [-1.0f, +1.0f] on both axes, where (0.0f, 0.0f) is conventional Center crop.
 */
data class CropAlignment(
    val biasX: Float = 0f,
    val biasY: Float = 0f
) {
    companion object {
        val Center = CropAlignment(0f, 0f)
    }
}

/**
 * Encapsulates both the native Bitmap (for ML Kit vision analysis) and Compose ImageBitmap (for rendering).
 * Both share the same underlying pixel buffer, requiring zero redundant memory allocation.
 */
data class DecodedPhoto(
    val imageBitmap: ImageBitmap,
    val bitmap: Bitmap
)

/**
 * State representing an individual photo within the Photo Frame slideshow.
 */
sealed interface PhotoItemState {
    /** Photo is actively decoding or waiting in queue */
    data object Loading : PhotoItemState

    /** Photo successfully decoded and ready for display with smart crop alignment */
    data class Success(
        val decodedPhoto: DecodedPhoto,
        val cropAlignment: CropAlignment = CropAlignment.Center
    ) : PhotoItemState {
        val bitmap: ImageBitmap get() = decodedPhoto.imageBitmap
    }

    /** Photo URI could not be accessed, decoded, or permission was revoked */
    data class Unavailable(val uriString: String, val reason: String? = null) : PhotoItemState
}
