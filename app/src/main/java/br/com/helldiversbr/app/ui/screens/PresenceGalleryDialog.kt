package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.helldiversbr.app.R
import br.com.helldiversbr.app.data.*
import coil.compose.AsyncImage
import kotlinx.serialization.json.*

private val IntelBackground = Color(0xFF090F14)
private val IntelYellow = Color(0xFFFFE600)
private val IntelRed = Color(0xFFDA2525)
private val IntelHeading = FontFamily(Font(R.font.oswald_bold, FontWeight.Bold))

@Composable
private fun IntelFrame(artwork: String?, label: String, modifier: Modifier = Modifier) {
    Box(modifier.background(Color(0xFF202020)).border(2.dp, Color(0xFFA87979))) {
        if (artwork != null) AsyncImage(artwork, label, Modifier.fillMaxSize().padding(10.dp), contentScale = ContentScale.Fit)
        else Text("IMAGEM NÃO DISPONÍVEL", Modifier.align(Alignment.Center).padding(12.dp),
            color = Color(0xFFC8C8C8), fontSize = 10.sp)
        Canvas(Modifier.matchParentSize().padding(5.dp)) {
            drawRect(IntelRed, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 3.dp.toPx()))))
            val arm = 12.dp.toPx()
            for ((x, y, dx, dy) in listOf(floatArrayOf(0f,0f,1f,1f), floatArrayOf(size.width,0f,-1f,1f),
                floatArrayOf(0f,size.height,1f,-1f), floatArrayOf(size.width,size.height,-1f,-1f))) {
                drawLine(IntelRed, Offset(x,y), Offset(x+dx*arm,y), 2.dp.toPx())
                drawLine(IntelRed, Offset(x,y), Offset(x,y+dy*arm), 2.dp.toPx())
            }
        }
    }
}

@Composable
private fun IntelStripe() {
    Canvas(Modifier.fillMaxWidth().height(6.dp)) {
        val step = 10.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            drawLine(IntelRed, Offset(x,size.height), Offset(x+size.height,0f), 3.dp.toPx()); x += step
        }
    }
}

private fun intelCopy(text: String) = androidx.compose.ui.text.buildAnnotatedString {
    append(text)
    for (term in listOf("incendiário", "chamas", "ciborgues", "cepa predadora", "cepa rompedora", "cepa de esporos",
        "Iluminadas", "apoio aliado", "última leitura salva", "não é uma contagem ao vivo")) {
        val start = text.indexOf(term, ignoreCase = true)
        if (start >= 0) addStyle(androidx.compose.ui.text.SpanStyle(color = IntelYellow), start, start + term.length)
    }
}

@Composable
fun PresenceGalleryDialog(presence: PlanetPresence, planet: Planet, stale: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val entry = remember(presence.key) {
        runCatching { context.assets.open("presence-gallery.json").bufferedReader().use {
            CentralApi.json.parseToJsonElement(it.readText()).jsonObject[presence.key]?.jsonObject
        } }.getOrNull()
    }
    val names = (entry?.get("names") as? JsonArray).orEmpty().map { it.jsonPrimitive.content }
    val missing = (entry?.get("missing") as? JsonArray).orEmpty().map { it.jsonPrimitive.content }
    var enlarged by remember(presence.key, planet.index) { mutableStateOf<String?>(null) }
    val columns = if (config.screenWidthDp >= 600) 3 else 2
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 780.dp).fillMaxWidth(.94f).heightIn(max = (config.screenHeightDp * .90f).dp),
            shape = RoundedCornerShape(18.dp), color = IntelBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF477E99))) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ARQUIVO DE INTELIGÊNCIA", color = Color(0xFFB8DCEA), fontSize = 10.sp, letterSpacing = 1.sp)
                        Text(presence.name.uppercase(), color = IntelYellow, fontFamily = IntelHeading, fontSize = 21.sp)
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Text("FECHAR", color = IntelYellow, fontSize = 11.sp)
                    }
                }
                IntelStripe()
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text(planet.nameText.uppercase() + if (stale) " · ÚLTIMA LEITURA SALVA" else "",
                            color = Color(0xFFBACBD2), fontSize = 11.sp)
                        if (stale) Text("Presença atual sem confirmação.", color = IntelYellow, fontSize = 11.sp)
                    }
                    item {
                        Column(Modifier.fillMaxWidth().background(Color(0xFF202020)).border(1.dp, Color(0xFF8B8B88)).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(intelCopy((entry?.get("note") as? JsonPrimitive)?.content.orEmpty()), color = Color(0xFFC8C8C8), fontSize = 12.sp, lineHeight = 17.sp)
                            Text(intelCopy("Ficha de reconhecimento. Missão e dificuldade podem mudar a composição; não é uma contagem ao vivo."),
                                color = Color(0xFFBACBD2), fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                    if (names.isNotEmpty()) item { Text("TOQUE NA IMAGEM PARA AMPLIAR", color = IntelYellow, fontSize = 10.sp) }
                    names.chunked(columns).forEach { row -> item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { original ->
                                val label = PresenceUnitNames.label(original)
                                val artwork = MapAssets.file("presence-unit:$original")
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    IntelFrame(artwork, label, Modifier.fillMaxWidth().height(130.dp).let {
                                        if (artwork != null) it.clickable(onClickLabel = "Ampliar $label") { enlarged = original } else it
                                    })
                                    Text(label, color = Color.White, fontFamily = IntelHeading, fontSize = 14.sp, lineHeight = 18.sp)
                                }
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    } }
                    if (missing.isNotEmpty()) item {
                        Text("Imagens ainda não fornecidas: ${missing.joinToString { PresenceUnitNames.label(it) }}.",
                            color = IntelYellow, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                    if (presence.key == "seaf") item { Text("APOIO ALIADO // SUPER TERRA", color = IntelYellow, fontFamily = IntelHeading, fontSize = 18.sp) }
                }
            }
        }
    }
    enlarged?.let { original ->
        val label = PresenceUnitNames.label(original)
        Dialog(onDismissRequest = { enlarged = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.widthIn(max = 720.dp).fillMaxWidth(.94f), shape = RoundedCornerShape(18.dp),
                color = IntelBackground, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF477E99))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(label.uppercase(), color = IntelYellow, fontFamily = IntelHeading, fontSize = 22.sp, lineHeight = 27.sp)
                    Text("Referência original: $original", color = Color(0xFFBACBD2), fontSize = 11.sp)
                    IntelFrame(MapAssets.file("presence-unit:$original"), label,
                        Modifier.fillMaxWidth().height((config.screenHeightDp * .48f).dp.coerceAtMost(420.dp)))
                    TextButton(onClick = { enlarged = null }, modifier = Modifier.align(Alignment.End)) {
                        Text("VOLTAR À GALERIA", color = IntelYellow)
                    }
                }
            }
        }
    }
}
