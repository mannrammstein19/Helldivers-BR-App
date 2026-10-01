package br.com.helldiversbr.app.ui.screens

import br.com.helldiversbr.app.ui.presentation.visualCampaignPercent
import br.com.helldiversbr.app.ui.presentation.PlanetCounters

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
import br.com.helldiversbr.app.data.localizedText
import br.com.helldiversbr.app.ui.HomeState
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ptBrWar = Locale("pt", "BR")
private fun fmtWar(n: Long): String = NumberFormat.getInstance(ptBrWar).format(n)
private fun pct(value: Double): String = "%.4f%%".format(ptBrWar, value)
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
    val data = when (state) {
        is HomeState.Ready -> state.data
        is HomeState.Error -> state.last
        else -> null
    }
    if (data != null) {
        WarList(data, (state as? HomeState.Ready)?.refreshing == true,
            (state as? HomeState.Error)?.message, onRefresh, onOpenMap, contentPadding)
    } else {
        Column(Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (state is HomeState.Error) {
                Text(state.message, color = HD.Text)
                YellowButton("TENTAR NOVAMENTE", onRefresh)
            } else CircularProgressIndicator(color = HD.Yellow)
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
        item(key = "war-section-1") {
            WarHeader(data, onRefresh)
            if (data.staleSources.isNotEmpty()) Text(
                "Última leitura válida mantida: ${data.staleSources.joinToString()}. Tentaremos novamente na próxima atualização.",
                color = HD.Gold, fontSize = 11.sp)
        }

        if (refreshing) {
            item(key = "war-section-2") {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().height(2.dp),
                    color = HD.Yellow,
                    trackColor = HD.SurfaceHigh,
                )
            }
        }

        if (errorBanner != null) {
            item(key = "war-section-3") {
                HdCard(accent = HD.Red) {
                    SectionLabel("Conexão degradada", HD.Red)
                    Text("$errorBanner Mostrando a última leitura válida.", color = HD.TextDim, fontSize = 12.sp)
                    TextButton(onClick = onRefresh) { Text("ATUALIZAR", color = HD.Yellow, fontWeight = FontWeight.Bold) }
                }
            }
        }

        item(key = "war-section-4") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Helldivers no front", fmtWar(data.helldiversOnFront), "efetivo em campanhas", HD.YellowBright, Modifier.weight(1f), backgroundKey = "war_players")
                StatTile("Frentes ativas", data.activeFronts.toString(), "campanhas detectadas", HD.SignalBlue, Modifier.weight(1f), backgroundKey = "war_fronts")
            }
        }
        item(key = "war-section-5") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Liberações", data.liberationCount.toString(), "ofensivas da Super Terra", HD.Green, Modifier.weight(1f), backgroundKey = "war_liberation")
                StatTile("Defesas", data.defenseCount.toString(), if (data.defenseCount > 0) "ALERTA: planeta sob ataque" else "planetas sob ataque", HD.Red, Modifier.weight(1f), backgroundKey = "war_defense", pulse = data.defenseCount > 0)
            }
        }

        item(key = "war-section-6") { OrderCard(data, collapsible = true, initiallyExpanded = true) }

        item(key = "war-dss") {
            DssWarCard(
                reading = data.dss,
                planetCatalog = data.planetCatalog,
                campaigns = data.campaigns,
            )
        }

        item(key = "war-section-7") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        SectionLabel("⚔ Frentes de batalha", HD.Yellow)
                        Text("CAMPANHAS ATIVAS", color = HD.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Text("${filtered.size} DE ${data.campaigns.size}", color = HD.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = HD.Surface.copy(alpha = .94f)),
                    border = BorderStroke(1.dp, HD.BorderSoft),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("LOCALIZAR FRENTE", color = HD.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                                Text("Planeta, setor, operação e facção", color = HD.TextMuted, fontSize = 10.sp)
                            }
                            if (searchQuery.isNotBlank() || modeFilter != "all" || factionFilter != "all") {
                                TextButton(onClick = {
                                    searchQuery = ""
                                    modeFilter = "all"
                                    factionFilter = "all"
                                }) {
                                    Text("LIMPAR", color = HD.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
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
                                focusedContainerColor = HD.BgDeep,
                                unfocusedContainerColor = HD.BgDeep,
                            ),
                            shape = RoundedCornerShape(14.dp),
                        )

                        HorizontalDivider(color = HD.BorderSoft)
                        SectionLabel("Tipo de operação", HD.TextMuted)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WarFilter("all", "TODAS", modeFilter, { modeFilter = it }, Modifier.weight(1f))
                            WarFilter("attack", "LIBERTAÇÃO", modeFilter, { modeFilter = it }, Modifier.weight(1f))
                            WarFilter("defense", "DEFESA", modeFilter, { modeFilter = it }, Modifier.weight(1f))
                        }

                        HorizontalDivider(color = HD.BorderSoft)
                        SectionLabel("Facção inimiga", HD.TextMuted)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FactionFilter("all", "TODAS", HD.Yellow, factionFilter, { factionFilter = it }, Modifier.weight(1f))
                            FactionFilter("terminids", "TERMINÍDEOS", HD.TerminidOrange, factionFilter, { factionFilter = it }, Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FactionFilter("automatons", "AUTÔMATOS", HD.AutomatonRed, factionFilter, { factionFilter = it }, Modifier.weight(1f))
                            FactionFilter("illuminates", "ILUMINADOS", HD.IlluminatePurple, factionFilter, { factionFilter = it }, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item(key = "war-section-12") { HdCard { Text("Nenhuma frente corresponde ao filtro selecionado.", color = HD.TextDim, fontSize = 13.sp) } }
        } else {
            itemsIndexed(
                filtered,
                key = { _, campaign -> "planet-${campaign.planet.index}" },
            ) { _, campaign ->
                CampaignCard(data, campaign) { selectedPlanetIndex = campaign.planet.index }
            }
        }

        item(key = "war-section-13") {
            HdCard(accent = HD.SignalBlue) {
                SectionLabel("Mapa tático", HD.SignalBlue)
                Text("MAPA GALÁCTICO COMPLETO", color = HD.Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("Visualize setores, planetas e linhas de suprimento no terminal do Mapa Galáctico.", color = HD.TextDim, fontSize = 12.sp, lineHeight = 18.sp)
                YellowButton("ABRIR MAPA GALÁCTICO  →", onOpenMap, Modifier.fillMaxWidth())
            }
        }

        if (data.dispatches.isNotEmpty()) item(key = "war-dispatch-feed") { DispatchFeed(data.dispatches) }

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
                Text("CENTRAL DE GUERRA", color = HD.Text, fontSize = 27.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 5.dp))
            }
            TextButton(onClick = onRefresh) { Text("ATUALIZAR", color = HD.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp) }
        }
        Text("Monitoramento das frentes ativas, efetivo Helldiver, Ordem Maior, Estação Democracia e comunicações do Alto Comando.", color = HD.TextDim, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp))
        val hasSavedTelemetry = data.telemetrySource == "cache" || "campanhas" in data.staleSources
        val time = if (data.updatedAtMillis > 0L) SimpleDateFormat("HH:mm:ss", ptBrWar).format(Date(data.updatedAtMillis)) else "SEM LEITURA"
        val sourceColor = when {
            hasSavedTelemetry -> HD.Gold
            data.telemetrySource == "direct" -> HD.SignalBlue
            data.telemetrySource == "mixed" -> HD.Yellow
            else -> HD.Green
        }
        val sourceLabel = when {
            hasSavedTelemetry -> "ÚLTIMA LEITURA SALVA"
            data.telemetrySource == "direct" -> "API DIRETA DO JOGO"
            data.telemetrySource == "mixed" -> "FONTES COMBINADAS"
            else -> "API DA COMUNIDADE"
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(50)).background(sourceColor).size(7.dp))
            Text("  $sourceLabel // $time", color = sourceColor, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
        }
    }
}

