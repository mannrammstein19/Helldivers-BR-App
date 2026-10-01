package br.com.helldiversbr.app.ui.presentation

import org.junit.Assert.*
import org.junit.Test

class NumberProjectionTest {
    private val start = 1_000_000L
    private fun observation(value: Double, percentage: Boolean = true, source: String = "community", key: String = "planet") =
        NumberObservation(key, value, percentage, source)
    private fun frame(first: Double, second: Double, percentage: Boolean = true): NumberFrame {
        val tracker = NumberProjectionTracker()
        tracker.update(listOf(observation(first, percentage)), start, start)
        return tracker.update(listOf(observation(second, percentage)), start + 60_000, start + 60_000)
    }

    @Test fun firstReadingStaysConfirmed() {
        val tracker = NumberProjectionTracker()
        val result = tracker.update(listOf(observation(41.4341)), start, start)
        assertEquals(41.4341, result.value("planet", 41.4341, start + 20_000), .00000001)
    }

    @Test fun positiveAndNegativeProgressFollowObservedRate() {
        assertEquals(41.43885, frame(41.4301, 41.4376).value("planet", 41.4376, start + 70_000), .00000001)
        assertEquals(41.43135, frame(41.4401, 41.4326).value("planet", 41.4326, start + 70_000), .00000001)
    }

    @Test fun microscopicProgressHasHardLimit() {
        assertEquals(50.0099, frame(10.0, 50.0).value("planet", 50.0, start + 120_000), .00000001)
        assertEquals(49.9901, frame(90.0, 50.0).value("planet", 50.0, start + 120_000), .00000001)
    }

    @Test fun completedAndUnstartedRemainConfirmed() {
        assertEquals(100.0, frame(99.0, 100.0).value("planet", 100.0, start + 70_000), 0.0)
        assertEquals(0.0, frame(1.0, 0.0).value("planet", 0.0, start + 70_000), 0.0)
        assertEquals(99.9999, frame(99.98, 99.995).value("planet", 99.995, start + 120_000), .00000001)
    }

    @Test fun staleReadingAndClockRollbackShowActual() {
        val result = frame(10.0, 10.005)
        assertEquals(10.005, result.value("planet", 10.005, start + 151_000), 0.0)
        assertEquals(10.005, result.value("planet", 10.005, start - 1), 0.0)
    }

    @Test fun countersAdvanceForAtMostOneObservedInterval() {
        val result = frame(1000.0, 7000.0, false)
        assertEquals(8000.0, result.value("planet", 7000.0, start + 70_000), 0.0)
        assertEquals(13000.0, result.value("planet", 7000.0, start + 140_000), 0.0)
    }

    @Test fun noCounterMotionWhenUnchangedOrReset() {
        assertEquals(7000.0, frame(7000.0, 7000.0, false).value("planet", 7000.0, start + 70_000), 0.0)
        assertEquals(1000.0, frame(7000.0, 1000.0, false).value("planet", 1000.0, start + 70_000), 0.0)
    }

    @Test fun sourceChangeAndLongGapDiscardTrend() {
        val tracker = NumberProjectionTracker()
        tracker.update(listOf(observation(10.0)), start, start)
        val direct = tracker.update(listOf(observation(20.0, source = "direct")), start + 60_000, start + 60_000)
        assertEquals(20.0, direct.value("planet", 20.0, start + 70_000), 0.0)
        val late = tracker.update(listOf(observation(30.0, source = "direct")), start + 160_000, start + 160_000)
        assertEquals(30.0, late.value("planet", 30.0, start + 170_000), 0.0)
    }

    @Test fun cacheOrFailureResetRequiresTwoNewReadings() {
        val tracker = NumberProjectionTracker()
        tracker.update(listOf(observation(10.0)), start, start)
        tracker.update(listOf(observation(10.005)), start + 60_000, start + 60_000)
        assertTrue(tracker.reset().entries.isEmpty())
        val recovered = tracker.update(listOf(observation(10.01)), start + 70_000, start + 70_000)
        assertEquals(10.01, recovered.value("planet", 10.01, start + 80_000), 0.0)
    }

    @Test fun differentSnapshotNeverUsesAnotherReadingsProjection() {
        assertEquals(10.1, frame(10.0, 10.005).value("planet", 10.1, start + 70_000), 0.0)
    }

    @Test fun rapidTapsAndInvalidNumbersDoNotCreateRate() {
        val tracker = NumberProjectionTracker()
        tracker.update(listOf(observation(10.0)), start, start)
        val quick = tracker.update(listOf(observation(11.0), observation(Double.NaN, key = "bad")), start + 500, start + 500)
        assertEquals(11.0, quick.value("planet", 11.0, start + 1000), 0.0)
        assertFalse(quick.entries.containsKey("bad"))
    }

    @Test fun removedOrStaleFieldCannotKeepPreviousHistory() {
        val tracker = NumberProjectionTracker()
        tracker.update(listOf(observation(10.0)), start, start)
        assertTrue(tracker.update(emptyList(), start + 60_000, start + 60_000).entries.isEmpty())
        val back = tracker.update(listOf(observation(20.0)), start + 70_000, start + 70_000)
        assertEquals(20.0, back.value("planet", 20.0, start + 80_000), 0.0)
    }
}
