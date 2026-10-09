package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage

@Composable
fun TcsCard(planet: Planet, stale: Boolean, planets: List<Planet> = emptyList(), readAtMillis: Long = 0L) {
    if (!TcsInfrastructure.has(planet)) return
    val historical = stale || planet.presenceHistoryStale || planet.savedTcsPresent
    val state = TcsInfrastructure.state(planet)
    val color = when (state) {
        TcsState.ALLIED -> Color(0xFF64C9FF)
        TcsState.ATTACKED -> Color(0xFFFFD064)
        TcsState.COMPROMISED -> Color(0xFFFF706B)
        TcsState.UNKNOWN -> HD.TextMuted
    }
    var expanded by remember(planet.index) { mutableStateOf(false) }
    val network = remember(planets) { planets.filter(TcsInfrastructure::has).sortedBy { it.nameText } }
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AsyncImage(MapAssets.file("tcs-plus"), "Infraestrutura TCS+", Modifier.size(30.dp))
            Column(Modifier.weight(1f)) {
                Text("SISTEMA DE CONTROLE DE TERMINÍDIOS+", color = color, fontSize = 10.sp)
                Text(state.label + if (historical) " · ÚLTIMA LEITURA" else "", color = color, fontSize = 10.sp)
            }
        }
        Text(state.note, color = HD.TextDim, fontSize = 11.sp)
        Text("Não confirma bônus de libertação, reparo ou operação das torres.", color = HD.TextMuted, fontSize = 10.sp)
        if (historical) Text("Situação atual não confirmada" + if (planet.effectsReadAtMillis > 0 || readAtMillis > 0) " · ${mapReadingTime(planet.effectsReadAtMillis.takeIf { it > 0 } ?: readAtMillis)}" else "", color = HD.Gold, fontSize = 10.sp)
        if (network.isNotEmpty()) {
            TextButton(onClick = { expanded = !expanded }) { Text("Rede TCS+ · ${network.size} planetas ${if (expanded) "−" else "+"}", fontSize = 11.sp) }
            if (expanded) network.forEach { p -> Text("${p.nameText} · ${TcsInfrastructure.state(p).label}", color = HD.TextDim, fontSize = 10.sp) }
        }
    }
}
