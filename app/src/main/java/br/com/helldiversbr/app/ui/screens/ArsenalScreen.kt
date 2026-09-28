package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.HelldiversApi
import br.com.helldiversbr.app.ui.theme.HD
import coil.compose.AsyncImage

private data class ArsenalCategory(
    val title: String,
    val english: String,
    val count: Int,
    val description: String,
)

private val offensive = listOf(
    ArsenalCategory("Estratagemas Orbitais", "Orbital Strikes", 12, "Bombardeios, canhões e ataques vindos diretamente do Super Destroyer."),
    ArsenalCategory("Ataques Águia", "Eagle Strikes", 7, "Passagens rápidas da Águia para destruir alvos e limpar setores inteiros."),
)

private val support = listOf(
    ArsenalCategory("Armas de Apoio", "Support Weapons", 32, "Armamentos pesados chamados durante a missão."),
    ArsenalCategory("Mochilas", "Backpacks", 13, "Sistemas de suporte, escudos, drones e equipamentos transportáveis."),
    ArsenalCategory("Veículos", "Vehicles", 8, "Exotrajes e veículos de mobilidade e combate."),
)

private val defensive = listOf(
    ArsenalCategory("Sentinelas", "Sentries", 14, "Torretas automáticas para controle de área e proteção de objetivos."),
    ArsenalCategory("Plataformas", "Emplacements", 9, "Armas fixas e fortificações para sustentar uma posição."),
    ArsenalCategory("Minas e Defesas", "Mines & Defense", 15, "Campos de negação, minas e outros recursos defensivos."),
)

@Composable
fun ArsenalScreen(
    onOpenCatalog: () -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 26.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ArsenalHero(onOpenCatalog) }
        item { ArsenalSection("Permissão Ofensiva", HD.Red, offensive, onOpenCatalog) }
        item { ArsenalSection("Permissão de Suprimento", HD.SignalBlue, support, onOpenCatalog) }
        item { ArsenalSection("Permissão Defensiva", HD.Green, defensive, onOpenCatalog) }
        item {
            HdCard(accent = HD.Yellow) {
                SectionLabel("Catálogo completo", HD.Yellow)
                Text("A V5 mantém o terminal nativo e oferece acesso ao catálogo completo do HELLDIVERS-BR enquanto as fichas individuais são portadas para Compose.", color = HD.TextDim, fontSize = 12.sp, lineHeight = 18.sp)
                YellowButton("ABRIR CATÁLOGO COMPLETO  →", onOpenCatalog, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ArsenalHero(onOpenCatalog: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HD.BgDeep.copy(alpha = 0.95f)),
        border = BorderStroke(1.dp, HD.Yellow.copy(alpha = 0.7f)),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 10f)) {
            AsyncImage(
                model = "${HelldiversApi.SITE_BASE}/imagens/fundos/site/wallpaper_principal_estratagema.png",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.20f),
                            HD.BgDeep.copy(alpha = 0.78f),
                            HD.BgDeep,
                        )
                    )
                )
            )
            Column(
                Modifier.align(Alignment.BottomStart).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                SectionLabel("◆ Super Terra // Terminal de armamento", HD.Yellow)
                Text("ARSENAL DE", color = HD.Text, fontSize = 27.sp, fontWeight = FontWeight.Black, lineHeight = 28.sp)
                Text("ESTRATAGEMAS", color = HD.Yellow, fontSize = 27.sp, fontWeight = FontWeight.Black, lineHeight = 28.sp)
                Text("Protocolos de ativação, aquisição e recarga para operações em toda a Guerra Galáctica.", color = HD.TextDim, fontSize = 11.sp, lineHeight = 16.sp)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.42f)),
                    border = BorderStroke(1.dp, HD.Yellow.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("REGISTROS TÁTICOS", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text("110", color = HD.Yellow, fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Text("CATALOGADOS", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArsenalLegend(HD.Red, "OFENSIVA")
                    ArsenalLegend(HD.SignalBlue, "SUPRIMENTO")
                    ArsenalLegend(HD.Green, "DEFENSIVA")
                }
            }
        }
    }
}

@Composable
private fun ArsenalLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(18.dp).height(2.dp).background(color))
        Text(label, color = HD.TextMuted, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
    }
}

@Composable
private fun ArsenalSection(
    title: String,
    accent: Color,
    categories: List<ArsenalCategory>,
    onOpenCatalog: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).height(31.dp).background(accent))
            Text(title, color = HD.Yellow, fontSize = 23.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 10.dp))
        }
        HorizontalDivider(color = accent.copy(alpha = 0.75f))
        categories.forEach { category ->
            ArsenalCategoryCard(category, accent, onOpenCatalog)
        }
    }
}

@Composable
private fun ArsenalCategoryCard(category: ArsenalCategory, accent: Color, onOpenCatalog: () -> Unit) {
    var expanded by rememberSaveable(category.title) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = HD.Surface.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("◆  ${category.title}", color = accent, fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.Black)
                    Text(category.english, color = HD.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, accent),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text("${category.count} DISPONÍVEIS", color = accent, fontSize = 8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
                Text(if (expanded) "  ⌃" else "  ⌄", color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            if (expanded) {
                HorizontalDivider(color = HD.BorderSoft)
                Text(category.description, color = HD.TextDim, fontSize = 11.sp, lineHeight = 16.sp)
                Text(
                    "ABRIR REGISTROS TÁTICOS  →",
                    color = HD.Yellow,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp,
                    modifier = Modifier.clickable { onOpenCatalog() }.padding(vertical = 5.dp),
                )
            }
        }
    }
}
