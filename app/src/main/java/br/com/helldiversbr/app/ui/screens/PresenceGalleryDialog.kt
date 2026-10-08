package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage
import kotlinx.serialization.json.*

@Composable
fun PresenceGalleryDialog(presence: PlanetPresence, planet: Planet, stale: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val entry = remember(presence.key) {
        context.assets.open("presence-gallery.json").bufferedReader().use {
            CentralApi.json.parseToJsonElement(it.readText()).jsonObject[presence.key]?.jsonObject
        }
    }
    val names = (entry?.get("names") as? JsonArray).orEmpty().map { it.jsonPrimitive.content }
    val missing = (entry?.get("missing") as? JsonArray).orEmpty().map { it.jsonPrimitive.content }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(presence.name) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("FECHAR") } },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .60f).dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text(planet.nameText + if (stale) " · Última leitura salva; presença atual não confirmada" else "", color = HD.TextDim) }
                item { Text((entry?.get("note") as? JsonPrimitive)?.content.orEmpty(), fontSize = 12.sp) }
                item { Text("Ficha de reconhecimento. Missão e dificuldade podem mudar a composição; não é uma contagem ao vivo.", fontSize = 11.sp) }
                names.chunked(2).forEach { row -> item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { name -> Column(Modifier.weight(1f)) {
                            val artwork = MapAssets.file("presence-unit:$name")
                            if (artwork != null) AsyncImage(artwork, name, Modifier.fillMaxWidth().height(112.dp), contentScale = ContentScale.Fit)
                            Text(name + if (artwork == null) " · imagem indisponível" else "", fontSize = 10.sp)
                        } }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                } }
                if (missing.isNotEmpty()) item { Text("Ainda sem imagem: ${missing.joinToString()}", color = HD.Gold, fontSize = 11.sp) }
            }
        })
}
