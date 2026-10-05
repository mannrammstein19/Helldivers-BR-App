package br.com.helldiversbr.app.ui.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.Planet
import br.com.helldiversbr.app.ui.theme.HD
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PlanetCounters(planet: Planet) {
    val stats = planet.statistics
    val counters = listOf(
        Triple("DISPAROS", "bulletsFired", stats.bulletsFired),
        Triple("ACERTOS", "bulletsHit", stats.bulletsHit),
        Triple("TERMINÍDEOS ELIMINADOS", "terminidKills", stats.terminidKills),
        Triple("AUTÔMATOS DESTRUÍDOS", "automatonKills", stats.automatonKills),
        Triple("ILUMINADOS ELIMINADOS", "illuminateKills", stats.illuminateKills),
    )
    val format = NumberFormat.getIntegerInstance(Locale("pt", "BR"))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider(color = HD.BorderSoft)
        Text("ESTATÍSTICAS DO PLANETA", color = HD.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        counters.forEach { (label, field, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(label, color = HD.TextDim, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text(if (value != null) format.format(visualPlanetCounter(planet, field, value)) else "Indisponível", color = HD.Text,
                    fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.widthIn(min = 132.dp))
            }
        }
        counterSavedNotice(planet)?.let { notice ->
            Text(notice, color = HD.TextMuted, fontSize = 8.sp,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
        }
    }
}

/** Um aviso por seção sem atribuir a todos os campos o horário do mais recente. */
fun counterSavedNotice(planet: Planet): String? {
    val stats = planet.statistics
    val saved = br.com.helldiversbr.app.data.CounterTelemetry.values(stats).mapNotNull { (field, value) ->
        stats.counterReadings[field]?.takeIf { value != null && it.stale }
    }
    if (saved.isEmpty()) return null
    val times = saved.map { it.readAtMillis }.filter { it > 0 }.distinct().sorted()
    val format = java.text.SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))
    fun time(value: Long) = format.format(java.util.Date(value))
    return when {
        times.isEmpty() -> "Dados salvos • horário não informado"
        saved.any { it.readAtMillis <= 0 } -> "Dados salvos • alguns horários não informados"
        time(times.first()) == time(times.last()) -> "Dados salvos • última leitura: ${time(times.first())}"
        else -> "Dados salvos • leituras entre ${time(times.first())} e ${time(times.last())}"
    }
}
