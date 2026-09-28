package br.com.helldiversbr.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    val fallback: ImageVector,
)

private val tabs = listOf(
    Tab("inicio", "Início", "inicio.png", Icons.Filled.Home),
    Tab("guerra", "Guerra", "guerra.png", Icons.Filled.Public),
    Tab("ordem", "Ordem", "ordem.png", Icons.Filled.Star),
    Tab("mapa", "Mapa", "mapa.png", Icons.Filled.Map),
    Tab("arsenal", "Arsenal", "arsenal.png", Icons.Filled.Shield),
    Tab("menu", "Menu", "menu.png", Icons.Filled.Menu),
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
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp).padding(top = 4.dp)) {
                            Icon(
                                imageVector = tab.fallback,
                                contentDescription = tab.label,
                                tint = if (selected) HD.Yellow else HD.TextDim,
                                modifier = Modifier.size(22.dp),
                            )
                            AsyncImage(
                                model = "${HelldiversApi.SITE_BASE}/icons/${tab.iconFile}",
                                contentDescription = null,
                                modifier = Modifier.size(27.dp),
                                contentScale = ContentScale.Fit,
                                alpha = 1f,
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
    ModalDrawerSheet(
        drawerContainerColor = HD.BgDeep,
        drawerContentColor = HD.Text,
        modifier = Modifier.fillMaxWidth(0.84f),
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("HELLDIVERS BR", color = HD.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
                    Text("MENU DE NAVEGAÇÃO", color = HD.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Text("✕", color = HD.TextDim, fontSize = 18.sp, modifier = Modifier.clickable { onClose() }.padding(8.dp))
            }
            HorizontalDivider(color = HD.Yellow)

            DrawerEntry("INÍCIO", "Terminal principal", current == "inicio") { onNavigate("inicio") }
            DrawerEntry("CENTRAL DE GUERRA", "Frentes, campanhas e telemetria", current == "guerra") { onNavigate("guerra") }
            DrawerEntry("ORDEM MAIOR", "Objetivos do Alto Comando", current == "ordem") { onNavigate("ordem") }
            DrawerEntry("MAPA GALÁCTICO", "Setores e linhas de suprimento", current == "mapa") { onNavigate("mapa") }
            DrawerEntry("ARSENAL", "Estratagemas e armamentos", current == "arsenal") { onNavigate("arsenal") }

            Text("ARQUIVOS DA SUPER TERRA", color = HD.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 8.dp))
            DrawerEntry("WARBONDS", "Catálogo de títulos de guerra", false) { onOpenSite("warbonds/warbonds-wiki.html") }
            DrawerEntry("FACÇÕES", "Dossiês de inimigos", false) { onOpenSite("faccoes.html") }
            DrawerEntry("SITE COMPLETO", "Abrir HELLDIVERS-BR no navegador", false) { onOpenSite("") }

            Text("TEMA", color = HD.TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 8.dp))
            ThemeEntry(
                title = "PADRÃO HELLDIVERS",
                description = "Preto + amarelo da Super Terra",
                selected = themeMode == HdThemeMode.DEFAULT,
                onClick = { onThemeMode(HdThemeMode.DEFAULT) },
            )
            ThemeEntry(
                title = "MERIDIA",
                description = "Buraco Negro // roxo operacional",
                selected = themeMode == HdThemeMode.MERIDIA,
                onClick = { onThemeMode(HdThemeMode.MERIDIA) },
            )

            Text(
                "APLICATIVO NATIVO // KOTLIN + JETPACK COMPOSE",
                color = HD.TextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
            )
        }
    }
}

@Composable
private fun DrawerEntry(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) HD.Yellow else HD.Border
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) HD.Yellow.copy(alpha = 0.10f) else HD.Surface)
            .clickable { onClick() }
            .padding(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(border))
            Text(title, color = if (selected) HD.Yellow else HD.Text, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 9.dp))
        }
        Text(subtitle, color = HD.TextMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 14.dp, top = 3.dp))
    }
}

@Composable
private fun ThemeEntry(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) HD.Yellow.copy(alpha = 0.12f) else HD.Surface)
            .clickable { onClick() }
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(18.dp).clip(RoundedCornerShape(50))
                .background(if (selected) HD.Yellow else HD.SurfaceHigh)
        )
        Column(Modifier.padding(start = 11.dp)) {
            Text(title, color = if (selected) HD.Yellow else HD.Text, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(description, color = HD.TextMuted, fontSize = 9.sp)
        }
    }
}
