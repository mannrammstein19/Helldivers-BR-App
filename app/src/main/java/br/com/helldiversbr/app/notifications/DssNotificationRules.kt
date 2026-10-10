package br.com.helldiversbr.app.notifications

/** First observation, unknown location and repeated polls are not relocation events. */
internal fun dssRelocationIsNew(previous: Long, current: Long): Boolean =
    previous > 0L && current > 0L && previous != current
