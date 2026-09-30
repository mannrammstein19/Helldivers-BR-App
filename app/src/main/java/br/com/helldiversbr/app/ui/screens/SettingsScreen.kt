package br.com.helldiversbr.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.rememberCoroutineScope
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
import br.com.helldiversbr.app.notifications.NotificationPreferences
import br.com.helldiversbr.app.ui.theme.HD
import br.com.helldiversbr.app.ui.theme.HdThemeMode
import br.com.helldiversbr.app.update.RemoteVersion
import br.com.helldiversbr.app.update.UpdateCheckResult
import br.com.helldiversbr.app.update.UpdateChecker
import br.com.helldiversbr.app.update.UpdatePreferences
import kotlinx.coroutines.launch

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
    onOpenNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var autoUpdates by remember(context) { mutableStateOf(UpdatePreferences.isAutoCheckEnabled(context)) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("TOQUE PARA VERIFICAR") }
    var availableUpdate by remember { mutableStateOf<RemoteVersion?>(null) }

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { Toast.makeText(context, "Não foi possível abrir este link.", Toast.LENGTH_SHORT).show() }
    }

    fun copyText(text: String, message: String = "Copiado para a área de transferência.") {
        clipboard.setText(AnnotatedString(text))
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun shareApp() {
        val text = "Conheça o HD2 BR — informações, Guerra Galáctica, Ordem Maior, mapa e arsenal em português.\n$SITE_URL"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "HD2 BR")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Compartilhar HD2 BR")) }
    }

    fun checkUpdates() {
        if (checkingUpdate) return
        checkingUpdate = true
        updateMessage = "VERIFICANDO..."
        scope.launch {
            when (val result = UpdateChecker.checkResult()) {
                is UpdateCheckResult.Available -> {
                    availableUpdate = result.version
                    updateMessage = "VERSÃO ${result.version.versionName} DISPONÍVEL"
                }
                UpdateCheckResult.Latest -> {
                    availableUpdate = null
                    updateMessage = "VOCÊ ESTÁ NA VERSÃO MAIS RECENTE"
                }
                is UpdateCheckResult.Failed -> {
                    availableUpdate = null
                    updateMessage = "NÃO FOI POSSÍVEL VERIFICAR AGORA"
                }
            }
            checkingUpdate = false
        }
    }

    val entries = listOf(
        CommunityEntry(
            title = "DISCORD OFICIAL",
            subtitle = "Servidor oficial da comunidade HELLDIVERS-BR.",
            imageRes = R.drawable.discord_oficial,
            url = DISCORD_OFFICIAL_URL,
        ),
        CommunityEntry(
            title = "FEEDBACK / DESENVOLVEDOR",
            subtitle = "$DISCORD_DEVELOPER · bugs e sugestões.",
            imageRes = R.drawable.developer_daryl,
            copyText = DISCORD_DEVELOPER,
            highlight = true,
        ),
        CommunityEntry(
            title = "GRUPO DO WHATSAPP",
            subtitle = "Comunidade brasileira do projeto.",
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
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("CONFIGURAÇÕES", color = HD.Text, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("HD2 BR · CENTRAL DE COMANDO", color = HD.Yellow, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("VERSÃO DO APP", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    Text(BuildConfig.VERSION_NAME, color = HD.Text, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        item { SettingsSectionLabel("ATUALIZAÇÕES") }

        item {
            SettingsCompactCard {
                SettingsTextRow(
                    symbol = "↻",
                    title = "VERIFICAR ATUALIZAÇÕES",
                    subtitle = updateMessage,
                    action = if (checkingUpdate) "..." else "VERIFICAR",
                    onClick = ::checkUpdates,
                )
                if (availableUpdate != null) {
                    HorizontalDivider(color = HD.BorderSoft)
                    SettingsTextRow(
                        symbol = "↓",
                        title = "BAIXAR ${availableUpdate!!.versionName}",
                        subtitle = availableUpdate!!.notes.ifBlank { "Nova versão disponível para download." },
                        action = "BAIXAR ↗",
                        onClick = { openUrl(availableUpdate!!.apkUrl) },
                        accent = true,
                    )
                }
                HorizontalDivider(color = HD.BorderSoft)
                SettingsSwitchRow(
                    symbol = "⌁",
                    title = "VERIFICAR AO ABRIR O APP",
                    subtitle = "Consulta automática leve ao iniciar o HD2 BR.",
                    checked = autoUpdates,
                    onCheckedChange = {
                        autoUpdates = it
                        UpdatePreferences.setAutoCheckEnabled(context, it)
                    },
                )
            }
        }

        item { SettingsSectionLabel("PREFERÊNCIAS") }

        item {
            SettingsCompactCard {
                val alertStatus = if (NotificationPreferences.isMasterEnabled(context)) "ATIVADAS · PERSONALIZAR CANAIS" else "DESATIVADAS · CONFIGURAR"
                SettingsTextRow(
                    symbol = "!",
                    title = "NOTIFICAÇÕES",
                    subtitle = alertStatus,
                    action = "ABRIR ›",
                    onClick = onOpenNotifications,
                    accent = NotificationPreferences.isMasterEnabled(context),
                )
                HorizontalDivider(color = HD.BorderSoft)
                SettingsSwitchRow(
                    symbol = if (themeMode == HdThemeMode.MERIDIA) "◉" else "◐",
                    title = "TEMA DO APLICATIVO",
                    subtitle = if (themeMode == HdThemeMode.MERIDIA) "Meridian" else "Padrão HELLDIVERS-BR",
                    checked = themeMode == HdThemeMode.MERIDIA,
                    onCheckedChange = { enabled -> onThemeMode(if (enabled) HdThemeMode.MERIDIA else HdThemeMode.DEFAULT) },
                )
            }
        }

        item { SettingsSectionLabel("COMUNIDADE") }

        item {
            SettingsCompactCard {
                SettingsTextRow(
                    symbol = "↗",
                    title = "COMPARTILHAR HD2 BR",
                    subtitle = "Envie o projeto pelo WhatsApp, Telegram, Discord ou outro aplicativo.",
                    action = "COMPARTILHAR",
                    onClick = ::shareApp,
                )
                HorizontalDivider(color = HD.BorderSoft)
                SettingsTextRow(
                    symbol = "◎",
                    title = "SITE HELLDIVERS-BR",
                    subtitle = SITE_URL.removePrefix("https://").removeSuffix("/"),
                    action = "ABRIR ↗",
                    onClick = { openUrl(SITE_URL) },
                )
            }
        }

        entries.forEach { entry ->
            item(key = entry.title) {
                val action: (() -> Unit)? = entry.url?.let { url -> { openUrl(url) } }
                    ?: entry.copyText?.let { text -> { copyText(text, "Discord do desenvolvedor copiado: $text") } }
                SettingsCommunityRow(entry, action)
            }
        }

        item { SettingsSectionLabel("SOBRE") }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(HD.Surface)
                    .border(1.dp, HD.Border, RoundedCornerShape(15.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.developer_daryl),
                        contentDescription = "Foto do desenvolvedor DarylDixon_19",
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(HD.SurfaceHigh)
                            .border(1.dp, HD.Yellow.copy(alpha = .72f), CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("DESENVOLVEDOR", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                        Text("DarylDixon_19", color = HD.Text, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text("Projeto brasileiro independente feito para a comunidade.", color = HD.TextDim, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
                HorizontalDivider(color = HD.BorderSoft)
                Text(
                    "HELLDIVERS-BR reúne informações, ferramentas e acompanhamento da Guerra Galáctica em português. HELLDIVERS e HELLDIVERS 2 pertencem aos seus respectivos detentores; este aplicativo não é oficial.",
                    color = HD.TextDim,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                )
            }
        }

        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalDivider(color = HD.Border)
                Spacer(Modifier.height(10.dp))
                Text(
                    "HELLDIVERS-BR // ${BuildConfig.VERSION_NAME}",
                    color = HD.TextMuted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = .7.sp,
                )
            }
        }
    }
}

@Composable
private fun SettingsCompactCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(HD.Surface)
            .border(1.dp, HD.Border, RoundedCornerShape(15.dp)),
        content = content,
    )
}

@Composable
private fun SettingsTextRow(
    symbol: String,
    title: String,
    subtitle: String,
    action: String,
    onClick: () -> Unit,
    accent: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(if (accent) HD.Yellow.copy(alpha = .10f) else HD.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, color = if (accent) HD.Yellow else HD.TextDim, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
        Column(Modifier.weight(1f).padding(start = 11.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = if (accent) HD.Yellow else HD.Text, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = HD.TextDim, fontSize = 9.5.sp, lineHeight = 13.sp, maxLines = 2)
        }
        Text(action, color = HD.Yellow, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = .4.sp)
    }
}

@Composable
private fun SettingsSwitchRow(
    symbol: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(HD.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, color = if (checked) HD.Yellow else HD.TextMuted, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
        Column(Modifier.weight(1f).padding(start = 11.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = HD.Text, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = HD.TextDim, fontSize = 9.5.sp, lineHeight = 13.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsCommunityRow(entry: CommunityEntry, onClick: (() -> Unit)?) {
    val enabled = onClick != null
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (entry.highlight) HD.Yellow.copy(alpha = .045f) else HD.Surface)
            .border(1.dp, if (entry.highlight) HD.Yellow.copy(alpha = .55f) else HD.Border, RoundedCornerShape(14.dp))
            .then(if (enabled) Modifier.clickable { onClick?.invoke() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(entry.imageRes),
            contentDescription = entry.title,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(HD.SurfaceHigh),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.weight(1f).padding(start = 11.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.title, color = if (entry.highlight) HD.Yellow else HD.Text, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
            Text(entry.subtitle, color = HD.TextDim, fontSize = 9.5.sp, lineHeight = 13.sp)
        }
        Text(
            if (entry.copyText != null) "COPIAR" else "ABRIR ↗",
            color = HD.Yellow,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(text, color = HD.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.0.sp)
}
