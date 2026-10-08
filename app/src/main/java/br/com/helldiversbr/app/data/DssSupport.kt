package br.com.helldiversbr.app.data

import java.time.Instant

/** Known action IDs and effects from the game's station response. No position/owner inference. */
enum class DssSupport(val actionId: Long, val label: String, val asset: String, val effects: Set<Long>) {
    EAGLE(4091660627L, "Águia Tempestiva", "EAGLE STORM.png", setOf(1209L, 1212L, 1216L, 1425L)),
    BLOCKADE(3248573007L, "Bloqueio Orbital", "ORBITAL BLOCKADE.png", setOf(1210L, 1213L, 1215L)),
    HEAVY(3578080409L, "Distribuição de Artilharia Pesada", "HEAVY ORDNANCE DISTRIBUTION.png", setOf(1237L, 1214L));

    val icon: String? get() = MapAssets.file("imagens/guerra/dss/$asset")

    companion object {
        fun kind(action: DssTacticalAction): DssSupport? = entries.firstOrNull { it.actionId == action.id32 }

        fun isCurrent(reading: DssReading?, now: Long): Boolean = reading?.isLive == true && readingIsFresh(reading, now)

        fun readingIsFresh(reading: DssReading?, now: Long): Boolean = reading != null && reading.source != "cache" && !reading.stale &&
            reading.fetchedAtMillis > 0 && now >= reading.fetchedAtMillis - 60_000L &&
            now - reading.fetchedAtMillis <= 300_000L

        fun actionIsActive(action: DssTacticalAction, now: Long): Boolean {
            return DssActionRules.phase(action, now) == DssActionPhase.ACTIVE
        }

        /** A fresh raw list is authoritative: absence does not inherit an old active action. */
        fun actionsFromEffects(ids: List<Long>): List<DssTacticalAction> = entries.filter { kind ->
            kind.effects.any(ids::contains)
        }.map { kind ->
            DssTacticalAction(id32 = kind.actionId, name = when (kind) {
                EAGLE -> "EAGLE STORM"
                BLOCKADE -> "ORBITAL BLOCKADE"
                HEAVY -> "HEAVY ORDNANCE DISTRIBUTION"
            }, status = 2, effectIds = ids.filter(kind.effects::contains))
        }

        fun forPlanet(reading: DssReading?, planetIndex: Long, now: Long): List<DssSupport> {
            if (!isCurrent(reading, now)) return emptyList()
            reading ?: return emptyList()
            val station = reading.station ?: return emptyList()
            if (station.planet.index != planetIndex) return emptyList()
            station.activeEffectIds?.let { ids -> return entries.filter { k -> k.effects.any(ids::contains) } }
            return station.tacticalActions.mapNotNull { action ->
                kind(action)?.takeIf {
                    actionIsActive(action, now)
                }
            }.distinct()
        }

        fun dateMillis(value: String): Long? = value.trim().toLongOrNull()?.let {
            if (it <= 0L) null else if (it < 1_000_000_000_000L) it * 1000L else it
        } ?: runCatching { Instant.parse(value.trim()).toEpochMilli() }.getOrNull()
    }
}
