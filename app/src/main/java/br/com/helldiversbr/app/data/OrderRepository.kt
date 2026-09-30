package br.com.helldiversbr.app.data

import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Estado de tela da Ordem Maior. */
@Serializable
data class OrderUi(
    val order: Assignment?,
    /** "active" | "completed" | "failed" | "pending" */
    val state: String,
    val percent: Double,
    val fromSnapshot: Boolean,
)

@Serializable
data class HomeData(
    val order: OrderUi,
    val dispatches: List<Dispatch>,
    val planetNames: Map<Long, String>,
    val planetCatalog: Map<Long, PlanetCatalogEntry>,
    val planets: List<Planet> = emptyList(),
    val campaigns: List<Campaign>,
    val campaignRates: Map<String, Double>,
    val dss: DssReading,
    val updatedAtMillis: Long,
    val orderRates: Map<Int, Double> = emptyMap(),
    val staleSources: List<String> = emptyList(),
    /** community | direct | cache | mixed */
    val telemetrySource: String = "community",
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

    private val objectiveTracker = OrderRateTracker()
    private var lastGood: HomeData? = null
    private val rateHistory = mutableMapOf<String, RateSnapshot>()

    /** Exibe instantaneamente o último estado salvo enquanto a rede é revalidada. */
    suspend fun loadCached(): HomeData? {
        val cached = lastGood ?: TelemetryCache.loadHome() ?: return null
        val stale = (cached.staleSources + listOf("campanhas", "planetas", "despachos", "Ordem Maior", "DSS")).distinct()
        return cached.copy(
            dss = cached.dss.copy(stale = true, source = "cache"),
            staleSources = stale,
            telemetrySource = "cache",
        ).also { lastGood = it }
    }

    /**
     * Paredão de dados da Home/Central de Guerra:
     * Community API -> API direta do jogo -> último cache persistente.
     * O snapshot do HELLDIVERS-BR continua sendo a autoridade para ordens já encerradas.
     */
    suspend fun load(): HomeData = coroutineScope {
        val disk = TelemetryCache.loadHome()
        val fallback = lastGood ?: disk

        val liveDeferred = async { runCatching { HelldiversApi.liveAssignments().firstOrNull { it.tasks.isNotEmpty() } } }
        val snapshotDeferred = async { runCatching { HelldiversApi.orderSnapshot() } }
        val dispatchDeferred = async { runCatching { HelldiversApi.dispatches().sortedByDescending { it.published.orEmpty() }.take(10) } }
        val campaignsDeferred = async { runCatching { HelldiversApi.campaigns() } }
        val planetsDeferred = async { runCatching { HelldiversApi.planets() } }
        val planetCatalogDeferred = async { runCatching { HelldiversApi.planetCatalog() } }
        val dssDeferred = async { DssRepository.load() }

        val communityOrder = liveDeferred.await()
        val snapshot = snapshotDeferred.await()
        val communityDispatches = dispatchDeferred.await()
        val communityCampaigns = campaignsDeferred.await()
        val communityPlanets = planetsDeferred.await()
        val planetCatalogResult = planetCatalogDeferred.await()
        val dss = dssDeferred.await()

        val catalog = planetCatalogResult.getOrElse { fallback?.planetCatalog.orEmpty() }
            .ifEmpty { fallback?.planetCatalog.orEmpty() }

        var directWar: Result<DirectGameApi.DirectWarData>? = null
        suspend fun directWarData(): Result<DirectGameApi.DirectWarData> {
            directWar?.let { return it }
            return runCatching { DirectGameApi.warData(catalog) }.also { directWar = it }
        }

        fun validDispatches(items: List<Dispatch>): Boolean =
            items.isNotEmpty() && items.any { it.text.isNotBlank() }

        fun validCampaigns(items: List<Campaign>): Boolean =
            items.isNotEmpty() && items.any { it.planet.index >= 0L && it.planet.nameText.isNotBlank() }

        fun validPlanets(items: List<Planet>): Boolean =
            items.isNotEmpty() && items.count { it.index >= 0L } >= 10

        val communityOrderValue = communityOrder.getOrNull()?.takeIf { it.tasks.isNotEmpty() }
        val communityDispatchList = communityDispatches.getOrNull().orEmpty()
        val communityCampaignList = communityCampaigns.getOrNull().orEmpty()
        val communityPlanetList = communityPlanets.getOrNull().orEmpty()

        val directOrderDeferred = async {
            if (communityOrderValue == null)
                runCatching { DirectGameApi.assignment() } else Result.success(null)
        }
        val directDispatchesDeferred = async {
            if (!validDispatches(communityDispatchList))
                runCatching { DirectGameApi.dispatches().take(10) } else Result.success(emptyList())
        }
        val directCampaignsDeferred = async {
            if (!validCampaigns(communityCampaignList) || !validPlanets(communityPlanetList)) directWarData() else null
        }
        val directOrder = directOrderDeferred.await()
        val directDispatches = directDispatchesDeferred.await()
        val directCampaigns = directCampaignsDeferred.await()

        val directOrderValue = directOrder.getOrNull()?.takeIf { it.tasks.isNotEmpty() }
        val directDispatchList = directDispatches.getOrNull().orEmpty()
        val directWarValue = directCampaigns?.getOrNull()
        val directCampaignList = directWarValue?.campaigns.orEmpty()
        val directPlanetList = directWarValue?.planets.orEmpty()

        val liveOrder = communityOrderValue ?: directOrderValue
        val orderSource = when {
            communityOrderValue != null -> "community"
            directOrderValue != null -> "direct"
            else -> "cache"
        }
        val snap = snapshot.getOrNull()

        val dispatchSource: String
        val dispatches = when {
            validDispatches(communityDispatchList) -> { dispatchSource = "community"; communityDispatchList }
            validDispatches(directDispatchList) -> { dispatchSource = "direct"; directDispatchList }
            else -> { dispatchSource = "cache"; fallback?.dispatches.orEmpty() }
        }

        val campaignSource: String
        val rawCampaigns = when {
            validCampaigns(communityCampaignList) -> { campaignSource = "community"; communityCampaignList }
            validCampaigns(directCampaignList) -> { campaignSource = "direct"; directCampaignList }
            else -> { campaignSource = "cache"; fallback?.campaigns.orEmpty() }
        }
        val campaigns = rawCampaigns.sortedWith(
            compareByDescending<Campaign> { it.planet.event != null }
                .thenByDescending { it.planet.statistics.playerCount }
        )

        val planetSource: String
        val planets = when {
            validPlanets(communityPlanetList) -> { planetSource = "community"; communityPlanetList }
            validPlanets(directPlanetList) -> { planetSource = "direct"; directPlanetList }
            else -> { planetSource = "cache"; fallback?.planets.orEmpty() }
        }

        val snapOrder = snap?.order
        fun visibleState(state: String, order: Assignment): String {
            if (state in listOf("completed", "failed", "pending", "unknown")) return state
            val expired = runCatching { Instant.parse(order.expiration).isBefore(Instant.now()) }.getOrDefault(false)
            return if (expired) "pending" else "active"
        }

        val ui = when {
            liveOrder != null && snap != null && snapOrder != null && liveOrder.id != null && liveOrder.id == snapOrder.id && snap.state in listOf("completed", "failed") -> OrderUi(
                order = snapOrder, state = snap.state,
                percent = snap.final_percent ?: computePercent(snapOrder), fromSnapshot = true,
            )
            liveOrder != null -> OrderUi(
                order = liveOrder,
                state = visibleState("active", liveOrder),
                percent = computePercent(liveOrder),
                fromSnapshot = false,
            )
            snap != null && snapOrder != null -> OrderUi(
                order = snapOrder,
                state = visibleState(snap.state.ifBlank { "pending" }, snapOrder),
                percent = snap.final_percent ?: computePercent(snapOrder),
                fromSnapshot = true,
            )
            fallback != null -> fallback.order
            else -> OrderUi(order = null, state = "pending", percent = 0.0, fromSnapshot = false)
        }

        // Se não sobrou absolutamente nenhum dado útil, a tela de erro continua válida.
        if (ui.order == null && dispatches.isEmpty() && campaigns.isEmpty() && fallback == null) {
            throw (communityCampaigns.exceptionOrNull()
                ?: directCampaigns?.exceptionOrNull()
                ?: communityOrder.exceptionOrNull()
                ?: directOrder.exceptionOrNull()
                ?: IllegalStateException("Sem dados de telemetria"))
        }

        val now = System.currentTimeMillis()
        val names = catalog.mapValues { (_, p) -> p.displayName }.filterValues { it.isNotBlank() }
        val campaignsFresh = campaignSource != "cache"
        val rates = if (campaignsFresh) updateCampaignRates(campaigns, now) else fallback?.campaignRates.orEmpty()
        val orderFresh = orderSource != "cache" && !ui.fromSnapshot
        val objectiveRates = if (ui.order != null && ui.state == "active" && orderFresh)
            objectiveTracker.update(ui.order, now) else fallback?.orderRates.orEmpty()

        // A hora exibida pertence ao dado central das campanhas. Em cache, preserva a hora antiga.
        val dataTimestamp = if (campaignsFresh) now else fallback?.updatedAtMillis ?: 0L
        val stale = buildList {
            if (campaignSource == "cache") add("campanhas")
            if (planetSource == "cache") add("planetas")
            if (dispatchSource == "cache") add("despachos")
            if (orderSource == "cache" || (ui.fromSnapshot && ui.state == "active")) add("Ordem Maior")
            if (dss.stale) add("DSS")
        }

        val usedSources = buildSet {
            add(campaignSource)
            add(planetSource)
            add(dispatchSource)
            add(orderSource)
            if (dss.source in setOf("community", "direct", "cache")) add(dss.source)
        }
        val telemetrySource = if (usedSources.size == 1) usedSources.first() else "mixed"

        HomeData(
            order = ui,
            dispatches = dispatches,
            planetNames = names,
            planetCatalog = catalog,
            planets = planets,
            campaigns = campaigns,
            campaignRates = rates,
            dss = dss,
            updatedAtMillis = dataTimestamp,
            orderRates = objectiveRates,
            staleSources = stale,
            telemetrySource = telemetrySource,
        ).also { result ->
            lastGood = result
            // Não regrava um fallback puro como se ele fosse novo.
            val gotFreshNetworkData = campaignSource != "cache" || planetSource != "cache" || dispatchSource != "cache" || orderSource != "cache" || snapshot.isSuccess || !dss.stale
            if (gotFreshNetworkData) TelemetryCache.saveHome(result)
        }
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
