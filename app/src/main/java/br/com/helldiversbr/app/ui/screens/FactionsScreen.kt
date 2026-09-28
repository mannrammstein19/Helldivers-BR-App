package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.HelldiversApi
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
private data class FactionEntry(val name: String, val badge: String, val image: String, val description: String, val details: String, val path: String, val faction: String)

@Composable
fun FactionsScreen(contentPadding: PaddingValues, onOpenSite: (String) -> Unit) {
    val context = LocalContext.current
    var factions by remember { mutableStateOf<List<FactionEntry>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { runCatching {
            context.assets.open("factions.json").bufferedReader().use {
                Json.decodeFromString(ListSerializer(FactionEntry.serializer()), it.readText())
            }
        } }
        factions = result.getOrDefault(emptyList()); failed = result.isFailure
    }
    LazyColumn(Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp, contentPadding.calculateTopPadding() + 12.dp, 14.dp, contentPadding.calculateBottomPadding() + 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SectionLabel("Panorama de inteligência tática")
            Text("FACÇÕES DA GUERRA GALÁCTICA", color = HD.Text, fontSize = 27.sp, fontWeight = FontWeight.Black)
        }
        if (failed) item { Text("Não foi possível abrir o catálogo de facções.", color = HD.Red) }
        items(factions, key = { it.name }) { faction ->
            HdCard(accent = factionColor(faction.faction)) {
                SectionLabel(faction.badge, factionColor(faction.faction))
                Text(faction.name, color = HD.Text, fontSize = 22.sp, fontWeight = FontWeight.Black)
                AsyncImage("${HelldiversApi.SITE_BASE}/${faction.image}", faction.name,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentScale = ContentScale.Crop)
                Text(faction.description, color = HD.TextDim, fontSize = 14.sp, lineHeight = 21.sp)
                Text(faction.details, color = HD.TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                if (faction.path.isNotEmpty()) YellowButton("DOSSIÊ COMPLETO NO SITE ↗", { onOpenSite(faction.path) })
            }
        }
    }
}
