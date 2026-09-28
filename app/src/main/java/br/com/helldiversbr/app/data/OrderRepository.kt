package br.com.helldiversbr.app.data

import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Estado de tela da Ordem Maior. */
data class OrderUi(
    val order: Assignment?,
    /** "active" | "completed" | "failed" | "pending" */
    val state: String,
    val percent: Double,
    val fromSnapshot: Boolean,
)

data class HomeData(
    val order: OrderUi,
    val dispatches: List<Dispatch>,
    val planetNames: Map<Long, String>,
    val campaigns: List<Campaign>,
    val updatedAtMillis: Long,
) {
    val helldiversOnFront: Long get() = campaigns.sumOf { it.planet.statistics.playerCount.coerceAtLeast(0) }
    val liberationCount: Int get() = campaigns.count { it.planet.event == null }
    val defenseCount: Int get() = campaigns.count { it.planet.event != null }
    val activeFronts: Int get() = campaigns.size
}

object OrderRepository {

    /**
     * Carrega as fontes em paralelo para a Home e a Central de Guerra.
     * Mantém a mesma lógica do site: API ao vivo primeiro e snapshot para a Ordem Maior.
     */
    suspend fun load(): HomeData = coroutineScope {
        val liveDeferred = async { runCatching { HelldiversApi.liveAssignments().firstOrNull { it.tasks.isNotEmpty() } } }
        val snapshotDeferred = async { runCatching { HelldiversApi.orderSnapshot() } }
        val dispatchDeferred = async { runCatching { HelldiversApi.dispatches().sortedByDescending { it.published.orEmpty() }.take(10) } }
        val campaignsDeferred = async { runCatching { HelldiversApi.campaigns() } }
        val planetNamesDeferred = async { runCatching { HelldiversApi.planetNames() } }

        val live = liveDeferred.await()
        val snapshot = snapshotDeferred.await()
        val dispatchesResult = dispatchDeferred.await()
        val campaignsResult = campaignsDeferred.await()
        val planetNamesResult = planetNamesDeferred.await()

        val liveOrder = live.getOrNull()
        val snap = snapshot.getOrNull()
        val dispatches = dispatchesResult.getOrDefault(emptyList())
        val campaigns = campaignsResult.getOrDefault(emptyList())
            .sortedWith(
                compareByDescending<Campaign> { it.planet.event != null }
                    .thenByDescending { it.planet.statistics.playerCount }
            )

        // Só tratamos como falha total quando nenhuma fonte dinâmica principal respondeu.
        if (liveOrder == null && snap == null && dispatches.isEmpty() && campaigns.isEmpty() &&
            (live.isFailure || snapshot.isFailure || dispatchesResult.isFailure || campaignsResult.isFailure)
        ) {
            throw (live.exceptionOrNull()
                ?: snapshot.exceptionOrNull()
                ?: campaignsResult.exceptionOrNull()
                ?: dispatchesResult.exceptionOrNull()
                ?: IllegalStateException("Sem dados"))
        }

        val snapOrder = snap?.order
        val ui = when {
            liveOrder != null -> OrderUi(
                order = liveOrder,
                state = "active",
                percent = computePercent(liveOrder),
                fromSnapshot = false,
            )
            snap != null && snapOrder != null -> OrderUi(
                order = snapOrder,
                state = snap.state.ifBlank { "pending" },
                percent = snap.final_percent ?: computePercent(snapOrder),
                fromSnapshot = true,
            )
            else -> OrderUi(order = null, state = "pending", percent = 0.0, fromSnapshot = false)
        }

        HomeData(
            order = ui,
            dispatches = dispatches,
            planetNames = planetNamesResult.getOrDefault(emptyMap()),
            campaigns = campaigns,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }

    /** Progresso médio das tarefas em % (progress[i] / meta da tarefa i). */
    fun computePercent(order: Assignment): Double {
        if (order.tasks.isEmpty()) return 0.0
        val parts = order.tasks.mapIndexed { i, task ->
            val goal = task.goal
            val progress = order.progress.getOrNull(i) ?: 0L
            when {
                goal != null && goal > 0 -> (progress.toDouble() / goal).coerceIn(0.0, 1.0)
                progress > 0 -> 1.0
                else -> 0.0
            }
        }
        return parts.average() * 100.0
    }

    /** Progresso efetivo de uma campanha, incluindo o sistema atual de regiões. */
    fun campaignPercent(campaign: Campaign): Double {
        val planet = campaign.planet
        val event = planet.event
        if (event != null && event.maxHealth > 0) {
            return (1.0 - event.health.toDouble() / event.maxHealth.toDouble())
                .coerceIn(0.0, 1.0) * 100.0
        }

        if (planet.maxHealth > 0) {
            val planetPercent = (1.0 - planet.health.toDouble() / planet.maxHealth.toDouble())
                .coerceIn(0.0, 1.0) * 100.0
            if (planetPercent > 0.001) return planetPercent
        }

        // Em campanhas baseadas em regiões, o planeta pode aparecer em 0% enquanto
        // a batalha real acontece em uma região desbloqueada.
        return planet.regions
            .asSequence()
            .filter { it.isAvailable != false && it.maxHealth > 0 && it.health != null }
            .map {
                (1.0 - (it.health ?: it.maxHealth).toDouble() / it.maxHealth.toDouble())
                    .coerceIn(0.0, 1.0) * 100.0
            }
            .maxOrNull() ?: 0.0
    }

    fun campaignMode(campaign: Campaign): String = if (campaign.planet.event != null) "defense" else "attack"

    fun campaignFaction(campaign: Campaign): String {
        return campaign.planet.event?.faction
            ?.takeIf { it.isNotBlank() }
            ?: campaign.faction.takeIf { it.isNotBlank() }
            ?: campaign.planet.currentOwner
    }

    fun factionLabel(raw: String): String {
        val n = raw.lowercase()
        return when {
            "terminid" in n -> "Terminídeos"
            "automaton" in n -> "Autômatos"
            "illuminate" in n -> "Iluminados"
            "human" in n || "super" in n -> "Super Terra"
            else -> raw.ifBlank { "Desconhecida" }
        }
    }

    /** Tempo restante formatado como no site (ex.: "3d 20h", "5h 12min"). */
    fun remaining(expiration: String?): String {
        if (expiration.isNullOrBlank()) return "prazo indisponível"
        val end = runCatching { Instant.parse(expiration) }.getOrNull() ?: return "prazo indisponível"
        val seconds = end.epochSecond - Instant.now().epochSecond
        if (seconds <= 0) return "prazo esgotado"
        val d = seconds / 86400
        val h = seconds % 86400 / 3600
        val m = seconds % 3600 / 60
        return when {
            d > 0 -> "${d}d ${h}h"
            h > 0 -> "${h}h ${m}min"
            else -> "${m}min"
        }
    }
}
