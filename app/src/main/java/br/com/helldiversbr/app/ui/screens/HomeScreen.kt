package br.com.helldiversbr.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.HelldiversApi
import br.com.helldiversbr.app.data.HomeData
import br.com.helldiversbr.app.data.Campaign
import br.com.helldiversbr.app.data.OrderRepository
import br.com.helldiversbr.app.ui.HomeState
import br.com.helldiversbr.app.ui.theme.HD
import br.com.helldiversbr.app.update.RemoteVersion
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ptBrHome = Locale("pt", "BR")
private fun fmtHome(n: Long): String = NumberFormat.getInstance(ptBrHome).format(n)

@Composable
fun HomeScreen(
    state: HomeState,
    update: RemoteVersion?,
    onRefresh: () -> Unit,
    onDismissUpdate: () -> Unit,
    onOpenWar: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenArsenal: () -> Unit,
    contentPadding: PaddingValues,
) {
    Box(Modifier.fillMaxSize().background(HD.Bg)) {
        when (state) {
            HomeState.Loading -> Column(
                Modifier.fillMaxSize().statusBarsPadding(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = HD.Yellow)
                Text(
                    "CONECTANDO À TELEMETRIA DA SUPER TERRA...",
                    color = HD.TextDim,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            is HomeState.Ready -> HomeList(
                data = state.data,
                refreshing = state.refreshing,
                errorBanner = null,
                update = update,
                onRefresh = onRefresh,
                onDismissUpdate = onDismissUpdate,
                onOpenWar = onOpenWar,
                onOpenMap = onOpenMap,
                onOpenArsenal = onOpenArsenal,
                contentPadding = contentPadding,
            )

            is HomeState.Error -> {
                val last = state.last
                if (last != null) {
                    HomeList(
                        data = last,
                        refreshing = false,
                        errorBanner = state.message,
                        update = update,
                        onRefresh = onRefresh,
                        onDismissUpdate = onDismissUpdate,
                        onOpenWar = onOpenWar,
                        onOpenMap = onOpenMap,
                        onOpenArsenal = onOpenArsenal,
                        contentPadding = contentPadding,
                    )
                } else {
                    Column(
                        Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SectionLabel("Telemetria indisponível", HD.Red)
                        Text(
                            state.message,
                            color = HD.Text,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Text(
                            "Verifique sua conexão e tente novamente.",
                            color = HD.TextDim,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                        )
                        YellowButton("TENTAR NOVAMENTE", onRefresh)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeList(
    data: HomeData,
    refreshing: Boolean,
    errorBanner: String?,
    update: RemoteVersion?,
    onRefresh: () -> Unit,
    onDismissUpdate: () -> Unit,
    onOpenWar: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenArsenal: () -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current

    fun openExternal(path: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${HelldiversApi.SITE_BASE}/$path")))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HomeHero(onRefresh) }

        if (refreshing) {
            item {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().height(2.dp),
                    color = HD.Yellow,
                    trackColor = HD.SurfaceHigh,
                )
            }
        }

        if (update != null) item { UpdateBanner(update, onDismissUpdate) }

        if (errorBanner != null) {
            item {
                HdCard(accent = HD.Red) {
                    SectionLabel("Conexão degradada", HD.Red)
                    Text("$errorBanner Mostrando os últimos dados carregados.", color = HD.TextDim, fontSize = 12.sp)
                    TextButton(onClick = onRefresh) { Text("TENTAR NOVAMENTE", color = HD.Yellow, fontWeight = FontWeight.Bold) }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    SectionLabel("Terminais de comando", HD.TextDim)
                    Text("SEU PRÓXIMO DESTINO", color = HD.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Text("DESLIZE →", color = HD.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    MissionCard(
                        number = "TERMINAL 01 / 05",
                        title = "CENTRAL DE GUERRA",
                        subtitle = "Acompanhe as frentes. Escolha onde lutar.",
                        action = "ABRIR CENTRAL",
                        imageUrl = "${HelldiversApi.SITE_BASE}/imagens/fundos/site/wallpaper_principal_page.png",
                        accent = HD.Yellow,
                        onClick = onOpenWar,
                    )
                }
                item {
                    MissionCard(
                        number = "TERMINAL 02 / 05",
                        title = "MAPA GALÁCTICO",
                        subtitle = "Toda a galáxia no seu radar.",
                        action = "EXPLORAR MAPA",
                        imageUrl = "${HelldiversApi.SITE_BASE}/imagens/fundos/fundos-grids/frente.jpg",
                        accent = HD.SignalBlue,
                        onClick = onOpenMap,
                    )
                }
                item {
                    MissionCard(
                        number = "TERMINAL 03 / 05",
                        title = "ESTRATAGEMAS",
                        subtitle = "Prepare o arsenal do próximo mergulho.",
                        action = "CONSULTAR CÓDIGOS",
                        imageUrl = "${HelldiversApi.SITE_BASE}/imagens/fundos/site/wallpaper_principal_estratagema.png",
                        accent = HD.Orange,
                        onClick = onOpenArsenal,
                    )
                }
                item {
                    MissionCard(
                        number = "TERMINAL 04 / 05",
                        title = "WARBONDS",
                        subtitle = "Encontre seu próximo equipamento.",
                        action = "VER PASSES",
                        imageUrl = "${HelldiversApi.SITE_BASE}/imagens/fundos/site/warbonds.png",
                        accent = HD.Green,
                        onClick = { openExternal("warbonds/warbonds-wiki.html") },
                    )
                }
                item {
                    MissionCard(
                        number = "TERMINAL 05 / 05",
                        title = "FACÇÕES",
                        subtitle = "Conheça as ameaças à democracia.",
                        action = "CONSULTAR INTELIGÊNCIA",
                        imageUrl = "${HelldiversApi.SITE_BASE}/imagens/fundos/fundos-grids/campo.jpg",
                        accent = HD.IlluminatePurple,
                        onClick = { openExternal("faccoes.html") },
                    )
                }
            }
        }

        item { OrderCard(data) }

        data.campaigns.maxByOrNull { it.planet.statistics.playerCount }?.let { spotlight ->
            item { FrontSpotlightCard(data, spotlight, onOpenWar) }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        SectionLabel("Telemetria ao vivo", HD.SignalBlue)
                        Text("SITUAÇÃO DA GUERRA", color = HD.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    val time = SimpleDateFormat("HH:mm", ptBrHome).format(Date(data.updatedAtMillis))
                    Text("ATUALIZADO $time", color = HD.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "Helldivers no front",
                        value = fmtHome(data.helldiversOnFront),
                        detail = "efetivo em campanhas ativas",
                        accent = HD.YellowBright,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Liberações",
                        value = data.liberationCount.toString(),
                        detail = "frentes ofensivas",
                        accent = HD.Green,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "Defesas",
                        value = data.defenseCount.toString(),
                        detail = "frentes em defesa",
                        accent = HD.Red,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = "Frentes ativas",
                        value = data.activeFronts.toString(),
                        detail = "campanhas detectadas",
                        accent = HD.SignalBlue,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            YellowButton("ABRIR CENTRAL DE GUERRA  →", onOpenWar, Modifier.fillMaxWidth())
        }

        if (data.dispatches.isNotEmpty()) {
            item {
                Column {
                    SectionLabel("Comunicações recentes", HD.TextDim)
                    Text("ÚLTIMO DESPACHO", color = HD.Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
            item { DispatchCard(data.dispatches.first()) }
        }
    }
}

@Composable
private fun HomeHero(onRefresh: () -> Unit) {
    Column(Modifier.statusBarsPadding().padding(top = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                SectionLabel("HELLDIVERS BR // COMANDO", HD.Yellow)
                Text("TERMINAL OPERACIONAL", color = HD.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(HD.Green)
                        .width(7.dp)
                        .height(7.dp)
                )
                Text(" ONLINE", color = HD.Green, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Atualizar telemetria", tint = HD.Yellow)
                }
            }
        }
        Text(
            "PRONTO PARA\nO PRÓXIMO MERGULHO?",
            color = HD.Text,
            fontSize = 30.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Informação, estratégia e guerra galáctica em um único terminal.",
            color = HD.TextDim,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun MissionCard(
    number: String,
    title: String,
    subtitle: String,
    action: String,
    imageUrl: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(292.dp)
            .height(178.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(7.dp),
        colors = CardDefaults.cardColors(containerColor = HD.Surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.16f), Color.Black.copy(alpha = 0.86f))
                        )
                    )
            )
            Box(Modifier.fillMaxWidth().height(3.dp).background(accent))
            Column(
                Modifier.fillMaxSize().padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(number, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Column {
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(subtitle, color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 2.dp))
                    Text(
                        "$action  ↗",
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FrontSpotlightCard(data: HomeData, campaign: Campaign, onOpenWar: () -> Unit) {
    val p = campaign.planet
    val catalog = data.planetCatalog[p.index]
    val defense = p.event != null
    val factionRaw = OrderRepository.campaignFaction(campaign)
    val accent = factionColor(factionRaw, defense)
    val progress = OrderRepository.campaignPercent(campaign)
    val image = PlanetVisuals.planetImage(p.index, p.nameText, catalog)
    val sector = p.sector.ifBlank { catalog?.sector.orEmpty() }.ifBlank { "Setor desconhecido" }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenWar),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = HD.Surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.75f)),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 7.2f)) {
                AsyncImage(model = image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)))))
                Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                    SectionLabel("FRENTE EM DESTAQUE // ${if (defense) "DEFESA" else "LIBERTAÇÃO"}", accent)
                    Text(p.nameText.uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(sector.uppercase(), color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("CONTROLE PLANETÁRIO", color = HD.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("%.2f%%".format(ptBrHome, progress), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                ProgressBar(progress, accent)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${fmtHome(p.statistics.playerCount)} HELLDIVERS OPERANDO", color = HD.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("ABRIR CENTRAL  →", color = HD.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
                }
            }
        }
    }
}

@Composable
private fun UpdateBanner(update: RemoteVersion, onDismiss: () -> Unit) {
    val context = LocalContext.current
    HdCard(accent = HD.Yellow) {
        SectionLabel("Nova versão disponível")
        Text(
            "Helldivers BR ${update.versionName}",
            color = HD.Text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
        )
        if (update.notes.isNotBlank()) {
            Text(update.notes, color = HD.TextDim, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            YellowButton("BAIXAR ATUALIZAÇÃO", onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.apkUrl)))
            })
            TextButton(onClick = onDismiss) { Text("DEPOIS", color = HD.TextDim, fontWeight = FontWeight.Bold) }
        }
    }
}
