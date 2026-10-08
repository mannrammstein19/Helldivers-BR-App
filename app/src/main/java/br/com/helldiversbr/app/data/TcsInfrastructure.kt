package br.com.helldiversbr.app.data

import kotlinx.serialization.json.*
import java.util.Locale

enum class TcsState(val label: String, val note: String) {
    ALLIED("CONTROLE ALIADO", "Controle aliado não confirma reparo ou operação das torres."),
    ATTACKED("SOB ATAQUE", "A defesa do planeta não confirma perda nem desligamento das torres."),
    COMPROMISED("COMPROMETIDO", "Controle inimigo; a API ainda registra a infraestrutura."),
    UNKNOWN("SEM CONFIRMAÇÃO", "Infraestrutura registrada, mas o controle não foi identificado.")
}
object TcsInfrastructure {
    private fun matches(value: JsonElement, planet: Long, requireBinding: Boolean): Boolean {
        if (value is JsonArray) return value.any { matches(it, planet, requireBinding) }
        val row = value as? JsonObject
        val binding = row?.get("planetIndex") ?: row?.get("PlanetIndex")
            ?: (if (requireBinding) row?.get("index") else null)
        if (binding != null && (binding as? JsonPrimitive)?.longOrNull != planet) return false
        if (requireBinding && binding == null) return false
        val id = PlanetPresences.effectId(value)
        if (id != null) return id == 1395L
        if (row != null && listOf("galacticEffectId", "GalacticEffectId", "effectId", "id").any { row[it] != null }) return false
        val name = if (value is JsonPrimitive) value.content else localizedText(row?.get("name") ?: row?.get("title") ?: row?.get("displayName"))
        return name.trim().equals("TERMINID CONTROL SYSTEM+", true)
    }
    fun has(p: Planet): Boolean =
        (p.activeEffects + p.effects + p.planetEffects + p.galacticEffects + p.modifiers).any { matches(it, p.index, false) } ||
            p.planetActiveEffects.any { matches(it, p.index, true) }
    fun state(p: Planet): TcsState = when (p.currentOwner.trim().lowercase(Locale.ROOT)) {
        "1", "human", "humans", "super earth", "super terra", "superterra" -> if (p.event == null) TcsState.ALLIED else TcsState.ATTACKED
        "2", "3", "4", "terminid", "terminids", "automaton", "automatons", "illuminate", "illuminates" -> TcsState.COMPROMISED
        else -> TcsState.UNKNOWN
    }
    /** Same deterministic offset and 40-frame/120ms cycle as the site. */
    fun frame(index: Long, elapsedMillis: Long): Int {
        var hash = 2166136261L
        index.toString().forEach { hash = ((hash xor it.code.toLong()) * 16777619L) and 0xffffffffL }
        return (Math.floorMod(elapsedMillis + hash % 4800L, 4800L) / 120L).toInt()
    }
}
