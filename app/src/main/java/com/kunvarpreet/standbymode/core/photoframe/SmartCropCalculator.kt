package com.kunvarpreet.standbymode.core.photoframe

/**
 * Pure mathematical crop calculator determining optimal Compose Alignment biases
 * to keep detected face regions framed gracefully within arbitrary pane dimensions.
 *
 * Implements deterministic calculation without Android or Compose framework dependencies.
 */
object SmartCropCalculator {

    const val DEFAULT_SAFETY_MARGIN_FACTOR = 0.15f

    /**
     * Calculates the optimal Compose crop alignment biases (biasX, biasY) in range [-1.0f, +1.0f],
     * where (0.0f, 0.0f) represents conventional Center Crop.
     *
     * @param imageWidth Source decoded image width in pixels.
     * @param imageHeight Source decoded image height in pixels.
     * @param containerWidth Visible target container pane width in pixels.
     * @param containerHeight Visible target container pane height in pixels.
     * @param faces List of detected face bounding boxes in image pixel coordinates.
     * @param safetyMarginFactor Padding fraction added around face bounding box boundaries.
     * @return Deterministic [CropAlignment] suitable for Compose Alignment(biasX, biasY).
     */
    fun calculateCropAlignment(
        imageWidth: Int,
        imageHeight: Int,
        containerWidth: Float,
        containerHeight: Float,
        faces: List<FaceBounds>?,
        safetyMarginFactor: Float = DEFAULT_SAFETY_MARGIN_FACTOR
    ): CropAlignment {
        // Priority 4: Fall back to conventional center crop when detections or dimensions are absent/invalid
        if (imageWidth <= 0 || imageHeight <= 0 || containerWidth <= 0f || containerHeight <= 0f) {
            return CropAlignment.Center
        }
        if (faces.isNullOrEmpty()) {
            return CropAlignment.Center
        }

        // Filter and clamp face bounding boxes to strictly valid source image boundaries
        val validFaces = faces.mapNotNull { face ->
            val clLeft = face.left.coerceIn(0f, imageWidth.toFloat())
            val clRight = face.right.coerceIn(0f, imageWidth.toFloat())
            val clTop = face.top.coerceIn(0f, imageHeight.toFloat())
            val clBottom = face.bottom.coerceIn(0f, imageHeight.toFloat())
            if (clRight > clLeft && clBottom > clTop) {
                FaceBounds(clLeft, clTop, clRight, clBottom)
            } else null
        }

        if (validFaces.isEmpty()) {
            return CropAlignment.Center
        }

        // Priority 2: Encompass multiple faces inside an inclusive union bounding box
        var minLeft = validFaces.minOf { it.left }
        var maxRight = validFaces.maxOf { it.right }
        var minTop = validFaces.minOf { it.top }
        var maxBottom = validFaces.maxOf { it.bottom }

        // Apply safety margins around face boundaries
        val unionWidth = maxRight - minLeft
        val unionHeight = maxBottom - minTop
        val marginX = unionWidth * safetyMarginFactor
        val marginY = unionHeight * safetyMarginFactor

        minLeft = (minLeft - marginX).coerceAtLeast(0f)
        maxRight = (maxRight + marginX).coerceAtMost(imageWidth.toFloat())
        minTop = (minTop - marginY).coerceAtLeast(0f)
        maxBottom = (maxBottom + marginY).coerceAtMost(imageHeight.toFloat())

        val facesCenterX = (minLeft + maxRight) / 2f
        val facesCenterY = (minTop + maxBottom) / 2f

        // Calculate aspect-preserving scale factor under ContentScale.Crop
        val scaleX = containerWidth / imageWidth.toFloat()
        val scaleY = containerHeight / imageHeight.toFloat()
        val scale = maxOf(scaleX, scaleY)

        val visibleWidthInImage = containerWidth / scale
        val visibleHeightInImage = containerHeight / scale

        // Priority 1 & 3: Horizontal axis evaluation
        val biasX = if (visibleWidthInImage >= imageWidth.toFloat()) {
            // Entire image width is visible; no horizontal scroll window
            0f
        } else {
            val maxScrollX = imageWidth.toFloat() - visibleWidthInImage
            val centerWindowLeft = maxScrollX / 2f
            val centerWindowRight = centerWindowLeft + visibleWidthInImage

            // Priority 3: Avoid unnecessary movement if faces are already inside default center crop
            if (minLeft >= centerWindowLeft && maxRight <= centerWindowRight) {
                0f
            } else {
                val idealLeft = facesCenterX - (visibleWidthInImage / 2f)
                val clampedLeft = idealLeft.coerceIn(0f, maxScrollX)
                if (maxScrollX > 0f) {
                    2f * (clampedLeft / maxScrollX) - 1f
                } else {
                    0f
                }
            }
        }

        // Priority 1 & 3: Vertical axis evaluation
        val biasY = if (visibleHeightInImage >= imageHeight.toFloat()) {
            // Entire image height is visible; no vertical scroll window
            0f
        } else {
            val maxScrollY = imageHeight.toFloat() - visibleHeightInImage
            val centerWindowTop = maxScrollY / 2f
            val centerWindowBottom = centerWindowTop + visibleHeightInImage

            // Priority 3: Avoid unnecessary movement if faces are already inside default center crop
            if (minTop >= centerWindowTop && maxBottom <= centerWindowBottom) {
                0f
            } else {
                // If faces fit within visible height, center them; if taller, anchor towards top (eyes/forehead)
                val idealTop = if (maxBottom - minTop <= visibleHeightInImage) {
                    facesCenterY - (visibleHeightInImage / 2f)
                } else {
                    minTop
                }
                val clampedTop = idealTop.coerceIn(0f, maxScrollY)
                if (maxScrollY > 0f) {
                    2f * (clampedTop / maxScrollY) - 1f
                } else {
                    0f
                }
            }
        }

        return CropAlignment(
            biasX = biasX.coerceIn(-1f, 1f),
            biasY = biasY.coerceIn(-1f, 1f)
        )
    }
}
