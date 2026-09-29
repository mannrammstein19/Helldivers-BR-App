package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.SiteAssets
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage

@Composable
fun SiteImage(assetKey: String, description: String?, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit, tint: androidx.compose.ui.graphics.Color? = null) {
    val context = LocalContext.current
    val sources = remember(assetKey) { SiteAssets.urls(context, assetKey) }
    var attempt by remember(assetKey) { mutableStateOf(0) }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (attempt < sources.size) {
            AsyncImage(model = sources[attempt], contentDescription = description,
                modifier = Modifier.fillMaxSize(), contentScale = scale, colorFilter = tint?.let { androidx.compose.ui.graphics.ColorFilter.tint(it) }, onError = { attempt++ })
        } else {
            Text("Imagem indisponível", color = HD.TextMuted, fontSize = 9.sp)
        }
    }
}
