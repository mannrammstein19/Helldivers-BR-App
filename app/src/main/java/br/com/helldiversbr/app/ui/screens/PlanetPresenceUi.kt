package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import coil.compose.AsyncImage
import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.theme.HD
import kotlinx.coroutines.delay

@Composable
fun PresenceIcons(planet: Planet, labels: Boolean = false, stale: Boolean = false, modifier: Modifier = Modifier) {
    val values = PlanetPresences.list(planet)
    var selectedKey by remember(planet.index) { mutableStateOf<String?>(null) }
    values.firstOrNull { it.key == selectedKey }?.let { PresenceGalleryDialog(it, planet, stale || planet.presenceHistoryStale) { selectedKey = null } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        values.forEach { presence ->
            Row(Modifier.clickable(onClickLabel = "Ver ficha da presença") { selectedKey = presence.key }.padding(2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AsyncImage(presence.artwork, presence.name + if(stale || planet.presenceHistoryStale) " · última leitura" else "", modifier=Modifier.size(26.dp),
                    colorFilter=if(presence.file.endsWith(".svg")) androidx.compose.ui.graphics.ColorFilter.tint(mapColor(presence.faction)) else null)
                if(labels) Text(presence.name + if(stale || planet.presenceHistoryStale) " · última leitura" else "",color=mapColor(presence.faction),fontSize=11.sp)
            }
        }
    }
}

@Composable
fun RotatingPlanetStatus(status: String, planet: Planet, stale: Boolean, color: Color) {
    val historical = stale || planet.presenceHistoryStale
    val notices = remember(status,planet,historical) { listOf(status) + PlanetPresences.list(planet).map { "${if (historical) "Última leitura" else "Presença"}: ${it.name}" } }
    var index by remember(notices) { mutableStateOf(0) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(notices,stale,lifecycle) {
        index=0
        if(!stale && notices.size>1) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while(true) { delay(10_000);index=(index+1)%notices.size }
        }
    }
    Text(notices.getOrElse(index) { status },color=color,fontSize=9.sp,fontWeight=FontWeight.Black,maxLines=1,
        overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.widthIn(max=145.dp))
}
