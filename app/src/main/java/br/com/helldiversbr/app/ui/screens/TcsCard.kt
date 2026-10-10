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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(5.dp)) {
                var x = -size.height
                while (x < size.width) {
                    drawLine(HD.Gold.copy(alpha = .4f), Offset(x,size.height), Offset(x+size.height,0f), 1.dp.toPx())
                    x += 7.dp.toPx()
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AsyncImage(MapAssets.file("tcs-plus"), "Infraestrutura TCS+", Modifier.size(34.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("SISTEMA DE CONTROLE DE TERMINÍDIOS+", color = HD.Gold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(state.label, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            if (network.isNotEmpty()) {
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                    Text("Rede TCS+ · ${network.size} planetas ${if (expanded) "−" else "+"}", color = HD.Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                if (expanded) network.forEach { p ->
                    val rowColor = tcsStateColor(TcsInfrastructure.state(p))
                    val rowState = TcsInfrastructure.state(p)
                    val nameColor = when (rowState) {
                        TcsState.ALLIED -> Color(0xFF64C9FF)
                        TcsState.ATTACKED -> Color(0xFFFFD064)
                        TcsState.COMPROMISED -> Color(0xFFFFA43D)
                        TcsState.UNKNOWN -> HD.TextMuted
                    }
                    Row(Modifier.fillMaxWidth().background(Color(0xFF1A2026), RoundedCornerShape(9.dp))
                        .drawBehind {
                            val inset = 3.dp.toPx()
                            drawRoundRect(color = if (rowState == TcsState.ALLIED) Color(0xFF58616A) else rowColor.copy(alpha = .6f),
                                topLeft = Offset(inset,inset), size = Size(size.width-2*inset,size.height-2*inset),
                                cornerRadius = CornerRadius(6.dp.toPx()),
                                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(),5.dp.toPx()))))
                        }.padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.width(3.dp).height(32.dp).background(rowColor, RoundedCornerShape(2.dp)))
                        Text(p.nameText, Modifier.weight(1.1f), color = nameColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(rowState.label, Modifier.weight(1f), color = rowColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            val oldRows = (if (expanded) network else listOf(planet)).filter {
                stale || it.presenceHistoryStale || it.savedTcsPresent
            }
            if (historical || oldRows.isNotEmpty()) {
                val references = oldRows.joinToString("; ") { p ->
                    val time = p.effectsReadAtMillis.takeIf { it > 0 } ?: readAtMillis
                    p.nameText + if (time > 0) " · ${mapReadingTime(time)}" else " · horário não informado"
                }
                Text("Última leitura · situação atual não confirmada" + if (references.isNotBlank()) "\n$references" else "",
                    color = HD.Gold, fontSize = 10.sp)
            }
            Text("Controle do planeta; reparo, operação das torres e bônus não confirmados.", color = HD.TextMuted, fontSize = 10.sp)
        }
    }
}
