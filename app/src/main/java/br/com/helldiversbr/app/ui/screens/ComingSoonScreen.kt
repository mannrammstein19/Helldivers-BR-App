package br.com.helldiversbr.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.HelldiversApi
import br.com.helldiversbr.app.ui.theme.HD

/** Tela provisória: leva para a página equivalente do site enquanto a versão nativa não existe. */
@Composable
fun ComingSoonScreen(title: String, sitePath: String, contentPadding: PaddingValues) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = HD.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "A versão nativa desta seção está em desenvolvimento.",
            color = HD.TextDim,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        YellowButton("ABRIR NO SITE", onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${HelldiversApi.SITE_BASE}/$sitePath")))
        })
    }
}
