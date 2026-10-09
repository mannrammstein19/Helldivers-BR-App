package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import br.com.helldiversbr.app.data.DssReading
import br.com.helldiversbr.app.data.DssSupport
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/** Expiry checks run only while the screen is visible. No polling of the API. */
@Composable
internal fun dssDisplayClock(refreshMillis: Long = 30_000L): Long {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, refreshMillis) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { now = System.currentTimeMillis(); delay(refreshMillis.coerceAtLeast(1_000L)) }
        }
    }
    return now
}

@Composable
fun DssSupportIcons(reading: DssReading?, planetIndex: Long, modifier: Modifier = Modifier) {
    val supports = DssSupport.forPlanet(reading, planetIndex, dssDisplayClock())
    if (supports.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        supports.forEach { support ->
            Card(shape = RoundedCornerShape(9.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = .74f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .55f))) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    AsyncImage(support.icon, "${support.label} · apoio da DSS confirmado neste planeta",
                        modifier = Modifier.size(29.dp), contentScale = ContentScale.Fit)
                }
            }
        }
    }
}
