package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.TelemetryRefreshPolicy
import org.junit.Assert.*
import org.junit.Test

class TelemetryRefreshPolicyTest {
    @Test fun staleAbsenceCannotEraseSavedStation() {
        assertTrue(TelemetryRefreshPolicy.preservePreviousStation(100L, 200L, true, false, true))
    }
    @Test fun regressiveReadingNeverReplacesSavedStation() {
        assertTrue(TelemetryRefreshPolicy.preservePreviousStation(200L, 100L, true, true, false))
    }
    @Test fun freshConfirmedAbsenceIsNotPresentedAsAnExistingStation() {
        assertFalse(TelemetryRefreshPolicy.preservePreviousStation(100L, 200L, true, false, false))
    }
    @Test fun newValidLocationCanReplaceHistory() {
        assertFalse(TelemetryRefreshPolicy.preservePreviousStation(100L, 200L, true, true, false))
    }
    @Test fun firstReadingDoesNotInventHistory() {
        assertFalse(TelemetryRefreshPolicy.preservePreviousStation(0L, 200L, false, false, true))
    }
}
