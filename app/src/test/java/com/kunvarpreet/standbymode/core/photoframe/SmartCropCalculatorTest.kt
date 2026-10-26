package com.kunvarpreet.standbymode.core.photoframe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests covering pure geometric face-aware smart crop calculation,
 * fallback behavior, multi-face unions, clamping, and aspect-ratio adaptation.
 */
class SmartCropCalculatorTest {

    @Test
    fun noFaces_returnsCenterAlignmentFallback() {
        val alignment = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 1200,
            imageHeight = 800,
            containerWidth = 600f,
            containerHeight = 600f,
            faces = emptyList()
        )
        assertEquals(0f, alignment.biasX, 0.001f)
        assertEquals(0f, alignment.biasY, 0.001f)
    }

    @Test
    fun nullOrEmptyFaces_returnsCenterAlignmentFallback() {
        val alignmentNull = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 1000,
            imageHeight = 1000,
            containerWidth = 500f,
            containerHeight = 500f,
            faces = null
        )
        assertEquals(CropAlignment.Center, alignmentNull)
    }

    @Test
    fun invalidDimensions_returnsCenterAlignmentFallback() {
        val zeroWidth = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 0,
            imageHeight = 800,
            containerWidth = 400f,
            containerHeight = 400f,
            faces = listOf(FaceBounds(10f, 10f, 50f, 50f))
        )
        assertEquals(CropAlignment.Center, zeroWidth)

        val zeroContainer = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 800,
            imageHeight = 800,
            containerWidth = 0f,
            containerHeight = 0f,
            faces = listOf(FaceBounds(10f, 10f, 50f, 50f))
        )
        assertEquals(CropAlignment.Center, zeroContainer)
    }

    @Test
    fun oneCenteredFace_remainsCenteredWithoutUnnecessaryShift() {
        // Landscape image 1200x600 cropped to square 600x600.
        // Visible window in image is 600x600, center window is [300, 900].
        // Face is centered around x=600, y=300.
        val face = FaceBounds(left = 500f, top = 200f, right = 700f, bottom = 400f)

        val alignment = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 1200,
            imageHeight = 600,
            containerWidth = 600f,
            containerHeight = 600f,
            faces = listOf(face)
        )

        // Since the face is already fully inside the default center crop, movement is avoided (Priority 3)
        assertEquals(0f, alignment.biasX, 0.001f)
        assertEquals(0f, alignment.biasY, 0.001f)
    }

    @Test
    fun oneOffCenterFace_horizontalShiftTowardsFace() {
        // Landscape image 1200x600 cropped to square 600x600.
        // Face is on the left third: x in [50, 250], center x = 150.
        val faceLeft = FaceBounds(left = 50f, top = 200f, right = 250f, bottom = 400f)

        val alignmentLeft = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 1200,
            imageHeight = 600,
            containerWidth = 600f,
            containerHeight = 600f,
            faces = listOf(faceLeft)
        )

        assertTrue("Expected negative biasX to shift crop leftwards, got ${alignmentLeft.biasX}", alignmentLeft.biasX < -0.5f)
        assertEquals(0f, alignmentLeft.biasY, 0.001f)

        // Face is on the right third: x in [950, 1150], center x = 1050.
        val faceRight = FaceBounds(left = 950f, top = 200f, right = 1150f, bottom = 400f)

        val alignmentRight = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 1200,
            imageHeight = 600,
            containerWidth = 600f,
            containerHeight = 600f,
            faces = listOf(faceRight)
        )

        assertTrue("Expected positive biasX to shift crop rightwards, got ${alignmentRight.biasX}", alignmentRight.biasX > 0.5f)
        assertEquals(0f, alignmentRight.biasY, 0.001f)
    }

    @Test
    fun oneOffCenterFace_verticalShiftTowardsFace_inPortraitImage() {
        // Portrait image 600x1200 cropped to landscape pane 600x300.
        // Visible window in image is 600x300.
        // Face near top: y in [50, 250], center y = 150.
        val faceTop = FaceBounds(left = 200f, top = 50f, right = 400f, bottom = 250f)

        val alignmentTop = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 600,
            imageHeight = 1200,
            containerWidth = 600f,
            containerHeight = 300f,
            faces = listOf(faceTop)
        )

        assertTrue("Expected negative biasY to shift crop towards top, got ${alignmentTop.biasY}", alignmentTop.biasY < -0.5f)
        assertEquals(0f, alignmentTop.biasX, 0.001f)

        // Face near bottom: y in [950, 1150], center y = 1050.
        val faceBottom = FaceBounds(left = 200f, top = 950f, right = 400f, bottom = 1150f)

        val alignmentBottom = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 600,
            imageHeight = 1200,
            containerWidth = 600f,
            containerHeight = 300f,
            faces = listOf(faceBottom)
        )

        assertTrue("Expected positive biasY to shift crop towards bottom, got ${alignmentBottom.biasY}", alignmentBottom.biasY > 0.5f)
        assertEquals(0f, alignmentBottom.biasX, 0.001f)
    }

    @Test
    fun multipleFaces_retainsAllFacesWhenFeasible() {
        // Landscape image 1600x800 cropped to square 800x800.
        // Visible window in image is 800x800.
        // Two faces grouped on the left:
        val face1 = FaceBounds(left = 80f, top = 300f, right = 240f, bottom = 480f)
        val face2 = FaceBounds(left = 300f, top = 320f, right = 460f, bottom = 500f)

        val alignment = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 1600,
            imageHeight = 800,
            containerWidth = 800f,
            containerHeight = 800f,
            faces = listOf(face1, face2)
        )

        assertTrue("Crop should frame the cluster on the left, got ${alignment.biasX}", alignment.biasX < -0.4f)
    }

    @Test
    fun faceBoxesOutsideImageBounds_clampedOrRejectedSafely() {
        // Completely invalid negative/inverted box
        val invalidFace = FaceBounds(left = -500f, top = -500f, right = -100f, bottom = -100f)
        val fallbackAlignment = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 1000,
            imageHeight = 1000,
            containerWidth = 500f,
            containerHeight = 500f,
            faces = listOf(invalidFace)
        )
        assertEquals(CropAlignment.Center, fallbackAlignment)

        // Partially overflowing box is clamped
        val overflowingFace = FaceBounds(left = 800f, top = 800f, right = 1500f, bottom = 1500f)
        val clampedAlignment = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 1000,
            imageHeight = 1000,
            containerWidth = 500f,
            containerHeight = 500f,
            faces = listOf(overflowingFace)
        )
        assertTrue(clampedAlignment.biasX in -1f..1f)
        assertTrue(clampedAlignment.biasY in -1f..1f)
    }

    @Test
    fun extremeAspectRatios_neverExceedValidCropBounds() {
        // Ultra-wide panorama 4000x500 in portrait container 500x1000
        val faceFarRight = FaceBounds(left = 3800f, top = 100f, right = 3950f, bottom = 300f)
        val wideAlignment = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 4000,
            imageHeight = 500,
            containerWidth = 500f,
            containerHeight = 1000f,
            faces = listOf(faceFarRight)
        )
        assertTrue("biasX must be in [-1, 1], got ${wideAlignment.biasX}", wideAlignment.biasX in -1f..1f)
        assertTrue("biasY must be in [-1, 1], got ${wideAlignment.biasY}", wideAlignment.biasY in -1f..1f)

        // Ultra-tall vertical 500x4000 in landscape container 1000x500
        val faceFarBottom = FaceBounds(left = 100f, top = 3800f, right = 300f, bottom = 3950f)
        val tallAlignment = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 500,
            imageHeight = 4000,
            containerWidth = 1000f,
            containerHeight = 500f,
            faces = listOf(faceFarBottom)
        )
        assertTrue("biasX must be in [-1, 1], got ${tallAlignment.biasX}", tallAlignment.biasX in -1f..1f)
        assertTrue("biasY must be in [-1, 1], got ${tallAlignment.biasY}", tallAlignment.biasY in -1f..1f)
    }

    @Test
    fun identicalInputs_produceIdenticalCropResults() {
        val faces = listOf(
            FaceBounds(120f, 150f, 300f, 350f),
            FaceBounds(400f, 180f, 560f, 360f)
        )
        val result1 = SmartCropCalculator.calculateCropAlignment(1920, 1080, 800f, 600f, faces)
        val result2 = SmartCropCalculator.calculateCropAlignment(1920, 1080, 800f, 600f, faces)

        assertEquals(result1.biasX, result2.biasX, 0.00001f)
        assertEquals(result1.biasY, result2.biasY, 0.00001f)
    }

    @Test
    fun faceTooTallForCropWindow_anchorsTowardsTopToProtectEyesAndForehead() {
        // Portrait image 800x1600 in landscape container 800x400.
        // Visible height in image is 400.
        // A huge portrait face spanning y from 200 to 1200 (height = 1000, much larger than visible height 400).
        val tallFace = FaceBounds(left = 100f, top = 200f, right = 700f, bottom = 1200f)

        val alignment = SmartCropCalculator.calculateCropAlignment(
            imageWidth = 800,
            imageHeight = 1600,
            containerWidth = 800f,
            containerHeight = 400f,
            faces = listOf(tallFace)
        )

        // Because the face is taller than the visible crop, it anchors towards the top (eyes/forehead)
        // rather than blindly centering on the mid-neck or chest.
        assertTrue("Expected negative biasY to anchor towards eyes/forehead, got ${alignment.biasY}", alignment.biasY < 0f)
    }
}
