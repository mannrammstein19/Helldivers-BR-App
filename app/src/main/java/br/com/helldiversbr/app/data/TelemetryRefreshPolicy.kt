package br.com.helldiversbr.app.data

/** Cadência de consulta; não altera a hora da leitura nem decide notificações. */
object TelemetryRefreshPolicy {
    const val INTERVAL_MILLIS = 30_000L

    fun preservePreviousStation(previousTime: Long, incomingTime: Long,
                                previousHasStation: Boolean, incomingHasLocation: Boolean,
                                incomingStale: Boolean): Boolean =
        previousHasStation && (incomingTime < previousTime || (incomingStale && !incomingHasLocation))
}
