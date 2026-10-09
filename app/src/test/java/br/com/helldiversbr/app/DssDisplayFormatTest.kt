package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.DssDisplayFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class DssDisplayFormatTest {
    @Test fun matchesShortCountdownInReference() {
        assertEquals("03h 55min 47s", DssDisplayFormat.duration(3 * 3600L + 55 * 60L + 47))
    }
    @Test fun matchesMultipleDayCountdownInReference() {
        assertEquals("2d 19h 05min 10s", DssDisplayFormat.duration(2 * 86400L + 19 * 3600L + 5 * 60L + 10))
    }
    @Test fun showsRemainingSecondsInsteadOfRoundingUpToOneMinute() {
        assertEquals("00h 00min 01s", DssDisplayFormat.duration(1))
        assertEquals("00h 00min 59s", DssDisplayFormat.duration(59))
    }
    @Test fun rollsOverAtDayBoundaryWithoutRepeatingTwentyFourHours() {
        assertEquals("23h 59min 59s", DssDisplayFormat.duration(86399))
        assertEquals("1d 00h 00min 00s", DssDisplayFormat.duration(86400))
    }
    @Test fun neverFormatsAnExpiredDurationAsAnUpcomingDeadline() {
        assertEquals("", DssDisplayFormat.duration(-1))
        assertEquals("00h 00min 00s", DssDisplayFormat.duration(0))
    }
}
