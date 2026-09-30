package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.HomeData
import br.com.helldiversbr.app.data.OrderRepository
import br.com.helldiversbr.app.data.OrderTask
import br.com.helldiversbr.app.data.translateKnown
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale

private val ptBr = Locale("pt", "BR")
private fun fmt(n: Long): String = NumberFormat.getInstance(ptBr).format(n)

@Composable
fun OrderCard(data: HomeData) {
    val ui = data.order
    val order = ui.order

    val accent: Color = when (ui.state) {
        "completed" -> HD.Green
        "failed" -> HD.Red
        else -> HD.Gold
    }
    val stateLabel = when (ui.state) {
        "completed" -> "VITÓRIA"
        "failed" -> "FALHA"
        "active" -> "EM ANDAMENTO"
        else -> "AGUARDANDO"
    }

    HdCard(accent = accent) {
        SiteImage(br.com.helldiversbr.app.data.SiteAssets.orderKey(ui.state), "Ordem Maior",
            Modifier.fillMaxWidth().height(140.dp), androidx.compose.ui.layout.ContentScale.Crop)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                SectionLabel("◆ Ordem Maior", accent)
                Text("ALTO COMANDO DA SUPER TERRA", color = HD.TextMuted, fontSize = 9.sp, letterSpacing = 0.8.sp)
            }
            Chip(stateLabel, accent)
        }

        if (order == null) {
            Text(
                "Nenhuma Ordem Maior registrada no momento. Aguardando novas instruções do Alto Comando.",
                color = HD.TextDim,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            return@HdCard
        }

        val title = translateKnown(order.titleText).ifBlank { "ORDEM MAIOR" }
        Text(title, color = HD.Text, fontSize = 22.sp, lineHeight = 25.sp, fontWeight = FontWeight.Black)

        if (order.briefingText.isNotBlank()) {
            LocalizedText(order.briefingText, color = HD.TextDim, fontSize = 13.sp, lineHeight = 19.sp)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OrderMiniStat(
                label = "TEMPO RESTANTE",
                value = if (ui.state == "active") OrderRepository.remaining(order.expiration) else if (ui.state in listOf("completed", "failed")) "ENCERRADA" else "AGUARDANDO",
                modifier = Modifier.weight(1f),
            )

        }

        OrderRewards(order, accent)

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PROGRESSO GERAL", color = HD.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${"%.1f".format(ptBr, ui.percent)}%",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            ProgressBar(ui.percent, accent)
        }

        if (order.tasks.isNotEmpty()) {
            SectionLabel("Objetivos da ordem", HD.TextDim)
        }
        order.tasks.forEachIndexed { index, task ->
            val orderPlanetName = task.planetId?.let { data.planetNames[it] }
            val campaign = task.planetId?.let { planetId ->
                data.campaigns.firstOrNull { it.planet.index == planetId && planetId != 0L }
                    ?: orderPlanetName?.let { wanted ->
                        data.campaigns.firstOrNull { it.planet.nameText.equals(wanted, ignoreCase = true) }
                    }
            }
            val livePercent = if (campaign != null && task.type in listOf(11, 12, 13)) {
                OrderRepository.campaignPercent(campaign)
            } else null
            val decodedTaskFaction = OrderRepository.taskFaction(task)
            val taskFaction = decodedTaskFaction
                .takeIf { it.isNotBlank() && !OrderRepository.isHumanFaction(it) }
                ?: campaign?.let { OrderRepository.enemyFaction(it) }.orEmpty()
            TaskRow(
                index = index,
                task = task,
                progress = order.progress.getOrNull(index) ?: 0L,
                planetName = orderPlanetName ?: campaign?.planet?.nameText,
                factionRaw = taskFaction,
                accent = taskFaction.takeIf { it.isNotBlank() }?.let { factionColor(it) }
                    ?: campaign?.let { factionColor(OrderRepository.enemyFaction(it), it.planet.event != null) }
                    ?: accent,
                livePercent = livePercent,
            )
        }

        if (ui.fromSnapshot && ui.state == "active") {
            Text(
                "ÚLTIMO REGISTRO SALVO // A telemetria ao vivo da ordem está temporariamente indisponível.",
                color = HD.TextMuted,
                fontSize = 9.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun OrderMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        Text(value, color = HD.Text, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun TaskRow(
    index: Int,
    task: OrderTask,
    progress: Long,
    planetName: String?,
    factionRaw: String,
    accent: Color,
    livePercent: Double?,
) {
    val goal = task.goal
    val title: String
    val detail: String
    val percent: Double

    if (livePercent != null && goal == null) {
        title = when (task.type) {
            11 -> planetName?.let { "Liberar $it" } ?: "Objetivo de libertação"
            12 -> planetName?.let { "Defender $it" } ?: "Objetivo de defesa"
            13 -> planetName?.let { "Controlar $it" } ?: "Objetivo de controle"
            else -> planetName ?: "Objetivo ${index + 1}"
        }
        detail = if (progress > 0) "CONCLUÍDO" else "${"%.2f".format(ptBr, livePercent)}%"
        percent = if (progress > 0) 100.0 else livePercent
    } else if (goal != null && goal > 0) {
        title = when (task.type) {
            3 -> "Eliminar ${br.com.helldiversbr.app.data.OrderTargets.label(task, OrderRepository.factionLabel(factionRaw))}"
            else -> "Objetivo ${index + 1}"
        }
        detail = "${fmt(progress)} / ${fmt(goal)}"
        percent = (progress.toDouble() / goal * 100.0).coerceIn(0.0, 100.0)
    } else {
        title = planetName?.let { "Planeta: $it" } ?: "Objetivo ${index + 1}"
        detail = if (progress > 0) "CONCLUÍDO" else "EM ANDAMENTO"
        percent = if (progress > 0) 100.0 else 0.0
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (factionRaw.isNotBlank() && !OrderRepository.isHumanFaction(factionRaw)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                AsyncImage(
                    model = PlanetVisuals.factionLogo(factionRaw),
                    contentDescription = OrderRepository.factionLabel(factionRaw),
                    modifier = Modifier.size(19.dp),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    "ALVO // ${OrderRepository.factionLabel(factionRaw).uppercase()}",
                    color = accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = HD.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(detail, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 10.dp))
        }
        ProgressBar(percent, accent)
    }
}
