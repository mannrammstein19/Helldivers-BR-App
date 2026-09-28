package br.com.helldiversbr.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.Dispatch
import br.com.helldiversbr.app.data.HomeData
import br.com.helldiversbr.app.ui.HomeState
import br.com.helldiversbr.app.ui.theme.HD
import br.com.helldiversbr.app.update.RemoteVersion

@Composable
fun HomeScreen(
    state: HomeState,
    update: RemoteVersion?,
    onRefresh: () -> Unit,
    onDismissUpdate: () -> Unit,
    contentPadding: PaddingValues,
) {
    Box(Modifier.fillMaxSize()) {
        when (state) {
            HomeState.Loading -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = HD.Yellow)
                Text(
                    "Conectando à telemetria...",
                    color = HD.TextDim,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            is HomeState.Ready -> HomeList(state.data, state.refreshing, null, update, onRefresh, onDismissUpdate, contentPadding)

            is HomeState.Error -> {
                val last = state.last
                if (last != null) {
                    HomeList(last, false, state.message, update, onRefresh, onDismissUpdate, contentPadding)
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(state.message, color = HD.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Verifique sua conexão e tente novamente.",
                            color = HD.TextDim,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                        )
                        YellowButton("TENTAR NOVAMENTE", onRefresh)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeList(
    data: HomeData,
    refreshing: Boolean,
    errorBanner: String?,
    update: RemoteVersion?,
    onRefresh: () -> Unit,
    onDismissUpdate: () -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (refreshing) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = HD.Yellow, trackColor = HD.SurfaceHigh) }
        }
        if (update != null) {
            item { UpdateBanner(update, onDismissUpdate) }
        }
        if (errorBanner != null) {
            item {
                HdCard(accent = HD.Red.copy(alpha = 0.5f)) {
                    Text("$errorBanner Mostrando os últimos dados carregados.", color = HD.TextDim, fontSize = 13.sp)
                    TextButton(onClick = onRefresh) { Text("Tentar novamente", color = HD.Yellow) }
                }
            }
        }

        item { OrderCard(data) }

        item { SectionLabel("Despachos do Alto Comando") }
        if (data.dispatches.isEmpty()) {
            item { Text("Nenhum despacho recente.", color = HD.TextDim, fontSize = 13.sp) }
        } else {
            // Chave composta com o índice: garante unicidade mesmo se a API repetir/zerar ids.
            itemsIndexed(data.dispatches, key = { index, d -> "${d.id}-$index" }) { _, d -> DispatchCard(d) }
        }
    }
}

@Composable
private fun UpdateBanner(update: RemoteVersion, onDismiss: () -> Unit) {
    val context = LocalContext.current
    HdCard(accent = HD.Yellow) {
        SectionLabel("Nova versão disponível")
        Text(
            "Helldivers BR ${update.versionName}",
            color = HD.Text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        if (update.notes.isNotBlank()) {
            Text(update.notes, color = HD.TextDim, fontSize = 13.sp, lineHeight = 19.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            YellowButton("BAIXAR ATUALIZAÇÃO", onClick = {
                // O download abre no navegador; o Android pede a confirmação de instalação.
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.apkUrl)))
            })
            TextButton(onClick = onDismiss) { Text("Depois", color = HD.TextDim) }
        }
    }
}

@Composable
private fun DispatchCard(dispatch: Dispatch) {
    val text = dispatch.text
    if (text.isBlank()) return
    HdCard {
        Text(text, color = HD.Text, fontSize = 14.sp, lineHeight = 20.sp)
    }
}
