package br.com.helldiversbr.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.helldiversbr.app.data.HelldiversApi
import br.com.helldiversbr.app.ui.MainViewModel
import br.com.helldiversbr.app.ui.screens.ArsenalScreen
import br.com.helldiversbr.app.ui.screens.ComingSoonScreen
import br.com.helldiversbr.app.ui.screens.HomeScreen
import br.com.helldiversbr.app.ui.screens.OrderScreen
import br.com.helldiversbr.app.ui.screens.WarScreen
import br.com.helldiversbr.app.ui.theme.HD
import br.com.helldiversbr.app.ui.theme.HdThemeMode
import br.com.helldiversbr.app.ui.theme.HelldiversTheme
import br.com.helldiversbr.app.ui.theme.ThemePreferences
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private data class Tab(
    val route: String,
    val label: String,
    val iconFile: String,
)

private val tabs = listOf(
    Tab("inicio", "Início", "inicio.png"),
    Tab("guerra", "Guerra", "guerra.png"),
    Tab("ordem", "Ordem", "ordem.png"),
    Tab("mapa", "Mapa", "mapa.png"),
    Tab("arsenal", "Arsenal", "arsenal.png"),
    Tab("menu", "Menu", "menu.png"),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(ThemePreferences.load(context)) }
            HelldiversTheme(themeMode) {
                App(
                    themeMode = themeMode,
                    onThemeMode = {
                        themeMode = it
                        ThemePreferences.save(context, it)
                    },
                )
            }
        }
    }
}

@Composable
private fun App(
    themeMode: HdThemeMode,
    onThemeMode: (HdThemeMode) -> Unit,
    vm: MainViewModel = viewModel(),
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "inicio"
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val home by vm.home.collectAsState()
    val update by vm.update.collectAsState()

    fun navigate(route: String) {
        navigateTab(nav, current, route)
        scope.launch { drawerState.close() }
    }

    fun openSite(path: String) {
        val url = "${HelldiversApi.SITE_BASE}/${path.trimStart('/')}"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                current = current,
                themeMode = themeMode,
                onNavigate = ::navigate,
                onOpenSite = ::openSite,
                onThemeMode = onThemeMode,
                onClose = { scope.launch { drawerState.close() } },
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.72f),
    ) {
        Box(Modifier.fillMaxSize().background(HD.Bg)) {
            AppBackdrop(themeMode)
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    AppTopHeader(onMenu = { scope.launch { drawerState.open() } })
                },
                bottomBar = {
                    AppBottomBar(
                        current = current,
                        menuOpen = drawerState.isOpen,
                        onNavigate = ::navigate,
                        onMenu = { scope.launch { drawerState.open() } },
                    )
                },
            ) { padding: PaddingValues ->
                NavHost(navController = nav, startDestination = "inicio") {
                    composable("inicio") {
                        HomeScreen(
                            state = home,
                            update = update,
                            onRefresh = vm::refresh,
                            onDismissUpdate = vm::dismissUpdate,
                            onOpenWar = { navigate("guerra") },
                            onOpenMap = { navigate("mapa") },
                            onOpenArsenal = { navigate("arsenal") },
                            contentPadding = padding,
                        )
                    }
                    composable("guerra") {
                        WarScreen(
                            state = home,
                            onRefresh = vm::refresh,
                            onOpenMap = { navigate("mapa") },
                            contentPadding = padding,
                        )
                    }
                    composable("ordem") {
                        OrderScreen(
                            state = home,
                            onRefresh = vm::refresh,
                            contentPadding = padding,
                        )
                    }
                    composable("mapa") {
                        ComingSoonScreen("Mapa Galáctico", "mapa-classico.html", padding)
                    }
                    composable("arsenal") {
                        ArsenalScreen(
                            onOpenCatalog = { openSite("estratagemas.html") },
                            contentPadding = padding,
                        )
                    }
                }
            }
        }
    }
}

private fun navigateTab(nav: NavHostController, current: String, route: String) {
    if (route == "menu" || current == route) return
    nav.navigate(route) {
        popUpTo("inicio") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppBackdrop(themeMode: HdThemeMode) {
    val image = if (themeMode == HdThemeMode.MERIDIA) {
        "${HelldiversApi.SITE_BASE}/imagens/planetas/Void_Source_Planet_Landscape_Void_Header.png"
    } else {
        "${HelldiversApi.SITE_BASE}/imagens/fundos/site/wallpaper_principal_page.png"
    }
    AsyncImage(
        model = image,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        alpha = if (themeMode == HdThemeMode.MERIDIA) 0.42f else 0.16f,
    )
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    HD.Bg.copy(alpha = 0.70f),
                    HD.Bg.copy(alpha = 0.90f),
                    HD.Bg.copy(alpha = 0.98f),
                )
            )
        )
    )
}

