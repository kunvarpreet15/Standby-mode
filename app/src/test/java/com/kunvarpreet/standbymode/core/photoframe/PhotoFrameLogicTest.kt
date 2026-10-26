package com.kunvarpreet.standbymode.core.photoframe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests covering pure photo selection, bounds clamping, deduplication,
 * serialization, circular slideshow indexing, and visibility timing policies.
 */
class PhotoFrameLogicTest {

    @Test
    fun constants_follow_roadmap_specifications() {
        assertEquals(50, PhotoFrameLogic.MAX_PHOTO_COUNT)
        assertEquals(15_000L, PhotoFrameLogic.SLIDESHOW_INTERVAL_MILLIS)
    }

    @Test
    fun sanitizeUriList_preservesOrder_removesDuplicates_andFiltersBlanks() {
        val input = listOf(
            "content://media/external/images/media/1",
            "   ",
            "content://media/external/images/media/2",
            "content://media/external/images/media/1", // duplicate
            "",
            "content://media/external/images/media/3",
            "content://media/external/images/media/2"  // duplicate
        )

        val sanitized = PhotoFrameLogic.sanitizeUriList(input)
        assertEquals(3, sanitized.size)
        assertEquals(
            listOf(
                "content://media/external/images/media/1",
                "content://media/external/images/media/2",
                "content://media/external/images/media/3"
            ),
            sanitized
        )
    }

    @Test
    fun sanitizeUriList_enforcesSelectionSizeLimit() {
        val manyUris = (1..75).map { "content://media/external/images/media/$it" }
        val sanitized = PhotoFrameLogic.sanitizeUriList(manyUris, maxCount = 50)

        assertEquals(50, sanitized.size)
        assertEquals("content://media/external/images/media/1", sanitized.first())
        assertEquals("content://media/external/images/media/50", sanitized.last())
    }

    @Test
    fun sanitizeUriList_handlesNullAndEmpty() {
        assertTrue(PhotoFrameLogic.sanitizeUriList(null).isEmpty())
        assertTrue(PhotoFrameLogic.sanitizeUriList(emptyList()).isEmpty())
    }

    @Test
    fun serializationAndDeserialization_roundTripPreservesOrder() {
        val original = listOf(
            "content://media/external/images/media/10",
            "content://media/external/images/media/20",
            "content://media/external/images/media/30"
        )
        val serialized = PhotoFrameLogic.serializeUriList(original)
        val deserialized = PhotoFrameLogic.deserializeUriList(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun deserializeUriList_handlesMalformedAndJsonArrayFormats() {
        // Null or blank
        assertTrue(PhotoFrameLogic.deserializeUriList(null).isEmpty())
        assertTrue(PhotoFrameLogic.deserializeUriList("   ").isEmpty())

        // JSON array format
        val json = "[\"content://media/external/images/media/1\", \"content://media/external/images/media/2\"]"
        val parsedJson = PhotoFrameLogic.deserializeUriList(json)
        assertEquals(2, parsedJson.size)
        assertEquals("content://media/external/images/media/1", parsedJson[0])
        assertEquals("content://media/external/images/media/2", parsedJson[1])

        // Newline separated format with surrounding whitespace
        val newlineSeparated = "\ncontent://media/1\n\ncontent://media/2\n"
        val parsedNewlines = PhotoFrameLogic.deserializeUriList(newlineSeparated)
        assertEquals(2, parsedNewlines.size)
        assertEquals("content://media/1", parsedNewlines[0])
        assertEquals("content://media/2", parsedNewlines[1])
    }

    @Test
    fun emptyPickerResult_leavesExistingSelectionUnchanged() {
        val existing = listOf("content://media/1", "content://media/2")
        val emptyIncoming = emptyList<String>()

        val result = PhotoFrameLogic.mergeSelections(existing, emptyIncoming)
        assertEquals(existing, result)
    }

    @Test
    fun mergeSelections_replacesWithNewValidSelection_preservingOrder() {
        val existing = listOf("content://media/1", "content://media/2")
        val incoming = listOf("content://media/3", "content://media/4", "content://media/3")

        val result = PhotoFrameLogic.mergeSelections(existing, incoming)
        assertEquals(listOf("content://media/3", "content://media/4"), result)
    }

    @Test
    fun findOrphanedUris_identifiesOnlyRemovedPermissions() {
        val previous = listOf("content://media/1", "content://media/2", "content://media/3")
        val current = listOf("content://media/2", "content://media/4")

        val orphaned = PhotoFrameLogic.findOrphanedUris(previous, current)
        assertEquals(listOf("content://media/1", "content://media/3"), orphaned)
    }

    @Test
    fun nextIndex_advancesAndWrapsDeterministically() {
        val totalPhotos = 4

        assertEquals(1, PhotoFrameLogic.nextIndex(currentIndex = 0, totalPhotos = totalPhotos))
        assertEquals(2, PhotoFrameLogic.nextIndex(currentIndex = 1, totalPhotos = totalPhotos))
        assertEquals(3, PhotoFrameLogic.nextIndex(currentIndex = 2, totalPhotos = totalPhotos))
        // Wraps from last to first
        assertEquals(0, PhotoFrameLogic.nextIndex(currentIndex = 3, totalPhotos = totalPhotos))
    }

    @Test
    fun nextIndex_singlePhoto_doesNotAdvance() {
        assertEquals(0, PhotoFrameLogic.nextIndex(currentIndex = 0, totalPhotos = 1))
        assertEquals(0, PhotoFrameLogic.nextIndex(currentIndex = 5, totalPhotos = 1))
    }

    @Test
    fun nextIndex_emptyList_returnsZero() {
        assertEquals(0, PhotoFrameLogic.nextIndex(currentIndex = 0, totalPhotos = 0))
    }

    @Test
    fun shouldAdvanceSlideshow_requiresAllConditions() {
        // All true -> true
        assertTrue(
            PhotoFrameLogic.shouldAdvanceSlideshow(
                showPhotoFramePref = true,
                isPhotoFrameVisible = true,
                isActivityForeground = true,
                photoCount = 3
            )
        )

        // Single photo -> false (no repeating transition)
        assertFalse(
            PhotoFrameLogic.shouldAdvanceSlideshow(
                showPhotoFramePref = true,
                isPhotoFrameVisible = true,
                isActivityForeground = true,
                photoCount = 1
            )
        )

        // Empty photos -> false
        assertFalse(
            PhotoFrameLogic.shouldAdvanceSlideshow(
                showPhotoFramePref = true,
                isPhotoFrameVisible = true,
                isActivityForeground = true,
                photoCount = 0
            )
        )

        // Preference disabled -> false
        assertFalse(
            PhotoFrameLogic.shouldAdvanceSlideshow(
                showPhotoFramePref = false,
                isPhotoFrameVisible = true,
                isActivityForeground = true,
                photoCount = 5
            )
        )

        // Hidden widget -> false
        assertFalse(
            PhotoFrameLogic.shouldAdvanceSlideshow(
                showPhotoFramePref = true,
                isPhotoFrameVisible = false,
                isActivityForeground = true,
                photoCount = 5
            )
        )

        // Activity backgrounded -> false
        assertFalse(
            PhotoFrameLogic.shouldAdvanceSlideshow(
                showPhotoFramePref = true,
                isPhotoFrameVisible = true,
                isActivityForeground = false,
                photoCount = 5
            )
        )
    }
}
