package br.com.helldiversbr.app.ui.screens

import br.com.helldiversbr.app.ui.presentation.visualCampaignPercent

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
import androidx.compose.ui.draw.clip
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
    val context = androidx.compose.ui.platform.LocalContext.current
    var options by remember { mutableStateOf(MapDisplayOptions.load(context)) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    var frontsOpen by rememberSaveable { mutableStateOf(false) }
    var frontId by rememberSaveable { mutableStateOf<Long?>(null) }
    val state by vm.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(vm, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { vm.refresh(); delay(60_000) }
        }
    }
    val data = when (home) { is HomeState.Ready -> home.data; is HomeState.Error -> home.last; else -> null }
    val campaigns = data?.campaigns.orEmpty().associateBy { it.planet.index }
    val dssStation = state.dss.station
    val dssPlanetName = dssStation?.let { station ->
        localizedText(station.planet.name)
            .ifBlank { data?.planetCatalog?.get(station.planet.index)?.displayName.orEmpty() }
            .ifBlank { state.planetCatalog[station.planet.index]?.displayName.orEmpty() }
            .ifBlank { data?.campaigns?.firstOrNull { it.planet.index == station.planet.index }?.planet?.nameText.orEmpty() }
    }.orEmpty()
    val preferCampaigns = data != null && data.campaignTelemetrySource != "cache" &&
        "campanhas" !in data.staleSources &&
        (state.telemetrySource == "cache" || data.campaignReadAtMillis >= (state.updatedAtMillis ?: 0L))
    val planets = remember(state.planets, campaigns, preferCampaigns) {
        mergeMapPlanets(state.planets, campaigns.values.toList(), preferCampaigns)
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
    var dssOpen by rememberSaveable { mutableStateOf(false) }
    var dssExpanded by rememberSaveable { mutableStateOf(false) }
    val contextIds = remember(planets, campaigns, state.dssHost, selectedId) {
        val core = planets.filter { it.index in campaigns || it.event != null }
            .map { it.index }.toSet()
        val nearby = planets.filter { it.index in core }.flatMap { it.waypoints + it.attacking }.toSet()
        core + nearby + planets.filter { p -> p.waypoints.any { it in core } || p.attacking.any { it in core } ||
            mapEarth(p) || mapSpecial(p) != null || p.index == state.dssHost || p.index == selectedId }.map { it.index }
    }
    val filtered = remember(planets, query, activeOnly, allPlanets, faction, campaigns, contextIds) {
        planets.filter {
            (!activeOnly || it.index in campaigns || it.event != null) &&
                (allPlanets || activeOnly || query.isNotBlank() || faction != "Todas" || it.index in contextIds) &&
                (faction == "Todas" || galaxyFaction(it) == faction) &&
                searchKey("${it.nameText} ${it.sector}").contains(searchKey(query.trim()))
        }
    }
    val matchedIds = remember(planets, query) { if(query.isBlank()) null else planets.filter { searchKey("${it.nameText} ${it.sector}").contains(searchKey(query.trim())) }.map { it.index }.toSet() }
    val mapVisible = if(matchedIds==null) filtered else planets.filter { p -> p.index in matchedIds || p.waypoints.any { it in matchedIds } || planets.any { it.index in matchedIds && p.index in it.waypoints } }
    val selected = filtered.firstOrNull { it.index == selectedId }
    LaunchedEffect(filtered, selectedId) {
        if (selectedId != null && selected == null) selectedId = null
    }
    val dossierPlanet = planets.firstOrNull { it.index == dossierId }
    if (dossierPlanet != null && data != null) PlanetDossierDialog(data, campaigns[dossierPlanet.index]?.copy(planet = dossierPlanet) ?: Campaign(planet = dossierPlanet)) { dossierId = null }
    val dssSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (dssOpen) ModalBottomSheet(
        onDismissRequest = { dssOpen = false },
        sheetState = dssSheetState,
        containerColor = HD.Surface,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).verticalScroll(rememberScrollState())) {
            DssPanel(
                reading = state.dss,
                planetCatalog = data?.planetCatalog ?: state.planetCatalog,
                campaigns = data?.campaigns.orEmpty(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
    if (filtersOpen) ModalBottomSheet(onDismissRequest = { filtersOpen = false }, containerColor = HD.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("VISIBILIDADE", color = HD.Yellow, fontSize = 12.sp)
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
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = {
                    val match=filtered.firstOrNull { searchKey(it.nameText)==searchKey(query.trim()) } ?: filtered.firstOrNull()
                    if(match!=null) { selectedId=match.index;listOpen=false }
                }),
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
                            AsyncImage(model = PlanetVisuals.planetImage(planet.index, planet.nameText, data?.planetCatalog?.get(planet.index)),
                                contentDescription = planet.nameText, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(28.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(7.dp)))
                            Column(Modifier.weight(1f)) {
                                Text(planetTitle(planet.nameText), color = mapNameColor(planet.currentOwner), fontWeight = FontWeight.Bold)
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
    if(settingsOpen) ModalBottomSheet(onDismissRequest={settingsOpen=false},containerColor=HD.Surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("OPÇÕES DO MAPA",color=HD.Yellow,fontWeight=FontWeight.Bold)
            Row {
                FilterChip(!options.clean,{options=MapDisplayOptions.preset(false);options.save(context)},label={Text("Completo")})
                Spacer(Modifier.width(8.dp))
                FilterChip(options.clean,{options=MapDisplayOptions.preset(true);options.save(context)},label={Text("Limpo")})
            }
            MapOptionRow("Todos os planetas",allPlanets) { allPlanets=it;activeOnly=false }
            fun update(next:MapDisplayOptions) { options=next;next.save(context) }
            MapOptionRow("Nomes",options.names) { update(options.copy(names=it)) }
            MapOptionRow("Helldivers",options.players) { update(options.copy(players=it)) }
            MapOptionRow("Porcentagens",options.progress) { update(options.copy(progress=it)) }
            MapOptionRow("Presenças",options.presences) { update(options.copy(presences=it)) }
            MapOptionRow("Modelos de naves",options.ships) { update(options.copy(ships=it)) }
            MapOptionRow("Faixas dos territórios",options.stripes) { update(options.copy(stripes=it)) }
            MapOptionRow("Movimento e pulsos",options.motion) { update(options.copy(motion=it)) }
            Text("Naves são ilustrações das presenças confirmadas, não contagem de unidades.",color=HD.TextMuted,fontSize=11.sp)
            Spacer(Modifier.height(20.dp))
        }
    }
    val readingWarning = state.error != null || state.telemetrySource == "cache" || home is HomeState.Error || data?.staleSources?.isNotEmpty() == true
    // The map owns the whole available destination, including the space behind overlays.
    BoxWithConstraints(Modifier.fillMaxSize().padding(contentPadding).background(Color(0xFF050810))) {
        val landscapeLayout = maxWidth > maxHeight
        val panelMaxHeight = if (landscapeLayout) (maxHeight - 128.dp).coerceAtLeast(80.dp) else (maxHeight * .56f).coerceAtMost(340.dp)
        GalaxyCanvas(mapVisible, planets, routes, sectors, territories, invasions, selectedId,
            campaigns.keys, state.dssHost, onSelect = { selectedId = it; frontsOpen = false }, modifier = Modifier.fillMaxSize(), options = options.copy(motion = options.motion && android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0), stale = state.telemetrySource == "cache", readAtMillis = state.updatedAtMillis ?: 0, dssLive = state.dss.isLive, routeFocus = matchedIds)
        Column(Modifier.align(Alignment.TopStart).fillMaxWidth(if (landscapeLayout) .48f else 1f).padding(8.dp),
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

                }
            }
            if(data!=null) {
                TextButton(onClick={frontsOpen=!frontsOpen}) { Text("${if(frontsOpen) "▾" else "▸"} FRENTES EM DESTAQUE",fontSize=11.sp) }
                if(frontsOpen) Column(Modifier.fillMaxWidth(if(landscapeLayout) 1f else .82f).heightIn(max=panelMaxHeight).verticalScroll(rememberScrollState())) {
                    data.campaigns.sortedByDescending { it.planet.statistics.playerCount }.take(3).forEach { c ->
                        if(frontId==c.planet.index) CampaignCard(data,c) { frontId=null }
                        else Surface(color=HD.Surface,shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth().padding(bottom=4.dp).clickable { frontId=c.planet.index }) {
                            Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
                                Text(planetTitle(c.planet.nameText),color=mapNameColor(c.planet.currentOwner),modifier=Modifier.weight(1f),fontWeight=FontWeight.Bold)
                                SiteImage("helldivers_active","Helldivers",Modifier.size(16.dp))
                                Text(mapPlayerCount(c.planet.statistics.playerCount),color=HD.Text,fontSize=11.sp)
                            }
                        }
                        if(frontId==c.planet.index) TextButton(onClick={dossierId=c.planet.index}) { Text("INTELIGÊNCIA ↗") }
                    }
                }
            }
            if (selected != null) {
                Surface(color = Color(0xF2090D12), shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, factionColor(selected.currentOwner)),
                    modifier = Modifier.fillMaxWidth().heightIn(max = panelMaxHeight)) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        FloatingPlanetCard(selected, campaigns[selected.index]?.copy(planet = selected), data, stale = home is HomeState.Error,
                            onClose = { selectedId = null }, onDossier = { dossierId = selected.index })
                    }
                }
            }
        }
        DssMapDock(
            expanded = dssExpanded,
            model = MapAssets.file(if(state.dss.isLive) "dss-operacional" else "dss-inoperante"),
            status = when {
                state.dss.isLive && dssPlanetName.isNotBlank() -> "Orbitando ${planetTitle(dssPlanetName)}"
                state.dss.availability == DssAvailability.LOCATION_UNKNOWN -> "Localização não informada"
                state.dss.availability == DssAvailability.ABSENT -> "Temporariamente indisponível"
                else -> "Telemetria indisponível"
            },
            stale = state.dss.stale,
            motion = options.motion && android.provider.Settings.Global.getFloat(context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0,
            onToggle = { dssExpanded = !dssExpanded },
            onDetails = { dssExpanded = false; dssOpen = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(top=if(landscapeLayout) 8.dp else 76.dp, end=8.dp)
                .widthIn(max=(maxWidth-16.dp).coerceAtMost(330.dp)),
        )
        if (infoOpen) MapReadingPanel(
            state = state, data = data, warning = readingWarning,
            onDismiss = { infoOpen = false },
            onPreferences = { infoOpen = false; settingsOpen = true },
            onRefresh = vm::refresh,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, end = 8.dp, bottom = 112.dp)
                .width((maxWidth * .78f).coerceAtMost(420.dp))
                .heightIn(max = (maxHeight - 132.dp).coerceAtLeast(80.dp).coerceAtMost(600.dp)),
        )
        androidx.activity.compose.BackHandler(enabled = infoOpen) { infoOpen = false }
        Column(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
            TextButton(onClick={settingsOpen=true},modifier=Modifier.background(HD.Surface,androidx.compose.foundation.shape.RoundedCornerShape(12.dp))) { Text("⚙",fontSize=22.sp) }
            TextButton(onClick={infoOpen=!infoOpen},modifier=Modifier.background(HD.Surface,androidx.compose.foundation.shape.RoundedCornerShape(12.dp))) { Text(if(readingWarning) "? •" else "?",fontSize=22.sp,color=if(readingWarning) HD.Gold else HD.Yellow) }
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
    val displayedPercent = campaign?.let { visualCampaignPercent(it) }
    val fresh = !stale && data != null && "campanhas" !in data.staleSources
    val rate = if (fresh && data != null && campaign != null) OrderRepository.campaignDisplayRate(data, campaign) else null
    val eta = if (percent != null) OrderRepository.etaFromRate(percent, rate) else null
    val accent = HD.DefenseBlue
    val landscape = PlanetVisuals.planetImage(planet.index, planet.nameText, data?.planetCatalog?.get(planet.index))
    Box {
        if (expanded) {
            Box(Modifier.fillMaxWidth().padding(top = 30.dp).height(145.dp)) {
                AsyncImage(model = landscape, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0xFF090D12), Color.Black.copy(alpha = .30f), Color(0xFF090D12)))))
            }
        }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 58.dp)) {
        Row(Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            SiteImage(mapFaction(planet.currentOwner), galaxyFaction(planet), Modifier.size(26.dp))
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(planetTitle(planet.nameText), color = mapNameColor(planet.currentOwner), fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 2)
                Text("${planet.sector} • ${if (defense) "DEFESA" else if (campaign != null) "LIBERTAÇÃO" else galaxyFaction(planet)}",
                    color = accent, fontSize = 10.sp)
            }
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(48.dp)
                .semantics { contentDescription = if (expanded) "Recolher ficha" else "Expandir ficha" }
) {
                Text(if (expanded) "−" else "+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color(0xD929303A), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 1.dp))
            }
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)
                .semantics { contentDescription = "Fechar ficha do planeta" }
) {
                Text("×", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color(0xD929303A), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 1.dp))
            }
        }
        }
        if (expanded) {
            Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if (percent != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (defense) "Progresso da defesa" else "Libertação", color = HD.TextDim, fontSize = 11.sp)
                    Text("%.4f%%".format(Locale("pt", "BR"), displayedPercent ?: percent), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                ProgressBar(displayedPercent ?: percent, accent)
                if (defense) OrderRepository.defenseEnemyProgress(planet.event, if(fresh) System.currentTimeMillis() else data?.campaignReadAtMillis ?: 0)?.let { invasion ->
                    Text("Invasão: ${mapPercent(invasion)} • prazo ${OrderRepository.remaining(planet.event?.endTime, if(fresh) System.currentTimeMillis() else data?.campaignReadAtMillis ?: 0)}", color = HD.Gold, fontSize = 11.sp)
                    ProgressBar(invasion, factionColor(planet.event?.faction.orEmpty()))
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
                    Text(if(rate == 0.0) "IMPASSE" else "RITMO / HORA", color = HD.TextDim, fontSize = 9.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(eta ?: "—", color = HD.Text, fontWeight = FontWeight.Bold)
                    Text("PREVISÃO", color = HD.TextDim, fontSize = 9.sp)
                }
            }
            if(rate!=null && data!=null && campaign!=null && OrderRepository.campaignRate(data,campaign)==null) Text("Média desde o início da defesa",color=HD.TextMuted,fontSize=10.sp)
            DssSupportIcons(data?.dss, planet.index)
            PresenceIcons(planet,labels=true,stale=!fresh)
            if (campaign != null && eta == null) Text(if (!fresh) "Previsão suspensa: aguardando dados atualizados." else if (rate == null) "Aguardando amostras para calcular o ritmo." else "Sem previsão de vitória no ritmo atual.", color = HD.TextMuted, fontSize = 10.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (planet.regions.isNotEmpty()) regionalSummary(planet) else galaxyFaction(planet), color = HD.TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onDossier, enabled = data != null) { Text("INTELIGÊNCIA ↗", fontSize = 11.sp) }
            }
            }
        }
    }
}
}

@Composable
private fun MapOptionRow(label:String,checked:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(label,color=HD.Text,modifier=Modifier.weight(1f));Switch(checked=checked,onCheckedChange=onChange)
    }
}
