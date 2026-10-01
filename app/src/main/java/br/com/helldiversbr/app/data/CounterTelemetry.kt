package br.com.helldiversbr.app.data

import kotlinx.serialization.Serializable

@Serializable
data class CounterReading(val value: Long, val source: String, val readAtMillis: Long, val stale: Boolean = false)

/** Combina somente contadores confirmados, preservando fonte e hora por campo. */
object CounterTelemetry {
    fun values(stats: PlanetStatistics): Map<String, Long?> = linkedMapOf(
        "bulletsFired" to stats.bulletsFired, "bulletsHit" to stats.bulletsHit,
        "terminidKills" to stats.terminidKills, "automatonKills" to stats.automatonKills,
        "illuminateKills" to stats.illuminateKills,
    )

    fun collect(previous: List<Planet>, previousTime: Long, fresh: List<Pair<List<Planet>, String>>, now: Long, freshTimes: List<Long> = emptyList()): Map<Long, Map<String, CounterReading>> {
        val result = mutableMapOf<Long, MutableMap<String, CounterReading>>()
        previous.forEach { planet ->
            val fields = result.getOrPut(planet.index) { mutableMapOf() }
            values(planet.statistics).forEach { (field, value) ->
                if (value != null && value >= 0) {
                    val saved = planet.statistics.counterReadings[field]
                        ?: CounterReading(value, "cache", previousTime)
                    val old = fields[field]
                    if (old == null || saved.readAtMillis >= old.readAtMillis) fields[field] = saved.copy(stale = true)
                }
            }
        }
        fresh.forEachIndexed { index, (planets, source) ->
            val readAt = freshTimes.getOrNull(index) ?: now
            if (source != "cache") planets.forEach { planet ->
                val fields = result.getOrPut(planet.index) { mutableMapOf() }
                values(planet.statistics).forEach { (field, value) ->
                    if (value != null && value >= 0) {
                        val old = fields[field]
                        if (old == null || readAt >= old.readAtMillis)
                            fields[field] = CounterReading(value, source, readAt, readAt <= 0 || now - readAt > 90_000)
                    }
                }
            }
        }
        return result
    }

    fun enrich(planet: Planet, readings: Map<Long, Map<String, CounterReading>>): Planet {
        val fields = readings[planet.index].orEmpty()
        if (fields.isEmpty()) return planet
        return planet.copy(statistics = planet.statistics.copy(
            bulletsFired = fields["bulletsFired"]?.value,
            bulletsHit = fields["bulletsHit"]?.value,
            terminidKills = fields["terminidKills"]?.value,
            automatonKills = fields["automatonKills"]?.value,
            illuminateKills = fields["illuminateKills"]?.value,
            counterReadings = fields,
        ))
    }
}
