package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.SubcomposeAsyncImage

@Composable
fun StratagemArtwork(model: String?, name: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(model, name, modifier, contentScale = ContentScale.Fit,
        error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sem imagem", color = HD.TextMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
        } })
}
