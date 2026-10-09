package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Progresso legível: números brancos dentro da faixa, compartilhada por Início e Ordem. */
@Composable
internal fun OrderProgressBand(percent: Double, count: String, percentage: String, accent: Color) {
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.fillMaxWidth().heightIn(min = 34.dp).clip(shape).background(Color(0xFF0F1112))
        .border(1.dp, Color(0xFF3A4044), shape)) {
        Box(Modifier.matchParentSize()) {
            if (percent.isFinite() && percent > 0) Box(Modifier.fillMaxHeight()
                .fillMaxWidth((percent / 100.0).coerceIn(0.0, 1.0).toFloat()).background(accent.copy(alpha = .34f)))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(count, Modifier.weight(1f), color = Color.White, fontSize = 14.sp,
                lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(percentage, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}
