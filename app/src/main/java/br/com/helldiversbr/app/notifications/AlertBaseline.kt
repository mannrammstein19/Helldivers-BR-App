package br.com.helldiversbr.app.notifications

/** Missing or stale observations cannot clear a confirmed alert baseline. */
internal fun retainUnobserved(previous: Set<String>, observed: Set<String>, current: Set<String>): Set<String> =
    (previous - observed) + current
