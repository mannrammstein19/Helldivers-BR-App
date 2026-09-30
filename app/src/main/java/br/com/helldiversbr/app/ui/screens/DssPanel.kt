package br.com.helldiversbr.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.Campaign
import br.com.helldiversbr.app.data.DssAvailability
import br.com.helldiversbr.app.data.DssReading
import br.com.helldiversbr.app.data.DssTacticalAction
import br.com.helldiversbr.app.data.HelldiversApi
import br.com.helldiversbr.app.data.OrderRepository
import br.com.helldiversbr.app.data.PlanetCatalogEntry
import br.com.helldiversbr.app.data.SpaceStation
import br.com.helldiversbr.app.data.localizedText
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale

private val dssLocale = Locale("pt", "BR")
private val dssNumber = NumberFormat.getInstance(dssLocale)

private val DSS_ASSET = "${HelldiversApi.SITE_BASE}/imagens/guerra/dss/"
private val DSS_UNAVAILABLE = "${DSS_ASSET}dss-indisponivel.webp"
private val DSS_MODEL = "${DSS_ASSET}DSS_Summary_Model.png"

private data class DssLocation(
    val index: Long,
    val name: String,
    val sector: String,
    val image: String,
)

private data class DssActionInfo(
    val name: String,
    val description: String,
    val icon: String?,
)

private data class DssVisualState(
    val label: String,
    val detail: String,
    val color: Color,
    val showProgress: Boolean,
)

private fun resolveDssLocation(
    station: SpaceStation,
    catalog: Map<Long, PlanetCatalogEntry>,
    campaigns: List<Campaign>,
): DssLocation? {
    val index = station.planet.index
    val campaignPlanet = campaigns.firstOrNull { it.planet.index == index }?.planet
    val catalogPlanet = catalog[index]
    val stationName = localizedText(station.planet.name)
    val name = stationName
        .ifBlank { campaignPlanet?.nameText.orEmpty() }
        .ifBlank { catalogPlanet?.displayName.orEmpty() }
        .trim()
    if (name.isBlank() || (index <= 0L && stationName.isBlank())) return null
    val sector = station.planet.sector
        .ifBlank { campaignPlanet?.sector.orEmpty() }
        .ifBlank { catalogPlanet?.sector.orEmpty() }
        .ifBlank { "SETOR NÃO INFORMADO" }
    return DssLocation(
        index = index,
        name = name,
        sector = sector,
        image = PlanetVisuals.planetImage(index, name, catalogPlanet),
    )
}

private fun dssActionInfo(action: DssTacticalAction): DssActionInfo {
    val raw = action.name.trim()
    val key = raw.lowercase()
    return when {
        "eagle storm" in key -> DssActionInfo(
            "Águia Tempestiva",
            "A DSS emprega ataques periódicos de Águia para apoiar as operações no planeta.",
            "${DSS_ASSET}EAGLE%20STORM.png",
        )
        "orbital blockade" in key -> DssActionInfo(
            "Bloqueio Orbital",
            "Impede o início de novas campanhas de Defesa no planeta e fornece suporte adicional às operações.",
            "${DSS_ASSET}ORBITAL%20BLOCKADE.png",
        )
        "heavy ordnance distribution" in key -> DssActionInfo(
            "Distribuição de Artilharia Pesada",
            "Fornece suporte de artilharia orbital e acelera os esforços de libertação.",
            "${DSS_ASSET}HEAVY%20ORDNANCE%20DISTRIBUTION.png",
        )
        else -> DssActionInfo(
            raw.ifBlank { "Ação Tática" },
            action.description.ifBlank { "Ação tática da Estação Democracia." },
            null,
        )
    }
}

private fun parseDssDate(value: String): Instant? {
    if (value.isBlank()) return null
    value.trim().toLongOrNull()?.let { numeric ->
        val millis = if (numeric < 1_000_000_000_000L) numeric * 1000L else numeric
        return runCatching { Instant.ofEpochMilli(millis) }.getOrNull()
    }
    return runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
}

