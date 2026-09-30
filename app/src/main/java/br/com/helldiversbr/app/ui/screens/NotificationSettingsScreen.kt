package br.com.helldiversbr.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import br.com.helldiversbr.app.notifications.AlertGroup
import br.com.helldiversbr.app.notifications.AlertType
import br.com.helldiversbr.app.notifications.NotificationPreferences
import br.com.helldiversbr.app.notifications.WarAlertManager
import br.com.helldiversbr.app.ui.theme.HD

private data class AlertGroupVisual(
    val group: AlertGroup,
    val title: String,
    val icon: String,
    val detail: String = "",
)

private val alertGroups = listOf(
    AlertGroupVisual(AlertGroup.PLANETS, "PLANETAS", "◉", "Campanhas, ataques e mudanças de controle"),
    AlertGroupVisual(AlertGroup.REGIONS, "REGIÕES", "⌗", "Alertas regionais quando a telemetria estiver disponível"),
    AlertGroupVisual(AlertGroup.NEWS, "NOTÍCIAS", "▤", "Novos despachos do Ministério da Verdade"),
    AlertGroupVisual(AlertGroup.MAJOR_ORDER, "ORDENS PRINCIPAIS", "▣", "Nova ordem, progresso e conclusão"),
    AlertGroupVisual(AlertGroup.DSS, "ESTAÇÃO ESPACIAL DA DEMOCRACIA", "✣", "Realocação, ações táticas e votação"),
)

@Composable
fun NotificationSettingsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var master by remember(context) { mutableStateOf(NotificationPreferences.isMasterEnabled(context)) }
    val states = remember(context) {
        mutableStateMapOf<AlertType, Boolean>().apply {
            AlertType.entries.forEach { put(it, NotificationPreferences.isEnabled(context, it)) }
        }
    }

    fun applyMaster(enabled: Boolean) {
        master = enabled
        WarAlertManager.setMasterEnabled(context, enabled)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) applyMaster(true)
        else {
            master = false
            Toast.makeText(context, "Permissão de notificações não concedida.", Toast.LENGTH_SHORT).show()
        }
    }

    fun setMaster(enabled: Boolean) {
        if (!enabled) {
            applyMaster(false)
            return
        }
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else applyMaster(true)
    }

    fun setAlert(type: AlertType, enabled: Boolean) {
        states[type] = enabled
        NotificationPreferences.setEnabled(context, type, enabled)
    }

    fun setGroup(group: AlertGroup, enabled: Boolean) {
        NotificationPreferences.types(group).forEach { states[it] = enabled }
        NotificationPreferences.setGroupEnabled(context, group, enabled)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HD.Surface)
                        .border(1.dp, HD.Border, RoundedCornerShape(12.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = HD.Yellow, modifier = Modifier.size(21.dp))
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("NOTIFICAÇÕES", color = HD.Text, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("MINISTÉRIO DA VERDADE · CANAIS DE ALERTA", color = HD.Yellow, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                }
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (master) HD.Green.copy(alpha = .055f) else HD.Surface)
                    .border(1.dp, if (master) HD.Green.copy(alpha = .50f) else HD.Border, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(HD.SurfaceHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = if (master) HD.Green else HD.TextMuted, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                        Text("ATIVAR NOTIFICAÇÕES", color = HD.Text, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text("Interruptor principal de todos os alertas escolhidos abaixo.", color = HD.TextDim, fontSize = 10.5.sp, lineHeight = 15.sp)
                    }
                    Switch(checked = master, onCheckedChange = ::setMaster)
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (master) HD.Green.copy(alpha = .11f) else HD.SurfaceHigh)
                        .border(1.dp, if (master) HD.Green.copy(alpha = .42f) else HD.Border, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(if (master) HD.Green else HD.TextMuted))
                    Text(
                        if (master) "  UPLINK ATIVO" else "  UPLINK DESATIVADO",
                        color = if (master) HD.Green else HD.TextMuted,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .8.sp,
                    )
                }
            }
        }

        item {
            Text("CANAIS DE ALERTA", color = HD.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
        }

        alertGroups.forEach { visual ->
            item(key = visual.group.name) {
                val types = NotificationPreferences.types(visual.group)
                val enabledCount = types.count { states[it] == true }
                val groupEnabled = enabledCount == types.size && types.isNotEmpty()
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(HD.Surface)
                        .border(1.dp, HD.Border, RoundedCornerShape(15.dp)),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(HD.Yellow.copy(alpha = .08f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(visual.icon, color = HD.Yellow, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        }
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(visual.title, color = HD.Text, fontSize = 10.5.sp, fontWeight = FontWeight.Black, letterSpacing = .25.sp)
                            if (visual.detail.isNotBlank()) Text(visual.detail, color = HD.TextMuted, fontSize = 8.5.sp, lineHeight = 12.sp)
                        }
                        Text(
                            "$enabledCount/${types.size}",
                            color = HD.TextDim,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(HD.SurfaceHigh)
                                .border(1.dp, HD.Border, RoundedCornerShape(50))
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Switch(checked = groupEnabled, onCheckedChange = { setGroup(visual.group, it) })
                    }
                    types.forEach { type ->
                        HorizontalDivider(color = HD.BorderSoft)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { setAlert(type, !(states[type] ?: false)) }
                                .padding(start = 16.dp, end = 13.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(type.label, color = if (master) HD.Text else HD.TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = states[type] ?: false,
                                onCheckedChange = { setAlert(type, it) },
                                enabled = true,
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(HD.Yellow.copy(alpha = .055f))
                    .border(1.dp, HD.Yellow.copy(alpha = .55f), RoundedCornerShape(14.dp))
                    .clickable {
                        runCatching {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = HD.Yellow, modifier = Modifier.size(18.dp))
                Text("ABRIR CONFIGURAÇÕES DO SISTEMA", color = HD.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = .5.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
