package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.theme.HD
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArsenalScreen(onOpenEntry: (StratagemEntry) -> Unit, contentPadding: PaddingValues, themeMode: br.com.helldiversbr.app.ui.theme.HdThemeMode) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<StratagemEntry>?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { runCatching { StratagemCatalog.load(context) } }
        entries = result.getOrNull()
        failed = result.isFailure
    }
    val catalog = entries.orEmpty()
    var query by rememberSaveable { mutableStateOf("") }
    var openCategories by rememberSaveable { mutableStateOf(listOf<String>()) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("hdbr_arsenal", 0) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())!!.toSet()) }
    val filtered = remember(catalog, query, favoritesOnly, favorites) {
        val key = searchKey(query.trim())
        catalog.filter {
            (!favoritesOnly || it.name in favorites) &&
                searchKey("${it.name} ${it.category} ${it.source}").contains(key)
        }
    }
    Box(Modifier.fillMaxSize()) {
    if (themeMode == br.com.helldiversbr.app.ui.theme.HdThemeMode.DEFAULT) {
        SiteImage("arsenal_background", null, Modifier.matchParentSize(), scale = ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .62f)))
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, contentPadding.calculateTopPadding() + 12.dp, 14.dp, contentPadding.calculateBottomPadding() + 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HdCard(accent = HD.Yellow) {
                SectionLabel("Super Terra // Registros táticos")
                Text("ARSENAL DE ESTRATAGEMAS", color = HD.Text, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("${catalog.size} registros • códigos, recargas e aquisição", color = HD.TextDim, fontSize = 12.sp)
                Text("Fichas, códigos e estatísticas disponíveis no app.", color = HD.TextMuted, fontSize = 11.sp)
            }
        }
        item {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Buscar equipamento, categoria ou fonte") },
                trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { query = "" }) { Text("Limpar") } })
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, label = { Text("★ Favoritos") })
                Text("${filtered.size} encontrados", color = HD.TextDim, fontSize = 11.sp)
            }
        }
        if (entries == null && !failed) item { CircularProgressIndicator(color = HD.Yellow) }
        if (failed) item { Text("Não foi possível abrir o catálogo local.", color = HD.Red) }
        if (entries != null && filtered.isEmpty()) item {
            HdCard { Text("Nenhum equipamento encontrado. Experimente outro nome ou remova os filtros.", color = HD.TextDim) }
        }
        filtered.groupBy { it.permission }.forEach { (permission, group) ->
            item(key = "permission-$permission") {
                Text(permission, color = HD.Yellow, fontSize = 23.sp, fontWeight = FontWeight.Black)
            }
            group.groupBy { it.category }.forEach { (category, rows) ->
                val groupKey = "$permission/$category"
                val opened = groupKey in openCategories || query.isNotBlank() || favoritesOnly
                item(key = "category-$groupKey") {
                    HdCard(modifier = Modifier.clickable {
                        openCategories = if (groupKey in openCategories) openCategories - groupKey else openCategories + groupKey
                    }, accent = permissionAccent(permission)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(category, color = permissionAccent(permission), fontSize = 18.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${rows.size} DISPONÍVEIS", color = HD.TextDim, fontSize = 10.sp)
                            Text(if (opened) "  −" else "  +", color = HD.Text, fontSize = 24.sp)
                        }
                    }
                }
                if (opened) items(rows, key = { it.name }) { entry ->
                    StratagemCard(entry, entry.name in favorites, onFavorite = {
                        favorites = if (entry.name in favorites) favorites - entry.name else favorites + entry.name
                        prefs.edit().putStringSet("favorites", favorites).apply()
                    }, onOpen = { onOpenEntry(entry) })
                }
            }
        }
    }
    }
}

private fun permissionAccent(permission: String) = when {
    "Ofensiva" in permission -> HD.Red
    "Suprimento" in permission -> HD.SignalBlue
    "Defensiva" in permission -> HD.Green
    else -> HD.Yellow
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StratagemCard(entry: StratagemEntry, favorite: Boolean, onFavorite: () -> Unit, onOpen: () -> Unit) {
    val accent = when {
        "Ofensiva" in entry.permission -> HD.Red
        "Suprimento" in entry.permission -> HD.SignalBlue
        "Defensiva" in entry.permission -> HD.Green
        else -> HD.Yellow
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = HD.Surface), border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StratagemArtwork(entry.imageModel(), entry.name, Modifier.size(34.dp))
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(entry.name, color = HD.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onFavorite, modifier = Modifier.size(48.dp)) { Text(if (favorite) "★" else "☆", fontSize = 23.sp) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                entry.code.forEach { direction ->
                    val key = when (direction) {
                        '↑' -> "arrow_up"; '↓' -> "arrow_down"
                        '←' -> "arrow_left"; '→' -> "arrow_right"
                        else -> null
                    }
                    if (key != null) SiteImage(key, direction.toString(), Modifier.size(20.dp))
                }
            }
            if (entry.code.none { it in "↑↓←→" }) Text(entry.code.ifBlank { "Sem código informado" }, color = HD.TextDim)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if ("Medalhas" in entry.cost) SiteImage("arsenal_medals", "Medalhas", Modifier.size(20.dp))
                else if (entry.cost.any { it.isDigit() }) SiteImage("requisition", "Requisições", Modifier.size(20.dp))
                Text(entry.cost, color = HD.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Text("Recarga: ${entry.cooldown}   •   Nível: ${entry.level}", color = HD.TextDim, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Aquisição: ${entry.source}", color = HD.TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
                if (entry.name.isNotBlank()) {
                    TextButton(onClick = onOpen, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("ABRIR FICHA", color = HD.Yellow, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
