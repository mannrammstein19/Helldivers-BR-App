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
