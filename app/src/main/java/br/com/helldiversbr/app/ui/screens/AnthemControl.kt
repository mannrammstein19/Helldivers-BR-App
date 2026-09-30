package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.ui.AnthemPlayer
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage
import kotlin.math.roundToInt

/**
 * Controle compacto do Hino da Super Terra.
 * Arte opcional preparada para: app/src/main/assets/backgrounds/super-earth-anthem.webp
 * Se o arquivo ainda não existir, o degradê mantém o card funcional sem imagem quebrada visível.
 */
@Composable
fun AnthemControl(player: AnthemPlayer) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(HD.Surface)
            .border(1.dp, HD.Yellow.copy(alpha = 0.48f), RoundedCornerShape(10.dp)),
    ) {
        AsyncImage(
            model = "file:///android_asset/backgrounds/super-earth-anthem.webp",
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.24f,
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.horizontalGradient(
                    listOf(
                        HD.BgDeep.copy(alpha = 0.97f),
                        HD.BgDeep.copy(alpha = 0.78f),
                        Color.Black.copy(alpha = 0.66f),
                    )
                )
            )
        )
        Box(Modifier.width(4.dp).height(92.dp).background(HD.Yellow))

        Column(
            Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("HINO DA SUPER TERRA", color = HD.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(
                        when {
                            player.loading -> "CARREGANDO TRANSMISSÃO..."
                            player.playing -> "TRANSMISSÃO EM EXECUÇÃO"
                            else -> "TRANSMISSÃO PATRIÓTICA"
                        },
                        color = HD.TextMuted,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                    )
                }
                Text(
                    when {
                        player.loading -> "CANCELAR"
                        player.playing -> "❚❚ PAUSAR"
                        else -> "▶ TOCAR"
                    },
                    color = HD.Yellow,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.42f))
                        .border(1.dp, HD.Yellow.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 9.dp, vertical = 7.dp)
                        .then(Modifier),
                )
            }

            // A área inteira do card alterna tocar/pausar; o slider permanece independente abaixo.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("VOL. ${(player.volume * 100).roundToInt()}%", color = HD.TextDim, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = player.volume,
                    onValueChange = player::updateVolume,
                    onValueChangeFinished = player::saveVolume,
                    modifier = Modifier.weight(1f).height(26.dp).padding(start = 5.dp),
                )
            }
            player.error?.let { Text(it, color = HD.Red, fontSize = 7.sp, maxLines = 1) }
        }

        // Botão transparente sobre a faixa superior, sem interceptar o slider de volume.
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 4.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .clickable(onClick = player::toggle)
        )
    }
}
