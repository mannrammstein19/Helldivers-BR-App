package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.PublicTextTranslation
import br.com.helldiversbr.app.data.translateKnown
import br.com.helldiversbr.app.ui.theme.HD
import kotlinx.coroutines.CancellationException

@Composable
fun LocalizedText(text: String, color: Color, fontSize: TextUnit, lineHeight: TextUnit = TextUnit.Unspecified,
                  maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip) {
    val context = LocalContext.current.applicationContext
    var translated by remember(text) { mutableStateOf(PublicTextTranslation.cached(context, text) ?: translateKnown(text)) }
    var original by remember(text) { mutableStateOf(false) }
    var translating by remember(text) { mutableStateOf(false) }
    var failed by remember(text) { mutableStateOf(false) }
    var retry by remember(text) { mutableStateOf(0) }
    LaunchedEffect(text, retry) {
        failed = false
        translating = PublicTextTranslation.needsTranslation(text) && translated == text
        try {
            translated = PublicTextTranslation.translate(context, text, retry > 0)
            failed = translated == text && PublicTextTranslation.needsTranslation(text)
        }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { failed = true }
        finally { translating = false }
    }
    Column {
        Text(if (original) text else translated, color = color, fontSize = fontSize,
            lineHeight = lineHeight, maxLines = maxLines, overflow = overflow)
        if (translating) Text("Traduzindo para PT-BR…", color = HD.TextMuted, fontSize = 10.sp)
        if (translated != text || failed) {
            TextButton(onClick = { if (failed) retry++ else original = !original }, contentPadding = PaddingValues(0.dp)) {
                Text(if (failed) "Original · tentar tradução novamente" else if (original) "Ver tradução em PT-BR" else "PT-BR automático · ver original",
                    color = HD.TextMuted, fontSize = 10.sp)
            }
        }
    }
}
