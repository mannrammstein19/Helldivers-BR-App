package br.com.helldiversbr.app.ui.screens

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

@Composable
fun ArsenalScreen(onOpenCatalog: (String) -> Unit, contentPadding: PaddingValues) {
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
    var category by rememberSaveable { mutableStateOf("Todos") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("hdbr_arsenal", 0) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())!!.toSet()) }
    val loader = remember(context) { ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build() }
    DisposableEffect(loader) { onDispose { loader.shutdown() } }
    val filtered = remember(catalog, query, category, favoritesOnly, favorites) {
        val key = searchKey(query.trim())
        catalog.filter {
            (category == "Todos" || category == it.category) &&
                (!favoritesOnly || it.name in favorites) &&
                searchKey("${it.name} ${it.category} ${it.source}").contains(key)
        }
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
                Text("Os dados do catálogo ficam disponíveis sem conexão. Imagens e fichas completas usam o site.", color = HD.TextMuted, fontSize = 11.sp)
            }
        }
        item {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Buscar equipamento, categoria ou fonte") },
                trailingIcon = { if (query.isNotEmpty()) TextButton(onClick = { query = "" }) { Text("Limpar") } })
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Todos") + catalog.map { it.category }.distinct()) { label ->
                    FilterChip(selected = category == label, onClick = { category = label }, label = { Text(label) })
                }
            }
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
        items(filtered, key = { it.name }) { entry ->
            StratagemCard(entry, entry.name in favorites, loader, onFavorite = {
                favorites = if (entry.name in favorites) favorites - entry.name else favorites + entry.name
                prefs.edit().putStringSet("favorites", favorites).apply()
            }, onOpen = { onOpenCatalog(entry.path) })
        }
    }
}

@Composable
private fun StratagemCard(entry: StratagemEntry, favorite: Boolean, loader: ImageLoader, onFavorite: () -> Unit, onOpen: () -> Unit) {
    var expanded by rememberSaveable(entry.name) { mutableStateOf(false) }
    val accent = when {
        "Ofensiva" in entry.permission -> HD.Red
        "Suprimento" in entry.permission -> HD.SignalBlue
        "Defensiva" in entry.permission -> HD.Green
        else -> HD.Yellow
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = HD.Surface), border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage("${HelldiversApi.SITE_BASE}/${entry.icon}", null, imageLoader = loader,
                    modifier = Modifier.size(44.dp), contentScale = ContentScale.Fit)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp).clickable { expanded = !expanded }) {
                    Text(entry.category.uppercase(), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(entry.name, color = HD.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onFavorite) { Text(if (favorite) "★" else "☆", fontSize = 24.sp) }
            }
            Text(entry.code.ifBlank { "Sem código informado" }, color = HD.Yellow, fontSize = 21.sp, fontWeight = FontWeight.Black)
            Text("Recarga: ${entry.cooldown}   •   Nível: ${entry.level}", color = HD.TextDim, fontSize = 12.sp)
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "OCULTAR AQUISIÇÃO −" else "VER AQUISIÇÃO +") }
            if (expanded) {
                HorizontalDivider(color = HD.Border)
                Text("Custo: ${entry.cost}\nFonte: ${entry.source}", color = HD.TextDim, fontSize = 13.sp)
                if (entry.path.isNotBlank() && entry.path != "#") {
                    YellowButton("FICHA COMPLETA NO SITE ↗", onOpen, Modifier.fillMaxWidth())
                }
            }
        }
    }
}
