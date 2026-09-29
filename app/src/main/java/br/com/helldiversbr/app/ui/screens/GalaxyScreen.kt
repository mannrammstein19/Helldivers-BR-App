package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.*
import br.com.helldiversbr.app.ui.theme.HD
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalaxyScreen(home: HomeState, contentPadding: PaddingValues, onOpenFullMap: () -> Unit, vm: GalaxyViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(vm, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { vm.refresh(); delay(60_000) }
        }
    }
    val data = when (home) { is HomeState.Ready -> home.data; is HomeState.Error -> home.last; else -> null }
    val campaigns = data?.campaigns.orEmpty().associateBy { it.planet.index }
    val planets = remember(state.planets, campaigns) {
        (state.planets.map { campaigns[it.index]?.planet?.copy(
            position = it.mapPosition, waypoints = it.waypoints, attacking = it.attacking, disabled = it.disabled) ?: it } +
            campaigns.values.map { it.planet }).distinctBy { it.index }
    }
    var query by rememberSaveable { mutableStateOf("") }
    var activeOnly by rememberSaveable { mutableStateOf(false) }
    var allPlanets by rememberSaveable { mutableStateOf(false) }
    var listOpen by rememberSaveable { mutableStateOf(false) }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var routes by rememberSaveable { mutableStateOf(true) }
    var sectors by rememberSaveable { mutableStateOf(true) }
    var territories by rememberSaveable { mutableStateOf(true) }
    var invasions by rememberSaveable { mutableStateOf(true) }
    var faction by rememberSaveable { mutableStateOf("Todas") }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dossierId by rememberSaveable { mutableStateOf<Long?>(null) }
    val contextIds = remember(planets, campaigns, state.dssHost) {
        val core = planets.filter { it.index in campaigns || it.event != null || mapFaction(it.currentOwner) != "human" }
            .map { it.index }.toSet()
        val nearby = planets.filter { it.index in core }.flatMap { it.waypoints + it.attacking }.toSet()
        core + nearby + planets.filter { p -> p.waypoints.any { it in core } || p.attacking.any { it in core } ||
            mapEarth(p) || mapSpecial(p) != null || p.index == state.dssHost }.map { it.index }
    }
    val filtered = remember(planets, query, activeOnly, allPlanets, faction, campaigns, contextIds) {
        planets.filter {
            (!activeOnly || it.index in campaigns || it.event != null) &&
                (allPlanets || activeOnly || query.isNotBlank() || faction != "Todas" || it.index in contextIds) &&
                (faction == "Todas" || galaxyFaction(it) == faction) &&
                searchKey("${it.nameText} ${it.sector}").contains(searchKey(query.trim()))
        }
    }
    val selected = filtered.firstOrNull { it.index == selectedId }
    LaunchedEffect(filtered, selectedId) {
        if (selectedId != null && selected == null) selectedId = null
    }
    val dossierPlanet = planets.firstOrNull { it.index == dossierId }
    if (dossierPlanet != null && data != null) PlanetDossierDialog(data, campaigns[dossierPlanet.index] ?: Campaign(planet = dossierPlanet)) { dossierId = null }
    if (filtersOpen) ModalBottomSheet(onDismissRequest = { filtersOpen = false }, containerColor = HD.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("VISIBILIDADE", color = HD.Yellow, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Todos os planetas", color = HD.Text, modifier = Modifier.weight(1f))
                Switch(checked = allPlanets, onCheckedChange = { allPlanets = it; activeOnly = false })
            }
            Text("Desligado: preserva inimigos, frentes, vizinhos das rotas e locais especiais. A busca e o filtro de facção também encontram planetas fora desse recorte.", color = HD.TextDim, fontSize = 11.sp)
            Text("CAMADAS DO MAPA", color = HD.TextMuted, fontSize = 11.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(activeOnly, { activeOnly = !activeOnly }, label = { Text("Frentes ativas") })
                FilterChip(routes, { routes = !routes }, label = { Text("Rotas") })
                FilterChip(invasions, { invasions = !invasions }, label = { Text("Invasão") })
                FilterChip(territories, { territories = !territories }, label = { Text("Territórios") })
                FilterChip(sectors, { sectors = !sectors }, label = { Text("Setores") })
            }
            Text("FACÇÕES", color = HD.TextMuted, fontSize = 11.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todas", "Super Terra", "Terminídeos", "Autômatos", "Iluminados").forEach { label ->
                    val key = when (label) {
                        "Super Terra" -> "human"
                        "Terminídeos" -> "terminid"
                        "Autômatos" -> "automaton"
                        "Iluminados" -> "illuminate"
                        else -> null
                    }
                    val color = key?.let { mapColor(it) } ?: HD.Yellow
                    FilterChip(faction == label, { faction = label },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        leadingIcon = { if (key != null) SiteImage(key, label, Modifier.size(22.dp), tint = color) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = HD.BgDeep, labelColor = HD.TextDim,
                            selectedContainerColor = color.copy(alpha = .20f), selectedLabelColor = color))
                }
            }
            TextButton(onClick = vm::refresh, enabled = !state.loading) { Text("ATUALIZAR TELEMETRIA") }
            TextButton(onClick = onOpenFullMap) { Text("MAPA DO SITE ↗") }
            TextButton(onClick = { filtersOpen = false }, modifier = Modifier.align(Alignment.End)) { Text("CONCLUÍDO") }
        }
    }
    androidx.activity.compose.BackHandler(enabled = selectedId != null && !filtersOpen && !listOpen && dossierId == null) {
        selectedId = null
    }
    if (listOpen) ModalBottomSheet(onDismissRequest = { listOpen = false }, containerColor = HD.Surface) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.85f).padding(horizontal = 14.dp)) {
            Text("LOCALIZAR PLANETA", color = HD.Yellow, fontWeight = FontWeight.Bold)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Nome ou setor") },
                trailingIcon = { if (query.isNotBlank()) TextButton(onClick = { query = "" }) { Text("Limpar") } })
            Text("${filtered.size} planetas • toque para localizar no mapa", color = HD.TextDim, fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (filtered.isEmpty()) item { Text("Nenhum resultado. Ajuste a busca ou os filtros.", color = HD.TextDim) }
                items(filtered, key = { it.index }) { planet ->
                    Surface(color = HD.BgDeep, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedId = planet.index; listOpen = false
                        }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SiteImage(mapFaction(planet.currentOwner), galaxyFaction(planet), Modifier.size(28.dp))
                            Column(Modifier.weight(1f)) {
                                Text(planet.nameText, color = HD.Text, fontWeight = FontWeight.Bold)
                                Text(planet.sector, color = HD.TextDim, fontSize = 11.sp)
                            }
                            SiteImage("helldivers_active", "Helldivers ativos", Modifier.size(17.dp))
                            Text("${mapPlayerCount(planet.statistics.playerCount)} HD", color = HD.Yellow, fontSize = 12.sp)
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
    // The map owns the whole available destination, including the space behind overlays.
    BoxWithConstraints(Modifier.fillMaxSize().padding(contentPadding).background(Color(0xFF050810))) {
        val panelMaxHeight = (maxHeight * .52f).coerceAtMost(310.dp)
        val visibilityButtonWidth = (maxWidth - 176.dp).coerceAtLeast(48.dp)
        GalaxyCanvas(filtered, planets, routes, sectors, territories, invasions, selectedId,
            campaigns.keys, state.dssHost, onSelect = { selectedId = it }, modifier = Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(color = Color(0xE6090D12), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 2.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("MAPA GALÁCTICO", color = HD.Yellow, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text("${filtered.size} planetas visíveis",
                                color = HD.TextDim, fontSize = 10.sp)
                        }
                        TextButton(onClick = { listOpen = true }) { Text("BUSCAR", fontSize = 11.sp) }
                        TextButton(onClick = { filtersOpen = true }) {
                            val key = when (faction) { "Super Terra" -> "human"; "Terminídeos" -> "terminid"; "Autômatos" -> "automaton"; "Iluminados" -> "illuminate"; else -> null }
                            if (key != null) {
                                SiteImage(key, faction, Modifier.size(20.dp), tint = mapColor(key))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text("FILTROS", fontSize = 11.sp, color = key?.let { mapColor(it) } ?: HD.Yellow)
                        }
                    }
                    if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = HD.Yellow)
                    if (state.error != null || home is HomeState.Error || data?.staleSources?.isNotEmpty() == true) {
                        Text("Dados sem atualização • exibindo a última leitura disponível", color = HD.Gold, fontSize = 10.sp)
                    }
                }
            }
            if (selected != null) {
                Surface(color = Color(0xF2090D12), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, factionColor(selected.currentOwner)),
                    modifier = Modifier.fillMaxWidth().heightIn(max = panelMaxHeight)) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        FloatingPlanetCard(selected, campaigns[selected.index], data, stale = home is HomeState.Error,
                            onClose = { selectedId = null }, onDossier = { dossierId = selected.index })
                    }
                }
            }
        }
        TextButton(onClick = { allPlanets = !(allPlanets && !activeOnly); activeOnly = false; query = ""; faction = "Todas" },
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).widthIn(max = visibilityButtonWidth)
                .background(Color(0xE6090D12), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))) {
            Text(if (allPlanets && !activeOnly) "TODOS ✓" else "TODOS OS PLANETAS", fontSize = 10.sp)
        }
    }
}

