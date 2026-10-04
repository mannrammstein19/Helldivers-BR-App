package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.screens.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.Instant

class CentralMapIntegrationTest {
    private val now = 1_800_000_000_000L
    private fun body(time: Long = now, stale: String="false", source:String="direct") = """{"data":[],"time":$time,"source":"$source","stale":$stale,"next":${now+60000}}"""
    @Test fun observationIsNotDownloadTime() { assertEquals(now-20000,CentralApi.decode(body(now-20000),now).time) }
    @Test fun savedReadingStaysSaved() { assertTrue(CentralApi.decode(body(now-20000,"true"),now).stale) }
    @Test fun invalidClockIsRejected() {
        listOf(0L,now+120000,now-86400001).forEach { assertTrue(runCatching { CentralApi.decode(body(it),now) }.isFailure) }
    }
    @Test fun unknownSourceCannotClaimFreshness() { assertTrue(runCatching { CentralApi.decode(body(source="cache"),now) }.isFailure) }
    @Test fun newOrderWinsOlderFinalSnapshot() {
        val old=Assignment(id=JsonPrimitive(1));val live=Assignment(id=JsonPrimitive(2))
        val ui=OrderRepository.resolveCentralOrder(live,OrderSnapshot(state="failed",order=old),null,true)
        assertEquals(live,ui.order);assertEquals("active",ui.state)
    }
    @Test fun confirmedResultWinsOnlySameOrder() {
        val order=Assignment(id=JsonPrimitive(1))
        assertEquals("failed",OrderRepository.resolveCentralOrder(order,OrderSnapshot(state="failed",order=order),null,true).state)
    }
    @Test fun expirationDoesNotInventFailure() {
        val order=Assignment(id=JsonPrimitive(1),expiration="2000-01-01T00:00:00Z")
        assertEquals("pending",OrderRepository.resolveCentralOrder(order,null,null,true).state)
    }
    @Test fun confirmedAbsenceClearsUnconfirmedOldOrder() {
        assertNull(OrderRepository.resolveCentralOrder(null,null,OrderUi(Assignment(),"active",40.0,false),true).order)
    }
    @Test fun regionsKeepActualReadingTimeAndStaleFlag() {
        val r=CentralApi.decode(body(now-10000,"true"),now)
        val p=Planet(regions=listOf(PlanetRegion(telemetryReadAtMillis=now-20000))).withCentralReading(r)
        assertEquals(now-20000,p.regions.single().telemetryReadAtMillis);assertTrue(p.regions.single().telemetryStale)
    }
    @Test fun independentDefenseProgress() {
        val e=PlanetEvent(health=750,maxHealth=1000,startTime=Instant.ofEpochMilli(now-3600000).toString(),endTime=Instant.ofEpochMilli(now+3600000).toString())
        assertEquals(25.0,OrderRepository.campaignPercent(Campaign(planet=Planet(event=e))),.001)
        assertEquals(50.0,OrderRepository.defenseEnemyProgress(e,now)!!,.001)
    }
    @Test fun missingClockNeverBecomesFullInvasion() { assertNull(OrderRepository.defenseEnemyProgress(PlanetEvent(),now)) }
    @Test fun savedInvasionUsesFrozenClock() {
        val e=PlanetEvent(startTime=Instant.ofEpochMilli(now-1000).toString(),endTime=Instant.ofEpochMilli(now+1000).toString())
        assertEquals(50.0,OrderRepository.defenseEnemyProgress(e,now)!!,.001)
    }
    private fun initPresences() { PlanetPresences.init(File("src/main/assets/map-presences.json").readText()) }
    @Test fun pairedIdsDeduplicateAndHumanOwnerDoesNotHidePresence() {
        initPresences();val p=Planet(currentOwner="Humans",activeEffects=listOf(JsonPrimitive(1379),JsonPrimitive(1380)))
        assertEquals(listOf("appropriators"),PlanetPresences.list(p).map { it.key })
    }
    @Test fun formationsFollowConfirmedCatalogModels() {
        initPresences();val p=Planet(activeEffects=listOf(JsonPrimitive(1377),JsonPrimitive(1402),JsonPrimitive(1413)))
        val entries=PlanetPresences.list(p).associateBy { it.key }
        assertEquals(3,entries.getValue("masses").formation);assertEquals("nave-iluminada",entries.getValue("masses").model)
        assertEquals("nave-raptores",entries.getValue("snatchers").model);assertEquals("nave-frota-iluminada",entries.getValue("fleet").model)
    }
    @Test fun campaignTypesNeverInventUrgency() {
        assertEquals("LIBERTAÇÃO",campaignLabel(Campaign(type=0)))
        assertEquals("RECONHECIMENTO",campaignLabel(Campaign(type=1)))
        assertEquals("CAMPANHA",campaignLabel(Campaign(type=999)))
        assertEquals("DEFESA",campaignLabel(Campaign(type=1,planet=Planet(event=PlanetEvent()))))
    }
    @Test fun titleCasePreservesRomanSuffixes() { assertEquals("Choohe Prime",planetTitle("CHOOHE PRIME"));assertEquals("Matar IV",planetTitle("MATAR IV")) }
    @Test fun everyBundledAssetExists() {
        val root=File("src/main/assets")
        listOf("map-assets.json","map-planet-icons.json").forEach { name ->
            CentralApi.json.parseToJsonElement(File(root,name).readText()).jsonObject.values.forEach {
                assertTrue(it.toString(),File(root,it.jsonPrimitive.content).isFile)
            }
        }
        assertEquals(271,CentralApi.json.parseToJsonElement(File(root,"map-planet-icons.json").readText()).jsonObject.size)
    }
    @Test fun mapPresetsDoNotTieNamesToCounters() {
        assertTrue(MapDisplayOptions().copy(names=false).players)
        assertFalse(MapDisplayOptions.preset(true).motion)
    }
    @Test fun oldSnapshotCannotReplaceNewerOrderAfterConnectionLoss() {
        val previous=OrderUi(Assignment(id=JsonPrimitive(2)),"active",50.0,false)
        val snapshot=OrderSnapshot(state="failed",order=Assignment(id=JsonPrimitive(1)))
        assertEquals(previous,OrderRepository.resolveCentralOrder(null,snapshot,previous,false))
    }
    @Test fun lastDssLocationIsNotOperationalProof() {
        val r=CentralApi.decode(body(),now)
        val old=DssReading(station=SpaceStation(planet=Planet(index=100)),stale=true)
        val fresh=Planet(index=200,activeEffects=listOf(JsonPrimitive(1217)))
        val resolved=old.withPlanetReference(listOf(fresh),r)
        assertEquals(200L,resolved.lastPlanetIndex);assertFalse(resolved.isLive)
    }

