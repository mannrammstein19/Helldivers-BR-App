package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val completed = order?.let { assignment ->
        if (ui.state == "completed") assignment.tasks.size else assignment.tasks.indices.count { i ->
            val task = assignment.tasks[i]
            val progress = assignment.progress.getOrNull(i) ?: 0L
            val goal = task.goal
            if (goal != null && goal > 0) progress >= goal else progress > 0
        }
    } ?: 0

    HdCard(accent = HD.Yellow) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ORDEM MAIOR", color = HD.Yellow, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(
                when (ui.state) {
                    "completed" -> "VITÓRIA"
                    "failed" -> "DERROTA"
                    "active" -> "EM ANDAMENTO"
                    else -> "AGUARDANDO"
                },
                color = HD.TextDim,
                fontSize = 9.sp,
            )
        }
        Text(
            order?.titleText?.let(::translateKnown)?.ifBlank { "ORDEM MAIOR" } ?: "ORDEM MAIOR",
            color = HD.Text,
            fontWeight = FontWeight.Black,
            fontSize = 21.sp,
        )
        LocalizedText(
            order?.briefingText?.ifBlank { order?.descriptionText.orEmpty() } ?: "Aguardando instruções do Alto Comando.",
            color = HD.TextDim,
            fontSize = 12.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        if (order != null) {
            Row(
                Modifier.fillMaxWidth().padding(top = 1.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                HomeInlineStat(
                    "TEMPO RESTANTE",
                    if (ui.state == "active") OrderRepository.remaining(order.expiration) else "ENCERRADA",
                    Modifier.weight(1f),
                )
                HomeInlineStat(
                    "OBJETIVOS CONCLUÍDOS",
                    "$completed / ${order.tasks.size}",
                    Modifier.weight(1f),
                    valueColor = HD.Yellow,
                )
            }

            order.tasks.forEachIndexed { index, task ->
                val campaign = task.planetId?.let { id -> data.campaigns.firstOrNull { it.planet.index == id } }
                val faction = OrderRepository.taskFaction(task).ifBlank { campaign?.let { OrderRepository.enemyFaction(it) }.orEmpty() }
                val factionAccent = faction.takeIf { it.isNotBlank() }?.let { factionColor(it) } ?: HD.Yellow
                val goal = task.goal
                val progress = order.progress.getOrElse(index) { 0L }
                val percent = when {
                    ui.state == "completed" -> 100.0
                    task.type in listOf(11, 12, 13) && campaign != null -> if (progress > 0) 100.0 else OrderRepository.campaignPercent(campaign)
                    goal != null && goal > 0 -> (progress.toDouble() / goal * 100).coerceIn(0.0, 100.0)
                    progress > 0 -> 100.0
                    else -> 0.0
                }
                val accent = if (percent >= 100.0) HD.Green else factionAccent
                val rate = if (task.type in listOf(11, 12, 13) && campaign != null) {
                    OrderRepository.campaignRate(data, campaign)
                } else data.orderRates[index]
                val fresh = ui.state == "active" && "Ordem Maior" !in data.staleSources && !(campaign != null && "campanhas" in data.staleSources)
                val eta = if (percent >= 100) "Concluído" else if (fresh) {
                    OrderRepository.etaFromRate(percent, rate) ?: if (rate == null) "Coletando leituras" else "Aguardando avanço"
                } else OrderRepository.etaFromRate(percent, rate) ?: "Aguardando"

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101214)),
                    border = BorderStroke(1.2.dp, accent.copy(alpha = 0.9f)),
                ) {
                    Column(Modifier.background(Brush.verticalGradient(listOf(accent.copy(alpha = .16f), Color(0xFF101214)))).padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("OBJETIVO ${index + 1} // ${objectiveKind(task)}", color = HD.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text(
                                if (faction.isNotBlank()) OrderRepository.factionLabel(faction).uppercase() else task.planetId?.let { data.planetNames[it]?.uppercase() } ?: "ALVO",
                                color = accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            if (mapFaction(faction) != "unknown") {
                                SiteImage(mapFaction(faction), OrderRepository.factionLabel(faction), Modifier.size(46.dp))
                            }
                            Text(
                                when {
                                    task.planetId != null -> "${when (task.type) { 12 -> "DEFENDER"; 13 -> "CONTROLAR"; else -> "LIBERTAR" }} ${task.planetId?.let { data.planetNames[it] } ?: campaign?.planet?.nameText ?: "PLANETA"}"
                                    task.type == 3 && goal != null -> "ELIMINAR ${number.format(goal)} ${OrderTargets.label(task, OrderRepository.factionLabel(faction)).uppercase()}"
                                    else -> "CUMPRIR OBJETIVO"
                                },
                                modifier = Modifier.weight(1f),
                                color = HD.Text,
                                fontSize = 18.sp,
                                lineHeight = 23.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(br.com.helldiversbr.app.R.font.oswald_bold, FontWeight.Bold)),
                                fontWeight = FontWeight.Black,
                            )
                        }
                        if (percent >= 100.0) Text("✓ CUMPRIDO", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        OrderProgressBand(percent,
                            if (goal != null) "${number.format(progress)} / ${number.format(goal)}" else "Progresso",
                            "%.2f%%".format(locale, percent), accent)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            HomeForecastBox(
                                label = "RITMO OBSERVADO",
                                value = if (percent >= 100.0) "Finalizado" else if (rate != null) "%+.2f%%/h".format(locale, rate) else if (fresh) "Coletando" else "Aguardando",
                                accent = accent,
                                modifier = Modifier.weight(1f),
                            )
                            HomeForecastBox(
                                label = "CONCLUSÃO ESTIMADA",
                                value = eta,
                                accent = accent,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (!fresh && percent < 100.0) Text("Última leitura salva · ritmo/previsão sem confirmação atual.",
                            color = HD.Gold, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
        TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Text("ACOMPANHAR MISSÃO  →", color = HD.Yellow, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun HomeInlineStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = HD.Text,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(label, color = HD.TextMuted, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = .35.sp)
        Text(value.uppercase(), color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun HomeForecastBox(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = .42f)),
        border = BorderStroke(1.dp, accent.copy(alpha = .28f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = HD.TextMuted, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = .25.sp)
            Text(value, color = HD.Text, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
