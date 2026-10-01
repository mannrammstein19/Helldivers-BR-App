package br.com.helldiversbr.app.ui.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    if (counters.none { it.third != null }) return
    val format = NumberFormat.getIntegerInstance(Locale("pt", "BR"))
    HorizontalDivider(color = HD.BorderSoft)
    Text("ESTATÍSTICAS DO PLANETA", color = HD.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    counters.forEach { (label, field, value) ->
        if (value != null) Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = HD.TextDim, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text(format.format(visualPlanetCounter(planet, field, value)), color = HD.Text,
                fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}
