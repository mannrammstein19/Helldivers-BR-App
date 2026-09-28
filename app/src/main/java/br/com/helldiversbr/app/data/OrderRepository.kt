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
    val planetCatalog: Map<Long, PlanetCatalogEntry>,
    val campaigns: List<Campaign>,
    val campaignRates: Map<String, Double>,
    val updatedAtMillis: Long,
) {
    val helldiversOnFront: Long get() = campaigns.sumOf { it.planet.statistics.playerCount.coerceAtLeast(0) }
    val liberationCount: Int get() = campaigns.count { it.planet.event == null }
    val defenseCount: Int get() = campaigns.count { it.planet.event != null }
    val activeFronts: Int get() = campaigns.size
}

object OrderRepository {

    private data class RateSnapshot(
        val progress: Double,
        val timeMillis: Long,
        val ratePerHour: Double? = null,
    )

    private val rateHistory = mutableMapOf<String, RateSnapshot>()

    /**
     * Carrega as fontes em paralelo para a Home e a Central de Guerra.
     * Mantém a mesma lógica do site: API ao vivo primeiro e snapshot para a Ordem Maior.
     */
    suspend fun load(): HomeData = coroutineScope {
        val liveDeferred = async { runCatching { HelldiversApi.liveAssignments().firstOrNull { it.tasks.isNotEmpty() } } }
        val snapshotDeferred = async { runCatching { HelldiversApi.orderSnapshot() } }
        val dispatchDeferred = async { runCatching { HelldiversApi.dispatches().sortedByDescending { it.published.orEmpty() }.take(10) } }
        val campaignsDeferred = async { runCatching { HelldiversApi.campaigns() } }
        val planetCatalogDeferred = async { runCatching { HelldiversApi.planetCatalog() } }

        val live = liveDeferred.await()
        val snapshot = snapshotDeferred.await()
        val dispatchesResult = dispatchDeferred.await()
        val campaignsResult = campaignsDeferred.await()
        val planetCatalogResult = planetCatalogDeferred.await()

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

        val now = System.currentTimeMillis()
        val catalog = planetCatalogResult.getOrDefault(emptyMap())
        val names = catalog.mapValues { (_, p) -> p.displayName }.filterValues { it.isNotBlank() }
        val rates = updateCampaignRates(campaigns, now)

        HomeData(
            order = ui,
            dispatches = dispatches,
            planetNames = names,
            planetCatalog = catalog,
            campaigns = campaigns,
            campaignRates = rates,
            updatedAtMillis = now,
        )
    }

    fun campaignKey(campaign: Campaign): String {
        val p = campaign.planet
        val mode = if (p.event != null) "defense" else "attack"
        return "${p.index}:$mode:${p.event?.id ?: 0}"
    }

    private fun updateCampaignRates(campaigns: List<Campaign>, now: Long): Map<String, Double> {
        val activeKeys = mutableSetOf<String>()
        campaigns.forEach { campaign ->
            val key = campaignKey(campaign)
            activeKeys += key
            val progress = campaignPercent(campaign)
            val previous = rateHistory[key]
            val elapsed = previous?.let { now - it.timeMillis } ?: 0L
            val nextRate = if (previous != null && elapsed >= 30_000L) {
                val hours = elapsed / 3_600_000.0
                if (hours > 0.0) (progress - previous.progress) / hours else previous.ratePerHour
            } else previous?.ratePerHour

            // Em menos de 30 s preservamos a amostra anterior para evitar ruído.
            if (previous == null || elapsed >= 30_000L) {
                rateHistory[key] = RateSnapshot(progress, now, nextRate)
            }
        }
        rateHistory.keys.retainAll(activeKeys)
        return rateHistory.mapNotNull { (key, snap) -> snap.ratePerHour?.let { key to it } }.toMap()
    }

    fun campaignRate(data: HomeData, campaign: Campaign): Double? = data.campaignRates[campaignKey(campaign)]

    /** Pressão inimiga de libertação convertida para % por hora, como no site. */
    fun liberationEnemyPressure(campaign: Campaign): Double? {
        val p = campaign.planet
        if (p.maxHealth <= 0 || p.regenPerSecond < 0) return null
        return (p.regenPerSecond * 3600.0 / p.maxHealth.toDouble()) * 100.0
    }