private fun galaxyFaction(p: Planet): String = when (p.currentOwner.lowercase()) {
    "humans", "human", "1" -> "Super Terra"
    "terminids", "terminid", "2" -> "Terminídeos"
    "automaton", "automatons", "cyborg", "3" -> "Autômatos"
    "illuminate", "illuminates", "4" -> "Iluminados"
    else -> p.currentOwner.ifBlank { "Desconhecida" }
}


@Composable
private fun FloatingPlanetCard(planet: Planet, campaign: Campaign?, data: HomeData?, stale: Boolean, onClose: () -> Unit, onDossier: () -> Unit) {
    var expanded by rememberSaveable(planet.index) { mutableStateOf(true) }
    val defense = planet.event != null
    val percent = campaign?.let { OrderRepository.campaignPercent(it) }
    val fresh = !stale && data != null && "campanhas" !in data.staleSources
    val rate = if (fresh && data != null && campaign != null) OrderRepository.campaignRate(data, campaign) else null
    val eta = if (percent != null) OrderRepository.etaFromRate(percent, rate) else null
    val accent = if (defense) HD.DefenseBlue else factionColor(planet.currentOwner)
    val count = planet.regions.count { it.isAvailable == true }
    val landscape = PlanetVisuals.planetImage(planet.index, planet.nameText, data?.planetCatalog?.get(planet.index))
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = if (expanded) 92.dp else 58.dp)) {
            if (expanded) {
                AsyncImage(model = landscape, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .2f), Color(0xFF090D12)))))
            }
        Row(Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            SiteImage(mapFaction(planet.currentOwner), galaxyFaction(planet), Modifier.size(26.dp))
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(planet.nameText, color = HD.Text, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 2)
                Text("${planet.sector} • ${if (defense) "DEFESA" else if (campaign != null) "LIBERTAÇÃO" else galaxyFaction(planet)}",
                    color = accent, fontSize = 10.sp)
            }
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(48.dp)
                .semantics { contentDescription = if (expanded) "Recolher ficha" else "Expandir ficha" }
                .background(Color(0xE629303A), androidx.compose.foundation.shape.CircleShape)) {
                Text(if (expanded) "−" else "+", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)
                .semantics { contentDescription = "Fechar ficha do planeta" }
                .background(Color(0xE629303A), androidx.compose.foundation.shape.CircleShape)) {
                Text("×", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
        }
        if (expanded) {
            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (percent != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (defense) "Progresso da defesa" else "Libertação", color = HD.TextDim, fontSize = 11.sp)
                    Text(mapPercent(percent), color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                ProgressBar(percent, accent)
                if (defense) OrderRepository.defenseEnemyProgress(planet.event)?.let { invasion ->
                    Text("Invasão: ${mapPercent(invasion)} • prazo ${OrderRepository.remaining(planet.event?.endTime) ?: "—"}", color = HD.Gold, fontSize = 11.sp)
                    ProgressBar(invasion, HD.Red)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SiteImage("helldivers_active", "Helldivers", Modifier.size(17.dp))
                        Text(mapPlayerCount(planet.statistics.playerCount), color = HD.Yellow, fontWeight = FontWeight.Bold)
                    }
                    Text("HELLDIVERS", color = HD.TextDim, fontSize = 9.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(rate?.let { "%+.2f%%/h".format(Locale("pt", "BR"), it) } ?: "—", color = HD.DefenseBlue, fontWeight = FontWeight.Bold)
                    Text("RITMO LÍQUIDO", color = HD.TextDim, fontSize = 9.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(eta ?: "—", color = HD.Text, fontWeight = FontWeight.Bold)
                    Text("PREVISÃO", color = HD.TextDim, fontSize = 9.sp)
                }
            }
            if (campaign != null && eta == null) Text(if (!fresh) "Previsão suspensa: aguardando dados atualizados." else if (rate == null) "Aguardando amostras para calcular o ritmo." else "Sem previsão de vitória no ritmo atual.", color = HD.TextMuted, fontSize = 10.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (count > 0) "$count regiões disponíveis" else "${galaxyFaction(planet)}", color = HD.TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onDossier, enabled = data != null) { Text("DOSSIÊ ↗", fontSize = 11.sp) }
            }
            }
        }
    }
}
