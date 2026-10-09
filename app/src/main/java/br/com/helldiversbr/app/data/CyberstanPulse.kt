package br.com.helldiversbr.app.data

/** Local sprite atlas: 53 frames, slowed from 30ms to 120ms, with softened alpha edges. */
object CyberstanPulse {
    const val FRAME_COUNT = 53
    const val COLUMNS = 8
    const val FRAME_SIZE = 192
    const val FRAME_MILLIS = 120L

    fun frame(elapsedMillis: Long, moving: Boolean): Int =
        if (moving) (Math.floorMod(elapsedMillis, FRAME_COUNT * FRAME_MILLIS) / FRAME_MILLIS).toInt() else 0
}
