package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
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

data class RegionPresentation(val status: String, val percent: Double?, val players: Long?, val note: String)

/** Region ownership is independent of the planet's ownership and operation. */
fun regionPresentation(region: PlanetRegion): RegionPresentation {
    val owner = localizedText(region.owner).lowercase()
    val human = owner in listOf("1", "human", "humans")
    val hp = region.health
    val valid = hp != null && region.maxHealth > 0 && hp in 0..region.maxHealth
    val completed = !human && valid && hp == 0L
    val unavailable = !human && !completed && region.isAvailable == false
    val percent = when {
        human || completed -> 100.0
        unavailable -> null
        valid -> (1.0 - hp!!.toDouble() / region.maxHealth) * 100.0
        else -> null
    }
    val status = when {
        human -> "Sob controle da Super Terra"
        completed -> "Objetivo regional concluído"
        unavailable -> "Indisponível para operações"
        region.isAvailable == true -> "Em operação"
        else -> "Disponibilidade não informada"
    }
    return RegionPresentation(status, percent,
        if (!human && !completed && !unavailable) region.players?.takeIf { it >= 0 } else null,
        if (unavailable) "Indisponibilidade não confirma conquista." else "")
}

@Composable
fun PlanetRegions(planet: Planet) {
    val available = planet.regions.filter { it.isAvailable == true }
    if (available.isEmpty()) return
    val context = LocalContext.current
    val types = remember {
        context.assets.open("region-types.json").bufferedReader().use {
            Json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), it.readText())
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel("Regiões do planeta · ${available.size}")
        available.forEachIndexed { index, region ->
            val info = regionPresentation(region)
            val type = types[region.hash?.toString()]
            val identity = if (type == "factory") "Megafábrica" else when (region.size) {
                "Settlement" -> "Assentamento"
                "Town" -> "Vila"
                "City" -> "Cidade"
                "MegaCity" -> "Megacidade"
                else -> "Metrópole"
            }
            Surface(color = HD.Surface, shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (info.percent == 100.0) HD.Green else HD.Border)) {
            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (type != null) SiteImage(type, identity, Modifier.size(28.dp))
                    Column(Modifier.weight(1f)) {
                        Text(localizedText(region.name).ifBlank { "Região ${index + 1}" }, color = HD.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)
                        Text("$identity • ${info.status}", color = HD.TextDim, fontSize = 11.sp, lineHeight = 14.sp)
                    }
                }
                Text(info.percent?.let { "Progresso da região: ${"%.2f".format(Locale("pt", "BR"), it)}%" }
                    ?: "Progresso indisponível", color = HD.Yellow, fontSize = 13.sp, lineHeight = 17.sp)
                info.percent?.let { ProgressBar(it, HD.Yellow) }
                info.players?.let { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SiteImage("helldivers_active", "Helldivers ativos", Modifier.size(17.dp))
                    Text("${NumberFormat.getInstance(Locale("pt", "BR")).format(it)} Helldivers na região", color = HD.TextDim, fontSize = 12.sp, lineHeight = 16.sp) } }
                if (info.note.isNotEmpty()) Text(info.note, color = HD.TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            }
        }
        Text("Progresso regional independente do progresso do planeta. Dados da última leitura da API.", color = HD.TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
    }
}
