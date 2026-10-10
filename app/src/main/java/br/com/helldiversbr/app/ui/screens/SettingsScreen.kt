package br.com.helldiversbr.app.ui.screens

import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.testTag
import br.com.helldiversbr.app.notifications.AlertGroup
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import coil.compose.SubcomposeAsyncImage

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
    dynamicNumbers: Boolean = true,
    onDynamicNumbers: (Boolean) -> Unit = {},
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
            title = "Helldivers BR",
            subtitle = "Servidor oficial da comunidade HELLDIVERS-BR.",
            imageRes = R.drawable.discord_oficial,
            url = DISCORD_OFFICIAL_URL,
        ),
        CommunityEntry(
            title = DISCORD_DEVELOPER,
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
            title = "Luis Games",
            subtitle = "Luis Play Games Oficial.",
            imageRes = R.drawable.youtube_canal,
            url = YOUTUBE_URL,
        ),
        CommunityEntry(
            title = "Pirata Perdido",
            subtitle = "HELLDIVERS — Pirata Perdido.",
            imageRes = R.drawable.canal_parceiro,
            url = PARTNER_CHANNEL_URL,
        ),
    )

    val alertsEnabled = NotificationPreferences.isMasterEnabled(context)
    val blocked = alertsEnabled && !NotificationManagerCompat.from(context).areNotificationsEnabled()
    val activeTypes = AlertGroup.entries.sumOf { NotificationPreferences.enabledCount(context, it) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CONFIGURAÇÕES", color = HD.Text, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("COMUNIDADE · PROJETO · PREFERÊNCIAS", color = HD.Yellow, fontSize = 8.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
                }
                Text(BuildConfig.VERSION_NAME, color = HD.TextMuted, fontSize = 10.sp)
            }
        }
        item {
            SettingsNotificationCard(alertsEnabled, blocked, activeTypes, onOpenNotifications)
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(HD.Surface).border(1.dp, HD.Yellow.copy(alpha = .7f), RoundedCornerShape(18.dp))
                .clickable { copyText(DISCORD_DEVELOPER, "Discord do desenvolvedor copiado: $DISCORD_DEVELOPER") }
                .padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.developer_daryl), "Desenvolvedor DarylDixon_19",
                        Modifier.size(76.dp).clip(CircleShape).border(1.dp, HD.Yellow, CircleShape), contentScale = ContentScale.Crop)
                    Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DESENVOLVEDOR · HELLDIVERS-BR", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("DarylDixon_19", color = HD.Text, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text("Projeto brasileiro independente feito para a comunidade.", color = HD.TextDim, fontSize = 11.sp, lineHeight = 15.sp)
                        Text("COPIAR CONTATO DO DISCORD", color = HD.Yellow, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { SettingsSectionLabel("COMUNIDADE") }
        item {
            SettingsCommunityGrid("DISCORD", "logo-discord", "DC", listOf(entries[0], entries[1])) { entry ->
                entry.url?.let(::openUrl) ?: entry.copyText?.let { copyText(it, "Discord do desenvolvedor copiado: $it") }
            }
        }
        item {
            SettingsCommunityGrid("YOUTUBE", "logo-youtube", "YT", listOf(entries[3], entries[4])) { entry ->
                entry.url?.let(::openUrl)
            }
        }
        item {
            SettingsCommunityGrid("WHATSAPP", "logo-whatsapp", "WA", listOf(entries[2])) { entry ->
                entry.url?.let(::openUrl)
            }
        }
        item {
            SettingsCompactCard {
                SettingsTextRow("↗", "COMPARTILHAR HD2 BR", "Convide seus amigos para a comunidade.", "COMPARTILHAR", ::shareApp, iconFile = "icone-compartilhar")
                HorizontalDivider(color = HD.BorderSoft)
                SettingsTextRow("◎", "SITE HELLDIVERS-BR", SITE_URL.removePrefix("https://").removeSuffix("/"), "", { openUrl(SITE_URL) }, iconFile = "icone-site")
            }
        }
        item { SettingsSectionLabel("PREFERÊNCIAS") }
        item {
            SettingsCompactCard {
                SettingsSwitchRow("↗", "CONTADORES DINÂMICOS", "Movimento dos números entre atualizações", dynamicNumbers, onDynamicNumbers, iconFile = "icone-contadores")
                HorizontalDivider(color = HD.BorderSoft)
                SettingsThemeRow(themeMode, onThemeMode)
            }
        }
        item { SettingsSectionLabel("ATUALIZAÇÕES") }
        item {
            SettingsCompactCard {
                SettingsTextRow("↻", "VERIFICAR ATUALIZAÇÕES", updateMessage, if (checkingUpdate) "..." else "VERIFICAR", ::checkUpdates, iconFile = "icone-atualizacao")
                availableUpdate?.let { version ->
                    HorizontalDivider(color = HD.BorderSoft)
                    SettingsTextRow("↓", "BAIXAR ${version.versionName}", version.notes.ifBlank { "Nova versão disponível." }, "BAIXAR ↗",
                        { openUrl(version.apkUrl) }, accent = true)
                }
                HorizontalDivider(color = HD.BorderSoft)
                SettingsSwitchRow("⌁", "VERIFICAR AO ABRIR O APP", "Consulta automática ao iniciar o HD2 BR.", autoUpdates,
                    { enabled -> autoUpdates = enabled; UpdatePreferences.setAutoCheckEnabled(context, enabled) }, iconFile = "icone-verificar-inicio")
            }
        }
        item { SettingsSectionLabel("SOBRE") }
        item {
            SettingsCompactCard {
                Text("HELLDIVERS-BR reúne informações, ferramentas e acompanhamento da Guerra Galáctica em português. HELLDIVERS e HELLDIVERS 2 pertencem aos seus respectivos detentores; este aplicativo não é oficial.",
                    color = HD.TextDim, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(14.dp))
            }
        }
        item {
            Text("HELLDIVERS-BR // ${BuildConfig.VERSION_NAME}", color = HD.TextMuted, fontSize = 8.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun SettingsNotificationCard(active: Boolean, blocked: Boolean, count: Int, onOpen: () -> Unit) {
    val accent = if (active) HD.Red else HD.Border
    Row(Modifier.fillMaxWidth().testTag("settings-notifications").clip(RoundedCornerShape(14.dp))
        .background(if (active) HD.Red.copy(alpha = .09f) else HD.Surface)
        .border(if (active) 1.5.dp else 1.dp, accent, RoundedCornerShape(14.dp))
        .clickable(onClick = onOpen).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(HD.SurfaceHigh), contentAlignment = Alignment.Center) {
            Text("!", color = if (active) HD.Red else HD.TextMuted, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("NOTIFICAÇÕES", color = HD.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(when {
                blocked -> "Ligadas no app · bloqueadas pelo Android. Toque para revisar."
                active && count == 0 -> "Ligadas · nenhum tipo selecionado. Toque para configurar."
                active -> "Ativadas · $count tipos de alerta. Personalizar canais ›"
                else -> "Desativadas · toque para configurar seus alertas."
            }, color = if (active) HD.Red else HD.TextDim, fontSize = 10.sp, lineHeight = 14.sp)
        }
        Text("ABRIR ›", color = HD.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsCommunityGrid(title: String, logoFile: String, fallback: String, entries: List<CommunityEntry>, onEntry: (CommunityEntry) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(HD.Surface)
        .border(1.dp, HD.BorderSoft, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SettingsCustomIcon(logoFile, fallback, Modifier.size(24.dp))
            Text(title, color = HD.Text, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            entries.forEach { entry ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onEntry(entry) }
                    .padding(horizontal = 6.dp, vertical = 4.dp).heightIn(min = 84.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Image(painterResource(entry.imageRes), null,
                        Modifier.size(58.dp).clip(CircleShape).background(HD.SurfaceHigh)
                            .border(1.dp, if (entry.highlight) HD.Yellow else HD.Border, CircleShape), contentScale = ContentScale.Crop)
                    Text(entry.title,
                        color = if (entry.highlight) HD.Yellow else HD.Text, fontSize = 10.sp,
                        lineHeight = 13.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
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
    iconFile: String? = null,
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
            if (iconFile != null) SettingsCustomIcon(iconFile, symbol, Modifier.fillMaxSize(), ContentScale.Crop)
            else Text(symbol, color = if (accent) HD.Yellow else HD.TextDim, fontSize = 17.sp, fontWeight = FontWeight.Black)
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
    iconFile: String? = null,
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
            if (iconFile != null) SettingsCustomIcon(iconFile, symbol, Modifier.fillMaxSize(), ContentScale.Crop)
            else Text(symbol, color = if (checked) HD.Yellow else HD.TextMuted, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
        Column(Modifier.weight(1f).padding(start = 11.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = HD.Text, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = HD.TextDim, fontSize = 9.5.sp, lineHeight = 13.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(text, color = HD.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.0.sp)
}

/** Ícones locais opcionais: ausência do arquivo mantém um símbolo legível. */
@Composable
private fun SettingsCustomIcon(fileName: String, fallback: String, modifier: Modifier = Modifier, scale: ContentScale = ContentScale.Fit) {
    SubcomposeAsyncImage(
        model = "file:///android_asset/icones-configuracoes/$fileName.png",
        contentDescription = null,
        modifier = modifier,
        contentScale = scale,
        loading = { SettingsIconFallback(fallback) },
        error = { SettingsIconFallback(fallback) },
    )
}

@Composable
private fun SettingsIconFallback(symbol: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(symbol, color = HD.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsThemeRow(mode: HdThemeMode, onSelect: (HdThemeMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        SettingsTextRow("◐", "TEMA DO APLICATIVO", mode.label, "ALTERAR", { open = true }, iconFile = "icone-tema")
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            HdThemeMode.entries.forEach { theme ->
                DropdownMenuItem(text = { Text(theme.label + if (theme == mode) " ✓" else "") },
                    onClick = { onSelect(theme); open = false })
            }
        }
    }
}
