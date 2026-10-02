package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.screens.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class RegionTelemetryTest {
    // Relevant fields from the real WarSeason 801 response inspected on 01/10/2026.
    private val body = """{"planetRegions":[
        {"planetIndex":199,"regionIndex":1,"owner":3,"health":400000,"isAvailable":false,"availabilityFactor":0.29649,"players":0,"regerPerSecond":1.1111112},
        {"planetIndex":199,"regionIndex":0,"owner":1,"health":100000,"isAvailable":false,"availabilityFactor":1,"players":583,"regerPerSecond":0.2777778},
        {"planetIndex":114,"regionIndex":1,"owner":3,"health":200000,"isAvailable":true,"availabilityFactor":1,"players":436}
    ]}"""
    private fun readings(time: Long = 1000) = RegionTelemetry.parse(body, time, "community-raw")
    private val songguo = PlanetRegion(id=0,hash=3421372928,name=JsonPrimitive("SONGGUO CUN"),maxHealth=100000)
    private val xin = PlanetRegion(id=1,hash=4087006817,name=JsonPrimitive("XIN FUZHOU"),maxHealth=400000)

    @Test fun realPayloadDistinguishesCleanBlockedAndAvailable() {
        val martale = RegionTelemetry.enrich(Planet(index=199,regions=listOf(songguo,xin)),readings(),null,1000,true)
        assertEquals(RegionState.RECOVERED,regionPresentation(martale.regions[0]).state)
        assertEquals(100000L,martale.regions[0].health)
        assertEquals(RegionState.BLOCKED,regionPresentation(martale.regions[1]).state)
        val aurora = RegionTelemetry.enrich(Planet(index=114,regions=listOf(xin.copy(maxHealth=200000))),readings(),null,1000,true)
        assertEquals(RegionState.AVAILABLE,regionPresentation(aurora.regions.single()).state)
        assertEquals(0.0,regionPresentation(aurora.regions.single()).percent!!,0.001)
    }
    @Test fun regionIdDoesNotDependOnResponseOrDisplayOrder() {
        val p=RegionTelemetry.enrich(Planet(index=199,regions=listOf(xin,songguo)),readings(),null,1000,true)
        assertEquals(3,RegionTelemetry.ownerId(p.regions[0].owner))
        assertEquals(1,RegionTelemetry.ownerId(p.regions[1].owner))
        assertEquals(0.2777778,p.regions[1].regenPerSecond!!,0.000001)
    }
    @Test fun missingRegionIdMustNotGuessByArrayPosition() {
        val p=RegionTelemetry.enrich(Planet(index=199,regions=listOf(songguo.copy(id=null))),readings(),null,1000,true)
        assertNull(p.regions.single().owner)
        assertEquals(RegionState.UNKNOWN,regionPresentation(p.regions.single()).state)
    }
    @Test fun zeroHealthDoesNotInventHumanOwnership() {
        val missing=PlanetRegion(health=0,maxHealth=100000,isAvailable=false)
        assertEquals(RegionState.UNKNOWN,regionPresentation(missing).state)
        assertEquals(RegionState.BLOCKED,regionPresentation(missing.copy(owner=JsonPrimitive(3))).state)
    }
    @Test fun savedConfirmationPreservesTimeAndNeverLooksLive() {
        val previous=RegionTelemetry.enrich(Planet(index=199,regions=listOf(songguo)),readings(),null,1000,true)
        val current=RegionTelemetry.enrich(Planet(index=199,regions=listOf(songguo)),emptyMap(),previous,10000,true)
        assertTrue(current.regions.single().telemetryStale)
        assertEquals(1000L,current.regions.single().telemetryReadAtMillis)
        assertEquals(RegionState.RECOVERED,regionPresentation(current.regions.single()).state)
    }
    @Test fun freshEnemyOwnershipOverridesSavedRecovery() {
        val previous=Planet(index=199,regions=listOf(songguo.copy(owner=JsonPrimitive(1),isAvailable=false,telemetryReadAtMillis=1000)))
        val lost=readings(2000).toMutableMap()
        val key=RegionTelemetry.Key(199,0)
        lost[key]=lost.getValue(key).copy(owner=3,available=true)
        val current=RegionTelemetry.enrich(previous,lost,previous,2000,true)
        assertEquals(3,RegionTelemetry.ownerId(current.regions.single().owner))
        assertFalse(current.regions.single().telemetryStale)
        assertEquals(RegionState.AVAILABLE,regionPresentation(current.regions.single()).state)
    }
    @Test fun expiredSnapshotCannotRefreshTimestamp() {
        val p=RegionTelemetry.enrich(Planet(index=199,regions=listOf(songguo)),readings(1000),null,100000,true)
        assertNull(p.regions.single().owner)
        assertTrue(p.regions.single().telemetryStale)
    }
    @Test fun absentOrUnknownOwnerCannotConfirmRecovery() {
        val invalid="""{"planetRegions":[{"planetIndex":199,"regionIndex":0,"owner":9},{"planetIndex":199,"regionIndex":1}]}"""
        assertTrue(RegionTelemetry.parse(invalid,1000,"direct").isEmpty())
        assertEquals(RegionState.AVAILABLE,regionPresentation(songguo.copy(owner=JsonPrimitive(1),isAvailable=true)).state)
        assertEquals(RegionState.UNKNOWN,regionPresentation(songguo.copy(owner=JsonPrimitive(1))).state)
    }
    @Test fun reusedIdWithDifferentHashCannotInheritSavedRecovery() {
        val previous=Planet(index=199,regions=listOf(songguo.copy(owner=JsonPrimitive(1),isAvailable=false)))
        val replacement=Planet(index=199,regions=listOf(songguo.copy(hash=123)))
        val current=RegionTelemetry.enrich(replacement,emptyMap(),previous,1000,true)
        assertNull(current.regions.single().owner)
        assertEquals(RegionState.UNKNOWN,regionPresentation(current.regions.single()).state)
    }

    @Test fun partialPayloadKeepsConfirmedRecoveryWithOriginalDate() {
        val previous = Planet(index=199, regions=listOf(songguo.copy(owner=JsonPrimitive(1),
            isAvailable=false,health=100000,telemetryReadAtMillis=1000)))
        val partial = RegionTelemetry.parse("""{"planetRegions":[{"planetIndex":199,"regionIndex":0,"owner":1}]}""",2000,"direct")
        val current = RegionTelemetry.enrich(Planet(index=199,regions=listOf(songguo)),partial,previous,2000,true).regions.single()
        assertEquals(RegionState.RECOVERED,regionPresentation(current).state)
        assertTrue(current.telemetryStale)
        assertEquals(1000L,current.telemetryReadAtMillis)
    }
    @Test fun changedOwnerCannotInheritRecoveredAvailabilityFromPartialPayload() {
        val previous = Planet(index=199, regions=listOf(songguo.copy(owner=JsonPrimitive(1),isAvailable=false)))
        val partial = RegionTelemetry.parse("""{"planetRegions":[{"planetIndex":199,"regionIndex":0,"owner":3}]}""",2000,"direct")
        val current = RegionTelemetry.enrich(Planet(index=199,regions=listOf(songguo)),partial,previous,2000,true).regions.single()
        assertEquals(3,RegionTelemetry.ownerId(current.owner))
        assertEquals(RegionState.UNKNOWN,regionPresentation(current).state)
    }
    @Test fun diskSnapshotCannotAppearAsLiveRegionalConfirmation() {
        val planet = Planet(regions=listOf(songguo.copy(owner=JsonPrimitive(1),isAvailable=false,telemetryReadAtMillis=1000)))
        val saved = planet.asSavedTelemetry().regions.single()
        assertTrue(saved.telemetryStale)
        assertEquals(1000L,saved.telemetryReadAtMillis)
        assertEquals(RegionState.RECOVERED,regionPresentation(saved).state)
    }
}
