package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.window.Dialog
import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StratagemDetailScreen(name: String, contentPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    var entry by remember(name) { mutableStateOf<StratagemEntry?>(null) }
    var detail by remember(name) { mutableStateOf<StratagemDetail?>(null) }
    var loading by remember(name) { mutableStateOf(true) }
    var enlarged by rememberSaveable(name) { mutableStateOf(false) }
    LaunchedEffect(name) {
        withContext(Dispatchers.IO) {
            val catalog = StratagemCatalog.load(context)
            val index = catalog.indexOfFirst { it.name == name }
            entry = catalog.getOrNull(index)
            detail = runCatching { StratagemDetails.load(context).getOrNull(index) }.getOrNull()
        }
        loading = false
    }
    val item = entry
    val prefs = remember { context.getSharedPreferences("hdbr_arsenal", 0) }
    var favorite by remember(name) { mutableStateOf(name in prefs.getStringSet("favorites", emptySet()).orEmpty()) }
    val image = if (detail?.image?.endsWith(".svg", true) == true || detail?.image.isNullOrBlank()) item?.imageModel() else detail?.image
    LazyColumn(Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, contentPadding.calculateTopPadding() + 8.dp, 14.dp, contentPadding.calculateBottomPadding() + 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = onBack) { Text("← VOLTAR", color = HD.Yellow) } }
        if (loading) item { CircularProgressIndicator(color = HD.Yellow) }
        if (!loading && item == null) item { Text("Equipamento não encontrado.", color = HD.Text) }
        if (item != null) {
            item {
                HdCard(accent = HD.Yellow) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, fontSize = 24.sp, fontWeight = FontWeight.Black, color = HD.Text, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            val favorites = prefs.getStringSet("favorites", emptySet()).orEmpty().toMutableSet()
                            if (favorite) favorites.remove(name) else favorites.add(name)
                            prefs.edit().putStringSet("favorites", favorites).apply(); favorite = !favorite
                        }) { Text(if (favorite) "★" else "☆", color = HD.Yellow, fontSize = 25.sp) }
                    }
                    Text(item.category, color = HD.TextDim, fontSize = 12.sp)
                    StratagemArtwork(image, item.name, Modifier.fillMaxWidth().height(170.dp).clickable { enlarged = true })
                    Text("CÓDIGO DE ATIVAÇÃO", color = HD.TextMuted, fontSize = 10.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        item.code.forEach { direction ->
                            val key = when (direction) { '↑' -> "arrow_up"; '↓' -> "arrow_down"; '←' -> "arrow_left"; '→' -> "arrow_right"; else -> null }
                            if (key != null) SiteImage(key, direction.toString(), Modifier.size(24.dp))
                        }
                    }
                    if (item.code.none { it in "↑↓←→" }) Text("Código ainda não informado no catálogo.", color = HD.TextDim, fontSize = 12.sp)
                    Text("Recarga: ${item.cooldown}  •  Nível: ${item.level}", color = HD.TextDim, fontSize = 13.sp)
                    Text("Aquisição: ${item.source}  •  ${item.cost}", color = HD.Text, fontSize = 13.sp)
                }
            }
            detail?.paragraphs?.forEachIndexed { index, paragraph ->
                item(key = "description-$index") { Text(paragraph, color = HD.TextDim, fontSize = 14.sp, lineHeight = 21.sp) }
            }
            detail?.sections?.forEachIndexed { index, section ->
                item(key = "section-$index") { NativeStatSection(section, index, name) }
            }
            if (detail?.available != true) item {
                HdCard { Text("Este estratagema de missão ainda não tem ficha detalhada no catálogo do site. Os dados disponíveis estão acima.", color = HD.TextDim) }
            }
        }
    }
    if (enlarged) Dialog(onDismissRequest = { enlarged = false }) {
        Surface(color = HD.BgDeep) {
            Column(Modifier.padding(12.dp)) {
                TextButton(onClick = { enlarged = false }) { Text("FECHAR", color = HD.Yellow) }
                StratagemArtwork(image, name, Modifier.fillMaxWidth().height(300.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NativeStatSection(section: StratagemSection, index: Int, name: String) {
    var open by rememberSaveable(name, index) { mutableStateOf(index < 2) }
    HdCard {
        TextButton(onClick = { open = !open }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            Text(section.title, modifier = Modifier.weight(1f), color = HD.Yellow, fontWeight = FontWeight.Bold)
            Text(if (open) "−" else "+", fontSize = 22.sp, color = HD.Yellow)
        }
        if (open) section.rows.forEach { cells ->
            val wide = cells.size > 2
            Row(if (wide) Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()) else Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                cells.forEach { cell ->
                    Column(if (wide) Modifier.width(150.dp) else Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (cell.images.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            cell.images.forEach { image -> AsyncImage(image.url, image.label, Modifier.size(22.dp), contentScale = ContentScale.Fit) }
                        }
                        if (cell.text.isNotBlank()) Text(cell.text, color = if (cells.size == 1) HD.Yellow else HD.TextDim,
                            fontSize = 12.sp, fontWeight = if (cells.size == 1) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            HorizontalDivider(color = HD.Border.copy(alpha = .5f), modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
