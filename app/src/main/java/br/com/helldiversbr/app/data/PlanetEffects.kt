package br.com.helldiversbr.app.data

import kotlinx.serialization.json.*

object PlanetEffects {
    fun values(planet: Planet): List<JsonElement> = buildList {
        fun addBound(value: JsonElement, requireBinding: Boolean = false) {
            if (value is JsonArray) { value.forEach { addBound(it, requireBinding) }; return }
            val row = value as? JsonObject
            val binding = row?.get("planetIndex") ?: row?.get("PlanetIndex")
                ?: (if (requireBinding) row?.get("index") else null)
            if (binding != null && (binding as? JsonPrimitive)?.longOrNull != planet.index) return
            if (requireBinding && binding == null) return
            add(value)
        }
        (planet.activeEffects + planet.effects + planet.planetEffects + planet.galacticEffects + planet.modifiers)
            .forEach { addBound(it) }
        planet.planetActiveEffects.forEach { addBound(it, true) }
    }
    fun name(value: JsonElement): String = when (value) {
        is JsonPrimitive -> value.content
        is JsonObject -> localizedText(value["name"] ?: value["title"] ?: value["displayName"])
        else -> ""
    }
    fun hasId(value: JsonElement): Boolean = value is JsonObject &&
        listOf("galacticEffectId", "GalacticEffectId", "effectId", "id").any { value[it] != null }
}

/** Inspect the payload before serialization supplies empty defaults. */
internal fun Planet.withEffectPayload(payload: JsonElement, readAtMillis: Long): Planet {
    val row = payload as? JsonObject
    val complete = listOf("activeEffects", "effects", "planetEffects", "galacticEffects", "modifiers", "planetActiveEffects")
        .any { row?.get(it) is JsonArray }
    return copy(effectsComplete = complete, effectsReadAtMillis = if (complete) readAtMillis else 0L)
}

/** Legacy objects with real IDs remain useful; an omitted empty list is not proof of absence. */
internal fun Planet.hasCompleteEffectReading(): Boolean = effectsComplete ?: PlanetEffects.values(this).isNotEmpty()

/** Campaigns may update combat metrics, but the map endpoint owns its effects. */
internal fun Planet.withMapEffectsFrom(map: Planet): Planet = copy(
    activeEffects = map.activeEffects, effects = map.effects, planetEffects = map.planetEffects,
    galacticEffects = map.galacticEffects, modifiers = map.modifiers, planetActiveEffects = map.planetActiveEffects,
    effectsComplete = map.effectsComplete, effectsReadAtMillis = map.effectsReadAtMillis,
    savedPresenceKeys = map.savedPresenceKeys, presenceHistoryStale = map.presenceHistoryStale,
    savedTcsPresent = map.savedTcsPresent,
)
