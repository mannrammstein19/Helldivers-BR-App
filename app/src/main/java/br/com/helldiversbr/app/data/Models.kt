package br.com.helldiversbr.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Recompensa da Ordem Maior. type 1 = Medalhas. */
@Serializable
data class Reward(
    val type: Int = 0,
    val amount: Long = 0,
)

@Serializable
data class OrderTask(
    val type: Int = 0,
    val values: List<Long> = emptyList(),
    val valueTypes: List<Int> = emptyList(),
) {
    /** Lê um valor da tarefa pelo valueType (ex.: 3 = meta numérica, 12 = id do planeta). */
    fun valueOf(valueType: Int): Long? {
        val i = valueTypes.indexOf(valueType)
        return if (i >= 0) values.getOrNull(i) else null
    }

    val goal: Long? get() = valueOf(3)
    val planetId: Long? get() = valueOf(12)?.takeIf { it != 0L }
}

/** Ordem Maior (item de /assignments). Textos podem vir como string ou como mapa de idiomas. */
@Serializable
data class Assignment(
    val id: Long = 0,
    val progress: List<Long> = emptyList(),
    val title: JsonElement? = null,
    val briefing: JsonElement? = null,
    val description: JsonElement? = null,
    val tasks: List<OrderTask> = emptyList(),
    val reward: Reward? = null,
    val rewards: List<Reward> = emptyList(),
    val expiration: String? = null,
) {
    val titleText: String get() = localizedText(title)
    val briefingText: String get() = localizedText(briefing)
    val descriptionText: String get() = localizedText(description)

    /** Recompensa principal: `reward` ou primeira de `rewards`. */
    val mainReward: Reward? get() = reward ?: rewards.firstOrNull()
}

/**
 * Snapshot persistente que o GitHub Actions do site salva em dados/major-order.json.
 * state: "active" | "completed" | "failed"
 */
@Serializable
data class OrderSnapshot(
    val state: String = "pending",
    val key: String? = null,
    val first_seen_at: String? = null,
    val last_seen_at: String? = null,
    val final_percent: Double? = null,
    val order: Assignment? = null,
)

/** Despacho (item de /dispatches). */
@Serializable
data class Dispatch(
    val id: Long = 0,
    val published: String? = null,
    val type: Int = 0,
    val message: JsonElement? = null,
) {
    val text: String get() = localizedText(message)
}

/** Converte string simples ou mapa de idiomas ({"pt-BR": "...", "en-US": "..."}) em texto limpo. */
fun localizedText(value: JsonElement?): String {
    val raw = when (value) {
        null -> ""
        is JsonPrimitive -> value.contentOrNull ?: ""
        is JsonObject -> {
            val preferred = listOf("pt-BR", "pt-PT", "en-US", "en")
                .firstNotNullOfOrNull { k -> (value[k] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() } }
            preferred ?: value.values.firstNotNullOfOrNull {
                (it as? JsonPrimitive)?.contentOrNull?.takeIf { s -> s.isNotBlank() }
            } ?: ""
        }
        else -> ""
    }
    return raw.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
}

/** Traduções fixas, as mesmas de ptbr.js do site. */
private val knownPtBr = mapOf(
    "MAJOR ORDER" to "ORDEM MAIOR",
    "LIBERATE" to "LIBERTAR",
    "DEFEND" to "DEFENDER",
    "ORDER COMPLETE" to "ORDEM CONCLUÍDA",
    "ORDER FAILED" to "ORDEM NÃO CUMPRIDA",
    "AWAITING ORDERS" to "AGUARDANDO ORDENS",
    "SUPER EARTH" to "SUPER TERRA",
)

fun translateKnown(text: String): String = knownPtBr[text.trim().uppercase()] ?: text
