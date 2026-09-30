package br.com.helldiversbr.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import br.com.helldiversbr.app.ui.theme.HD

object FirstRunPreferences {
    private const val PREFS = "helldivers_br_prefs"
    private const val KEY_DRAWER_TUTORIAL = "drawer_swipe_tutorial_seen_v1"

    fun shouldShow(context: Context): Boolean =
        !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DRAWER_TUTORIAL, false)

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DRAWER_TUTORIAL, true)
            .apply()
    }
}

@Composable
fun FirstRunDrawerHint(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HD.BgDeep.copy(alpha = 0.98f), RoundedCornerShape(18.dp))
                    .border(1.dp, HD.Yellow.copy(alpha = 0.72f), RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(HD.Yellow.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.MenuOpen, null, tint = HD.Yellow, modifier = Modifier.size(27.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("NAVEGAÇÃO LIBERADA", color = HD.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        Text("Abra o menu com um gesto", color = HD.Text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                }

                Text(
                    "A partir de agora, deslize a partir da borda esquerda para a direita para abrir o menu de navegação.",
                    color = HD.TextDim,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.36f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(4.dp).size(width = 4.dp, height = 34.dp).background(HD.Yellow, RoundedCornerShape(50)))
                    Spacer(Modifier.width(12.dp))
                    Text("BORDA ESQUERDA", color = HD.TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                    Spacer(Modifier.weight(1f))
                    repeat(2) {
                        Icon(Icons.Filled.ArrowForward, null, tint = HD.Yellow, modifier = Modifier.size(22.dp))
                    }
                }

                Text(
                    "Este aviso aparece apenas uma vez.",
                    color = HD.TextMuted,
                    fontSize = 9.sp,
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HD.Yellow, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("ENTENDI", fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 0.8.sp)
                }
            }
        }
    }
}
