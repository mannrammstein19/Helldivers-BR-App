package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import br.com.helldiversbr.app.data.Campaign
import br.com.helldiversbr.app.data.HomeData
import br.com.helldiversbr.app.data.OrderRepository
import br.com.helldiversbr.app.ui.HomeState
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ptBrWar = Locale("pt", "BR")
private fun fmtWar(n: Long): String = NumberFormat.getInstance(ptBrWar).format(n)
private fun pct(value: Double): String = "%.2f%%".format(ptBrWar, value)
private fun rateText(rate: Double?): String = when {
    rate == null || !rate.isFinite() -> "—"
    rate > 0 -> "+%.2f%%/h".format(ptBrWar, rate)
    else -> "%.2f%%/h".format(ptBrWar, rate)
}

@Composable
fun WarScreen(
    state: HomeState,
    onRefresh: () -> Unit,
    onOpenMap: () -> Unit,
    contentPadding: PaddingValues,
) {
    Box(Modifier.fillMaxSize().background(Color.Transparent)) {
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
    var modeFilter by rememberSaveable { mutableStateOf("all") }
    var factionFilter by rememberSaveable { mutableStateOf("all") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedPlanetIndex by rememberSaveable { mutableStateOf<Long?>(null) }

    val normalizedQuery = searchQuery.trim().lowercase()
    val filtered = data.campaigns.filter { campaign ->
        val modeOk = when (modeFilter) {
            "attack" -> campaign.planet.event == null
            "defense" -> campaign.planet.event != null
            else -> true
        }
        val enemyFaction = OrderRepository.enemyFaction(campaign)
        val factionOk = factionFilter == "all" || OrderRepository.factionKey(enemyFaction) == factionFilter
        val catalog = data.planetCatalog[campaign.planet.index]
        val sector = campaign.planet.sector.ifBlank { catalog?.sector.orEmpty() }
        val textOk = normalizedQuery.isBlank() ||
            campaign.planet.nameText.lowercase().contains(normalizedQuery) ||
            sector.lowercase().contains(normalizedQuery) ||
            OrderRepository.factionLabel(enemyFaction).lowercase().contains(normalizedQuery)
        modeOk && factionOk && textOk
    }
    val selected = selectedPlanetIndex?.let { index -> data.campaigns.firstOrNull { it.planet.index == index } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 22.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { WarHeader(data, onRefresh) }

        if (refreshing) {
            item {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().height(2.dp),
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
                StatTile("Helldivers no front", fmtWar(data.helldiversOnFront), "efetivo em campanhas", HD.YellowBright, Modifier.weight(1f), backgroundKey = "war_players")
                StatTile("Frentes ativas", data.activeFronts.toString(), "campanhas detectadas", HD.SignalBlue, Modifier.weight(1f), backgroundKey = "war_fronts")
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Liberações", data.liberationCount.toString(), "ofensivas da Super Terra", HD.Green, Modifier.weight(1f), backgroundKey = "war_liberation")
                StatTile("Defesas", data.defenseCount.toString(), "planetas sob ataque", HD.Red, Modifier.weight(1f), backgroundKey = "war_defense")
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

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = HD.TextMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Limpar pesquisa", tint = HD.TextMuted)
                            }
                        }
                    },
                    placeholder = { Text("BUSCAR PLANETA OU SETOR", color = HD.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HD.Yellow,
                        unfocusedBorderColor = HD.Border,
                        focusedTextColor = HD.Text,
                        unfocusedTextColor = HD.Text,
                        cursorColor = HD.Yellow,
                        focusedContainerColor = HD.Surface,
                        unfocusedContainerColor = HD.Surface,
                    ),
                )

                SectionLabel("Tipo de operação", HD.TextMuted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WarFilter("all", "TODAS", modeFilter, { modeFilter = it }, Modifier.weight(1f))
                    WarFilter("attack", "LIBERTAÇÃO", modeFilter, { modeFilter = it }, Modifier.weight(1f))
                    WarFilter("defense", "DEFESA", modeFilter, { modeFilter = it }, Modifier.weight(1f))
                }

                SectionLabel("Facção inimiga", HD.TextMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FactionFilter("all", "TODAS", HD.Yellow, factionFilter) { factionFilter = it } }
                    item { FactionFilter("terminids", "TERMINÍDEOS", HD.TerminidOrange, factionFilter) { factionFilter = it } }
                    item { FactionFilter("automatons", "AUTÔMATOS", HD.AutomatonRed, factionFilter) { factionFilter = it } }
                    item { FactionFilter("illuminates", "ILUMINADOS", HD.IlluminatePurple, factionFilter) { factionFilter = it } }
                }
            }
        }

        if (filtered.isEmpty()) {
            item { HdCard { Text("Nenhuma frente corresponde ao filtro selecionado.", color = HD.TextDim, fontSize = 13.sp) } }
        } else {
            itemsIndexed(
                filtered,
                key = { index, campaign -> "${campaign.id ?: campaign.planet.nameText}-${campaign.planet.event?.id ?: 0}-$index" },
            ) { _, campaign ->
                CampaignCard(data, campaign) { selectedPlanetIndex = campaign.planet.index }
            }
        }

        item {
            HdCard(accent = HD.SignalBlue) {
                SectionLabel("Mapa tático", HD.SignalBlue)
                Text("MAPA GALÁCTICO COMPLETO", color = HD.Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("Visualize setores, planetas e linhas de suprimento no terminal do Mapa Galáctico.", color = HD.TextDim, fontSize = 12.sp, lineHeight = 18.sp)
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
            itemsIndexed(data.dispatches.take(5), key = { index, dispatch -> "dispatch-${dispatch.id}-$index" }) { _, dispatch -> DispatchCard(dispatch) }
        }
    }

    if (selected != null) {
        PlanetDossierDialog(data, selected, onDismiss = { selectedPlanetIndex = null })
    }
}

