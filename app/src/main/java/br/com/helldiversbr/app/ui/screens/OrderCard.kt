package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.HomeData
import br.com.helldiversbr.app.data.OrderRepository
import br.com.helldiversbr.app.data.OrderTask
import br.com.helldiversbr.app.data.translateKnown
import br.com.helldiversbr.app.ui.theme.HD
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
        "completed" -> "ORDEM CONCLUÍDA"
        "failed" -> "ORDEM NÃO CUMPRIDA"
        "active" -> "EM ANDAMENTO"
        else -> "AGUARDANDO ORDENS"
    }

    HdCard(accent = accent.copy(alpha = 0.6f)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Ordem Maior", accent)
            Chip(stateLabel, accent)
        }

        if (order == null) {
            Text(
                "Nenhuma Ordem Maior ativa no momento. Aguarde novas instruções do Alto Comando.",
                color = HD.TextDim,
                fontSize = 14.sp,
            )
            return@HdCard
        }

        val title = translateKnown(order.titleText).ifBlank { "ORDEM MAIOR" }
        Text(title, color = HD.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        if (order.briefingText.isNotBlank()) {
            Text(order.briefingText, color = HD.TextDim, fontSize = 14.sp, lineHeight = 20.sp)
        }
        if (order.descriptionText.isNotBlank()) {
            Text(order.descriptionText, color = HD.TextDim, fontSize = 13.sp, lineHeight = 19.sp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progresso geral", color = HD.TextDim, fontSize = 12.sp)
                Text(
                    "${"%.1f".format(ptBr, ui.percent)}%",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            ProgressBar(ui.percent, accent)
        }

        order.tasks.forEachIndexed { index, task ->
            TaskRow(
                index = index,
                task = task,
                progress = order.progress.getOrNull(index) ?: 0L,
                planetName = task.planetId?.let { data.planetNames[it] },
                accent = accent,
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val reward = order.mainReward
            Text(
                text = if (reward != null && reward.amount > 0) "Recompensa: ${fmt(reward.amount)} medalhas" else "Recompensa: —",
                color = HD.Yellow,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (ui.state == "active") {
                Text(
                    "Restam ${OrderRepository.remaining(order.expiration)}",
                    color = HD.TextDim,
                    fontSize = 13.sp,
                )
            }
        }

        if (ui.fromSnapshot && ui.state == "active") {
            Text(
                "Exibindo o último registro salvo da ordem.",
                color = HD.TextDim,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun TaskRow(index: Int, task: OrderTask, progress: Long, planetName: String?, accent: Color) {
    val goal = task.goal
    val title: String
    val detail: String
    val fraction: Double

    if (goal != null && goal > 0) {
        title = "Objetivo ${index + 1}"
        detail = "${fmt(progress)} / ${fmt(goal)}"
        fraction = (progress.toDouble() / goal * 100.0)
    } else {
        // Tarefas de planeta: o progresso é 0/1 (não concluído / concluído).
        title = planetName?.let { "Planeta: $it" } ?: "Objetivo ${index + 1}"
        detail = if (progress > 0) "Concluído" else "Em andamento"
        fraction = if (progress > 0) 100.0 else 0.0
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = HD.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = HD.TextDim, fontSize = 12.sp)
        }
        ProgressBar(fraction, accent)
    }
}
