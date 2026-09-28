package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
            position = it.mapPosition, waypoints = it.waypoints) ?: it } +
            campaigns.values.map { it.planet }).distinctBy { it.index }
    }
    var query by rememberSaveable { mutableStateOf("") }
    var activeOnly by rememberSaveable { mutableStateOf(false) }
    var routes by rememberSaveable { mutableStateOf(true) }
    var faction by rememberSaveable { mutableStateOf("Todas") }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val filtered = remember(planets, query, activeOnly, faction, campaigns) {
        planets.filter {
            (!activeOnly || it.index in campaigns) &&
                (faction == "Todas" || galaxyFaction(it) == faction) &&
                searchKey("${it.nameText} ${it.sector}").contains(searchKey(query.trim()))
        }
    }
    val selected = planets.firstOrNull { it.index == selectedId }
    val listPlanets = if (query.isNotBlank() || activeOnly || faction != "Todas") filtered
        else filtered.filter { it.index in campaigns }
    LazyColumn(Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, contentPadding.calculateTopPadding() + 12.dp, 14.dp, contentPadding.calculateBottomPadding() + 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionLabel("Super Terra // Reconhecimento orbital")
            Text("MAPA GALÁCTICO", color = HD.Text, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                val time = state.updatedAtMillis?.let { SimpleDateFormat("HH:mm:ss", Locale("pt", "BR")).format(Date(it)) }
                Text(time?.let { "Leitura às $it" } ?: "Aguardando telemetria", color = HD.TextDim, fontSize = 11.sp)
                TextButton(onClick = vm::refresh, enabled = !state.loading) { Text("ATUALIZAR") }
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = HD.Yellow)
            state.error?.let { Text(it, color = HD.Gold, fontSize = 12.sp) }
            if (home is HomeState.Error) Text("Frentes ativas sem atualização. ${home.message}", color = HD.Gold, fontSize = 12.sp)
        }
        item {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Buscar planeta ou setor") },
                trailingIcon = { if (query.isNotBlank()) TextButton(onClick = { query = "" }) { Text("Limpar") } })
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(activeOnly, { activeOnly = !activeOnly }, label = { Text("Frentes ativas") }) }
                item { FilterChip(routes, { routes = !routes }, label = { Text("Rotas") }) }
                items(listOf("Todas", "Super Terra", "Terminídeos", "Autômatos", "Iluminados")) { label ->
                    FilterChip(faction == label, { faction = label }, label = { Text(label) })
                }
            }
        }
        item {
            GalaxyCanvas(filtered, planets, routes, selectedId, campaigns.keys, onSelect = { selectedId = it })
            Text("Arraste para mover • Use dois dedos para ampliar • Toque num planeta", color = HD.TextDim, fontSize = 11.sp)
            Text("${filtered.size} planetas no filtro. A lista abaixo também permite selecionar as frentes.", color = HD.TextMuted, fontSize = 11.sp)
        }
        if (selected != null) item {
            HdCard(accent = factionColor(selected.currentOwner)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(selected.nameText, color = HD.Text, fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    TextButton(onClick = { selectedId = null }) { Text("FECHAR") }
                }
                Text("${selected.sector} • ${galaxyFaction(selected)}", color = HD.TextDim)
                Text("Helldivers: ${java.text.NumberFormat.getInstance(Locale("pt", "BR")).format(selected.statistics.playerCount)}", color = HD.Text)
                campaigns[selected.index]?.let { campaign ->
                    val percent = OrderRepository.campaignPercent(campaign)
                    Text("${if (selected.event != null) "Defendendo" else "Libertação"}: ${"%.2f".format(Locale("pt", "BR"), percent)}%", color = HD.Yellow)
                    ProgressBar(percent, HD.Yellow)
                }
                data?.planetCatalog?.get(selected.index)?.let { Text(PlanetVisuals.biomeLabel(it), color = HD.TextDim) }
            }
        }
        item { SectionLabel(if (query.isNotBlank() || faction != "Todas") "Planetas encontrados" else "Frentes em operação") }
        if (listPlanets.isEmpty()) item { Text("Nenhum planeta nesta lista. Pesquise um nome ou ajuste os filtros.", color = HD.TextDim) }
        items(listPlanets, key = { it.index }) { planet ->
            HdCard(modifier = Modifier.clickable { selectedId = planet.index }, accent = factionColor(planet.currentOwner)) {
                Text(planet.nameText, color = HD.Text, fontWeight = FontWeight.Bold)
                Text("${planet.sector} • ${galaxyFaction(planet)}", color = HD.TextDim, fontSize = 12.sp)
            }
        }
        item { TextButton(onClick = onOpenFullMap) { Text("MAPA COMPLETO DO SITE ↗") } }
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
private fun GalaxyCanvas(planets: List<Planet>, all: List<Planet>, routes: Boolean, selected: Long?, active: Set<Long>, onSelect: (Long) -> Unit) {
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val positioned = planets.filter { it.mapPosition?.let { pos -> pos.x.isFinite() && pos.y.isFinite() } == true }
    val extent = remember(all) { all.mapNotNull { it.mapPosition }.flatMap { listOf(abs(it.x), abs(it.y)) }
        .filter { it.isFinite() }.maxOrNull()?.coerceAtLeast(1.0)?.toFloat() ?: 1f }
    val colorById = positioned.associate { it.index to factionColor(it.currentOwner) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { zoom = (zoom / 1.4f).coerceAtLeast(1f) }) { Text("−") }
            TextButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Text("CENTRALIZAR") }
            TextButton(onClick = { zoom = (zoom * 1.4f).coerceAtMost(8f) }) { Text("+") }
        }
        if (positioned.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(220.dp).background(HD.BgDeep), contentAlignment = Alignment.Center) {
                Text("Sem coordenadas para os filtros atuais.", color = HD.TextDim)
            }
        } else Canvas(Modifier.fillMaxWidth().height(370.dp).clipToBounds().background(Color(0xFF050810))
            .pointerInput(Unit) {
                detectTransformGestures { centroid, move, scale, _ ->
                    val next = (zoom * scale).coerceIn(1f, 8f)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    pan = centroid - center - (centroid - center - pan) * (next / zoom) + move
                    val limit = size.width.coerceAtLeast(size.height) * next
                    pan = Offset(pan.x.coerceIn(-limit, limit), pan.y.coerceIn(-limit, limit))
                    zoom = next
                }
            }
            .pointerInput(positioned, extent) {
                detectTapGestures { tap ->
                    val scale = minOf(size.width, size.height) * 0.43f * zoom / extent
                    val center = Offset(size.width / 2f, size.height / 2f) + pan
                    positioned.minByOrNull { p ->
                        val pos = p.mapPosition!!
                        (center + Offset(pos.x.toFloat() * scale, -pos.y.toFloat() * scale) - tap).getDistance()
                    }?.let { p ->
                        val pos = p.mapPosition!!
                        if ((center + Offset(pos.x.toFloat() * scale, -pos.y.toFloat() * scale) - tap).getDistance() <= 24.dp.toPx()) onSelect(p.index)
                    }
                }
            }) {
            val scale = size.minDimension * 0.43f * zoom / extent
            val origin = center + pan
            fun point(p: Planet): Offset {
                val pos = p.mapPosition!!
                return origin + Offset(pos.x.toFloat() * scale, -pos.y.toFloat() * scale)
            }
            repeat(4) { i -> drawCircle(Color(0xFF1D283A), size.minDimension * (i + 1) / 9f * zoom, origin, style = Stroke(1f)) }
            drawLine(Color(0xFF1D283A), Offset(origin.x, 0f), Offset(origin.x, size.height))
            drawLine(Color(0xFF1D283A), Offset(0f, origin.y), Offset(size.width, origin.y))
            val byId = positioned.associateBy { it.index }
            if (routes) {
                val drawn = mutableSetOf<Pair<Long, Long>>()
                positioned.forEach { p -> p.waypoints.forEach { id ->
                    val edge = minOf(p.index, id) to maxOf(p.index, id)
                    if (drawn.add(edge)) byId[id]?.let { drawLine(Color(0xFF3A495B), point(p), point(it), 1.dp.toPx()) }
                } }
            }
            val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 10.sp.toPx()
                setShadowLayer(3f, 0f, 1f, android.graphics.Color.BLACK)
            }
            positioned.forEach { p ->
                val color = colorById.getValue(p.index)
                val radius = if (p.index in active) 5.dp.toPx() else 3.dp.toPx()
                if (p.index in active) drawCircle(color.copy(alpha = 0.18f), radius * 2.5f, point(p))
                drawCircle(color, radius, point(p))
                if (p.index == selected || (zoom >= 2f && p.index in active) || positioned.size <= 10) {
                    val at = point(p)
                    drawContext.canvas.nativeCanvas.drawText(p.nameText, at.x + radius + 4.dp.toPx(), at.y, labelPaint)
                }
                if (p.index == selected) drawCircle(Color.White, radius + 5.dp.toPx(), point(p), style = Stroke(2.dp.toPx()))
            }
        }
    }
}
