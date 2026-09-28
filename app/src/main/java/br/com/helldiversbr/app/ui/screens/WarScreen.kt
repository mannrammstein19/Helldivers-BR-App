package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.Campaign
import br.com.helldiversbr.app.data.HomeData
import br.com.helldiversbr.app.data.OrderRepository
import br.com.helldiversbr.app.ui.HomeState
import br.com.helldiversbr.app.ui.theme.HD
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ptBrWar = Locale("pt", "BR")
private fun fmtWar(n: Long): String = NumberFormat.getInstance(ptBrWar).format(n)

@Composable
fun WarScreen(
    state: HomeState,
    onRefresh: () -> Unit,
    onOpenMap: () -> Unit,
    contentPadding: PaddingValues,
) {
    Box(Modifier.fillMaxSize().background(HD.Bg)) {
        when (state) {
            HomeState.Loading -> Column(
                Modifier.fillMaxSize().statusBarsPadding(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = HD.Yellow)
                Text(
                    "MAPEANDO FRENTES DE BATALHA...",
                    color = HD.TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            is HomeState.Ready -> WarList(state.data, state.refreshing, null, onRefresh, onOpenMap, contentPadding)

            is HomeState.Error -> {
                val last = state.last
                if (last != null) {
                    WarList(last, false, state.message, onRefresh, onOpenMap, contentPadding)
                } else {
                    Column(
                        Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SectionLabel("Central offline", HD.Red)
                        Text(state.message, color = HD.Text, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 10.dp))
                        YellowButton("TENTAR NOVAMENTE", onRefresh, Modifier.padding(top = 18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WarList(
    data: HomeData,
    refreshing: Boolean,
    errorBanner: String?,
    onRefresh: () -> Unit,
    onOpenMap: () -> Unit,
    contentPadding: PaddingValues,
) {
    var filter by rememberSaveable { mutableStateOf("all") }
    val filtered = data.campaigns.filter {
        when (filter) {
            "attack" -> it.planet.event == null
            "defense" -> it.planet.event != null
            else -> true
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 22.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { WarHeader(data, onRefresh) }

        if (refreshing) {
            item {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth(),
                    color = HD.Yellow,
                    trackColor = HD.SurfaceHigh,
                )
            }
        }

        if (errorBanner != null) {
            item {
                HdCard(accent = HD.Red) {
                    SectionLabel("Conexão degradada", HD.Red)
                    Text("$errorBanner Mostrando a última leitura válida.", color = HD.TextDim, fontSize = 12.sp)
                    TextButton(onClick = onRefresh) { Text("ATUALIZAR", color = HD.Yellow, fontWeight = FontWeight.Bold) }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "Helldivers no front",
                    fmtWar(data.helldiversOnFront),
                    "efetivo em campanhas",
                    HD.YellowBright,
                    Modifier.weight(1f),
                )
                StatTile(
                    "Frentes ativas",
                    data.activeFronts.toString(),
                    "campanhas detectadas",
                    HD.SignalBlue,
                    Modifier.weight(1f),
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "Liberações",
                    data.liberationCount.toString(),
                    "ofensivas da Super Terra",
                    HD.Green,
                    Modifier.weight(1f),
                )
                StatTile(
                    "Defesas",
                    data.defenseCount.toString(),
                    "planetas sob ataque",
                    HD.Red,
                    Modifier.weight(1f),
                )
            }
        }

        item { OrderCard(data) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        SectionLabel("⚔ Frentes de batalha", HD.Yellow)
                        Text("CAMPANHAS ATIVAS", color = HD.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Text("${filtered.size} EXIBIDAS", color = HD.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WarFilter("all", "TODAS", filter, { filter = it }, Modifier.weight(1f))
                    WarFilter("attack", "LIBERAÇÃO", filter, { filter = it }, Modifier.weight(1f))
                    WarFilter("defense", "DEFESA", filter, { filter = it }, Modifier.weight(1f))
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                HdCard {
                    Text("Nenhuma frente corresponde ao filtro selecionado.", color = HD.TextDim, fontSize = 13.sp)
                }
            }
        } else {
            itemsIndexed(
                filtered,
                key = { index, campaign -> "${campaign.id ?: campaign.planet.nameText}-${campaign.planet.event?.id ?: 0}-$index" },
            ) { _, campaign ->
                CampaignCard(campaign)
            }
        }

        item {
            HdCard(accent = HD.SignalBlue) {
                SectionLabel("Mapa tático", HD.SignalBlue)
                Text("MAPA GALÁCTICO COMPLETO", color = HD.Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(
                    "Visualize setores, planetas e linhas de suprimento no terminal do Mapa Galáctico.",
                    color = HD.TextDim,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                YellowButton("ABRIR MAPA GALÁCTICO  →", onOpenMap, Modifier.fillMaxWidth())
            }
        }

        if (data.dispatches.isNotEmpty()) {
            item {
                Column {
                    SectionLabel("📡 Despachos", HD.SignalBlue)
                    Text("COMUNICAÇÕES RECENTES", color = HD.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }
            itemsIndexed(
                data.dispatches.take(5),
                key = { index, dispatch -> "dispatch-${dispatch.id}-$index" },
            ) { _, dispatch -> DispatchCard(dispatch) }
        }
    }
}

@Composable
private fun WarHeader(data: HomeData, onRefresh: () -> Unit) {
    Column(Modifier.statusBarsPadding().padding(top = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                SectionLabel("COMANDO E CONTROLE // SUPREMA AUTORIDADE", HD.Yellow)
                Text("CENTRAL DE GUERRA", color = HD.Text, fontSize = 27.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 5.dp))
            }
            TextButton(onClick = onRefresh) {
                Text("ATUALIZAR", color = HD.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
            }
        }
        Text(
            "Monitoramento das frentes ativas, efetivo Helldiver, Ordem Maior e comunicações do Alto Comando.",
            color = HD.TextDim,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        val time = SimpleDateFormat("HH:mm:ss", ptBrWar).format(Date(data.updatedAtMillis))
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.background(HD.Green, RoundedCornerShape(50)).padding(4.dp))
            Text("  TELEMETRIA ONLINE // $time", color = HD.Green, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
        }
    }
}

@Composable
private fun WarFilter(
    id: String,
    label: String,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = selected == id
    Card(
        modifier = modifier.clickable { onSelect(id) },
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = if (active) HD.Yellow else HD.Surface),
        border = BorderStroke(1.dp, if (active) HD.Yellow else HD.Border),
    ) {
        Text(
            label,
            color = if (active) Color.Black else HD.TextDim,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun CampaignCard(campaign: Campaign) {
    val planet = campaign.planet
    val defense = planet.event != null
    val rawFaction = OrderRepository.campaignFaction(campaign)
    val faction = OrderRepository.factionLabel(rawFaction)
    val accent = factionColor(rawFaction, defense)
    val percent = OrderRepository.campaignPercent(campaign)
    val mode = if (defense) "DEFESA" else "LIBERTAÇÃO"

    HdCard(accent = accent) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(mode, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Text(
                    planet.nameText.uppercase(),
                    color = HD.Text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Chip(faction.uppercase(), accent)
        }

        if (planet.sector.isNotBlank()) {
            Text("SETOR ${planet.sector.uppercase()}", color = HD.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("PROGRESSO", color = HD.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("${"%.2f".format(ptBrWar, percent)}%", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        ProgressBar(percent, accent)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampaignMiniStat("HELLDIVERS", fmtWar(planet.statistics.playerCount), Modifier.weight(1f))
            CampaignMiniStat(
                if (defense) "PRAZO" else "REGENERAÇÃO",
                if (defense) OrderRepository.remaining(planet.event?.endTime) else if (planet.regenPerSecond > 0) "+${"%.1f".format(ptBrWar, planet.regenPerSecond)}/s" else "—",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CampaignMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        Text(value, color = HD.Text, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 2.dp))
    }
}
