package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.CentralApi
import br.com.helldiversbr.app.data.HomeData
import br.com.helldiversbr.app.ui.GalaxyState
import br.com.helldiversbr.app.ui.theme.HD

@Composable
internal fun MapReadingPanel(
    state: GalaxyState,
    data: HomeData?,
    warning: Boolean,
    onDismiss: () -> Unit,
    onPreferences: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val assignments = CentralApi.latest("/api/v1/assignments")
    val dispatches = CentralApi.latest("/api/v1/dispatches")
    val planets = CentralApi.latest("/api/v1/planets")
    val regions = state.planets.flatMap { it.regions }.filter { it.telemetryReadAtMillis > 0L }
    val regionTime = regions.maxOfOrNull { it.telemetryReadAtMillis }
    val regionSources = regions.map { it.telemetrySource }.distinct()
    val regionSource = if (regionSources.size == 1) regionSources.first() else if (regionSources.isNotEmpty()) "mixed" else state.telemetrySource
    val pending = warning || assignments?.stale == true || dispatches?.stale == true || state.dss.stale || regions.any { it.telemetryStale }
    Surface(modifier = modifier, color = Color(0xFF0E181F), shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF596572)), shadowElevation = 8.dp) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(when {
                    state.loading -> "Atualizando · dados preservados"
                    pending -> "Atualização pendente · dados preservados"
                    state.planets.isEmpty() -> "Aguardando a primeira leitura"
                    else -> "Telemetria atualizada"
                }, modifier = Modifier.weight(1f), color = Color(0xFFD2D8DE),
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
                TextButton(onClick = onDismiss, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(28.dp)) {
                    Text("×", color = HD.TextDim, fontSize = 22.sp)
                }
            }
            ReadingBlock("Ordem Maior", assignments?.source ?: if(data?.telemetrySource == "cache") "cache" else "unavailable",
                assignments?.time, assignments?.stale == true || data?.telemetrySource == "cache")
            ReadingBlock("DSS", state.dss.source, state.dss.fetchedAtMillis, state.dss.stale)
            ReadingBlock("Planetas e regiões", regionSource, regionTime ?: planets?.time ?: state.updatedAtMillis,
                regions.any { it.telemetryStale } || planets?.stale == true || state.telemetrySource == "cache")
            ReadingBlock("Planetas", planets?.source ?: state.telemetrySource, planets?.time ?: state.updatedAtMillis,
                planets?.stale == true || state.telemetrySource == "cache")
            ReadingBlock("Despachos", dispatches?.source ?: if(data?.telemetrySource == "cache") "cache" else "unavailable",
                dispatches?.time, dispatches?.stale == true || data?.telemetrySource == "cache")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = onRefresh, enabled = !state.loading,
                    shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(if(state.loading) "Atualizando…" else "Tentar atualizar", fontSize = 12.sp, color = HD.Text)
                }
                TextButton(onClick = onPreferences, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text("Preferências", color = HD.Yellow, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ReadingBlock(title: String, source: String, millis: Long?, saved: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Color(0xFFFFEB27), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(mapReadingTime(millis), color = Color(0xFFB5DBCE), fontSize = 13.sp)
        Text(mapReadingSource(source), color = Color(0xFF82BFF2), fontSize = 13.sp)
        if (saved) Text("Última leitura salva · aguardando atualização", color = Color(0xFFE2C686), fontSize = 12.sp)
        HorizontalDivider(color = Color(0xFF394650), modifier = Modifier.padding(top = 4.dp))
    }
}
