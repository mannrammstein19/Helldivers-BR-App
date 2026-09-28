package br.com.helldiversbr.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.HelldiversApi
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage

/** Terminal visual provisório usado pelo Mapa enquanto a versão Compose completa é portada. */
@Composable
fun ComingSoonScreen(title: String, sitePath: String, contentPadding: PaddingValues) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = contentPadding.calculateTopPadding() + 14.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = HD.BgDeep.copy(alpha = 0.95f)),
                border = BorderStroke(1.dp, HD.SignalBlue.copy(alpha = 0.7f)),
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 11f)) {
                    AsyncImage(
                        model = "${HelldiversApi.SITE_BASE}/imagens/fundos/fundos-grids/frente.jpg",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.18f), HD.BgDeep.copy(alpha = 0.72f), HD.BgDeep)
                            )
                        )
                    )
                    Column(
                        Modifier.align(Alignment.BottomStart).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SectionLabel("◆ Super Terra // Navegação tática", HD.SignalBlue)
                        Text(title.uppercase(), color = HD.Text, fontSize = 29.sp, lineHeight = 31.sp, fontWeight = FontWeight.Black)
                        Text(
                            "Toda a galáxia no seu radar. Setores, frentes e linhas de suprimento em um único terminal.",
                            color = HD.TextDim,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Status", "ONLINE", "terminal web operacional", HD.Green, Modifier.weight(1f))
                StatTile("Modo", "TÁTICO", "mapa galáctico completo", HD.SignalBlue, Modifier.weight(1f))
            }
        }
        item {
            HdCard(accent = HD.SignalBlue) {
                SectionLabel("Mapa nativo em migração", HD.SignalBlue)
                Text(
                    "A V5 já integra o Mapa à navegação oficial do app. Enquanto o mapa clássico é portado para Compose sem perder setores, filtros e linhas de suprimento, este terminal abre a versão completa já publicada no HELLDIVERS-BR.",
                    color = HD.TextDim,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                YellowButton("ABRIR MAPA GALÁCTICO  →", onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${HelldiversApi.SITE_BASE}/$sitePath")))
                }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
