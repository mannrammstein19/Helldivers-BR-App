package br.com.helldiversbr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.helldiversbr.app.ui.MainViewModel
import br.com.helldiversbr.app.ui.screens.ComingSoonScreen
import br.com.helldiversbr.app.ui.screens.HomeScreen
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
            NavigationBar(containerColor = HD.Surface) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            if (current != tab.route) {
                                nav.navigate(tab.route) {
                                    popUpTo("inicio") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = HD.Yellow,
                            indicatorColor = HD.Yellow,
                            unselectedIconColor = HD.TextDim,
                            unselectedTextColor = HD.TextDim,
                        ),
                    )
                }
            }
        },
    ) { padding: PaddingValues ->
        NavHost(nav, startDestination = "inicio", modifier = Modifier) {
            composable("inicio") {
                HomeScreen(
                    state = home,
                    update = update,
                    onRefresh = vm::refresh,
                    onDismissUpdate = vm::dismissUpdate,
                    contentPadding = padding,
                )
            }
            composable("guerra") { ComingSoonScreen("Central de Guerra", "guerra.html", padding) }
            composable("mapa") { ComingSoonScreen("Mapa Galáctico", "mapa-galatico.html", padding) }
            composable("arsenal") { ComingSoonScreen("Arsenal e Estratagemas", "estratagemas.html", padding) }
        }
    }
}
