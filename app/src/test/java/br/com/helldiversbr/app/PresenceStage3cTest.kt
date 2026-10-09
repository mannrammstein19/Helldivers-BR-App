package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import kotlinx.serialization.json.*
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PresenceStage3cTest {
    @Before fun catalog() { PlanetPresences.init(File("src/main/assets/map-presences.json").readText()) }
    @Test fun limitedFallbackRetainsKnownShipsOnSamePlanet() {
        val old = Planet(index = 21, effects = listOf(JsonPrimitive(1248)))
        val limited = Planet(index = 21).withSavedPresences(old, true)
        assertTrue(limited.presenceHistoryStale)
        assertEquals("fire", PlanetPresences.list(limited).single().key)
        assertEquals("nave-automata", PlanetPresences.list(limited).single().model)
        assertTrue(PlanetEffects.values(limited).isEmpty()) // histórico não vira efeito tático
    }
    @Test fun historyNeverTransfersToAnotherPlanet() {
        val old = Planet(index = 21, effects = listOf(JsonPrimitive(1248)))
        assertTrue(PlanetPresences.list(Planet(index = 22).withSavedPresences(old, true)).isEmpty())
    }
    @Test fun authoritativeRemovalClearsHistoryAndNewPresenceReplacesIt() {
        val old = Planet(index = 21, effects = listOf(JsonPrimitive(1248)))
        val saved = Planet(index = 21).withSavedPresences(old, true)
        val removed = saved.withSavedPresences(saved, false)
        assertTrue(PlanetPresences.list(removed).isEmpty())
        assertFalse(removed.presenceHistoryStale)
        val fresh = Planet(index = 21, effects = listOf(JsonPrimitive(1360))).withSavedPresences(saved, false)
        assertEquals("cyborg", PlanetPresences.list(fresh).single().key)
    }
    @Test fun explicitBindingCannotLeak() {
        val row=buildJsonObject { put("planetIndex",21);put("galacticEffectId",1202) }
        assertTrue(PlanetPresences.list(Planet(index=20,activeEffects=listOf(row))).isEmpty())
        assertEquals("jet",PlanetPresences.list(Planet(index=21,activeEffects=listOf(row))).single().key)
    }
    @Test fun groupedEffectNeedsBinding() {
        assertTrue(PlanetPresences.list(Planet(planetActiveEffects=listOf(JsonPrimitive(1202)))).isEmpty())
    }
    @Test fun alternateFieldsAndExactAliasesWork() {
        assertEquals("jet",PlanetPresences.list(Planet(modifiers=listOf(JsonPrimitive("THE JET BRIGADE")))).single().key)
        assertEquals("jet",PlanetPresences.list(Planet(effects=listOf(JsonPrimitive(1202)))).single().key)
        assertTrue(PlanetPresences.list(Planet(modifiers=listOf(JsonPrimitive("Maybe THE JET BRIGADE")))).isEmpty())
    }
    @Test fun unknownIdNeverBorrowsName() {
        val row=buildJsonObject { put("id",999);put("name","THE JET BRIGADE") }
        assertTrue(PlanetPresences.list(Planet(activeEffects=listOf(row))).isEmpty())
    }
    @Test fun pairedIdsDeduplicateAndRemovalClearsPresence() {
        assertEquals(1,PlanetPresences.list(Planet(activeEffects=listOf(JsonPrimitive(1202),JsonPrimitive(1203)))).size)
        assertTrue(PlanetPresences.list(Planet(currentOwner="Automatons")).isEmpty())
    }
    @Test fun galleryUsesRealLocalImagesAndListsMissingUnits() {
        val assets=File("src/main/assets")
        val mapping=CentralApi.json.parseToJsonElement(File(assets,"map-assets.json").readText()).jsonObject
        val gallery=CentralApi.json.parseToJsonElement(File(assets,"presence-gallery.json").readText()).jsonObject
        gallery.values.forEach { entry -> entry.jsonObject.getValue("names").jsonArray.forEach { name ->
            assertTrue(File(assets,mapping.getValue("presence-unit:"+name.jsonPrimitive.content).jsonPrimitive.content).isFile)
        } }
        assertEquals(listOf("Obtruder","Gatekeeper","Veracitor"),gallery.getValue("appropriators").jsonObject.getValue("missing").jsonArray.map { it.jsonPrimitive.content })
        assertEquals("Crusher",gallery.getValue("snatchers").jsonObject.getValue("missing").jsonArray.single().jsonPrimitive.content)
    }
}
