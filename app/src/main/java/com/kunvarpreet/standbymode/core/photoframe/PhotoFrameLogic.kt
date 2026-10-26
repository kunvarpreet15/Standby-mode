package com.kunvarpreet.standbymode.core.photoframe

/**
 * Pure policy and validation rules governing photo selection, URI persistence,
 * selection ordering, bounds enforcement, and slideshow timing.
 */
object PhotoFrameLogic {

    /** Maximum allowed photo selection size */
    const val MAX_PHOTO_COUNT = 50

    /** Configured slideshow advancement interval in milliseconds */
    const val SLIDESHOW_INTERVAL_MILLIS = 15_000L

    /**
     * Sanitizes an incoming list of URI strings:
     * - Trims whitespace
     * - Discards empty or blank entries
     * - Eliminates duplicates while strictly preserving original selection order
     * - Binds the output list to [maxCount] entries
     */
    fun sanitizeUriList(uris: List<String>?, maxCount: Int = MAX_PHOTO_COUNT): List<String> {
        if (uris.isNullOrEmpty()) return emptyList()
        val result = ArrayList<String>(minOf(uris.size, maxCount))
        val seen = HashSet<String>()
        for (uri in uris) {
            val trimmed = uri.trim()
            if (trimmed.isNotEmpty() && seen.add(trimmed)) {
                result.add(trimmed)
                if (result.size >= maxCount) break
            }
        }
        return result
    }

    /**
     * Serializes an ordered list of URI strings into a persistent string representation.
     */
    fun serializeUriList(uris: List<String>): String {
        return uris.joinToString(separator = "\n")
    }

    /**
     * Deserializes a stored raw string into a sanitized list of URI strings.
     * Resilient against malformed, blank, or JSON-like strings.
     */
    fun deserializeUriList(raw: String?, maxCount: Int = MAX_PHOTO_COUNT): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val trimmedRaw = raw.trim()
        val lines = if (trimmedRaw.startsWith("[") && trimmedRaw.endsWith("]")) {
            trimmedRaw.removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
        } else {
            raw.lines()
        }
        return sanitizeUriList(lines, maxCount)
    }

    /**
     * Deterministically calculates the next slideshow index in a circular fashion.
     * If there are 0 or 1 photos, returns index 0 without advancing.
     */
    fun nextIndex(currentIndex: Int, totalPhotos: Int): Int {
        if (totalPhotos <= 1) return 0
        if (currentIndex < 0 || currentIndex >= totalPhotos) return 0
        return (currentIndex + 1) % totalPhotos
    }

    /**
     * Evaluates whether the slideshow timer should actively tick.
     * All conditions must be satisfied:
     * 1. Photo Frame preference is enabled
     * 2. Widget is visibly active in the UI
     * 3. Activity is in the foreground
     * 4. More than one photo is available
     */
    fun shouldAdvanceSlideshow(
        showPhotoFramePref: Boolean,
        isPhotoFrameVisible: Boolean,
        isActivityForeground: Boolean,
        photoCount: Int
    ): Boolean {
        return showPhotoFramePref && isPhotoFrameVisible && isActivityForeground && photoCount > 1
    }

    /**
     * Resolves the list of URIs to retain after an update, ensuring order preservation.
     */
    fun mergeSelections(
        existing: List<String>,
        incoming: List<String>,
        maxCount: Int = MAX_PHOTO_COUNT
    ): List<String> {
        if (incoming.isEmpty()) return existing
        return sanitizeUriList(incoming, maxCount)
    }

    /**
     * Determines which persisted URI permissions can be safely released when selection changes.
     * Only URIs present in [previousUris] but absent in [currentUris] are released.
     */
    fun findOrphanedUris(previousUris: List<String>, currentUris: List<String>): List<String> {
        val currentSet = currentUris.toSet()
        return previousUris.filter { it !in currentSet }.distinct()
    }
}