@Composable
private fun WarFilter(id: String, label: String, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val active = selected == id
    Card(
        modifier = modifier.clickable { onSelect(id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (active) HD.Yellow else HD.Surface),
        border = BorderStroke(1.dp, if (active) HD.Yellow else HD.Border),
    ) {
        Text(label, color = if (active) Color.Black else HD.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun FactionFilter(id: String, label: String, accent: Color, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val active = selected == id
    Card(
        modifier = modifier.clickable { onSelect(id) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (active) accent.copy(alpha = 0.15f) else HD.BgDeep),
        border = BorderStroke(if (active) 1.4.dp else 1.dp, if (active) accent else HD.Border),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (id != "all") {
                SiteImage(
                    when (id) {
                        "terminids" -> "terminid"
                        "automatons" -> "automaton"
                        else -> "illuminate"
                    },
                    label,
                    Modifier.size(25.dp),
                )
                Box(Modifier.size(7.dp))
            } else {
                Text("◎", color = if (active) accent else HD.TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Box(Modifier.size(7.dp))
            }
            Text(
                label,
                color = if (active) accent else HD.TextDim,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = .15.sp,
            )
        }
    }
}

@Composable
fun CampaignCard(data: HomeData, campaign: Campaign, onOpen: () -> Unit) {
    val planet = campaign.planet
    val catalog = data.planetCatalog[planet.index]
    val dssStation = data.dss.station
    val dssHere = dssStation?.let { station ->
        station.planet.index == planet.index ||
            localizedText(station.planet.name).equals(planet.nameText, ignoreCase = true)
    } == true
    val defense = planet.event != null
    val ownerFactionRaw = OrderRepository.ownerFaction(campaign)
    val enemyFactionRaw = OrderRepository.enemyFaction(campaign)
    val ownerFaction = OrderRepository.factionLabel(ownerFactionRaw)
    val enemyFaction = OrderRepository.factionLabel(enemyFactionRaw)
    val accent = factionColor(enemyFactionRaw, defense)
    val modeColor = if (defense) HD.DefenseBlue else accent
    val percent = OrderRepository.campaignPercent(campaign)
    val displayedPercent = visualCampaignPercent(campaign)
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

            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(planet.nameText.uppercase(), color = HD.Text, fontSize = 23.sp, lineHeight = 24.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)), fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(sector.uppercase(), color = HD.TextMuted, fontSize = 10.sp, lineHeight = 12.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    if (dssHere) {
                        Text("◆ DSS // ESTAÇÃO DEMOCRACIA", color = HD.Yellow, fontSize = 8.sp, lineHeight = 11.sp, fontWeight = FontWeight.Black, letterSpacing = .6.sp)
                    }
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
                    ProgressBlock("DEFESA HELLDIVERS", displayedPercent, HD.DefenseBlue)
                    ProgressBlock("INVASÃO ${enemyFaction.uppercase()}", invasionProgress ?: 0.0, accent, valueOverride = invasionProgress?.let(::pct) ?: "—")
                } else {
                    ProgressBlock("CONTROLE PLANETÁRIO", displayedPercent, accent)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TacticalMetric("HELLDIVERS OPERANDO", fmtWar(planet.statistics.playerCount), "%.1f%% do efetivo ativo".format(ptBrWar, share), HD.Text, Modifier.weight(1f))
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
                if (planet.regions.isNotEmpty()) Text("${planet.regions.size} regiões • ${planet.regions.count { it.isAvailable == true }} disponíveis", color = HD.TextDim, fontSize = 11.sp)
                Text("↗ TOQUE PARA ABRIR DOSSIÊ TÁTICO", color = HD.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            }
        }
    }
}

@Composable
private fun ProgressBlock(label: String, value: Double, color: Color, valueOverride: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = HD.TextDim, fontSize = 10.sp)
        Text(valueOverride ?: pct(value), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = HD.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (label == "HELLDIVERS OPERANDO") SiteImage("helldivers_active", "Helldivers ativos", Modifier.size(18.dp))
            Text(value, color = valueColor, fontSize = 18.sp, lineHeight = 20.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
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
                        DossierFact("CONTROLE", pct(visualCampaignPercent(campaign)), Modifier.weight(1f))
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
                    PlanetCounters(p)
                    PlanetRegions(p)
                    Text("TELEMETRIA NATIVA // DADOS SINCRONIZADOS COM A CENTRAL DE GUERRA", color = HD.TextMuted, fontSize = 8.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

@Composable
private fun DossierFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(end = 8.dp)) {
        Text(label, color = HD.TextMuted, fontSize = 9.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = HD.Text, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
