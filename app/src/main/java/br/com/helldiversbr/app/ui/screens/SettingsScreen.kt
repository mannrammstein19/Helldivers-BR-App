package br.com.helldiversbr.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.BuildConfig
import br.com.helldiversbr.app.R
import br.com.helldiversbr.app.notifications.WarAlertManager
import br.com.helldiversbr.app.ui.theme.HD
import br.com.helldiversbr.app.ui.theme.HdThemeMode
import androidx.core.content.ContextCompat

private const val DISCORD_DEVELOPER = "adonai_elohim"
private const val DISCORD_OFFICIAL_URL = "https://discord.gg/helldiversbr"
private const val WHATSAPP_URL = "https://chat.whatsapp.com/GZTXzdMy7sL9RlZurBorIe"
private const val YOUTUBE_URL = "https://www.youtube.com/@LuisPlayGamesOficial"
private const val PARTNER_CHANNEL_URL = "https://www.youtube.com/@HELLDIVERS-PirataPerdido"
private const val SITE_URL = "https://helldivers-br.pages.dev/"

private data class CommunityEntry(
    val title: String,
    val subtitle: String,
    val imageRes: Int,
    val url: String? = null,
    val copyText: String? = null,
    val highlight: Boolean = false,
)

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    themeMode: HdThemeMode,
    onThemeMode: (HdThemeMode) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var invasionAlerts by remember(context) { mutableStateOf(WarAlertManager.isEnabled(context)) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        invasionAlerts = granted
        WarAlertManager.setEnabled(context, granted)
        if (!granted) Toast.makeText(context, "Permissão de notificações não concedida.", Toast.LENGTH_SHORT).show()
    }

    fun setInvasionAlerts(enabled: Boolean) {
        if (!enabled) {
            invasionAlerts = false
            WarAlertManager.setEnabled(context, false)
            return
        }
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else {
            invasionAlerts = true
            WarAlertManager.setEnabled(context, true)
        }
    }

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(context, "Não foi possível abrir este link.", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyText(text: String) {
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, "Usuário copiado: $text", Toast.LENGTH_SHORT).show()
    }

    val entries = listOf(
        CommunityEntry(
            title = "DISCORD OFICIAL",
            subtitle = "Entre no servidor oficial da comunidade HELLDIVERS-BR.",
            imageRes = R.drawable.discord_oficial,
            url = DISCORD_OFFICIAL_URL,
        ),
        CommunityEntry(
            title = "DISCORD DO DESENVOLVEDOR",
            subtitle = "$DISCORD_DEVELOPER · feedback, bugs e sugestões.",
            imageRes = R.drawable.developer_daryl,
            copyText = DISCORD_DEVELOPER,
            highlight = true,
        ),
        CommunityEntry(
            title = "GRUPO DO WHATSAPP",
            subtitle = "Entre no grupo da comunidade HELLDIVERS-BR.",
            imageRes = R.drawable.logo_whatsapp,
            url = WHATSAPP_URL,
        ),
        CommunityEntry(
            title = "CANAL NO YOUTUBE",
            subtitle = "Luis Play Games Oficial.",
            imageRes = R.drawable.youtube_canal,
            url = YOUTUBE_URL,
        ),
        CommunityEntry(
            title = "CANAL PARCEIRO",
            subtitle = "HELLDIVERS — Pirata Perdido.",
            imageRes = R.drawable.canal_parceiro,
            url = PARTNER_CHANNEL_URL,
        ),
        CommunityEntry(
            title = "SITE HELLDIVERS-BR",
            subtitle = "Abrir o portal completo no navegador.",
            imageRes = R.drawable.ic_launcher_foreground,
            url = SITE_URL,
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
                    Image(
                        painter = painterResource(R.drawable.developer_daryl),
                        contentDescription = "Foto do desenvolvedor DarylDixon_19",
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(HD.SurfaceHigh)
                            .border(1.dp, HD.Yellow.copy(alpha = 0.72f), CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "DESENVOLVEDOR · HELLDIVERS-BR",
                            color = HD.TextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.0.sp,
                        )
                        Text("DarylDixon_19", color = HD.Text, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text(
                            "Projeto brasileiro independente feito para a comunidade.",
                            color = HD.TextDim,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
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

        item { SettingsSectionLabel("ALERTAS DE GUERRA") }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (invasionAlerts) HD.Red.copy(alpha = 0.055f) else HD.Surface)
                    .border(1.dp, if (invasionAlerts) HD.Red.copy(alpha = 0.72f) else HD.Border, RoundedCornerShape(12.dp))
                    .clickable { setInvasionAlerts(!invasionAlerts) }
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(HD.SurfaceHigh), contentAlignment = Alignment.Center) {
                    Text("!", color = if (invasionAlerts) HD.Red else HD.TextMuted, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("ALERTA DE INVASÃO PLANETÁRIA", color = HD.Text, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (invasionAlerts) "Ativo · alerta de novas defesas. Com o app aberto, a leitura acompanha a telemetria; em segundo plano, o Android verifica periodicamente."
                        else "Receba um aviso quando uma nova defesa da Super Terra for detectada.",
                        color = HD.TextDim,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                    )
                }
                Switch(checked = invasionAlerts, onCheckedChange = ::setInvasionAlerts)
            }
        }

        item { SettingsSectionLabel("COMUNIDADE & FEEDBACK") }

        entries.forEach { entry ->
            item(key = entry.title) {
                val action: (() -> Unit)? = entry.url?.let { url ->
                    { openUrl(url) }
                } ?: entry.copyText?.let { text ->
                    { copyText(text) }
                }
                SettingsEntry(entry = entry, onClick = action)
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
        Image(
            painter = painterResource(entry.imageRes),
            contentDescription = entry.title,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(HD.SurfaceHigh),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.title, color = if (entry.highlight) HD.Yellow else HD.Text, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(entry.subtitle, color = HD.TextDim, fontSize = 10.sp, lineHeight = 14.sp)
        }
        Text(
            when {
                entry.copyText != null -> "COPIAR"
                enabled -> "ABRIR ↗"
                else -> "EM BREVE"
            },
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
