package br.com.helldiversbr.app.ui.presentation


/** Apenas apresentação. Nenhum valor desta classe é gravado no cache ou enviado aos alertas. */
data class NumberObservation(val key: String, val value: Double, val percentage: Boolean, val source: String)

data class NumberProjection(
    val actual: Double,
    val sampledAt: Long,
    val perSecond: Double,
    val horizonMillis: Long,
    val percentage: Boolean,
) {
    fun valueAt(now: Long): Double {
        val age = now - sampledAt
        if (age < 0 || age > NumberProjectionTracker.MAX_AGE || !actual.isFinite()) return actual
        val seconds = age.coerceAtMost(horizonMillis) / 1000.0
        val change = perSecond * seconds
        if (!change.isFinite()) return actual
        return if (percentage) {
            // Nunca antecipa 0/100% nem desloca mais de um centésimo de ponto percentual.
            if (actual <= 0.0 || actual >= 100.0) actual
            else (actual + change.coerceIn(-.0099, .0099)).coerceIn(
                minOf(actual, .0001), maxOf(actual, 99.9999),
            )
        } else (actual + change.coerceAtLeast(0.0)).coerceAtLeast(actual)
    }
}

data class NumberFrame(val entries: Map<String, NumberProjection> = emptyMap()) {
    fun value(key: String, actual: Double, now: Long): Double {
        val projection = entries[key] ?: return actual
        // Uma ficha do mapa pode ter outra leitura: nesse caso mostramos seu valor confirmado.
        return if (projection.actual == actual) projection.valueAt(now) else actual
    }
}

class NumberProjectionTracker {
    private data class Sample(val observation: NumberObservation, val time: Long)
    private val history = mutableMapOf<String, Sample>()

    fun reset(): NumberFrame {
        history.clear()
        return NumberFrame()
    }

    fun update(observations: List<NumberObservation>, sampledAt: Long, now: Long): NumberFrame {
        if (sampledAt <= 0 || now < sampledAt || now - sampledAt > MAX_AGE) return reset()
        val frame = observations.filter { it.value.isFinite() && it.value >= 0 }.associate { observation ->
            val old = history[observation.key]
            val elapsed = old?.let { sampledAt - it.time } ?: 0L
            val comparable = old != null && old.observation.source == observation.source &&
                old.observation.percentage == observation.percentage && elapsed in MIN_INTERVAL..MAX_AGE
            val delta = if (comparable) observation.value - old!!.observation.value else 0.0
            val rate = if (comparable && (observation.percentage || delta >= 0)) delta / (elapsed / 1000.0) else 0.0
            // Amostras iguais, reinício de contador, fonte diferente ou lacuna longa não geram movimento.
            if (old == null || elapsed >= MIN_INTERVAL || elapsed < 0 || old.observation.source != observation.source || delta < 0 && !observation.percentage) {
                history[observation.key] = Sample(observation, sampledAt)
            }
            observation.key to NumberProjection(observation.value, sampledAt, rate,
                elapsed.coerceIn(0L, MAX_HORIZON), observation.percentage)
        }
        history.keys.retainAll(frame.keys)
        return NumberFrame(frame)
    }

    companion object {
        const val MIN_INTERVAL = 15_000L
        const val MAX_AGE = 90_000L
        const val MAX_HORIZON = 60_000L
    }
}