private fun dssTimeLabel(date: Instant?): String {
    if (date == null) return ""
    var seconds = (date.toEpochMilli() - System.currentTimeMillis()) / 1000L
    if (seconds <= 0L) return ""
    val days = seconds / 86_400L
    seconds %= 86_400L
    val hours = seconds / 3_600L
    val minutes = (seconds % 3_600L) / 60L
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}min"
        else -> "${minutes.coerceAtLeast(1)}min"
    }
}

/**
 * Replica a regra atual do guerra.js do HELLDIVERS-BR web.
 * No portal, status numérico 2 é tratado como ação ATIVA; os demais estados são
 * inferidos pelo financiamento e pelo prazo futuro.
 */
private fun dssActionState(action: DssTacticalAction, pct: Double?): DssVisualState {
    val rawStatus = sequenceOf(
        localizedText(action.statusName),
        localizedText(action.state),
        localizedText(action.statusText),
        action.status.toString(),
    ).firstOrNull { it.isNotBlank() }.orEmpty().lowercase()
    val future = parseDssDate(action.statusExpire)?.takeIf { it.isAfter(Instant.now()) }
    val activeByText = Regex("(^|\\b)(active|ativa|activated|ativada)(\\b|$)", RegexOption.IGNORE_CASE)
        .containsMatchIn(rawStatus)
    if (action.status == 2 || activeByText) {
        return DssVisualState(
            label = "ATIVA",
            detail = future?.let { "Termina em ${dssTimeLabel(it)}" }.orEmpty(),
            color = HD.Green,
            showProgress = false,
        )
    }
    if (Regex("cooldown|recharg|recarreg|unavailable|indispon", RegexOption.IGNORE_CASE).containsMatchIn(rawStatus)) {
        return DssVisualState(
            label = "RECARREGANDO",
            detail = future?.let { "Disponível novamente em ${dssTimeLabel(it)}" }.orEmpty(),
            color = HD.Red,
            showProgress = false,
        )
    }
    if (pct != null && pct < 100.0) {
        val financed = "%.2f".format(dssLocale, pct).trimEnd('0').trimEnd(',')
        return DssVisualState(
            label = "PREPARANDO",
            detail = "$financed% FINANCIADO",
            color = HD.Yellow,
            showProgress = true,
        )
    }
    if (pct != null && pct >= 100.0) {
        return DssVisualState(
            label = "ATIVANDO",
            detail = "FINANCIAMENTO CONCLUÍDO",
            color = HD.Yellow,
            showProgress = false,
        )
    }
    if (future != null) {
        return DssVisualState(
            label = "RECARREGANDO",
            detail = "Disponível novamente em ${dssTimeLabel(future)}",
            color = HD.Red,
            showProgress = false,
        )
    }
    return DssVisualState(
        label = "DESATIVADA",
        detail = "",
        color = HD.Red,
        showProgress = false,
    )
}

private fun dssUnavailableCopy(reading: DssReading): Pair<String, String> = when (reading.availability) {
    DssAvailability.LOCATION_UNKNOWN -> "LOCALIZAÇÃO NÃO INFORMADA" to
        "A leitura atual não informa o planeta da estação."
    DssAvailability.ABSENT -> "DSS TEMPORARIAMENTE INDISPONÍVEL" to
        "Nenhuma estação foi informada na última resposta da API. Aguardando novas informações."
    else -> "SINAL DA DSS INDISPONÍVEL" to
        "Não foi possível obter uma leitura válida da estação. Nova tentativa automática."
}

@Composable
private fun DssStatusDot(color: Color, pulse: Boolean) {
    val transition = rememberInfiniteTransition(label = "dss-status-dot")
    val alpha by transition.animateFloat(
        initialValue = if (pulse) .35f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(725), RepeatMode.Reverse),
        label = "dss-status-dot-alpha",
    )
    Box(Modifier.size(8.dp).clip(CircleShape).background(color.copy(alpha = alpha)))
}

