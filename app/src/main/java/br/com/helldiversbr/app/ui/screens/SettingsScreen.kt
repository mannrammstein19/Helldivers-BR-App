package br.com.helldiversbr.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.BuildConfig
import br.com.helldiversbr.app.data.HelldiversApi
import br.com.helldiversbr.app.ui.theme.HD
import br.com.helldiversbr.app.ui.theme.HdThemeMode

private data class CommunityEntry(
    val title: String,
    val subtitle: String,
    val url: String? = null,
    val icon: @Composable () -> Unit,
    val highlight: Boolean = false,
)

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    themeMode: HdThemeMode,
    onThemeMode: (HdThemeMode) -> Unit,
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    val entries = listOf(
        CommunityEntry(
            title = "DISCORD OFICIAL",
            subtitle = "Entre na comunidade HELLDIVERS-BR.",
            url = "${HelldiversApi.SITE_BASE}/discord/index.html",
            icon = { Icon(Icons.Filled.Groups, null, tint = HD.Yellow, modifier = Modifier.size(22.dp)) },
        ),
        CommunityEntry(
            title = "DISCORD DO DESENVOLVEDOR",
            subtitle = "Canal direto para feedback e sugestões · link será adicionado.",
            icon = { Icon(Icons.Filled.Chat, null, tint = HD.Yellow, modifier = Modifier.size(22.dp)) },
            highlight = true,
        ),
        CommunityEntry(
            title = "GRUPO DO WHATSAPP",
            subtitle = "Atalho da comunidade · link será adicionado.",
            icon = { Icon(Icons.Filled.Chat, null, tint = HD.TextDim, modifier = Modifier.size(22.dp)) },
        ),
        CommunityEntry(
            title = "CANAL NO YOUTUBE",
            subtitle = "Conteúdo do projeto · link será adicionado.",
            icon = { Icon(Icons.Filled.PlayCircle, null, tint = HD.TextDim, modifier = Modifier.size(22.dp)) },
        ),
        CommunityEntry(
            title = "CANAL PARCEIRO",
            subtitle = "Espaço reservado para o canal parceiro do projeto.",
            icon = { Icon(Icons.Filled.PlayCircle, null, tint = HD.TextDim, modifier = Modifier.size(22.dp)) },
        ),
        CommunityEntry(
            title = "SITE HELLDIVERS-BR",
            subtitle = "Abrir o portal completo no navegador.",
            url = HelldiversApi.SITE_BASE,
            icon = { Icon(Icons.Filled.Language, null, tint = HD.TextDim, modifier = Modifier.size(22.dp)) },
        ),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 18.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("CONFIGURAÇÕES", color = HD.Text, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(
                    "COMUNIDADE · PROJETO · CRÉDITOS",
                    color = HD.Yellow,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp,
                )
            }
        }

        item {
            HdCard(accent = HD.Yellow) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(HD.SurfaceHigh)
                            .border(1.dp, HD.Yellow.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        // A foto do desenvolvedor entra aqui quando o arquivo definitivo for enviado.
                        Icon(Icons.Filled.Person, contentDescription = null, tint = HD.Yellow, modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("DESENVOLVEDOR", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        Text("HELLDIVERS-BR", color = HD.Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Projeto brasileiro independente feito para a comunidade.", color = HD.TextDim, fontSize = 11.sp)
                    }
                }
            }
        }

        item { SettingsSectionLabel("APARÊNCIA") }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HD.Surface)
                    .border(1.dp, HD.Border, RoundedCornerShape(12.dp))
                    .clickable {
                        onThemeMode(if (themeMode == HdThemeMode.DEFAULT) HdThemeMode.MERIDIA else HdThemeMode.DEFAULT)
                    }
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(HD.SurfaceHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (themeMode == HdThemeMode.MERIDIA) "◉" else "◐", color = HD.Yellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("TEMA DO APLICATIVO", color = HD.Text, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(if (themeMode == HdThemeMode.MERIDIA) "Meridian" else "Padrão HELLDIVERS-BR", color = HD.TextDim, fontSize = 10.sp)
                }
                Text("ALTERAR", color = HD.Yellow, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 0.6.sp)
            }
        }

        item { SettingsSectionLabel("COMUNIDADE & FEEDBACK") }

        entries.forEach { entry ->
            item(key = entry.title) {
                SettingsEntry(entry = entry, onClick = entry.url?.let { { openUrl(it) } })
            }
        }

        item { SettingsSectionLabel("DEDICATÓRIA & CRÉDITOS") }

        item {
            HdCard {
                Text(
                    "FEITO NO BRASIL, PARA HELLDIVERS",
                    color = HD.Yellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    "HELLDIVERS-BR é um projeto comunitário brasileiro independente, criado para reunir informações, ferramentas e acompanhamento da Guerra Galáctica em português.",
                    color = HD.Text,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                Text(
                    "O projeto reconhece e credita as comunidades, APIs e fontes de referência que ajudam a manter as informações organizadas e acessíveis. HELLDIVERS e HELLDIVERS 2 são propriedades de seus respectivos detentores de direitos; este aplicativo não é oficial.",
                    color = HD.TextDim,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                )
            }
        }

        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalDivider(color = HD.Border)
                Spacer(Modifier.height(12.dp))
                Text(
                    "HELLDIVERS-BR // ${BuildConfig.VERSION_NAME}",
                    color = HD.TextMuted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                )
            }
        }
    }
}

@Composable
private fun SettingsEntry(entry: CommunityEntry, onClick: (() -> Unit)?) {
    val enabled = onClick != null
    val border = when {
        entry.highlight -> HD.Yellow.copy(alpha = 0.78f)
        enabled -> HD.Border
        else -> HD.Border.copy(alpha = 0.65f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (entry.highlight) HD.Yellow.copy(alpha = 0.055f) else HD.Surface)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable { onClick?.invoke() } else Modifier)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(HD.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) { entry.icon() }
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.title, color = if (entry.highlight) HD.Yellow else HD.Text, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(entry.subtitle, color = HD.TextDim, fontSize = 10.sp, lineHeight = 14.sp)
        }
        Text(
            if (enabled) "ABRIR ↗" else "EM BREVE",
            color = if (enabled) HD.Yellow else HD.TextMuted,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text, color = HD.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.0.sp)
        HorizontalDivider(color = HD.Yellow.copy(alpha = 0.24f))
    }
}
