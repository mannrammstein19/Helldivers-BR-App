package br.com.helldiversbr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.theme.HD
import java.text.NumberFormat
import java.util.Locale

data class RewardPresentation(val kind: String, val label: String, val amount: Long?)

/** Same identification order as order-rewards.js; unknown items stay unidentified. */
fun describeReward(r: Reward): RewardPresentation {
    val amount = (r.amount.takeIf { it > 0 } ?: r.quantity ?: r.value)?.takeIf { it > 0 }
    val id = localizedText(r.id ?: r.id32 ?: r.itemId ?: r.itemID)
    val name = localizedText(r.name).ifBlank { localizedText(r.description) }
    val type = localizedText(r.type)
    val text = "$name $type".lowercase()
    fun has(words: String) = Regex("\\b(?:$words)\\b").containsMatchIn(text)
    val kind = when {
        id == "897894480" || has("medal|medals|medalha|medalhas") -> "medalhas"
        has("cape|capa") -> "capa"
        has("helmet|helmets|capacete|capacetes") -> "capacete"
        !has("stratagem|estratagema") && has("weapon|weapons|gun|guns|rifle|rifles|arma|armas") -> "arma"
        has("armor|armour|armadura") -> "armadura"
        has("stratagem|estratagema") -> "estratagema"
        name.isEmpty() && (id.isEmpty() || id == "0") && type == "1" && (amount ?: 0) >= 2 -> "medalhas"
        else -> "generica"
    }
    val label = when (kind) {
        "medalhas" -> if (amount == 1L) "Medalha" else "Medalhas"
        "generica" -> name.ifBlank { "Recompensa especial — item não identificado" }
        else -> name.ifBlank { kind.replaceFirstChar { it.uppercase() } }
    }
    return RewardPresentation(kind, label, amount)
}

@Composable
fun OrderRewards(order: Assignment, accent: Color) {
    val rewards = order.rewards.ifEmpty { listOfNotNull(order.reward) }
    if (rewards.isEmpty()) Text("Recompensa não informada", color = HD.TextDim, fontSize = 12.sp)
    rewards.forEach { reward ->
        val info = describeReward(reward)
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SiteImage("reward_${info.kind}", info.label, Modifier.size(30.dp))
            Text(listOfNotNull(info.amount?.let { NumberFormat.getInstance(Locale("pt", "BR")).format(it) }, info.label).joinToString(" "),
                color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }
}

