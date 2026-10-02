package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.notifications.retainUnobserved
import br.com.helldiversbr.app.update.*
import org.junit.Assert.*
import org.junit.Test

class ReviewReliabilityTest {
    @Test fun missingRegionsDoNotEraseAttackBaseline() {
        val previous=setOf("199:0", "199:1")
        assertEquals(previous,retainUnobserved(previous,emptySet(),emptySet()))
        assertEquals(setOf("199:1"),retainUnobserved(previous,setOf("199:0"),emptySet()))
        assertTrue((setOf("199:0") - retainUnobserved(previous,emptySet(),emptySet())).isEmpty())
    }
    @Test fun latestMapOwnershipWinsAgainstOlderCampaign() {
        val map=Planet(index=199,currentOwner="Humans")
        val old=Campaign(planet=map.copy(currentOwner="Automatons"))
        assertEquals("Humans",mergeMapPlanets(listOf(map),listOf(old),false).single().currentOwner)
    }
    @Test fun fresherCampaignCanReplaceMapWithoutDuplicatePlanet() {
        val map=Planet(index=199,currentOwner="Automatons",waypoints=listOf(114))
        val fresh=Campaign(planet=map.copy(currentOwner="Humans",waypoints=emptyList()))
        val merged=mergeMapPlanets(listOf(map),listOf(fresh),true).single()
        assertEquals("Humans",merged.currentOwner)
        assertEquals(listOf(114L),merged.waypoints)
    }
    @Test fun emptyVersionMetadataCannotClaimLatest() {
        assertTrue(evaluateVersion(RemoteVersion(),31) is UpdateCheckResult.Failed)
    }
    @Test fun newerVersionWithInvalidDownloadCannotClaimLatest() {
        assertTrue(evaluateVersion(RemoteVersion(32,"32.0.0","https://"),31) is UpdateCheckResult.Failed)
    }
    @Test fun validNewVersionIsAvailableAndInstalledVersionIsLatest() {
        val remote=RemoteVersion(32,"32.0.0","https://example.com/app.apk")
        assertTrue(evaluateVersion(remote,31) is UpdateCheckResult.Available)
        assertEquals(UpdateCheckResult.Latest,evaluateVersion(remote,32))
    }

    @Test fun endedInvasionCannotReturnFromAnOlderHomeCampaign() {
        val newer = Planet(index=199,currentOwner="Humans",event=null)
        val old = Campaign(planet=newer.copy(event=PlanetEvent(id=10)))
        assertNull(mergeMapPlanets(listOf(newer),listOf(old),false).single().event)
    }
    @Test fun newlyInvadedPlanetKeepsItsNewerMapEvent() {
        val invaded = Planet(index=199,currentOwner="Humans",event=PlanetEvent(id=11))
        val old = Campaign(planet=invaded.copy(event=null))
        assertEquals(11L,mergeMapPlanets(listOf(invaded),listOf(old),false).single().event!!.id)
    }
}
