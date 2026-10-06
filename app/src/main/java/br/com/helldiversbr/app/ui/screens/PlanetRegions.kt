package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import br.com.helldiversbr.app.R
import br.com.helldiversbr.app.data.RegionTelemetry
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.Planet
import br.com.helldiversbr.app.data.PlanetRegion
import br.com.helldiversbr.app.data.localizedText
import br.com.helldiversbr.app.ui.theme.HD
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.text.NumberFormat
import java.util.Locale

enum class RegionState { AVAILABLE, BLOCKED, RECOVERED, UNKNOWN }

data class RegionPresentation(
    val status: String, val percent: Double?, val players: Long?, val note: String,
    val state: RegionState = RegionState.UNKNOWN,
)

/** Ownership is authoritative: captured regions may already have reset to full health. */
fun regionPresentation(region: PlanetRegion): RegionPresentation {
    val owner = RegionTelemetry.ownerId(region.owner)
    val state = when {
        region.telemetryStale && !(owner == 1 && region.isAvailable == false) -> RegionState.UNKNOWN
        region.isAvailable == true && owner != null -> RegionState.AVAILABLE
        owner == 1 && region.isAvailable == false -> RegionState.RECOVERED
        owner in 2..4 && region.isAvailable == false -> RegionState.BLOCKED
        else -> RegionState.UNKNOWN
    }
    val hp = region.health
    val valid = hp != null && region.maxHealth > 0 && hp in 0..region.maxHealth
    val percent = when {
        state == RegionState.RECOVERED -> 100.0
        state == RegionState.AVAILABLE && valid && owner in 2..4 ->
            (1.0 - hp!!.toDouble() / region.maxHealth) * 100.0
        else -> null
    }
    return RegionPresentation(when (state) {
        RegionState.AVAILABLE -> "Disponível para operações"
        RegionState.RECOVERED -> "Limpo / Recuperado"
        RegionState.BLOCKED -> "Bloqueado para operações"
        RegionState.UNKNOWN -> "Aguardando confirmação"
    }, percent, if (state == RegionState.AVAILABLE) region.players?.takeIf { it >= 0 } else null,
        "", state)
}

fun regionalSummary(planet: Planet): String {
    val states = planet.regions.groupingBy { regionPresentation(it).state }.eachCount()
    return listOf(
        RegionState.AVAILABLE to "disponíveis", RegionState.RECOVERED to "recuperadas",
        RegionState.BLOCKED to "bloqueadas", RegionState.UNKNOWN to "sem confirmação",
    ).mapNotNull { (state, label) -> states[state]?.takeIf { it > 0 }?.let { "$it $label" } }
        .joinToString(" • ")
}

@Composable
fun PlanetRegions(planet: Planet) {
    val regions = planet.regions
    if (regions.isEmpty()) return
    val context = LocalContext.current
    val types = remember {
        context.assets.open("region-types.json").bufferedReader().use {
            Json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), it.readText())
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel("Regiões do planeta · ${regions.size}")
        regions.forEachIndexed { index, region ->
            val info = regionPresentation(region)
            val type = types[region.hash?.toString()]
            val identity = if (type == "factory") "Megafábrica" else when (region.size) {
                "Settlement", "0" -> "Assentamento"
                "Town", "1" -> "Vila"
                "City", "2" -> "Cidade"
                "MegaCity", "3" -> "Megacidade"
                else -> "Metrópole"
            }
            val accent = when (info.state) {
                RegionState.AVAILABLE -> mapColor(mapFaction(br.com.helldiversbr.app.data.OrderRepository.factionFromRaceId(RegionTelemetry.ownerId(region.owner))))
                RegionState.BLOCKED -> mapColor(mapFaction(br.com.helldiversbr.app.data.OrderRepository.factionFromRaceId(RegionTelemetry.ownerId(region.owner))))
                RegionState.RECOVERED -> HD.DefenseBlue
                RegionState.UNKNOWN -> HD.TextMuted
            }
            val artwork = when (info.state) {
                RegionState.AVAILABLE -> R.drawable.region_operacao
                RegionState.BLOCKED -> R.drawable.region_bloqueado
                RegionState.RECOVERED -> R.drawable.region_recuperado
                RegionState.UNKNOWN -> R.drawable.region_sem_confirmacao
            }
            Surface(color = HD.Surface, shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = .55f))) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))) {
                    artwork?.let {
                        Image(painterResource(it), contentDescription = null,
                            contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                    }
                    Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = .82f), Color.Black.copy(alpha = .55f)))))
                    Column(Modifier.fillMaxWidth().heightIn(min = 94.dp).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (type != null) SiteImage(type, identity, Modifier.size(28.dp), tint=accent)
                            Column(Modifier.weight(1f)) {
                                Text(localizedText(region.name).ifBlank { "Região ${index + 1}" },
                                    color = HD.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)
                                Text(identity, color = HD.TextDim, fontSize = 11.sp)
                            }
                            Icon(when (info.state) {
                                RegionState.AVAILABLE -> Icons.Filled.PlayArrow
                                RegionState.BLOCKED -> Icons.Filled.Lock
                                RegionState.RECOVERED -> Icons.Filled.CheckCircle
                                RegionState.UNKNOWN -> Icons.Filled.HelpOutline
                            }, null, tint = accent, modifier = Modifier.size(20.dp))
                        }
                        Text(info.status, color = accent, fontWeight = FontWeight.Bold,
                            fontSize = 12.sp, lineHeight = 16.sp)
                        if (info.state == RegionState.AVAILABLE) {
                            info.percent?.let {
                                Text("Libertação: ${"%.2f".format(Locale("pt", "BR"), it)}%",
                                    color = HD.Text, fontSize = 12.sp)
                                ProgressBar(it, HD.DefenseBlue)
                            }
                            info.players?.let {
                                Text("${NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(it)} Helldivers na região",
                                    color = HD.TextDim, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        val savedTime = regions.filter { it.telemetryStale }.map { it.telemetryReadAtMillis }.filter { it > 0 }.minOrNull()
        if (regions.any { it.telemetryStale && RegionTelemetry.ownerId(it.owner) != null }) {
            val date = savedTime?.let {
                java.text.SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(java.util.Date(it))
            } ?: "sem data registrada"
            Text("Regiões: última confirmação salva · $date", color = HD.TextMuted, fontSize = 10.sp)
        }
        Text("Progresso regional independente do progresso do planeta. Dados da última leitura da API.", color = HD.TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
    }
}
