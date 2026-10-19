package com.kunvarpreet.standbymode.core.burnin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.sqrt

class BurnInProtectionPolicyTest {

    @Test
    fun offsetIsNeutral_whenProtectionDisabled() {
        val instants = listOf(
            Instant.ofEpochSecond(0L),
            Instant.ofEpochSecond(300L),
            Instant.ofEpochSecond(600L),
            Instant.ofEpochSecond(1729350000L),
            Instant.ofEpochSecond(2000000000L)
        )

        for (instant in instants) {
            val offset = BurnInProtectionPolicy.calculateOffset(
                instant = instant,
                isEnabled = false
            )
            assertEquals(
                "Disabled protection must always resolve to neutral (0, 0)",
                BurnInShiftOffset.NEUTRAL,
                offset
            )
        }
    }

    @Test
    fun sameTimeBucket_producesSameOffset() {
        // Given a 5-minute bucket starting at epoch 300 (00:05:00 UTC)
        val bucketStart = Instant.ofEpochSecond(300L)
        val midBucket = Instant.ofEpochSecond(450L) // +2.5 min
        val justBeforeEnd = Instant.ofEpochSecond(599L) // 00:09:59 UTC

        val offset1 = BurnInProtectionPolicy.calculateOffset(bucketStart, isEnabled = true)
        val offset2 = BurnInProtectionPolicy.calculateOffset(midBucket, isEnabled = true)
        val offset3 = BurnInProtectionPolicy.calculateOffset(justBeforeEnd, isEnabled = true)

        assertEquals("Timestamps in the same 5-minute bucket must yield identical offsets", offset1, offset2)
        assertEquals("Timestamps in the same 5-minute bucket must yield identical offsets", offset1, offset3)
    }

    @Test
    fun shiftIndexAdvances_onlyAtIntendedInterval() {
        // Boundary transition: 10:04:59 to 10:05:00
        val beforeBoundary = Instant.ofEpochSecond(300L * 10L - 1L) // 2999s -> bucket 9
        val atBoundary = Instant.ofEpochSecond(300L * 10L)          // 3000s -> bucket 10

        val offsetBefore = BurnInProtectionPolicy.calculateOffset(beforeBoundary, isEnabled = true)
        val offsetAt = BurnInProtectionPolicy.calculateOffset(atBoundary, isEnabled = true)

        val bucketBefore = BurnInProtectionPolicy.calculateBucketIndex(beforeBoundary.epochSecond)
        val bucketAt = BurnInProtectionPolicy.calculateBucketIndex(atBoundary.epochSecond)

        assertEquals("Bucket index must advance by exactly 1 at boundary", bucketBefore + 1, bucketAt)

        val expectedBefore = BurnInProtectionPolicy.SHIFT_SEQUENCE[(bucketBefore % BurnInProtectionPolicy.SHIFT_SEQUENCE.size).toInt()]
        val expectedAt = BurnInProtectionPolicy.SHIFT_SEQUENCE[(bucketAt % BurnInProtectionPolicy.SHIFT_SEQUENCE.size).toInt()]

        assertEquals(expectedBefore, offsetBefore)
        assertEquals(expectedAt, offsetAt)
    }

    @Test
    fun offsetSequence_staysWithinConfiguredBounds() {
        val maxBound = BurnInProtectionPolicy.MAX_DISPLACEMENT_DP

        for (offset in BurnInProtectionPolicy.SHIFT_SEQUENCE) {
            assertTrue(
                "X offset (${offset.xDp}) must not exceed maximum displacement ($maxBound dp)",
                Math.abs(offset.xDp) <= maxBound
            )
            assertTrue(
                "Y offset (${offset.yDp}) must not exceed maximum displacement ($maxBound dp)",
                Math.abs(offset.yDp) <= maxBound
            )
        }

        // Test over a full year of 5-minute intervals to ensure calculated offsets remain bounded
        var currentEpoch = 1729350000L // arbitrary modern epoch
        for (i in 0 until 1000) {
            val offset = BurnInProtectionPolicy.calculateOffset(
                instant = Instant.ofEpochSecond(currentEpoch),
                isEnabled = true
            )
            assertTrue(Math.abs(offset.xDp) <= maxBound)
            assertTrue(Math.abs(offset.yDp) <= maxBound)
            currentEpoch += 300L
        }
    }

    @Test
    fun sequenceCyclesDeterministically() {
        val stepCount = BurnInProtectionPolicy.SHIFT_SEQUENCE.size
        val cycleSeconds = stepCount * BurnInProtectionPolicy.INTERVAL_MINUTES * 60L // 60 minutes = 3600s

        val baseInstant = Instant.ofEpochSecond(1729350000L)
        val fullCycleLater = baseInstant.plusSeconds(cycleSeconds)
        val twoCyclesLater = baseInstant.plusSeconds(cycleSeconds * 2)

        val offsetBase = BurnInProtectionPolicy.calculateOffset(baseInstant, isEnabled = true)
        val offsetCycle1 = BurnInProtectionPolicy.calculateOffset(fullCycleLater, isEnabled = true)
        val offsetCycle2 = BurnInProtectionPolicy.calculateOffset(twoCyclesLater, isEnabled = true)

        assertEquals("Offset must match after exactly 1 full sequence cycle", offsetBase, offsetCycle1)
        assertEquals("Offset must match after 2 full sequence cycles", offsetBase, offsetCycle2)
    }

    @Test
    fun timeCorrectionsAndBoundaries_doNotProduceInvalidOffsets() {
        // Negative epoch seconds (before 1970)
        val pre1970 = Instant.ofEpochSecond(-1200L)
        val offsetPre1970 = BurnInProtectionPolicy.calculateOffset(pre1970, isEnabled = true)
        assertTrue(Math.abs(offsetPre1970.xDp) <= BurnInProtectionPolicy.MAX_DISPLACEMENT_DP)
        assertTrue(Math.abs(offsetPre1970.yDp) <= BurnInProtectionPolicy.MAX_DISPLACEMENT_DP)

        // Epoch zero (1970-01-01 00:00:00)
        val epochZero = Instant.ofEpochSecond(0L)
        val offsetZero = BurnInProtectionPolicy.calculateOffset(epochZero, isEnabled = true)
        assertEquals(BurnInProtectionPolicy.SHIFT_SEQUENCE[0], offsetZero)

        // Far future epoch (e.g. year 2040)
        val farFuture = Instant.ofEpochSecond(2208988800L)
        val offsetFuture = BurnInProtectionPolicy.calculateOffset(farFuture, isEnabled = true)
        assertTrue(Math.abs(offsetFuture.xDp) <= BurnInProtectionPolicy.MAX_DISPLACEMENT_DP)
        assertTrue(Math.abs(offsetFuture.yDp) <= BurnInProtectionPolicy.MAX_DISPLACEMENT_DP)
    }

    @Test
    fun consecutiveStepTransitions_areGentle() {
        val seq = BurnInProtectionPolicy.SHIFT_SEQUENCE
        val maxAllowedStepJump = 3.5f // Ensure step transitions never exceed 3.5dp for imperceptible animation

        for (i in seq.indices) {
            val curr = seq[i]
            val next = seq[(i + 1) % seq.size]
            val dx = next.xDp - curr.xDp
            val dy = next.yDp - curr.yDp
            val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

            assertTrue(
                "Transition from step $i to ${(i + 1) % seq.size} has distance $distance dp which must not exceed $maxAllowedStepJump dp",
                distance <= maxAllowedStepJump
            )
        }
    }
}
