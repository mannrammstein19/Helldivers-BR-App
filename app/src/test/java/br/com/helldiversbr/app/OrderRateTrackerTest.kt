package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.Assignment
import br.com.helldiversbr.app.data.OrderRateTracker
import br.com.helldiversbr.app.data.OrderTask
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class OrderRateTrackerTest {
    private fun order(progress: Long, expiry: String = "2026-10-01T19:30:03.9961414Z") = Assignment(
        id = JsonPrimitive(1715805482L), progress = listOf(progress), expiration = expiry,
        tasks = listOf(OrderTask(type = 3, valueTypes = listOf(1, 3, 4), values = listOf(2L, 25000000L, 2651633799L))))

    @Test fun expirationDriftDoesNotResetMeasurements() {
        val tracker = OrderRateTracker()
        assertTrue(tracker.update(order(13000000), 1000).isEmpty())
        val next = order(13025000, "2026-10-01T19:30:09.2962369Z")
        assertEquals(6.0, tracker.update(next, 61000).getValue(0), 0.00001)
    }
    @Test fun rapidRefreshDoesNotReplaceBaselineAndZeroProgressIsMeasured() {
        val tracker = OrderRateTracker()
        tracker.update(order(10), 1000)
        assertTrue(tracker.update(order(10), 2000).isEmpty())
        assertEquals(0.0, tracker.update(order(10), 61000).getValue(0), 0.0)
    }
    @Test fun newOrderAndDifferentTargetNeedNewSamples() {
        val tracker = OrderRateTracker()
        tracker.update(order(0), 1000)
        tracker.update(order(1000), 61000)
        assertTrue(tracker.update(order(2000).copy(id = JsonPrimitive(999L)), 121000).isEmpty())
        val changed = order(3000).copy(id = JsonPrimitive(999L), tasks = listOf(OrderTask(3, listOf(2L,25000000L,2514244534L),listOf(1,3,4))))
        assertTrue(tracker.update(changed, 181000).isEmpty())
    }
    @Test fun missingProgressIsNotInventedAndLongGapsReset() {
        val tracker = OrderRateTracker()
        tracker.update(order(100),1000)
        assertTrue(tracker.update(order(100).copy(progress = emptyList()),61000).isEmpty())
        assertTrue(tracker.update(order(200),121000).isEmpty())
        assertTrue(tracker.update(order(300),1121000).isEmpty())
    }
    @Test fun decreasingCounterResetsInsteadOfReportingFalseNegativeRate() {
        val tracker=OrderRateTracker()
        tracker.update(order(5000),1000)
        assertTrue(tracker.update(order(1000),61000).isEmpty())
        assertTrue(tracker.update(order(2000),121000).getValue(0)>0)
    }
}
