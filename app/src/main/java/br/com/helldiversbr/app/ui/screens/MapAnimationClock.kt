package br.com.helldiversbr.app.ui.screens

/** Frame time belongs to the visible map, never to a telemetry request. */
internal class MapAnimationClock {
    private var originNanos: Long? = null
    private var lastSeconds = 0f

    fun secondsAt(frameTimeNanos: Long): Float {
        val origin = originNanos ?: frameTimeNanos.also { originNanos = it }
        val seconds = ((frameTimeNanos - origin).coerceAtLeast(0L) / 1_000_000_000.0).toFloat()
        lastSeconds = maxOf(lastSeconds, seconds)
        return lastSeconds
    }
}

/** Keep the GIF's real duration; modulo occurs before converting to Int. */
internal fun mapGifTimeMillis(seconds: Float, durationMillis: Int): Int {
    if (durationMillis <= 0 || !seconds.isFinite()) return 0
    return ((seconds.coerceAtLeast(0f).toDouble() * 1000.0).toLong() % durationMillis).toInt()
}
