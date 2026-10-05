package br.com.helldiversbr.app.ui.screens

import androidx.compose.material3.TextButton
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.Dispatch
import br.com.helldiversbr.app.ui.theme.HD

@Composable
fun HdCard(
    modifier: Modifier = Modifier,
    accent: Color = HD.Border,
    contentSpacing: androidx.compose.ui.unit.Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HD.Surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.8f)),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(accent)
            )
            Column(
                Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(contentSpacing),
                content = content,
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, color: Color = HD.Yellow) {
    Text(
        text = text.uppercase(),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.8.sp,
    )
}

@Composable
fun ProgressBar(percent: Double, color: Color, modifier: Modifier = Modifier) {
    val fraction = (percent / 100.0).coerceIn(0.0, 1.0).toFloat()
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(HD.SurfaceHigh),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(color),
            )
        }
    }
}

@Composable
fun YellowButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = HD.Yellow, contentColor = Color.Black),
        shape = RoundedCornerShape(4.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 15.dp, vertical = 10.dp),
    ) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 0.6.sp)
    }
}

@Composable
fun Chip(text: String, color: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    detail: String,
    accent: Color,
    modifier: Modifier = Modifier,
    backgroundKey: String? = null,
    pulse: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "stat-pulse-$label")
    val pulseAlpha by transition.animateFloat(
        initialValue = if (pulse) 0.45f else 0.90f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(animation = tween(650), repeatMode = RepeatMode.Reverse),
        label = "stat-pulse-alpha-$label",
    )
    val accentAlpha = if (pulse) pulseAlpha else 0.9f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HD.Surface),
        border = BorderStroke(1.dp, HD.Border.copy(alpha = 0.72f)),
    ) {
        Box {
            if (backgroundKey != null) {
                SiteImage(backgroundKey, label, Modifier.matchParentSize(), scale = androidx.compose.ui.layout.ContentScale.Crop)
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.38f)))
            }
            if (pulse) {
                Box(Modifier.matchParentSize().background(accent.copy(alpha = 0.07f * pulseAlpha)))
            }
            Box(Modifier.align(Alignment.TopStart).fillMaxWidth().height(2.dp).background(accent.copy(alpha = accentAlpha)))
            Box(Modifier.align(Alignment.TopStart).fillMaxHeight().width(2.dp).background(accent.copy(alpha = accentAlpha)))
            Column(Modifier.padding(horizontal = 11.dp, vertical = 11.dp)) {
                Text(
                    label.uppercase(),
                    color = HD.TextDim,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                )
                Text(
                    value,
                    color = if (pulse) accent else HD.Text,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    detail,
                    color = HD.TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
fun DispatchCard(dispatch: Dispatch, modifier: Modifier = Modifier) {
    var expanded by androidx.compose.runtime.saveable.rememberSaveable(dispatch.id.toString()) { androidx.compose.runtime.mutableStateOf(false) }
    val text = dispatch.text
    if (text.isBlank()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = HD.Surface),
        border = BorderStroke(1.dp, HD.Border.copy(alpha = .78f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("Transmissão", HD.SignalBlue)
                Text("ALTO COMANDO", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            LocalizedText(
                text,
                color = HD.Text,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (expanded) "RECOLHER  ▲" else "LER DESPACHO  →",
                color = HD.Yellow,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.clickable { expanded = !expanded }.padding(vertical = 2.dp),
            )
        }
    }
}

fun factionColor(raw: String, defense: Boolean = false): Color {
    val n = raw.lowercase()
    return when {
        "terminid" in n || n == "2" -> HD.TerminidOrange
        "automaton" in n || "cyborg" in n || n == "3" -> HD.AutomatonRed
        "illuminate" in n || "squid" in n || n == "4" -> HD.IlluminatePurple
        "human" in n || "super" in n || n == "1" -> HD.DefenseBlue
        defense -> HD.DefenseBlue
        else -> HD.TextDim
    }
}
