package br.com.helldiversbr.app.ui.screens

import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.R
import br.com.helldiversbr.app.data.SteamReading
import br.com.helldiversbr.app.data.SteamNewsRepository
import br.com.helldiversbr.app.ui.theme.HD
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SteamNewsPanel(reading: SteamReading) {
    val context = LocalContext.current
    fun open(url: String) {
        if (SteamNewsRepository.validUrl(url)) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
    HdCard(accent = HD.SignalBlue, contentSpacing = 4.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(R.drawable.steam_logo, null, Modifier.size(27.dp))
            Text("STEAM", color = HD.Text, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 9.dp).weight(1f))
            Text("ATUALIZAÇÕES ↗", color = HD.SignalBlue, fontSize = 8.sp,
                modifier = Modifier.clickable { open(SteamNewsRepository.NEWS_URL) }.padding(vertical = 10.dp))
        }
        if (reading.items.isEmpty()) Text(if (reading.loading) "Buscando notícias…" else reading.message ?: "Nenhuma notícia disponível.", color = HD.TextDim, fontSize = 11.sp)
        reading.items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = HD.BorderSoft)
            Column(Modifier.fillMaxWidth().clickable { open(item.url) }.padding(vertical = 3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                LocalizedText(item.title, color = HD.Text, fontSize = 12.sp, maxLines = 3)
                Text(if (item.publishedAtMillis > 0L) SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(item.publishedAtMillis)) else "Data não informada",
                    color = HD.TextMuted, fontSize = 9.sp)
            }
        }
        if (reading.stale && reading.readAtMillis > 0L) Text("Última leitura salva · " + SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(reading.readAtMillis)), color = HD.TextMuted, fontSize = 9.sp)
    }
}
