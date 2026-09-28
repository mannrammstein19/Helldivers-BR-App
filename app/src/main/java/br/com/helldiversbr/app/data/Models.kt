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
    /** Raça/facção alvo codificada pela API: 1 Humanos, 2 Terminídeos, 3 Autômatos, 4 Iluminados. */
    val factionId: Int? get() = valueOf(1)?.toInt()
    /** Identificador interno do alvo/unidade quando a ordem é de eliminação. */
    val targetUnitId: Long? get() = valueOf(4)?.takeIf { it != 0L }
    val planetId: Long? get() = valueOf(12)?.takeIf { it != 0L }
}

/** Ordem Maior (item de /assignments). Textos podem vir como string ou como mapa de idiomas. */
@Serializable
data class Assignment(
    val id: JsonElement? = null,
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

/** Estatísticas agregadas que acompanham cada planeta retornado por /campaigns. */
@Serializable
data class PlanetStatistics(
    val playerCount: Long = 0,
    val missionsWon: Long? = null,
    val missionsLost: Long? = null,
)

/** Evento ativo de um planeta. Quando presente, normalmente representa uma defesa. */
@Serializable
data class PlanetEvent(
    val id: Long = 0,
    val eventType: Int = 0,
    val faction: String = "",
    val health: Long = 0,
    val maxHealth: Long = 0,
    val startTime: String? = null,
    val endTime: String? = null,
)

/** Regiões internas dos planetas mais recentes. */
@Serializable
data class PlanetRegion(
    val name: String? = null,
    val health: Long? = null,
    val maxHealth: Long = 0,
    val regenPerSecond: Double? = null,
    val isAvailable: Boolean? = null,
    val players: Long? = null,
)

@Serializable
data class PlanetPosition(val x: Double = 0.0, val y: Double = 0.0)

/** Planeta agregado pela API comunitária. Mantemos apenas os campos usados no app. */
@Serializable
data class Planet(
    val index: Long = 0,
    val name: JsonElement? = null,
    val sector: String = "",
    val health: Long = 0,
    val maxHealth: Long = 0,
    val regenPerSecond: Double = 0.0,
    val currentOwner: String = "",
    val initialOwner: String = "",
    val position: PlanetPosition? = null,
    val positionX: Double? = null,
    val positionY: Double? = null,
    val position_x: Double? = null,
    val position_y: Double? = null,
    val waypoints: List<Long> = emptyList(),
    val statistics: PlanetStatistics = PlanetStatistics(),
    val event: PlanetEvent? = null,
    val regions: List<PlanetRegion> = emptyList(),
) {
    val nameText: String get() = localizedText(name).ifBlank { "PLANETA #$index" }
    val mapPosition: PlanetPosition? get() = position
        ?: if (positionX != null && positionY != null) PlanetPosition(positionX, positionY)
        else if (position_x != null && position_y != null) PlanetPosition(position_x, position_y)
        else null
}


@Serializable
data class PlanetCatalogEntry(
    val name: JsonElement? = null,
    val names: JsonElement? = null,
    val sector: String = "",
    val biome: String = "",
    val type: String = "",
    val environmentals: List<String> = emptyList(),
    val weather_effects: List<String> = emptyList(),
) {
    val displayName: String get() = localizedText(names).ifBlank { localizedText(name) }
}

/** Campanha ativa retornada por /campaigns. */
@Serializable
data class Campaign(
    val id: JsonElement? = null,
    val planet: Planet = Planet(),
    val faction: String = "",
)

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
