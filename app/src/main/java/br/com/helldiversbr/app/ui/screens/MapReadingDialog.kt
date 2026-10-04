package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.HomeData
import br.com.helldiversbr.app.ui.GalaxyState
import br.com.helldiversbr.app.ui.theme.HD

@Composable
internal fun MapReadingDialog(
    state: GalaxyState,
    data: HomeData?,
    warning: Boolean,
    onDismiss: () -> Unit,
    onPreferences: () -> Unit,
    onRefresh: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HD.Surface,
        title = { Text("LEITURA DO MAPA", color = HD.Yellow, fontWeight = FontWeight.Bold) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("ENTENDI") } },
        dismissButton = { TextButton(onClick = onPreferences) { Text("PREFERÊNCIAS") } },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(when {
                    state.loading -> "Atualizando telemetria"
                    warning -> "Exibindo as últimas leituras disponíveis"
                    state.planets.isEmpty() -> "Aguardando a primeira leitura"
                    else -> "Telemetria recebida"
                }, color = if (warning) HD.Gold else HD.Text, fontWeight = FontWeight.Bold)
                ReadingBlock("PLANETAS", state.telemetrySource, state.updatedAtMillis,
                    "${state.planets.size} planetas na leitura")
                data?.let {
                    ReadingBlock("FRENTES ATIVAS",
                        if ("campanhas" in it.staleSources) "cache" else it.campaignTelemetrySource,
                        it.campaignReadAtMillis, "${it.campaigns.size} frentes")
                }
                ReadingBlock("ESTAÇÃO DEMOCRACIA",
                    if (state.dss.stale) "cache" else state.dss.source,
                    state.dss.fetchedAtMillis,
                    if (state.dss.stale) "Leitura preservada" else if (state.dss.isLive) "Leitura atual" else "Sem confirmação atual")
                if (warning) Text("Os horários acima pertencem às leituras recebidas. As animações continuam mesmo enquanto os dados aguardam atualização.",
                    color = HD.Gold, fontSize = 12.sp)
                TextButton(onClick = onRefresh, enabled = !state.loading) { Text("ATUALIZAR TELEMETRIA") }
                HorizontalDivider(color = HD.TextDim.copy(alpha = .25f))
                Text("LEGENDA", color = HD.Yellow, fontWeight = FontWeight.Bold)
                listOf("human" to "Super Terra", "automaton" to "Autômatos",
                    "terminid" to "Terminídeos", "illuminate" to "Iluminados").forEach { (key, name) ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SiteImage(key, name, Modifier.size(22.dp), tint = mapColor(key))
                        Text(name, color = mapColor(key), fontSize = 13.sp)
                    }
                }
                Text("Defesa e invasão têm anéis independentes. As setas indicam os vínculos de invasão conhecidos. Naves ilustram presenças confirmadas, sem representar a quantidade de unidades.",
                    color = HD.TextDim, fontSize = 12.sp)
                Text("COMO USAR", color = HD.Yellow, fontWeight = FontWeight.Bold)
                Text("Toque num planeta para abrir sua ficha. Arraste para mover e use dois dedos para ampliar. BUSCAR localiza planetas; FILTROS escolhe facções e camadas.",
                    color = HD.TextDim, fontSize = 12.sp)
                Text("PREFERÊNCIAS permite escolher Completo ou Limpo e ajustar nomes, Helldivers, porcentagens, presenças, naves, faixas e movimento. Suas escolhas ficam salvas no aparelho.",
                    color = HD.TextDim, fontSize = 12.sp)
                Text("As condições planetárias indicam fenômenos associados ao planeta; não confirmam que estão ocorrendo na missão atual.",
                    color = HD.TextMuted, fontSize = 11.sp)
            }
        },
    )
}

@Composable
private fun ReadingBlock(title: String, source: String, millis: Long?, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = HD.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(mapReadingSource(source), color = if (source == "cache") HD.Gold else HD.Text, fontSize = 13.sp)
        Text(mapReadingTime(millis), color = HD.TextDim, fontSize = 12.sp)
        Text(detail, color = HD.TextMuted, fontSize = 11.sp)
    }
}
