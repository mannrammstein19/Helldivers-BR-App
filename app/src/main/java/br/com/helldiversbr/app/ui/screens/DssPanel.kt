package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.Campaign
import br.com.helldiversbr.app.data.DssAvailability
import br.com.helldiversbr.app.data.DssReading
import br.com.helldiversbr.app.data.DssTacticalAction
import br.com.helldiversbr.app.data.PlanetCatalogEntry
import br.com.helldiversbr.app.data.SpaceStation
import br.com.helldiversbr.app.data.localizedText
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale

private val dssLocale = Locale("pt", "BR")

private val DSS_UNAVAILABLE get() = br.com.helldiversbr.app.data.MapAssets.file("dss-inoperante").orEmpty()
private val DSS_MODEL get() = br.com.helldiversbr.app.data.MapAssets.file("dss-operacional").orEmpty()

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
    br.com.helldiversbr.app.data.DssSupport.kind(action)?.let { known ->
        return DssActionInfo(if (known == br.com.helldiversbr.app.data.DssSupport.EAGLE) "Tempestade da Águia" else known.label, when (known) {
            br.com.helldiversbr.app.data.DssSupport.EAGLE -> "Ataques de gás Águia durante as missões. Retarda o avanço inimigo em campanhas de defesa."
            br.com.helldiversbr.app.data.DssSupport.BLOCKADE -> "Campanhas de defesa não podem ser originadas deste planeta. O impulsor de Otimização Espacial Hellpod fica ativo para todas as missões."
            br.com.helldiversbr.app.data.DssSupport.HEAVY -> "Concede acesso à Barragem Orbital de Alto Explosivo de 380 mm durante as missões. Acelera o progresso nas campanhas de libertação."
        }, known.icon)
    }
    val raw = action.name.trim()
    val key = raw.lowercase()
    return when {
        "eagle storm" in key -> DssActionInfo(
            "Águia Tempestiva",
            "Ataques de gás Águia durante as missões. Retarda o avanço inimigo em campanhas de defesa.",
            br.com.helldiversbr.app.data.MapAssets.file("imagens/guerra/dss/EAGLE STORM.png"),
        )
        "orbital blockade" in key -> DssActionInfo(
            "Bloqueio Orbital",
            "Impede o início de novas campanhas de Defesa no planeta e fornece suporte adicional às operações.",
            br.com.helldiversbr.app.data.MapAssets.file("imagens/guerra/dss/ORBITAL BLOCKADE.png"),
        )
        "heavy ordnance distribution" in key -> DssActionInfo(
            "Distribuição de Artilharia Pesada",
            "Fornece suporte de artilharia orbital e acelera os esforços de libertação.",
            br.com.helldiversbr.app.data.MapAssets.file("imagens/guerra/dss/HEAVY ORDNANCE DISTRIBUTION.png"),
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

private fun dssTimeLabel(date: Instant?, now: Long = System.currentTimeMillis()): String {
    if (date == null) return ""
    return br.com.helldiversbr.app.data.DssDisplayFormat.duration((date.toEpochMilli() - now) / 1000L)
}

/** State follows explicit telemetry, never contribution alone. */
private fun dssActionState(action: DssTacticalAction, now: Long): DssVisualState {
    val end = parseDssDate(br.com.helldiversbr.app.data.DssActionRules.deadline(action))
    val duration = end?.let { dssTimeLabel(it, now) }.orEmpty()
    return when (br.com.helldiversbr.app.data.DssActionRules.phase(action, now)) {
        br.com.helldiversbr.app.data.DssActionPhase.ACTIVE -> DssVisualState("ATIVA",
            if (duration.isBlank()) "Prazo não informado" else "Ativa por: $duration", HD.Green, false)
        br.com.helldiversbr.app.data.DssActionPhase.COOLDOWN -> DssVisualState("RECARREGANDO", "Disponível em $duration", HD.Red, false)
        br.com.helldiversbr.app.data.DssActionPhase.FUNDING -> DssVisualState("PREPARANDO", "Contribuição informada pela API", HD.Yellow, true)
        br.com.helldiversbr.app.data.DssActionPhase.OFFLINE -> DssVisualState("INDISPONÍVEL", "", HD.TextMuted, false)
        br.com.helldiversbr.app.data.DssActionPhase.PENDING -> DssVisualState("AGUARDANDO ATUALIZAÇÃO", "Estado ou prazo não confirmado", HD.Gold, false)
    }
}

private fun dssUnavailableCopy(reading: DssReading): Pair<String, String> = when (reading.availability) {
    DssAvailability.LOCATION_UNKNOWN -> "LOCALIZAÇÃO NÃO INFORMADA" to
        "A leitura atual não informa o planeta da estação."
    DssAvailability.ABSENT -> "DSS TEMPORARIAMENTE INDISPONÍVEL" to
        "Nenhuma estação foi informada na última resposta da API. Aguardando novas informações."
    else -> "AGUARDANDO LEITURA DA DSS" to
        "Ainda não há uma leitura salva da estação. Nova tentativa automática."
}

@Composable
private fun DssUnavailableCard(reading: DssReading, tall: Boolean = false, portrait: Boolean = false) {
    val (title, body) = dssUnavailableCopy(reading)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HD.Surface),
        border = BorderStroke(1.dp, HD.Border),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                AsyncImage(
                    model = DSS_UNAVAILABLE,
                    contentDescription = "Estação Espacial da Democracia",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = if (portrait) ContentScale.Fit else ContentScale.Crop,
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

private val DssHeadingFont = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(br.com.helldiversbr.app.R.font.oswald_bold, FontWeight.Bold))

@Composable
private fun DssHero(location: DssLocation, station: SpaceStation, stale: Boolean, clock: Long) {
    val election = parseDssDate(station.electionEnd)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090F14)),
        border = BorderStroke(1.dp, Color(0xFF34444E))) {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(PlanetVisuals.factionLogo(station.planet.currentOwner), null,
                    Modifier.size(28.dp), contentScale = ContentScale.Fit)
                Column(Modifier.weight(1f)) {
                    Text(location.name, color = factionColor(station.planet.currentOwner), fontSize = 19.sp,
                        fontFamily = DssHeadingFont, fontWeight = FontWeight.Bold, letterSpacing = .65.sp)
                    Text(location.sector.uppercase(), color = Color(0xFFBACBD2), fontSize = 10.sp, letterSpacing = 1.2.sp)
                }
                AsyncImage(br.com.helldiversbr.app.data.MapAssets.file("imagens/guerra/dss/DSS_Summary_Model.png"),
                    "DSS", Modifier.size(46.dp), contentScale = ContentScale.Fit)
            }
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                val landscapeHeight = (maxWidth / 2.75f).coerceIn(110.dp, 165.dp)
                if (location.image.isNotBlank()) AsyncImage(location.image, location.name,
                    Modifier.fillMaxWidth().height(landscapeHeight), contentScale = ContentScale.Crop)
                else Box(Modifier.fillMaxWidth().height(landscapeHeight).background(Color(0xFF111D29)),
                    contentAlignment = Alignment.Center) {
                    Text("Paisagem não disponível", color = Color(0xFFBACBD2), fontSize = 11.sp)
                }
            }
            Text(if (stale) "Prazo da votação sem confirmação atual" else if (election == null) "Prazo não informado"
                else if (election.toEpochMilli() <= clock) "Aguardando atualização da votação"
                else dssTimeLabel(election, clock), Modifier.align(Alignment.End).padding(horizontal = 12.dp, vertical = 10.dp),
                color = Color(0xFFD8EDF6), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun dssDescription(text: String): androidx.compose.ui.text.AnnotatedString =
    androidx.compose.ui.text.buildAnnotatedString {
        append(text)
        listOf("Ataques de gás Águia", "campanhas de defesa", "Campanhas de defesa",
            "Otimização Espacial Hellpod", "Barragem Orbital de Alto Explosivo de 380 mm", "campanhas de libertação")
            .forEach { term ->
                val start = text.indexOf(term)
                if (start >= 0) addStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFFFE600)), start, start + term.length)
            }
    }

