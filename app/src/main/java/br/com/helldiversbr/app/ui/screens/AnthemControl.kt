package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.ui.AnthemPlayer
import br.com.helldiversbr.app.ui.theme.HD
import kotlin.math.roundToInt

@Composable
fun AnthemControl(player: AnthemPlayer) {
    HdCard {
        Text("HINO DA SUPER TERRA", color = HD.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Black)
        TextButton(onClick = player::toggle, modifier = Modifier.fillMaxWidth()) {
            Text(if (player.loading) "CANCELAR CARREGAMENTO" else if (player.playing) "PAUSAR MÚSICA" else "TOCAR MÚSICA", color = HD.Yellow, fontSize = 11.sp)
        }
        Text("Volume: ${(player.volume * 100).roundToInt()}%", color = HD.TextDim, fontSize = 11.sp)
        Slider(value = player.volume, onValueChange = player::updateVolume, onValueChangeFinished = player::saveVolume)
        player.error?.let { Text(it, color = HD.TextDim, fontSize = 11.sp) }
    }
}