@Composable
private fun DssUnavailableCard(reading: DssReading, tall: Boolean = false) {
    val (title, body) = dssUnavailableCopy(reading)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HD.Surface),
        border = BorderStroke(1.dp, HD.Border),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(if (tall) 4f / 3f else 16f / 7.2f)) {
                AsyncImage(
                    model = DSS_UNAVAILABLE,
                    contentDescription = "Estação Espacial da Democracia",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = .30f), HD.Surface.copy(alpha = .96f))
                        )
                    )
                )
                Text(
                    "ESTAÇÃO DEMOCRACIA",
                    color = HD.Yellow,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                )
            }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = HD.Text, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(body, color = HD.TextDim, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun DssHero(location: DssLocation, station: SpaceStation, stale: Boolean, tall: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HD.BgDeep),
        border = BorderStroke(1.dp, HD.Yellow.copy(alpha = .62f)),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(if (tall) 4f / 3f else 16f / 7.0f)) {
                AsyncImage(
                    model = location.image,
                    contentDescription = location.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = .76f), Color.Black.copy(alpha = .28f), Color.Black.copy(alpha = .55f))
                        )
                    )
                )
                AsyncImage(
                    model = DSS_MODEL,
                    contentDescription = "DSS",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = if (tall) 10.dp else 6.dp)
                        .fillMaxHeight(if (tall) .88f else .76f)
                        .fillMaxWidth(if (tall) .45f else .42f),
                    contentScale = ContentScale.Fit,
                )
                Column(
                    Modifier.align(Alignment.CenterStart).fillMaxWidth(if (tall) .58f else .67f).padding(start = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("ESTAÇÃO DEMOCRACIA", color = HD.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                    Text(location.name.uppercase(), color = Color.White, fontSize = 22.sp, lineHeight = 24.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(location.sector.uppercase(), color = Color.White.copy(alpha = .70f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                    if (stale) Text("ÚLTIMA LEITURA PRESERVADA", color = HD.Gold, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
            val electionEnded = parseDssDate(station.electionEnd)?.isBefore(Instant.now()) ?: station.electionEnd.isBlank()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("PRÓXIMA ELEIÇÃO", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (electionEnded) "ENCERRADA" else OrderRepository.remaining(station.electionEnd).uppercase(),
                        color = HD.Text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("AÇÕES TÁTICAS", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text(station.tacticalActions.size.toString(), color = HD.Yellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun DssActionCard(action: DssTacticalAction, rich: Boolean) {
    val info = dssActionInfo(action)
    val cost = action.costs.firstOrNull()
    val pct = cost?.takeIf { it.targetValue > 0.0 }
        ?.let { (it.currentValue / it.targetValue * 100.0).coerceIn(0.0, 100.0) }
    val state = dssActionState(action, pct)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HD.Surface),
        border = BorderStroke(1.dp, state.color.copy(alpha = .42f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                if (info.icon != null) {
                    AsyncImage(
                        model = info.icon,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.width(10.dp))
                } else {
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(9.dp)).background(HD.BgDeep),
                        contentAlignment = Alignment.Center,
                    ) { Text("◆", color = HD.Yellow, fontSize = 17.sp) }
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(info.name.uppercase(), color = HD.Text, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Black)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DssStatusDot(state.color, state.label == "ATIVA")
                        Text(state.label, color = state.color, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)
                    }
                    if (state.detail.isNotBlank()) Text(state.detail.uppercase(), color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(info.description, color = HD.TextDim, fontSize = 11.sp, lineHeight = 16.sp)

            if (state.showProgress && pct != null) {
                ProgressBar(pct, HD.Yellow)
            }

            if (rich && cost != null && cost.targetValue > 0.0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("CONTRIBUIÇÃO", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text(
                        "${dssNumber.format(cost.currentValue)} / ${dssNumber.format(cost.targetValue)}",
                        color = HD.Text,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (!state.showProgress && pct != null) ProgressBar(pct, state.color)
                val perHour = cost.deltaPerSecond * 3600.0
                if (perHour != 0.0) {
                    Text(
                        "%+.1f/h".format(dssLocale, perHour),
                        color = HD.TextMuted,
                        fontSize = 9.sp,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }

            if (rich) {
                val strategic = action.strategicDescription
                    .replace(Regex("<[^>]*>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                if (strategic.isNotBlank()) {
                    HorizontalDivider(color = HD.BorderSoft)
                    Text(strategic, color = HD.TextMuted, fontSize = 10.sp, lineHeight = 15.sp, fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}

/** Painel completo usado no Mapa Galáctico. */
@Composable
fun DssPanel(
    reading: DssReading,
    planetCatalog: Map<Long, PlanetCatalogEntry> = emptyMap(),
    campaigns: List<Campaign> = emptyList(),
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ESTAÇÃO ESPACIAL DA DEMOCRACIA", color = HD.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.0.sp)
                Text("DSS", color = HD.Text, fontSize = 27.sp, fontWeight = FontWeight.Black)
            }
            val statusColor = when {
                reading.stale -> HD.Gold
                reading.isLive -> HD.Green
                else -> HD.TextMuted
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DssStatusDot(statusColor, reading.isLive)
                Text(
                    when {
                        reading.stale -> "LEITURA SALVA"
                        reading.isLive -> "CONECTADA"
                        else -> "INDISPONÍVEL"
                    },
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        val station = reading.station
        val location = station?.let { resolveDssLocation(it, planetCatalog, campaigns) }
        if (station == null) {
            DssUnavailableCard(reading)
            return@Column
        }
        if (location == null) {
            DssUnavailableCard(reading.copy(availability = DssAvailability.LOCATION_UNKNOWN))
            return@Column
        }

        DssHero(location, station, reading.stale)

        if (station.tacticalActions.isEmpty()) {
            HdCard { Text("NENHUMA AÇÃO TÁTICA ATIVA NO MOMENTO.", color = HD.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        } else {
            SectionLabel("Ações táticas", HD.Text)
            station.tacticalActions.forEach { action -> DssActionCard(action, rich = true) }
        }
    }
}

/** Versão enxuta para a Central de Guerra, seguindo o painel lateral do site. */
@Composable
fun DssWarCard(
    reading: DssReading,
    planetCatalog: Map<Long, PlanetCatalogEntry>,
    campaigns: List<Campaign>,
) {
    HdCard(accent = if (reading.hasStation) HD.Yellow else HD.Border) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                SectionLabel("🛰 DSS", HD.Yellow)
                Text("ESTAÇÃO DEMOCRACIA", color = HD.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
            }
            val color = when {
                reading.stale -> HD.Gold
                reading.isLive -> HD.Green
                else -> HD.TextMuted
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                DssStatusDot(color, reading.isLive)
                Text(if (reading.stale) "SALVA" else if (reading.isLive) "ONLINE" else "INDISPONÍVEL", color = color, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }

        val station = reading.station
        val location = station?.let { resolveDssLocation(it, planetCatalog, campaigns) }
        if (station == null) {
            DssUnavailableCard(reading, tall = true)
            return@HdCard
        }
        if (location == null) {
            DssUnavailableCard(reading.copy(availability = DssAvailability.LOCATION_UNKNOWN), tall = true)
            return@HdCard
        }

        DssHero(location, station, reading.stale, tall = true)
        if (station.tacticalActions.isEmpty()) {
            Text("Nenhuma ação tática ativa no momento.", color = HD.TextMuted, fontSize = 10.sp)
        } else {
            station.tacticalActions.forEach { DssActionCard(it, rich = false) }
        }
    }
}
