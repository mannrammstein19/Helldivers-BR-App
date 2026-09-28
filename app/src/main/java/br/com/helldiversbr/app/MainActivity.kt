package br.com.helldiversbr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.helldiversbr.app.ui.MainViewModel
import br.com.helldiversbr.app.ui.screens.ComingSoonScreen
import br.com.helldiversbr.app.ui.screens.HomeScreen
import br.com.helldiversbr.app.ui.screens.WarScreen
import br.com.helldiversbr.app.ui.theme.HD
import br.com.helldiversbr.app.ui.theme.HelldiversTheme

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("inicio", "Início", Icons.Filled.Home),
    Tab("guerra", "Guerra", Icons.Filled.Public),
    Tab("mapa", "Mapa", Icons.Filled.Map),
    Tab("arsenal", "Arsenal", Icons.Filled.Shield),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelldiversTheme {
                App()
            }
        }
    }
}

@Composable
private fun App(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "inicio"

    val home by vm.home.collectAsState()
    val update by vm.update.collectAsState()

    Scaffold(
        containerColor = HD.Bg,
        bottomBar = {
            AppBottomBar(current = current, onNavigate = { route -> navigateTab(nav, current, route) })
        },
    ) { padding: PaddingValues ->
        NavHost(navController = nav, startDestination = "inicio") {
            composable("inicio") {
                HomeScreen(
                    state = home,
                    update = update,
                    onRefresh = vm::refresh,
                    onDismissUpdate = vm::dismissUpdate,
                    onOpenWar = { navigateTab(nav, current, "guerra") },
                    onOpenMap = { navigateTab(nav, current, "mapa") },
                    onOpenArsenal = { navigateTab(nav, current, "arsenal") },
                    contentPadding = padding,
                )
            }
            composable("guerra") {
                WarScreen(
                    state = home,
                    onRefresh = vm::refresh,
                    onOpenMap = { navigateTab(nav, "guerra", "mapa") },
                    contentPadding = padding,
                )
            }
            composable("mapa") { ComingSoonScreen("Mapa Galáctico", "mapa-classico.html", padding) }
            composable("arsenal") { ComingSoonScreen("Arsenal e Estratagemas", "estratagemas.html", padding) }
        }
    }
}

private fun navigateTab(nav: NavHostController, current: String, route: String) {
    if (current == route) return
    nav.navigate(route) {
        popUpTo("inicio") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppBottomBar(current: String, onNavigate: (String) -> Unit) {
    Surface(color = HD.Surface, shadowElevation = 12.dp) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = HD.Border, thickness = 1.dp)
            Row(Modifier.fillMaxWidth().height(64.dp)) {
                tabs.forEach { tab ->
                    val selected = current == tab.route
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(tab.route) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(if (selected) HD.Yellow else Color.Transparent)
                        )
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (selected) HD.Yellow else HD.TextMuted,
                            modifier = Modifier.padding(top = 9.dp),
                        )
                        Text(
                            tab.label.uppercase(),
                            color = if (selected) HD.Yellow else HD.TextMuted,
                            fontSize = 9.sp,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
