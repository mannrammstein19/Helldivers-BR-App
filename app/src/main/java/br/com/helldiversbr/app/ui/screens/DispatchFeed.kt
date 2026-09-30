package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.Dispatch
import br.com.helldiversbr.app.ui.theme.HD

@Composable
fun DispatchFeed(dispatches: List<Dispatch>) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("DESPACHOS RECENTES", color = HD.Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
        dispatches.take(if (expanded) dispatches.size else 3).forEach { dispatch ->
            key(dispatch.id.toString()) { DispatchCard(dispatch) }
        }
        if (dispatches.size > 3) TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(if (expanded) "MOSTRAR APENAS 3" else "VER MAIS ${dispatches.size - 3} DESPACHOS", color = HD.Yellow)
        }
    }
}