    /** Relógio da invasão em uma defesa, de 0 a 100%. */
    fun defenseEnemyProgress(event: PlanetEvent?): Double? {
        if (event == null) return null
        val start = event.startTime?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        val end = event.endTime?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        val total = end.toEpochMilli() - start.toEpochMilli()
        if (total <= 0L) return null
        return (((System.currentTimeMillis() - start.toEpochMilli()).toDouble() / total) * 100.0).coerceIn(0.0, 100.0)
    }

    fun defenseEnemyRate(event: PlanetEvent?): Double? {
        if (event == null) return null
        val start = event.startTime?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        val end = event.endTime?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        val hours = (end.toEpochMilli() - start.toEpochMilli()) / 3_600_000.0
        return if (hours > 0.0) 100.0 / hours else null
    }

    fun etaFromRate(progress: Double, rate: Double?): String? {
        if (rate == null || !rate.isFinite() || rate <= 0.0 || progress >= 100.0) return null
        val hours = (100.0 - progress) / rate
        if (!hours.isFinite() || hours <= 0.0) return null
        if (hours > 24.0 * 365.0) return ">1 ano"
        val minutes = kotlin.math.max(1L, kotlin.math.round(hours * 60.0).toLong())
        val days = minutes / 1440L
        val h = (minutes % 1440L) / 60L
        val m = minutes % 60L
        return when {
            days > 0 -> "~${days}d ${h}h"
            h > 0 -> "~${h}h ${m}min"
            else -> "~${m}min"
        }
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

    /**
     * Facção que efetivamente controla o planeta. Em libertações, esta é a informação
     * que precisa aparecer no card (o planeta ainda pertence ao inimigo).
     */
    fun ownerFaction(campaign: Campaign): String {
        val current = campaign.planet.currentOwner.trim()
        if (current.isNotBlank()) return current
        return if (campaign.planet.event != null) "Humans"
        else campaign.faction.takeIf { it.isNotBlank() } ?: campaign.planet.initialOwner
    }

    /**
     * Facção inimiga da frente. Em defesa vem do atacante do evento; em libertação
     * vem do dono atual do planeta. Isto também alimenta filtro, cor e pressão inimiga.
     */
    fun enemyFaction(campaign: Campaign): String {
        val eventFaction = campaign.planet.event?.faction?.trim().orEmpty()
        if (eventFaction.isNotBlank() && !isHumanFaction(eventFaction)) return eventFaction

        val current = campaign.planet.currentOwner.trim()
        if (current.isNotBlank() && !isHumanFaction(current)) return current

        val campaignFaction = campaign.faction.trim()
        if (campaignFaction.isNotBlank() && !isHumanFaction(campaignFaction)) return campaignFaction

        val initial = campaign.planet.initialOwner.trim()
        if (initial.isNotBlank() && !isHumanFaction(initial)) return initial

        return eventFaction.ifBlank { current.ifBlank { campaignFaction.ifBlank { initial } } }
    }

    /** Compatibilidade com os componentes já existentes: facção de campanha = inimigo da frente. */
    fun campaignFaction(campaign: Campaign): String = enemyFaction(campaign)

    fun isHumanFaction(raw: String): Boolean {
        val n = raw.lowercase()
        return "human" in n || "super" in n || n == "1"
    }

    fun factionKey(raw: String): String {
        val n = raw.lowercase()
        return when {
            "terminid" in n || n == "2" -> "terminids"
            "automaton" in n || "cyborg" in n || n == "3" -> "automatons"
            "illuminate" in n || "squid" in n || n == "4" -> "illuminates"
            isHumanFaction(raw) -> "humans"
            else -> "unknown"
        }
    }

    fun factionFromRaceId(id: Int?): String = when (id) {
        1 -> "Humans"
        2 -> "Terminids"
        3 -> "Automatons"
        4 -> "Illuminate"
        else -> ""
    }

    fun taskFaction(task: OrderTask): String = factionFromRaceId(task.factionId)

    fun factionLabel(raw: String): String {
        return when (factionKey(raw)) {
            "terminids" -> "Terminídeos"
            "automatons" -> "Autômatos"
            "illuminates" -> "Iluminados"
            "humans" -> "Super Terra"
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
