package br.com.helldiversbr.app.ui.screens

import br.com.helldiversbr.app.data.Planet
import br.com.helldiversbr.app.data.PlanetPosition
import br.com.helldiversbr.app.data.searchKey
import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

fun mapFaction(raw: String): String = when {
    "human" in raw.lowercase() || raw == "1" -> "human"
    "terminid" in raw.lowercase() || raw == "2" -> "terminid"
    "automaton" in raw.lowercase() || raw == "3" -> "automaton"
    "illuminate" in raw.lowercase() || raw == "4" -> "illuminate"
    else -> "unknown"
}
fun mapColor(key: String): Color = when (key) {
    "human" -> Color(0xFF4DA6FF)
    "terminid" -> Color(0xFFFF9900)
    "automaton" -> Color(0xFFFF4242)
    "illuminate" -> Color(0xFF8B3FD6)
    else -> Color(0xFF6B7280)
}
fun mapName(p: Planet) = p.nameText.lowercase().replace('’', '\'').replace('‘', '\'').trim()
fun mapEarth(p: Planet) = mapName(p) in listOf("super earth", "super terra", "superterra")
fun mapSpecial(p: Planet): String? = when (mapName(p)) {
    "penta" -> "penta"
    "meridia" -> "meridia"
    "ivis", "moradesh", "angel's venture" -> "wreckage"
    else -> null
}
fun mapPosition(p: Planet, all: List<Planet>): PlanetPosition? {
    if (mapEarth(p)) return PlanetPosition(0.0, 0.0)
    val pos = p.mapPosition ?: return null
    if (p.disabled || !pos.x.isFinite() || !pos.y.isFinite() || (abs(pos.x) < 1e-7 && abs(pos.y) < 1e-7)) return null
    val name = mapName(p)
    if (name == "meridia" || name == "ivis") {
        val other = all.firstOrNull { mapName(it) == if (name == "meridia") "ivis" else "meridia" }?.mapPosition
        if (other != null && hypot(pos.x - other.x, pos.y - other.y) < .09)
            return PlanetPosition((pos.x + other.x) / 2 + if (name == "meridia") -.045 else .045, pos.y)
    }
    return pos
}
fun mapOffensive(p: Planet, active: Set<Long>) = mapSpecial(p) == null && p.event == null && p.index in active && mapFaction(p.currentOwner) !in listOf("human", "unknown")
fun mapProgress(p: Planet): Double? {
    val health = p.event?.health ?: p.health
    val max = p.event?.maxHealth ?: p.maxHealth
    return if (max > 0 && health in 0..max) (1.0 - health.toDouble() / max) * 100 else null
}
fun mapInvasionProgress(p: Planet, now: Long): Double? {
    val e = p.event ?: return null
    return runCatching {
        val start = Instant.parse(e.startTime).toEpochMilli()
        val end = Instant.parse(e.endTime).toEpochMilli()
        if (end > start) ((now - start).toDouble() / (end - start) * 100).coerceIn(0.0, 100.0) else null
    }.getOrNull()
}
fun mapPercent(value: Double?): String {
    if (value == null || !value.isFinite()) return "—"
    val pct = value.coerceIn(0.0, 100.0)
    val digits = if (pct > 0 && pct < .1) 2 else if (pct < 10) 1 else 0
    return "%.$digits".plus("f%%").format(Locale("pt", "BR"), pct)
}
fun sectorKey(name: String) = searchKey(name).replace(Regex("[^a-z0-9]"), "")

fun mapPlayerCount(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM".format(Locale("pt", "BR"), n / 1_000_000.0)
    n >= 1_000 -> "%.1fK".format(Locale("pt", "BR"), n / 1_000.0)
    else -> n.toString()
}

/** Evento de defesa recebido da API; libertações não são alertas de invasão. */
fun isPlanetUnderAttack(planet: br.com.helldiversbr.app.data.Planet): Boolean =
    !planet.disabled && planet.event != null && mapSpecial(planet) == null

/** Name color is independent from blue human progress and supply routes. */
fun mapNameColor(owner: String): Color = if (mapFaction(owner) == "human") Color(0xFFD7D52C) else mapColor(mapFaction(owner))
fun mapRouteColor(owner: String): Color = if (mapFaction(owner) == "human") Color(0xFF66C9F1) else mapColor(mapFaction(owner))

data class MapAttackLink(val source: Long, val target: Long, val faction: String)
/** Direction comes from attacking IDs, never merely from a border or a presence. */
fun mapAttackLinks(planets: List<Planet>, active: Set<Long>): List<MapAttackLink> {
    val indexed = planets.associateBy { it.index }
    return planets.flatMap { source ->
        val faction = mapFaction(source.currentOwner)
        if (source.disabled || mapSpecial(source) != null || faction == "unknown") emptyList()
        else source.attacking.mapNotNull { id ->
            val target = indexed[id] ?: return@mapNotNull null
            val a = mapPosition(source, planets) ?: return@mapNotNull null
            val b = mapPosition(target, planets) ?: return@mapNotNull null
            if (id == source.index || a == b || mapSpecial(target) != null) return@mapNotNull null
            val confirmed = if (faction == "human") source.event == null && mapOffensive(target, active)
                else target.event != null && mapFaction(target.currentOwner) == "human" && mapFaction(target.event.faction) == faction
            if (confirmed) MapAttackLink(source.index, id, faction) else null
        }
    }.distinct()
}

/** Permanent ownership ring is distinct from independent campaign progress rings. */
fun mapOwnerColor(owner: String): Color = mapRouteColor(owner)
fun mapLabelTop(radiusPx: Float, defense: Boolean, offensive: Boolean, gapPx: Float, special: Boolean = false, selected: Boolean = false): Float =
    radiusPx * when { special -> 3.65f; selected -> 3.5f; defense || offensive -> 2.95f; else -> 1f } + gapPx

/** The two lines follow font metrics, rather than a fixed gap from the map coordinate. */
fun mapLabelBaselines(topPx: Float, nameAscent: Float, nameDescent: Float, countAscent: Float, showName: Boolean, gapPx: Float): Pair<Float, Float> {
    val name = topPx - nameAscent
    val count = if (showName) name + nameDescent + gapPx - countAscent else topPx - countAscent
    return name to count
}
