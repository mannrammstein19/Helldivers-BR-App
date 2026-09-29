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
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
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
    onOpenOrder: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenArsenal: () -> Unit,
    onOpenFactions: () -> Unit,
    contentPadding: PaddingValues,
) {
    Box(Modifier.fillMaxSize().background(Color.Transparent)) {
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
                onOpenOrder = onOpenOrder,
                onOpenMap = onOpenMap,
                onOpenArsenal = onOpenArsenal,
                onOpenFactions = onOpenFactions,
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
                onOpenOrder = onOpenOrder,
                        onOpenMap = onOpenMap,
                        onOpenArsenal = onOpenArsenal,
                        onOpenFactions = onOpenFactions,
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
    onOpenOrder: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenArsenal: () -> Unit,
    onOpenFactions: () -> Unit,
    contentPadding: PaddingValues,
) {
    val carousel = rememberLazyListState()
    val scope = rememberCoroutineScope()
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
                    Text("SEU PRÓXIMO DESTINO", color = HD.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Text("DESLIZE →", color = HD.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }

        item {
            LazyRow(state = carousel, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        onClick = onOpenFactions,
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(5) { index ->
                    IconButton(onClick = { scope.launch { carousel.animateScrollToItem(index) } }) {
                        Box(Modifier.width(if(carousel.firstVisibleItemIndex == index) 20.dp else 5.dp).height(5.dp)
                            .clip(RoundedCornerShape(50)).background(if(carousel.firstVisibleItemIndex == index) HD.Yellow else HD.TextMuted))
                    }
                }
            }
        }
        item { HomeOrderCard(data, onOpenOrder) }
        item {
            HdCard(accent = Color(0xFF7478F6)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage("${HelldiversApi.SITE_BASE}/icons/discord.png", "Discord", Modifier.size(48.dp))
                    Column {
                        Text("DISCORD · HELLDIVERS BR", color = HD.TextDim, fontSize = 10.sp)
                        Text("Encontre seu esquadrão.", color = HD.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }
                Text("Combine partidas e lute pela Super Terra com a comunidade.", color = HD.TextDim, fontSize = 12.sp)
                YellowButton("ENTRAR NO DISCORD ↗", { openExternal("discord/index.html") }, Modifier.fillMaxWidth())
            }
        }

        if (data.dispatches.isNotEmpty()) {
            item {
                Column {
                    SectionLabel("Comunicações recentes", HD.TextDim)
                    Text("DESPACHOS RECENTES", color = HD.Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
            data.dispatches.take(5).forEachIndexed { index, dispatch ->
                item(key = "home-dispatch-${dispatch.id}-$index") { DispatchCard(dispatch) }
            }
        }
    }
}

@Composable
private fun HomeHero(onRefresh: () -> Unit) {
    Column(Modifier.padding(top = 6.dp)) {
        SectionLabel("HELLDIVERS BR // COMANDO", HD.Yellow)
        Text(buildAnnotatedString {
            append("PRONTO PARA\nO PRÓXIMO ")
            withStyle(SpanStyle(color = HD.Yellow)) { append("MERGULHO?") }
        }, color = HD.Text, fontSize = 29.sp, lineHeight = 31.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 12.dp))
        Text("Escolha seu destino. Pela Super Terra.", color = HD.TextDim, fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp))
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
            .height(240.dp)
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
                        fontSize = 25.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsyncImage(
                            model = PlanetVisuals.factionLogo(factionRaw),
                            contentDescription = OrderRepository.factionLabel(factionRaw),
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Text(p.nameText.uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        "${sector.uppercase()}  •  ${OrderRepository.factionLabel(factionRaw).uppercase()}",
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("CONTROLE PLANETÁRIO", color = HD.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("%.2f%%".format(ptBrHome, progress), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
                ProgressBar(progress, accent)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SiteImage("helldivers_active", "Helldivers ativos", Modifier.size(17.dp))
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
