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
    HorizontalDivider(color = HD.BorderSoft)
    Text("ESTATÍSTICAS DO PLANETA", color = HD.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    counters.forEach { (label, field, value) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = HD.TextDim, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text(if (value != null) format.format(visualPlanetCounter(planet, field, value)) else "Indisponível", color = HD.Text,
                fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.widthIn(min = 132.dp))
        }
        val saved = stats.counterReadings[field]?.takeIf { it.stale && value != null }
        if (saved != null) Text(
            if (saved.readAtMillis > 0) "Última leitura: " + java.text.SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(java.util.Date(saved.readAtMillis)) else "Última leitura salva",
            color = HD.TextMuted, fontSize = 8.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End,
        )
    }
}
