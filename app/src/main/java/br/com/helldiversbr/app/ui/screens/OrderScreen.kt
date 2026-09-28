package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.SiteAssets
import br.com.helldiversbr.app.data.HelldiversApi
import br.com.helldiversbr.app.data.HomeData
import br.com.helldiversbr.app.data.OrderRepository
import br.com.helldiversbr.app.data.OrderTask
import br.com.helldiversbr.app.data.translateKnown
import br.com.helldiversbr.app.ui.HomeState
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale

private val orderLocale = Locale("pt", "BR")
private fun orderFmt(n: Long): String = NumberFormat.getInstance(orderLocale).format(n)

@Composable
fun OrderScreen(
    state: HomeState,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
) {
    when (state) {
        HomeState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = HD.Yellow)
                Text("RECEBENDO ORDENS DO ALTO COMANDO...", color = HD.TextDim, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 14.dp))
            }
        }
        is HomeState.Ready -> OrderContent(state.data, state.refreshing, null, onRefresh, contentPadding)
        is HomeState.Error -> {
            val last = state.last
            if (last != null) OrderContent(last, false, state.message, onRefresh, contentPadding)
            else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(30.dp)) {
                    SectionLabel("Ordem indisponível", HD.Red)
                    Text(state.message, color = HD.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                    YellowButton("TENTAR NOVAMENTE", onRefresh, Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun OrderContent(
    data: HomeData,
    refreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
) {
    val ui = data.order
    val order = ui.order
    val accent = when (ui.state) {
        "completed" -> HD.Green
        "failed" -> HD.Red
        else -> HD.Yellow
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = contentPadding.calculateTopPadding() + 10.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (refreshing) item {
            LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = HD.Yellow, trackColor = HD.SurfaceHigh)
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("◆", color = HD.Text, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("  Ordem Maior", color = HD.Text, fontSize = 19.sp, fontWeight = FontWeight.Black)
                }
                Text("ALTO COMANDO DA SUPER TERRA", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = HD.BgDeep.copy(alpha = 0.94f)),
                border = BorderStroke(1.dp, HD.Border),
            ) {
                Column {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 7.2f)) {
                        SiteImage(
                            assetKey = SiteAssets.orderKey(ui.state),
                            description = "Ordem Maior: ${ui.state}",
                            modifier = Modifier.fillMaxSize(),
                            scale = ContentScale.Crop,
                        )
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.8f to Color.Transparent, 1f to HD.BgDeep.copy(alpha = 0.55f))))
                    }

                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (order == null) {
                            Text("ORDEM MAIOR", color = HD.Text, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Text("Aguardando novas instruções do Alto Comando.", color = HD.TextDim, fontSize = 13.sp)
                            YellowButton("ATUALIZAR", onRefresh)
                        } else {
                            val completed = if (ui.state == "completed") order.tasks.size else order.tasks.indices.count { i ->
                                val task = order.tasks[i]
                                val progress = order.progress.getOrNull(i) ?: 0L
                                var showForecast by rememberSaveable(data.order.order?.id, data.order.order?.expiration, index) { mutableStateOf(false) }
    val goal = task.goal
                                if (goal != null && goal > 0) progress >= goal else progress > 0
                            }
                            Text(
                                translateKnown(order.titleText).ifBlank { "ORDEM MAIOR" },
                                color = HD.Text,
                                fontSize = 25.sp,
                                lineHeight = 27.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OrderHeaderStat("TEMPO RESTANTE", if (ui.state == "active") OrderRepository.remaining(order.expiration) else if (ui.state in listOf("completed", "failed")) "ENCERRADA" else "AGUARDANDO", Modifier.weight(1f))
                                OrderHeaderStat("OBJETIVOS CONCLUÍDOS", "$completed / ${order.tasks.size}", Modifier.weight(1f))
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = HD.SurfaceSoft),
                                shape = RoundedCornerShape(5.dp),
                                border = BorderStroke(1.dp, HD.BorderSoft),
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                    OrderRewards(order, accent)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (error != null) item {
            HdCard(accent = HD.Red) {
                SectionLabel("Conexão degradada", HD.Red)
                Text("$error Exibindo a última leitura válida.", color = HD.TextDim, fontSize = 11.sp)
                TextButton(onClick = onRefresh) { Text("ATUALIZAR", color = HD.Yellow, fontWeight = FontWeight.Black) }
            }
        }

        if (order != null) {
            item {
                HdCard(accent = HD.Yellow) {
                    SectionLabel("Ordem Maior ativa // ${order.tasks.size} objetivos", HD.Yellow)
                    Text(
                        order.briefingText.ifBlank { order.descriptionText.ifBlank { "Execute os objetivos definidos pelo Alto Comando da Super Terra." } },
                        color = HD.Text,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("◆ Objetivos da ordem", HD.Text)
                    Text("${order.tasks.size} FRENTES / OBJETIVOS // EM ANDAMENTO", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                }
            }

            items(order.tasks.size) { index ->
                val task = order.tasks[index]
                val progress = order.progress.getOrNull(index) ?: 0L
                OrderObjectiveCard(data, task, index, progress)
            }

            item {
                Text(
                    "ORDEM EM EXECUÇÃO // ${order.tasks.size} OBJETIVOS REGISTRADOS\n\nALTO COMANDO",
                    color = HD.TextMuted,
                    fontSize = 8.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun OrderHeaderStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
        Text(value.uppercase(), color = HD.Text, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun OrderObjectiveCard(data: HomeData, task: OrderTask, index: Int, progress: Long) {
    val goal = task.goal
    val planetName = task.planetId?.let { data.planetNames[it] }
    val campaign = task.planetId?.let { id -> data.campaigns.firstOrNull { it.planet.index == id } }
    val factionRaw = OrderRepository.taskFaction(task)
        .takeIf { it.isNotBlank() && !OrderRepository.isHumanFaction(it) }
        ?: campaign?.let { OrderRepository.enemyFaction(it) }.orEmpty()
    val accent = if (factionRaw.isNotBlank()) factionColor(factionRaw) else HD.Yellow
    val factionLabel = if (factionRaw.isNotBlank()) OrderRepository.factionLabel(factionRaw).uppercase() else "ALVO CLASSIFICADO"
    val percent = when {
        goal != null && goal > 0 -> (progress.toDouble() / goal.toDouble() * 100.0).coerceIn(0.0, 100.0)
        campaign != null -> OrderRepository.campaignPercent(campaign)
        progress > 0 -> 100.0
        else -> 0.0
    }
    val done = percent >= 99.999 || (goal != null && goal > 0 && progress >= goal)

    val headline = when {
        task.type == 3 && goal != null && goal > 0 && factionRaw.isNotBlank() -> "ELIMINAR ${orderFmt(goal)} $factionLabel"
        task.type == 11 && planetName != null -> "LIBERTAR ${planetName.uppercase()}"
        task.type == 12 && planetName != null -> "DEFENDER ${planetName.uppercase()}"
        task.type == 13 && planetName != null -> "CONTROLAR ${planetName.uppercase()}"
        planetName != null -> planetName.uppercase()
        goal != null && goal > 0 -> "OBJETIVO DE ERRADICAÇÃO // ${orderFmt(goal)}"
        else -> "OBJETIVO ${index + 1}"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.055f)),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.95f)),
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("OBJETIVO ${index + 1} // ERRADICAÇÃO", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
                Text(factionLabel, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (factionRaw.isNotBlank()) {
                    AsyncImage(
                        model = PlanetVisuals.factionLogo(factionRaw),
                        contentDescription = factionLabel,
                        modifier = Modifier.size(28.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                Text(
                    headline,
                    color = HD.Text,
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = if (factionRaw.isNotBlank()) 10.dp else 0.dp).weight(1f),
                )
            }
            ProgressBar(percent, accent)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (goal != null && goal > 0) "${orderFmt(progress)} / ${orderFmt(goal)}" else "${"%.1f".format(orderLocale, percent)}%",
                    color = HD.TextMuted,
                    fontSize = 9.sp,
                )
                Text("${"%.1f".format(orderLocale, percent)}%", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            TextButton(onClick = { showForecast = !showForecast }) {
                Text(if (showForecast) "RITMO E PREVISÃO −" else "RITMO E PREVISÃO +", color = accent)
            }
            if (showForecast) {
                val rate = if (task.type in listOf(11, 12, 13) && campaign != null)
                    OrderRepository.campaignRate(data, campaign) else data.orderRates[index]
                val eta = OrderRepository.etaFromRate(percent, rate)
                Text(when {
                    done -> "Objetivo concluído."
                    "Ordem Maior" in data.staleSources || (campaign != null && "campanhas" in data.staleSources) -> "Telemetria sem atualização. Previsão pausada até receber uma nova leitura válida."
                    data.order.state != "active" -> "Sem previsão: aguardando confirmação ou ordem encerrada."
                    rate == null -> "Coletando amostras. São necessárias duas leituras válidas, separadas por pelo menos 30 segundos. A atualização automática ocorre a cada 60 segundos enquanto o app está aberto."
                    rate <= 0 -> "Sem avanço positivo na última amostra. Ainda não há previsão de conclusão."
                    else -> "Ritmo observado: ${"%.2f".format(orderLocale, rate)}%/h. Conclusão estimada em ${eta ?: "—"}."
                }, color = HD.TextDim, fontSize = 12.sp, lineHeight = 17.sp)
                Text("Estimativa baseada na variação recente; pode mudar com o esforço dos jogadores.", color = HD.TextMuted, fontSize = 11.sp)
            }
        }
    }
}
