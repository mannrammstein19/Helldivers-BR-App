package br.com.helldiversbr.app.notifications

/** Never advance an alert baseline from cache, repeated clocks or historical observations. */
internal fun notificationReadingIsNew(time: Long, previous: Long, now: Long): Boolean =
    time > 0L && time > previous && time <= now + 60_000L && now - time <= 300_000L
