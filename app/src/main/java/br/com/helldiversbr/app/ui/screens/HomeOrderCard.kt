package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.theme.HD
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeOrderCard(data: HomeData, onOpen: () -> Unit) {
    val ui = data.order
    val order = ui.order
    val locale = Locale("pt", "BR")
    val number = NumberFormat.getInstance(locale)
    HdCard(accent = HD.Yellow) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ORDEM MAIOR", color = HD.Yellow, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(when(ui.state) { "completed" -> "VITÓRIA"; "failed" -> "DERROTA"; "active" -> "EM ANDAMENTO"; else -> "AGUARDANDO" },
                color = HD.TextDim, fontSize = 9.sp)
        }
        Text(order?.titleText?.let(::translateKnown)?.ifBlank { "ORDEM MAIOR" } ?: "ORDEM MAIOR",
            color = HD.Text, fontWeight = FontWeight.Black, fontSize = 21.sp)
        LocalizedText(order?.briefingText?.ifBlank { order?.descriptionText.orEmpty() } ?: "Aguardando instruções do Alto Comando.",
            color = HD.TextDim, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (order != null) {
            Text("Tempo restante: ${if(ui.state == "active") OrderRepository.remaining(order.expiration) else "—"}",
                color = HD.TextDim, fontSize = 11.sp)
            order.tasks.forEachIndexed { index, task ->
                val campaign = task.planetId?.let { id -> data.campaigns.firstOrNull { it.planet.index == id } }
                val faction = OrderRepository.taskFaction(task).ifBlank { campaign?.let { OrderRepository.enemyFaction(it) }.orEmpty() }
                val accent = faction.takeIf { it.isNotBlank() }?.let { factionColor(it) } ?: HD.Yellow
                val goal = task.goal
                val progress = order.progress.getOrElse(index) { 0L }
                val percent = when {
                    ui.state == "completed" -> 100.0
                    task.type in listOf(11,12,13) && campaign != null -> if(progress > 0) 100.0 else OrderRepository.campaignPercent(campaign)
                    goal != null && goal > 0 -> (progress.toDouble()/goal*100).coerceIn(0.0,100.0)
                    progress > 0 -> 100.0
                    else -> 0.0
                }
                val rate = if(task.type in listOf(11,12,13) && campaign != null) OrderRepository.campaignRate(data,campaign) else data.orderRates[index]
                val fresh = ui.state == "active" && "Ordem Maior" !in data.staleSources && !(campaign != null && "campanhas" in data.staleSources)
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HD.BgDeep), border = BorderStroke(1.dp,accent)) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("OBJETIVO ${index+1}", color = HD.TextMuted, fontSize = 9.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            if (mapFaction(faction) != "unknown") SiteImage(mapFaction(faction), OrderRepository.factionLabel(faction), Modifier.size(23.dp))
                            Text(when {
                                task.planetId != null -> "${when(task.type){12 -> "DEFENDER";13 -> "CONTROLAR";else -> "LIBERTAR"}} ${task.planetId?.let { data.planetNames[it] } ?: campaign?.planet?.nameText ?: "PLANETA"}"
                                task.type == 3 && goal != null -> "ELIMINAR ${number.format(goal)} ${OrderTargets.label(task, OrderRepository.factionLabel(faction)).uppercase()}"
                                else -> "CUMPRIR OBJETIVO"
                            }, modifier = Modifier.weight(1f), color = HD.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        ProgressBar(percent,accent)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if(goal != null) "${number.format(progress)} / ${number.format(goal)}" else "Progresso", color = HD.TextDim,fontSize = 9.sp)
                            Text("%.2f%%".format(locale,percent),color = accent,fontSize = 10.sp,fontWeight = FontWeight.Bold)
                        }
                        Text("RITMO OBSERVADO",color = HD.TextMuted,fontSize = 8.sp)
                        Text(if(fresh && rate != null) "%+.2f%%/h".format(locale,rate) else "Coletando leituras",color = HD.Text,fontSize = 11.sp)
                        Text("CONCLUSÃO ESTIMADA",color = HD.TextMuted,fontSize = 8.sp)
                        Text(if(percent >= 100) "Concluído" else if(fresh) OrderRepository.etaFromRate(percent,rate) ?: "Aguardando avanço" else "Indisponível",
                            color = HD.Text,fontSize = 11.sp)
                    }
                }
            }
        }
        TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("ACOMPANHAR MISSÃO  →",color = HD.Yellow) }
    }
}
