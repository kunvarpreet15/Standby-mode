package com.kunvarpreet.standbymode.core.burnin

import java.time.Instant

/**
 * Immutable model representing a 2D pixel-shift displacement in density-independent pixels (dp).
 */
data class BurnInShiftOffset(
    val xDp: Float,
    val yDp: Float
) {
    fun coerceIn(maxDisplacementDp: Float): BurnInShiftOffset {
        return BurnInShiftOffset(
            xDp = xDp.coerceIn(-maxDisplacementDp, maxDisplacementDp),
            yDp = yDp.coerceIn(-maxDisplacementDp, maxDisplacementDp)
        )
    }

    companion object {
        val NEUTRAL = BurnInShiftOffset(0f, 0f)
    }
}

/**
 * Pure policy determining periodic, deterministic pixel shifting on OLED displays.
 *
 * Distributes static high-contrast pixels (digits, hands, borders) across nearby positions
 * within approximately 4dp of the neutral position.
 * Shifts infrequently (every 5 minutes) using existing time updates without background alarms or tickers.
 */
object BurnInProtectionPolicy {

    /**
     * Interval in minutes between pixel-shift steps.
     * Approximately 5 minutes per step ensures minimal visual disruption while preventing
     * stationary prolonged pixel excitation on OLED screens.
     */
    const val INTERVAL_MINUTES: Long = 5L

    /**
     * Maximum allowed horizontal and vertical displacement in density-independent pixels (dp).
     */
    const val MAX_DISPLACEMENT_DP: Float = 4f

    /**
     * Deterministic 12-step offset sequence cycling over a 60-minute period (12 * 5 min).
     *
     * Characteristics:
     * - All points stay strictly within [-4dp, +4dp] for both X and Y.
     * - Every transition between consecutive steps is small (between 2.2dp and 3.2dp)
     *   for subtle, imperceptible animation.
     * - Explores all four quadrants and the neutral center to distribute wear uniformly.
     */
    val SHIFT_SEQUENCE: List<BurnInShiftOffset> = listOf(
        BurnInShiftOffset(0f, 0f),    // Step 0: Neutral center
        BurnInShiftOffset(2f, 1f),    // Step 1: Quadrant I (gentle right/down)
        BurnInShiftOffset(3f, 3f),    // Step 2: Quadrant I (diagonal right/down)
        BurnInShiftOffset(4f, 0f),    // Step 3: Positive X axis (max right)
        BurnInShiftOffset(3f, -2f),   // Step 4: Quadrant IV (right/up)
        BurnInShiftOffset(1f, -4f),   // Step 5: Quadrant IV (max up)
        BurnInShiftOffset(-1f, -3f),  // Step 6: Quadrant III (left/up)
        BurnInShiftOffset(-3f, -2f),  // Step 7: Quadrant III (diagonal left/up)
        BurnInShiftOffset(-4f, 0f),   // Step 8: Negative X axis (max left)
        BurnInShiftOffset(-3f, 2f),   // Step 9: Quadrant II (left/down)
        BurnInShiftOffset(-1f, 4f),   // Step 10: Quadrant II (max down)
        BurnInShiftOffset(1f, 2f)     // Step 11: Return path towards center
    )

    /**
     * Calculates the deterministic time bucket index for the given epoch second.
     */
    fun calculateBucketIndex(epochSecond: Long, intervalMinutes: Long = INTERVAL_MINUTES): Long {
        val epochMinutes = Math.floorDiv(epochSecond, 60L)
        return Math.floorDiv(epochMinutes, intervalMinutes)
    }

    /**
     * Resolves the shift offset for a given [Instant] and feature state.
     *
     * @param instant Current point in time.
     * @param isEnabled Whether OLED burn-in protection is active.
     * @param intervalMinutes Interval length in minutes.
     * @param maxDisplacementDp Maximum displacement constraint.
     */
    fun calculateOffset(
        instant: Instant,
        isEnabled: Boolean,
        intervalMinutes: Long = INTERVAL_MINUTES,
        maxDisplacementDp: Float = MAX_DISPLACEMENT_DP
    ): BurnInShiftOffset {
        if (!isEnabled) {
            return BurnInShiftOffset.NEUTRAL
        }

        val bucketIndex = calculateBucketIndex(instant.epochSecond, intervalMinutes)
        val sequenceIndex = Math.floorMod(bucketIndex, SHIFT_SEQUENCE.size.toLong()).toInt()
        val offset = SHIFT_SEQUENCE[sequenceIndex]

        return offset.coerceIn(maxDisplacementDp)
    }
}