@Composable
private fun AppTopHeader(onMenu: () -> Unit) {
    Surface(color = Color.Black.copy(alpha = 0.92f), shadowElevation = 8.dp) {
        Column(Modifier.statusBarsPadding().clickable { onMenu() }) {
            Row(
                Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Menu, contentDescription = null, tint = HD.Yellow, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "MENU DE NAVEGAÇÃO",
                    color = HD.Yellow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.2.sp,
                )
            }
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(HD.Yellow)
            )
            HorizontalDivider(color = HD.Yellow, thickness = 1.dp)
        }
    }
}

@Composable
private fun AppBottomBar(
    current: String,
    menuOpen: Boolean,
    onNavigate: (String) -> Unit,
    onMenu: () -> Unit,
) {
    Surface(color = Color(0xFF090909).copy(alpha = 0.97f), shadowElevation = 16.dp) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = HD.Border, thickness = 1.dp)
            Row(Modifier.fillMaxWidth().height(67.dp)) {
                tabs.forEach { tab ->
                    val selected = if (tab.route == "menu") menuOpen else current == tab.route
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (selected) HD.Yellow.copy(alpha = 0.055f) else Color.Transparent)
                            .clickable { if (tab.route == "menu") onMenu() else onNavigate(tab.route) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.fillMaxWidth().height(2.dp)
                                .background(if (selected) HD.Yellow else Color.Transparent)
                        )
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(30.dp).padding(top = 4.dp)) {
                            // Usa somente os ícones originais do HELLDIVERS-BR. Na V5 havia um
                            // Material Icon por baixo da PNG e as duas silhuetas podiam se sobrepor.
                            AsyncImage(
                                model = "${HelldiversApi.SITE_BASE}/icons/${tab.iconFile}",
                                contentDescription = tab.label,
                                modifier = Modifier.size(28.dp),
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.tint(if (selected) HD.Yellow else HD.TextDim),
                            )
                        }
                        Text(
                            tab.label,
                            color = if (selected) HD.Yellow else HD.TextDim,
                            fontSize = 8.5.sp,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppDrawer(
    current: String,
    themeMode: HdThemeMode,
    onNavigate: (String) -> Unit,
    onOpenSite: (String) -> Unit,
    onThemeMode: (HdThemeMode) -> Unit,
    onClose: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val query = search.trim().lowercase()
    fun show(vararg labels: String): Boolean = query.isBlank() || labels.any { it.lowercase().contains(query) }

    ModalDrawerSheet(
        drawerContainerColor = HD.BgDeep,
        drawerContentColor = HD.Text,
        modifier = Modifier.fillMaxWidth(0.72f),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Cabeçalho semelhante ao painel lateral mobile do site.
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { onClose() },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    Modifier.fillMaxWidth().height(48.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✕", color = HD.Yellow, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "FECHAR MENU",
                        color = HD.Yellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.0.sp,
                    )
                }
                Box(
                    Modifier.width(42.dp).height(3.dp).clip(RoundedCornerShape(50)).background(HD.Yellow)
                )
                HorizontalDivider(color = HD.Yellow, thickness = 1.dp)
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Logo nativa do projeto: não depende de rede para aparecer no menu.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(HD.Surface)
                        .border(1.dp, HD.Border, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = R.drawable.ic_launcher_foreground,
                        contentDescription = "HELLDIVERS-BR",
                        modifier = Modifier.size(142.dp),
                        contentScale = ContentScale.Fit,
                    )
                }

                DrawerSearch(search = search, onSearch = { search = it })

                DrawerThemeControl(
                    themeMode = themeMode,
                    onToggle = {
                        onThemeMode(
                            if (themeMode == HdThemeMode.DEFAULT) HdThemeMode.MERIDIA else HdThemeMode.DEFAULT
                        )
                    },
                )

                if (show("comando", "página principal", "central de guerra", "ordem maior", "mapa galáctico")) {
                    DrawerGroupLabel("COMANDO")
                    if (show("página principal", "inicio", "início")) {
                        DrawerCompactEntry("Página Principal", current == "inicio") { onNavigate("inicio") }
                    }
                    if (show("central de guerra", "guerra")) {
                        DrawerCompactEntry("⚔ Central de Guerra", current == "guerra") { onNavigate("guerra") }
                    }
                    if (show("ordem maior", "ordem")) {
                        DrawerCompactEntry("Ordem Maior", current == "ordem") { onNavigate("ordem") }
                    }
                    if (show("mapa galáctico", "mapa")) {
                        DrawerCompactEntry("Mapa Galáctico", current == "mapa") { onNavigate("mapa") }
                    }
                }

                if (show("aquisições", "passes de guerra", "warbonds")) {
                    DrawerGroupLabel("AQUISIÇÕES")
                    if (show("passes de guerra", "warbonds")) {
                        DrawerCompactEntry("Passes de Guerra", false) { onOpenSite("warbonds/warbonds-wiki.html") }
                    }
                }

                if (show("equipamento", "estratagemas", "arsenal")) {
                    DrawerGroupLabel("EQUIPAMENTO")
                    if (show("estratagemas", "arsenal")) {
                        DrawerCompactEntry("Estratagemas", current == "arsenal") { onNavigate("arsenal") }
                    }
                    if (show("catálogo", "site", "arsenal completo")) {
                        DrawerCompactEntry("Catálogo completo", false) { onOpenSite("estratagemas.html") }
                    }
                }

                if (show("inimigos", "facções", "faccoes")) {
                    DrawerGroupLabel("INIMIGOS & FACÇÕES")
                    DrawerCompactEntry("Facções", false) { onOpenSite("faccoes.html") }
                }

                if (show("comunidade", "site completo", "helldivers br")) {
                    DrawerGroupLabel("COMUNIDADE")
                    DrawerCompactEntry("Site HELLDIVERS-BR", false) { onOpenSite("") }
                }

                Text(
                    "APLICATIVO NATIVO // KOTLIN + JETPACK COMPOSE",
                    color = HD.TextMuted,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun DrawerSearch(search: String, onSearch: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(HD.Surface)
            .border(1.dp, HD.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("BUSCA GLOBAL HDBR", color = HD.Text, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.0.sp)
            Text("MENU NATIVO", color = HD.TextMuted, fontSize = 6.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(39.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(HD.BgDeep)
                .border(1.dp, HD.Yellow.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⌕", color = HD.Yellow, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (search.isBlank()) {
                    Text("Pesquisar no menu...", color = HD.TextMuted, fontSize = 11.sp)
                }
                BasicTextField(
                    value = search,
                    onValueChange = onSearch,
                    singleLine = true,
                    textStyle = TextStyle(color = HD.Text, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (search.isNotBlank()) {
                Text("✕", color = HD.TextMuted, fontSize = 12.sp, modifier = Modifier.clickable { onSearch("") }.padding(5.dp))
            }
        }
    }
}

@Composable
private fun DrawerThemeControl(themeMode: HdThemeMode, onToggle: () -> Unit) {
    val meridia = themeMode == HdThemeMode.MERIDIA
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(HD.Surface)
            .border(1.dp, HD.Border, RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .border(1.dp, if (meridia) HD.Yellow else HD.TextMuted, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (meridia) "◉" else "◐", color = if (meridia) HD.Yellow else HD.TextDim, fontSize = 13.sp)
        }
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text("TEMA", color = HD.TextMuted, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 1.0.sp)
            Text(if (meridia) "MERIDIA" else "PADRÃO", color = HD.Text, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
        }
        Text("ALTERAR", color = HD.Yellow, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun DrawerGroupLabel(label: String) {
    Column(Modifier.fillMaxWidth().padding(top = 5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = HD.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.0.sp)
        HorizontalDivider(color = HD.Yellow.copy(alpha = 0.25f))
    }
}

@Composable
private fun DrawerCompactEntry(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(47.dp)
            .background(if (selected) HD.Yellow.copy(alpha = 0.11f) else HD.Surface)
            .border(1.dp, if (selected) HD.Yellow.copy(alpha = 0.75f) else HD.Border, RoundedCornerShape(3.dp))
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(47.dp).background(if (selected) HD.Yellow else HD.Yellow.copy(alpha = 0.35f)))
        Text(
            title,
            color = if (selected) HD.Yellow else HD.Text,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp),
            maxLines = 1,
        )
    }
}