@Composable
private fun WarHeader(data: HomeData, onRefresh: () -> Unit) {
    Column(Modifier.padding(top = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("COMANDO E CONTROLE // SUPREMA AUTORIDADE", HD.Yellow)
                Text("CENTRAL DE GUERRA", color = HD.Text, fontSize = 27.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 5.dp))
            }
            TextButton(onClick = onRefresh) { Text("ATUALIZAR", color = HD.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp) }
        }
        Text("Monitoramento das frentes ativas, efetivo Helldiver, Ordem Maior e comunicações do Alto Comando.", color = HD.TextDim, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp))
        val time = SimpleDateFormat("HH:mm:ss", ptBrWar).format(Date(data.updatedAtMillis))
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(50)).background(HD.Green).size(7.dp))
            Text("  TELEMETRIA ONLINE // $time", color = HD.Green, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
        }
    }
}

@Composable
private fun WarFilter(id: String, label: String, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val active = selected == id
    Card(
        modifier = modifier.clickable { onSelect(id) },
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = if (active) HD.Yellow else HD.Surface),
        border = BorderStroke(1.dp, if (active) HD.Yellow else HD.Border),
    ) {
        Text(label, color = if (active) Color.Black else HD.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun FactionFilter(id: String, label: String, accent: Color, selected: String, onSelect: (String) -> Unit) {
    val active = selected == id
    Card(
        modifier = Modifier.clickable { onSelect(id) },
        shape = RoundedCornerShape(50.dp),
        colors = CardDefaults.cardColors(containerColor = if (active) accent.copy(alpha = 0.18f) else HD.Surface),
        border = BorderStroke(1.dp, if (active) accent else HD.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(accent))
            Text(label, color = if (active) accent else HD.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.45.sp)
        }
    }
}

@Composable
private fun CampaignCard(data: HomeData, campaign: Campaign, onOpen: () -> Unit) {
    val planet = campaign.planet
    val catalog = data.planetCatalog[planet.index]
    val defense = planet.event != null
    val ownerFactionRaw = OrderRepository.ownerFaction(campaign)
    val enemyFactionRaw = OrderRepository.enemyFaction(campaign)
    val ownerFaction = OrderRepository.factionLabel(ownerFactionRaw)
    val enemyFaction = OrderRepository.factionLabel(enemyFactionRaw)
    val accent = factionColor(enemyFactionRaw, defense)
    val modeColor = if (defense) HD.DefenseBlue else accent
    val percent = OrderRepository.campaignPercent(campaign)
    val rate = OrderRepository.campaignRate(data, campaign)
    val enemyPressure = if (defense) OrderRepository.defenseEnemyRate(planet.event) else OrderRepository.liberationEnemyPressure(campaign)
    val invasionProgress = if (defense) OrderRepository.defenseEnemyProgress(planet.event) else null
    val etaWin = OrderRepository.etaFromRate(percent, rate)
    val etaDeadline = if (defense) OrderRepository.remaining(planet.event?.endTime) else null
    val headerEta = if (defense) etaDeadline else etaWin
    val totalPlayers = data.helldiversOnFront.coerceAtLeast(1L)
    val share = planet.statistics.playerCount.toDouble() / totalPlayers.toDouble() * 100.0
    val sector = planet.sector.ifBlank { catalog?.sector.orEmpty() }.ifBlank { "Setor desconhecido" }
    val image = PlanetVisuals.planetImage(planet.index, planet.nameText, catalog)
    val hazards = PlanetVisuals.hazards(catalog).take(4)
    val biome = PlanetVisuals.biomeLabel(catalog)
    val status = when {
        defense && invasionProgress != null && percent > invasionProgress + 0.15 -> "▲ VENCENDO"
        defense && invasionProgress != null && invasionProgress > percent + 0.15 -> "▼ PERDENDO"
        defense -> "◆ EQUILIBRADO"
        rate == null -> "◌ COLETANDO"
        rate > 0.005 -> "▲ AVANÇO"
        rate < -0.005 -> "▼ RECUO"
        else -> "◆ ESTÁVEL"
    }
    val statusColor = when {
        "VENCENDO" in status || "AVANÇO" in status -> HD.Green
        "PERDENDO" in status || "RECUO" in status -> HD.Red
        else -> HD.TextDim
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HD.BgDeep),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.85f)),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(HD.Surface).padding(horizontal = 13.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    AsyncImage(
                        model = if (defense) "https://helldivers-br.pages.dev/imagens/guerra/operacoes/defesa.png" else "https://helldivers-br.pages.dev/imagens/guerra/operacoes/libertacao.png",
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Text(if (defense) "DEFESA" else "LIBERTAÇÃO", color = modeColor, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                }
                Text(status, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
                Text(headerEta ?: "—", color = HD.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(planet.nameText.uppercase(), color = HD.Text, fontSize = 25.sp, lineHeight = 26.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(sector.uppercase(), color = HD.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 1.dp))
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        AsyncImage(
                            model = PlanetVisuals.factionLogo(ownerFactionRaw, defense),
                            contentDescription = ownerFaction,
                            modifier = Modifier.size(26.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Text(ownerFaction.uppercase(), color = factionColor(ownerFactionRaw, defense), fontSize = 11.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    }
                    if (defense && enemyFactionRaw.isNotBlank() && !OrderRepository.isHumanFaction(enemyFactionRaw)) {
                        Text("ATACANTE: ${enemyFaction.uppercase()}", color = accent, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
                    }
                }
            }

            Box(Modifier.fillMaxWidth().aspectRatio(16f / 7.2f)) {
                AsyncImage(model = image, contentDescription = "${planet.nameText} — $biome", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.36f)))))
                if (hazards.isNotEmpty()) {
                    Row(Modifier.align(Alignment.BottomStart).padding(11.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        hazards.forEach { HazardBadge(it) }
                    }
                }
            }

            Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (defense) {
                    ProgressBlock("DEFESA HELLDIVERS", percent, HD.DefenseBlue)
                    ProgressBlock("INVASÃO ${enemyFaction.uppercase()}", invasionProgress ?: 0.0, accent, valueOverride = invasionProgress?.let(::pct) ?: "—")
                } else {
                    ProgressBlock("CONTROLE PLANETÁRIO", percent, accent)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TacticalMetric("👥 HELLDIVERS OPERANDO", fmtWar(planet.statistics.playerCount), "%.1f%% do efetivo ativo".format(ptBrWar, share), HD.Text, Modifier.weight(1f))
                    TacticalMetric("■ ${if (defense) "AVANÇO DA DEFESA / HORA" else "AVANÇO LÍQUIDO / HORA"}", rateText(rate), if (rate == null) "aguardando nova amostra" else "saldo planetário observado", if ((rate ?: 0.0) >= 0) HD.DefenseBlue else HD.Red, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TacticalMetric("PRESSÃO ${enemyFaction.uppercase()}", rateText(enemyPressure), if (defense) "ritmo do relógio da invasão" else "regeneração registrada na API", accent, Modifier.weight(1f))
                    TacticalMetric(if (defense) "🏁 TEMPO DA DEFESA" else "🏁 VITÓRIA ESTIMADA", if (defense) etaWin ?: "calculando" else etaWin ?: "calculando", if (defense) "prazo inimigo: ${etaDeadline ?: "—"}" else "projeção no ritmo atual", HD.Text, Modifier.weight(1f))
                }

                HorizontalDivider(color = HD.BorderSoft)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("BIOMA: $biome", color = HD.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(status, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                if (planet.regions.isNotEmpty()) Text("${planet.regions.size} regiões neste planeta", color = HD.TextDim, fontSize = 11.sp)
                Text("↗ TOQUE PARA ABRIR DOSSIÊ TÁTICO", color = HD.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            }
        }
    }
}

@Composable
private fun ProgressBlock(label: String, value: Double, color: Color, valueOverride: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = HD.TextDim, fontSize = 10.sp)
        Text(valueOverride ?: pct(value), color = HD.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
    ProgressBar(value, color)
}

@Composable
private fun TacticalMetric(label: String, value: String, detail: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = HD.Surface),
        border = BorderStroke(1.dp, HD.Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = HD.TextDim, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.35.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(value, color = valueColor, fontSize = 18.sp, lineHeight = 20.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, color = HD.TextMuted, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun HazardBadge(item: PlanetVisuals.HazardVisual) {
    Card(
        shape = RoundedCornerShape(9.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.74f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
    ) {
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            val url = PlanetVisuals.hazardIconUrl(item)
            if (url != null) {
                AsyncImage(model = url, contentDescription = item.label, modifier = Modifier.size(29.dp), contentScale = ContentScale.Fit)
            } else {
                Text(item.symbol, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun PlanetDossierDialog(data: HomeData, campaign: Campaign, onDismiss: () -> Unit) {
    val p = campaign.planet
    val catalog = data.planetCatalog[p.index]
    val defense = p.event != null
    val ownerFactionRaw = OrderRepository.ownerFaction(campaign)
    val enemyFactionRaw = OrderRepository.enemyFaction(campaign)
    val accent = factionColor(enemyFactionRaw, defense)
    val image = PlanetVisuals.planetImage(p.index, p.nameText, catalog)
    val sector = p.sector.ifBlank { catalog?.sector.orEmpty() }.ifBlank { "Setor desconhecido" }
    val biome = PlanetVisuals.biomeLabel(catalog)
    val hazards = PlanetVisuals.hazards(catalog)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = HD.BgDeep),
            border = BorderStroke(1.dp, accent),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 7f)) {
                    AsyncImage(model = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.05f), HD.BgDeep))))
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(50))) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                    Column(Modifier.align(Alignment.BottomStart).padding(15.dp)) {
                        SectionLabel(if (defense) "DEFESA // DOSSIÊ TÁTICO" else if (data.campaigns.any { it.planet.index == p.index }) "LIBERTAÇÃO // DOSSIÊ TÁTICO" else "DOSSIÊ TÁTICO", accent)
                        Text(p.nameText.uppercase(), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text(sector.uppercase(), color = HD.TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    }
                }
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DossierFact("CONTROLE ATUAL", OrderRepository.factionLabel(ownerFactionRaw).uppercase(), Modifier.weight(1f))
                        DossierFact("BIOMA", biome, Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DossierFact(if (defense) "ATACANTE" else "FACÇÃO INIMIGA", OrderRepository.factionLabel(enemyFactionRaw).uppercase(), Modifier.weight(1f))
                        DossierFact("SETOR", sector.uppercase(), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DossierFact("HELLDIVERS", fmtWar(p.statistics.playerCount), Modifier.weight(1f))
                        DossierFact("CONTROLE", pct(OrderRepository.campaignPercent(campaign)), Modifier.weight(1f))
                    }
                    if (hazards.isNotEmpty()) {
                        HorizontalDivider(color = HD.BorderSoft)
                        SectionLabel("Condições planetárias", HD.TextDim)
                        hazards.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                HazardBadge(item)
                                Text(item.label.uppercase(), color = HD.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    HorizontalDivider(color = HD.BorderSoft)
                    PlanetRegions(p)
                    Text("TELEMETRIA NATIVA // DADOS SINCRONIZADOS COM A CENTRAL DE GUERRA", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
            }
        }
    }
}

@Composable
private fun DossierFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(end = 8.dp)) {
        Text(label, color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
        Text(value, color = HD.Text, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
