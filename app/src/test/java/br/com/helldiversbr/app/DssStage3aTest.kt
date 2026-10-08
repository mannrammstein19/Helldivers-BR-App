package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import org.junit.Assert.*
import org.junit.Test

class DssStage3aTest {
    private val now = 1_800_000_000_000L
    @Test fun unknownLocationHasNoDefaultPlanet() {
        assertNull(DssReading().lastPlanetIndex)
        assertEquals(0L, DssReading().lastLocationReadAtMillis)
    }
    @Test fun oldSeedIsRemovedAndRealLocationSurvives() {
        assertNull(DssReading(lastPlanetIndex=136, lastLocationReadAtMillis=1791081370763L).withoutSeededLocation().lastPlanetIndex)
        assertEquals(136L, DssReading(lastPlanetIndex=136, lastLocationReadAtMillis=now).withoutSeededLocation().lastPlanetIndex)
    }
    @Test fun fullyFundedIsStillFunding() {
        val a = DssTacticalAction(status=1, costs=listOf(DssCost(currentValue=100.0,targetValue=100.0)))
        assertEquals(DssActionPhase.FUNDING,DssActionRules.phase(a,now))
    }
    @Test fun unknownStateAndFutureDateCannotInventCooldown() {
        assertEquals(DssActionPhase.PENDING,DssActionRules.phase(DssTacticalAction(statusExpire="2099-01-01T00:00:00Z"),now))
    }
    @Test fun cooldownNeedsValidFutureDeadline() {
        for (date in listOf("", "bad", "2020-01-01T00:00:00Z"))
            assertEquals(DssActionPhase.PENDING,DssActionRules.phase(DssTacticalAction(status=3,statusExpire=date),now))
        assertEquals(DssActionPhase.COOLDOWN,DssActionRules.phase(DssTacticalAction(status=3,statusExpiresAt="2099-01-01T00:00:00Z"),now))
    }
    @Test fun missingContributionIsNotZero() {
        assertNull(DssActionRules.percent(DssCost(targetValue=100.0)))
        assertEquals(.7/86400*100,DssActionRules.percent(DssCost(currentValue=.7,targetValue=86400.0))!!,.0000001)
    }
    @Test fun estimatesRequirePositiveObservedRate() {
        for (rate in listOf(null,0.0,-1.0)) assertNull(DssActionRules.estimateSeconds(DssCost(currentValue=20.0,targetValue=100.0,deltaPerSecond=rate)))
        assertEquals(40.0,DssActionRules.estimateSeconds(DssCost(currentValue=20.0,targetValue=100.0,deltaPerSecond=2.0))!!,.001)
    }
    @Test fun oldAndSavedReadingsAreNotCurrent() {
        assertFalse(DssSupport.readingIsFresh(DssReading(fetchedAtMillis=now-300001),now))
        assertFalse(DssSupport.readingIsFresh(DssReading(fetchedAtMillis=now,source="cache"),now))
    }
}
