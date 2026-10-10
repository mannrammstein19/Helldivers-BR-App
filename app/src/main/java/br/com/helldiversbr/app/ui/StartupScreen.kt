package br.com.helldiversbr.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.R
import br.com.helldiversbr.app.ui.theme.HD
import kotlinx.coroutines.delay

@Composable
fun StartupScreen(home: HomeState) {
    val motion = remember { Animatable(0f) }
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { motion.animateTo(1f, tween(4_000, easing = LinearEasing)) }
    LaunchedEffect(Unit) { delay(1_100); stage = 1; delay(1_800); stage = 2 }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).clipToBounds()) {
        val landscape = maxWidth > maxHeight
        Image(painterResource(R.drawable.startup_helldiver), contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = 1f + motion.value * .045f
                scaleY = scaleX
            })
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .14f)))
        Box(Modifier.fillMaxWidth().fillMaxHeight(if (landscape) .65f else .48f)
            .align(Alignment.BottomCenter).background(Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = .82f), Color.Black))))
        Image(painterResource(R.drawable.ic_launcher_foreground), "HELLDIVERS-BR",
            modifier = Modifier.align(Alignment.Center).size(if (landscape) 112.dp else 184.dp).clip(CircleShape),
            contentScale = ContentScale.Fit)
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = if (landscape) 16.dp else 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("HELLDIVERS-BR", color = HD.Yellow, fontWeight = FontWeight.Black,
                fontSize = if (landscape) 17.sp else 22.sp, letterSpacing = 2.sp)
            Text(when {
                stage == 0 -> "Inicializando os sistemas…"
                stage == 2 -> "Preparando a Central de Guerra…"
                home is HomeState.Ready && home.data.telemetrySource == "cache" -> "Última leitura recuperada • conectando…"
                home is HomeState.Ready && !home.refreshing -> "Telemetria recebida"
                home is HomeState.Error -> "Conexão indisponível • preparando acesso…"
                else -> "Atualizando telemetria…"
            }, color = Color(0xFFD7D9DC), fontSize = 12.sp, textAlign = TextAlign.Center)
            // Indica a apresentação de abertura, não porcentagem de download ou da API.
            LinearProgressIndicator(progress = { motion.value }, modifier = Modifier.fillMaxWidth().height(2.dp),
                color = HD.Yellow, trackColor = Color.White.copy(alpha = .15f))
        }
    }
}
