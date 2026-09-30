package br.com.helldiversbr.app.data

/** Stable task identity: API expiration is reconstructed and drifts between responses. */
class OrderRateTracker {
    private data class Sample(val time: Long, val progress: Long)
    private val history = mutableMapOf<String, MutableList<Sample>>()
    private val rates = mutableMapOf<String, Double>()

    fun update(order: Assignment, now: Long): Map<Int, Double> {
        val result = mutableMapOf<Int, Double>()
        val activeKeys = mutableSetOf<String>()
        order.tasks.forEachIndexed { index, task ->
            val goal = task.goal ?: return@forEachIndexed
            val progress = order.progress.getOrNull(index) ?: return@forEachIndexed
            if (goal <= 0 || progress < 0) return@forEachIndexed
            val definition = task.valueTypes.zip(task.values).sortedBy { it.first }.joinToString()
            val key = "${order.id}:$index:${task.type}:$definition"
            activeKeys += key
            val samples = history.getOrPut(key) { mutableListOf() }
            val last = samples.lastOrNull()
            if (last != null && (now < last.time || now - last.time > 900_000L || progress < last.progress)) {
                samples.clear()
                rates.remove(key)
            }
            if (samples.isEmpty() || now - samples.last().time >= 30_000L) {
                samples.add(Sample(now, progress))
                while (samples.size > 2 && now - samples.first().time > 300_000L) samples.removeAt(0)
                val first = samples.first()
                val elapsed = now - first.time
                if (elapsed >= 30_000L) {
                    rates[key] = (progress - first.progress).toDouble() / goal * 100.0 / (elapsed / 3_600_000.0)
                }
            }
            rates[key]?.let { result[index] = it }
        }
        history.keys.retainAll(activeKeys)
        rates.keys.retainAll(activeKeys)
        return result
    }
}
