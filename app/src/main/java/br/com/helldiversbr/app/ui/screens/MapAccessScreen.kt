package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.DssReading
import br.com.helldiversbr.app.ui.HomeState
import br.com.helldiversbr.app.ui.theme.HD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapAccessScreen(home: HomeState, contentPadding: PaddingValues, onOpenSite: () -> Unit) {
    var mapOpen by rememberSaveable { mutableStateOf(false) }
    var dssOpen by rememberSaveable { mutableStateOf(false) }
    if (mapOpen) {
        GalaxyScreen(home, contentPadding, onOpenSite)
        return
    }
    val data = when (home) {
        is HomeState.Ready -> home.data
        is HomeState.Error -> home.last
        else -> null
    }
    Column(Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("NAVEGAÇÃO GALÁCTICA", color = HD.Gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Escolha o terminal", color = HD.TextDim, fontSize = 13.sp)
        MapTerminalOption("MAPA GALÁCTICO", "Planetas, rotas e frentes de batalha", HD.SignalBlue) { mapOpen = true }
        MapTerminalOption("DSS", "Estação Espacial da Democracia", HD.Gold) { dssOpen = true }
    }
    if (dssOpen) ModalBottomSheet(onDismissRequest = { dssOpen = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color(0xFF090909)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("ESTAÇÃO ESPACIAL DA DEMOCRACIA", Modifier.weight(1f), color = HD.Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { dssOpen = false }) { Text("FECHAR", color = HD.Text) }
            }
            DssPanel(data?.dss ?: DssReading(), data?.planetCatalog.orEmpty(), data?.campaigns.orEmpty())
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MapTerminalOption(title: String, detail: String, accent: Color, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0C141C), border = BorderStroke(1.dp, accent.copy(alpha = .65f))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = accent, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = HD.TextDim, fontSize = 13.sp)
        }
    }
}