    @Test fun directClockIsAnchoredToStatusNotWarStartDate() {
        assertEquals(now-100000L,DirectGameApi.clockBase(100L,now))
        assertNull(DirectGameApi.clockBase(null,now));assertNull(DirectGameApi.clockBase(-1L,now))
    }
    @Test fun unknownPresenceIdNeverAcquiresKnownName() {
        initPresences()
        assertTrue(PlanetPresences.list(Planet(activeEffects=listOf(JsonObject(mapOf("id" to JsonPrimitive(999999),"name" to JsonPrimitive("CYBORGS")))))).isEmpty())
    }

    private fun defenseData(elapsed:Long=3600000,stale:Boolean=false,ended:Boolean=false,observed:Double?=null): Pair<HomeData,Campaign> {
        val event=PlanetEvent(id=77,health=750,maxHealth=1000,startTime=Instant.ofEpochMilli(now-elapsed).toString(),endTime=Instant.ofEpochMilli(now+if(ended)-1000 else 3600000).toString())
        val c=Campaign(planet=Planet(index=10,event=event))
        return HomeData(order=OrderUi(null,"pending",0.0,false),dispatches=emptyList(),planetNames=emptyMap(),planetCatalog=emptyMap(),
            campaigns=listOf(c),campaignRates=observed?.let { mapOf(OrderRepository.campaignKey(c) to it) }.orEmpty(),dss=DssReading(),updatedAtMillis=now,
            staleSources=if(stale) listOf("campanhas") else emptyList(),campaignReadAtMillis=now) to c
    }
    @Test fun defenseAverageUsesActualElapsedObservationTime() {
        val (d,c)=defenseData();assertEquals(25.0,OrderRepository.campaignDisplayRate(d,c)!!,.001)
    }
    @Test fun newlyStartedDefenseWaitsForSamples() {
        val (d,c)=defenseData(elapsed=60000);assertNull(OrderRepository.campaignDisplayRate(d,c))
    }
    @Test fun savedDefenseDoesNotCreateFreshForecast() {
        val (d,c)=defenseData(stale=true,observed=4.0);assertNull(OrderRepository.campaignDisplayRate(d,c))
    }
    @Test fun expiredDefenseDoesNotCreateAverageProjection() {
        val (d,c)=defenseData(ended=true);assertNull(OrderRepository.campaignDisplayRate(d,c))
    }
    @Test fun actualZeroRateIsNotReplacedWithAverage() {
        val (d,c)=defenseData(observed=0.0);assertEquals(0.0,OrderRepository.campaignDisplayRate(d,c)!!,.001)
    }

}