@Composable
private fun DssFundingPanel(cost: br.com.helldiversbr.app.data.DssCost, saved: Boolean) {
    val pct = br.com.helldiversbr.app.data.DssActionRules.percent(cost)
    val resource = br.com.helldiversbr.app.data.DssActionRules.resource(cost)
    val key = when (cost.itemMixId ?: cost.id.toLongOrNull()) {
        3992382197L -> "dss-resource-common"
        2985106497L -> "dss-resource-rare"
        3608481516L -> "dss-resource-requisition"
        else -> null
    }
    Column(Modifier.fillMaxWidth().background(Color(0xFF091C24)).border(2.dp, Color(0xFF264A5A))
        .padding(horizontal = 10.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(Modifier.weight(1f).height(13.dp).background(Color(0xFF05090D)).padding(3.dp)) {
                Box(Modifier.fillMaxSize().background(Color(0xFF484848))) {
                    if (pct != null && pct > 0.0) Box(Modifier.fillMaxHeight()
                        .fillMaxWidth((pct / 100.0).toFloat()).background(Color(0xFF8BD7F4)))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (key != null) AsyncImage(br.com.helldiversbr.app.data.MapAssets.file(key), resource,
                    Modifier.size(15.dp), contentScale = ContentScale.Fit)
                else Text(resource, color = Color(0xFFDCE7EA), fontSize = 10.sp)
                Text(pct?.let { "%.3f%%".format(dssLocale, it) } ?: "—", color = Color(0xFFDCE7EA),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        val estimate = if (saved) null else br.com.helldiversbr.app.data.DssActionRules.estimateSeconds(cost)
        Text(if (saved) "Estimativa sem confirmação atual" else estimate?.let {
            br.com.helldiversbr.app.data.DssDisplayFormat.duration(kotlin.math.ceil(it).toLong())
        } ?: "Estimativa indisponível", Modifier.fillMaxWidth(), color = Color(0xFFC7D1D5),
            fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun DssActionCard(action: DssTacticalAction, reading: DssReading, clock: Long) {
    val info = dssActionInfo(action)
    val saved = !br.com.helldiversbr.app.data.DssSupport.isCurrent(reading, clock)
    val observed = dssActionState(action, if (saved) reading.fetchedAtMillis else clock)
    val active = observed.label == "ATIVA"
    val unavailable = observed.label in listOf("RECARREGANDO", "INDISPONÍVEL")
    val pending = observed.label == "AGUARDANDO ATUALIZAÇÃO"
    val borderColor = when { active -> Color(0xFFFFE600); unavailable -> Color(0xFF53281B)
        pending -> Color(0xFF5A6267); else -> Color(0xFF477E99) }
    val background = if (active) Color(0xFF1C1C10) else if (unavailable) Color(0xFF190E0A) else Color(0xFF162E39)
    Card(Modifier.fillMaxWidth(), shape = RectangleShape,
        colors = CardDefaults.cardColors(containerColor = background), border = BorderStroke(2.dp, borderColor)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(info.name.uppercase(), color = if (unavailable) Color(0xFFDB4800) else Color(0xFFB8DCEA),
                fontFamily = DssHeadingFont, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(dssDescription(info.description), Modifier.weight(1f).background(Color(0xFF202020))
                    .border(2.dp, Color(0xFF8B8B88)).padding(horizontal = 8.dp, vertical = 7.dp),
                    color = Color(0xFFC8C8C8), fontSize = 12.sp, lineHeight = 17.4.sp)
                info.icon?.let { AsyncImage(it, info.name, Modifier.size(56.dp).background(Color(0xFF202020))
                    .border(2.dp, if (active && !saved) Color(0xFFFFE600) else Color(0xFFA87979))
                    .padding(4.dp), contentScale = ContentScale.Fit) }
            }
            when {
                unavailable -> DssHazardStrip(if (saved) "${observed.label} · prazo sem confirmação atual"
                    else if (observed.label == "RECARREGANDO") observed.detail.replace("Disponível em ", "Disponível em: ")
                    else observed.label)
                active -> {
                    val detail = when (br.com.helldiversbr.app.data.DssSupport.kind(action)) {
                        br.com.helldiversbr.app.data.DssSupport.EAGLE -> "A DSS mantém uma frota rotativa de caças Águia, apoiando os Helldivers com suporte aéreo e retardando as ofensivas inimigas."
                        br.com.helldiversbr.app.data.DssSupport.BLOCKADE -> "A DSS intercepta grandes naves inimigas que tentam deixar a atmosfera e oferece suporte logístico aos Super Destroyers."
                        br.com.helldiversbr.app.data.DssSupport.HEAVY -> "A frota logística da DSS fornece munição de 380 mm aos Super Destroyers e apoio de artilharia às operações da SEAF."
                        else -> "Ação informada como ativa nesta leitura."
                    }
                    Text(detail, Modifier.fillMaxWidth().background(Color(0xFF25220B)).border(1.dp, Color(0xFFB8A600)).padding(8.dp),
                        color = Color(0xFFF0DF76), fontSize = 11.sp, lineHeight = 16.sp)
                    Text(if (saved) "Ativação sem confirmação atual" else observed.detail,
                        Modifier.fillMaxWidth().background(Color(0xFFFFE600)).padding(8.dp),
                        color = Color(0xFF10120B), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                observed.showProgress -> {
                    if (action.costs.isEmpty()) Text("Contribuição não informada", color = Color(0xFFC7D1D5), fontSize = 11.sp)
                    action.costs.forEach { DssFundingPanel(it, saved) }
                }
                else -> Text("${observed.label} · ${observed.detail}", color = Color(0xFFE2C784), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun DssHazardStrip(label: String) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF512417)).padding(5.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        @Composable fun Stripes() {
            Canvas(Modifier.width(30.dp).height(26.dp).clip(RectangleShape)) {
                val step = 8.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(Color(0xFFFF5200), Offset(x, size.height), Offset(x + size.height, 0f), 4.dp.toPx())
                    x += step
                }
            }
        }
        Stripes()
        Text(label, Modifier.weight(1f), color = Color(0xFFFF5200), fontSize = 12.sp, lineHeight = 16.sp,
            fontWeight = FontWeight.ExtraBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Stripes()
    }
}

/** Mesmo painel completo no Mapa e na Guerra: uma única implementação visual. */
@Composable
fun DssPanel(
    reading: DssReading,
    planetCatalog: Map<Long, PlanetCatalogEntry> = emptyMap(),
    campaigns: List<Campaign> = emptyList(),
) {
    val clock = dssDisplayClock(1_000L)
    val currentReading = br.com.helldiversbr.app.data.DssSupport.readingIsFresh(reading, clock)
    Column(Modifier.fillMaxWidth().background(Color(0xFF090909)).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val station = reading.station
        val location = station?.let { resolveDssLocation(it, planetCatalog, campaigns) }
        if (station == null) DssUnavailableCard(reading, portrait = true)
        else if (location == null) DssUnavailableCard(reading.copy(availability = DssAvailability.LOCATION_UNKNOWN), portrait = true)
        else {
            DssHero(location, station, !currentReading, clock)
            if (station.tacticalActions.isEmpty()) Text("Nenhuma ação tática informada nesta leitura.",
                color = Color(0xFFADB6B9), fontSize = 11.sp)
            else station.tacticalActions.forEach { DssActionCard(it, reading, clock) }
        }
        val stamp = if (reading.fetchedAtMillis > 0L) java.text.SimpleDateFormat("dd/MM/yyyy, HH:mm:ss", dssLocale)
            .format(java.util.Date(reading.fetchedAtMillis)) else "horário não informado"
        Text("${if (currentReading) "Leitura" else "Última leitura"} · $stamp · ${reading.source}" +
            if (!currentReading) " · situação atual sem confirmação" else "",
            Modifier.fillMaxWidth(), color = Color(0xFFADB6B9), fontSize = 10.sp, lineHeight = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

/** Entrada da Guerra delega ao mesmo componente exibido no Mapa. */
@Composable
fun DssWarCard(reading: DssReading, planetCatalog: Map<Long, PlanetCatalogEntry>, campaigns: List<Campaign>) {
    DssPanel(reading, planetCatalog, campaigns)
}
