package br.com.helldiversbr.app.ui.screens

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun mapReadingSource(source: String): String = when (source) {
    "community" -> "API da comunidade"
    "direct" -> "API direta do jogo"
    "mixed" -> "Fontes combinadas"
    "central" -> "Central BR"
    "cache" -> "Última leitura salva"
    else -> "Fonte indisponível"
}

internal fun mapReadingTime(millis: Long?, zone: ZoneId = ZoneId.systemDefault()): String {
    if (millis == null || millis <= 0L) return "Sem leitura disponível"
    return DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss", Locale("pt", "BR"))
        .withZone(zone).format(Instant.ofEpochMilli(millis))
}
