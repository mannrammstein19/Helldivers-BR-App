package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage

internal fun tcsStateColor(state: TcsState): Color = when (state) {
    TcsState.ALLIED -> Color(0xFF64C9FF)
    TcsState.ATTACKED -> Color(0xFFFFD064)
    TcsState.COMPROMISED -> Color(0xFFFF706B)
    TcsState.UNKNOWN -> HD.TextMuted
}

@Composable
fun TcsCard(planet: Planet, stale: Boolean, planets: List<Planet> = emptyList(), readAtMillis: Long = 0L) {
    if (!TcsInfrastructure.has(planet)) return
    val historical = stale || planet.presenceHistoryStale || planet.savedTcsPresent
    val state = TcsInfrastructure.state(planet)
    val color = tcsStateColor(state)
    var expanded by remember(planet.index) { mutableStateOf(false) }
    val network = remember(planets) { planets.filter(TcsInfrastructure::has).sortedBy { it.nameText } }
    Surface(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0C141C), border = BorderStroke(1.dp, color.copy(alpha = .55f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AsyncImage(MapAssets.file("tcs-plus"), "Infraestrutura TCS+", Modifier.size(34.dp), colorFilter = ColorFilter.tint(color))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("SISTEMA DE CONTROLE DE TERMINÍDIOS+", color = HD.Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(state.label, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            if (historical) Text("Última leitura · situação atual não confirmada" +
                if (planet.effectsReadAtMillis > 0 || readAtMillis > 0) " · ${mapReadingTime(planet.effectsReadAtMillis.takeIf { it > 0 } ?: readAtMillis)}" else "",
                color = HD.Gold, fontSize = 11.sp)
            Text("Controle do planeta; reparo, operação das torres e bônus não confirmados.", color = HD.TextMuted, fontSize = 11.sp)
            if (network.isNotEmpty()) {
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                    Text("Rede TCS+ · ${network.size} planetas ${if (expanded) "−" else "+"}", color = HD.Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                if (expanded) network.forEach { p ->
                    val rowColor = tcsStateColor(TcsInfrastructure.state(p))
                    val rowHistorical = stale || p.presenceHistoryStale || p.savedTcsPresent
                    Row(Modifier.fillMaxWidth().background(rowColor.copy(alpha = .08f), RoundedCornerShape(7.dp)).padding(9.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.width(3.dp).height(32.dp).background(rowColor, RoundedCornerShape(2.dp)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(p.nameText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(TcsInfrastructure.state(p).label + if (rowHistorical) " · Última leitura" else "", color = rowColor, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
