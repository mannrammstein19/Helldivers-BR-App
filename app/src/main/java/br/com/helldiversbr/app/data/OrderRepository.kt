package br.com.helldiversbr.app.data

import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.coroutineScope

/** Estado de tela da Ordem Maior. */
@Serializable
data class OrderUi(
    val order: Assignment?,
    /** "active" | "completed" | "failed" | "pending" */
    val state: String,
    val percent: Double,
    val fromSnapshot: Boolean,
    val observedAtMillis: Long = 0L,
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
    val campaignTelemetrySource: String = "community",
    val campaignReadAtMillis: Long = 0L,
    val viaCentral: Boolean = false,
    val dispatchReadAtMillis: Long = 0L,
    val campaignListReadAtMillis: Long = 0L,
    val planetReadAtMillis: Long = 0L,
) {
    val helldiversOnFront: Long get() = campaigns.sumOf { it.planet.statistics.playerCount.coerceAtLeast(0) }
    val liberationCount: Int get() = campaigns.count { it.planet.event == null }
    val defenseCount: Int get() = campaigns.count { it.planet.event != null }
    val activeFronts: Int get() = campaigns.size
}

object OrderRepository {
    private val loadGate = kotlinx.coroutines.sync.Mutex()
    private var lastLoadAttempt = 0L

    private data class RateSnapshot(
        val progress: Double,
        val timeMillis: Long,
        val ratePerHour: Double? = null,
    )

    private val objectiveTracker = OrderRateTracker()
    @Volatile private var lastGood: HomeData? = null
    private val rateHistory = mutableMapOf<String, RateSnapshot>()

    /** Exibe instantaneamente o último estado salvo enquanto a rede é revalidada. */
    suspend fun loadCached(): HomeData? {
        val cached = lastGood ?: TelemetryCache.loadHome() ?: return null
        val stale = (cached.staleSources + listOf("campanhas", "planetas", "despachos", "Ordem Maior", "DSS")).distinct()
        return cached.copy(
            planets = cached.planets.map { it.asSavedTelemetry() },
            campaigns = cached.campaigns.map { it.copy(planet = it.planet.asSavedTelemetry()) },
            dss = cached.dss.copy(stale = true, source = "cache"),
            staleSources = stale,
            telemetrySource = "cache",
            campaignTelemetrySource = "cache",
        )
    }

    /**
     * Paredão de dados da Home/Central de Guerra:
     * Community API -> API direta do jogo -> último cache persistente.
     * O snapshot do HELLDIVERS-BR continua sendo a autoridade para ordens já encerradas.
     */
    suspend fun load(): HomeData = loadGate.withLock { coroutineScope {
        val disk = TelemetryCache.loadHome()
        val fallback = lastGood ?: disk
        val loadTime = System.currentTimeMillis()
        if (loadTime - lastLoadAttempt < 15_000 && fallback != null)
            return@coroutineScope loadCached()!!
        lastLoadAttempt = loadTime
        val central = attempt { loadCentral(fallback) }
        central.getOrNull()?.let { lastGood = it; TelemetryCache.saveHome(it); return@coroutineScope it }
        if (fallback?.viaCentral == true) return@coroutineScope loadCached()!!.also { lastGood = it }

        var communityCampaignTime = 0L
        var communityPlanetTime = 0L
        var directWarTime = 0L
        var communityOrderTime = 0L
        var directOrderTime = 0L
        var communityDispatchTime = 0L
        var directDispatchTime = 0L
        val liveDeferred = async { runCatching { HelldiversApi.liveAssignments().also { communityOrderTime = System.currentTimeMillis() }.firstOrNull { it.tasks.isNotEmpty() } } }
        val snapshotDeferred = async { runCatching { HelldiversApi.orderSnapshot() } }
        val dispatchDeferred = async { runCatching { HelldiversApi.dispatches().also { communityDispatchTime = System.currentTimeMillis() }.sortedByDescending { it.published.orEmpty() }.take(10) } }
        val campaignsDeferred = async { runCatching { HelldiversApi.campaigns().also { communityCampaignTime = System.currentTimeMillis() } } }
        val planetsDeferred = async { runCatching { HelldiversApi.planets().also { communityPlanetTime = System.currentTimeMillis() } } }
        val planetCatalogDeferred = async { runCatching { HelldiversApi.planetCatalog() } }
        val dssDeferred = async { DssRepository.load() }
        val regionStatusDeferred = async { RegionTelemetry.load() }

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
            return runCatching { DirectGameApi.warData(catalog).also { directWarTime = System.currentTimeMillis() } }.also { directWar = it }
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
                runCatching { DirectGameApi.assignment().also { directOrderTime = System.currentTimeMillis() } } else Result.success(null)
        }
        val directDispatchesDeferred = async {
            if (!validDispatches(communityDispatchList))
                runCatching { DirectGameApi.dispatches().also { directDispatchTime = System.currentTimeMillis() }.take(10) } else Result.success(emptyList())
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

        val ui = resolveCentralOrder(liveOrder, snap, fallback?.order,
            communityOrderTime > 0 || directOrderTime > 0,
            if (communityOrderValue != null) communityOrderTime else maxOf(communityOrderTime, directOrderTime))

        // Se não sobrou absolutamente nenhum dado útil, a tela de erro continua válida.
        if (ui.order == null && dispatches.isEmpty() && campaigns.isEmpty() && fallback == null) {
            throw (communityCampaigns.exceptionOrNull()
                ?: directCampaigns?.exceptionOrNull()
                ?: communityOrder.exceptionOrNull()
                ?: directOrder.exceptionOrNull()
                ?: IllegalStateException("Sem dados de telemetria"))
        }

        val regionReadings = regionStatusDeferred.await()
        val now = System.currentTimeMillis()
        val counterReadings = CounterTelemetry.collect(
            fallback?.let { it.planets + it.campaigns.map { c -> c.planet } }.orEmpty(),
            fallback?.updatedAtMillis ?: 0L,
            listOf(planets to planetSource, campaigns.map { it.planet } to campaignSource), now,
            listOf(if (planetSource == "community") communityPlanetTime else directWarTime,
                if (campaignSource == "community") communityCampaignTime else directWarTime),
        )
        val priorPlanets = fallback?.let { it.campaigns.map { c -> c.planet } + it.planets }.orEmpty().associateBy { it.index }
        val enrichedPlanets = planets.map {
            RegionTelemetry.enrich(CounterTelemetry.enrich(it, counterReadings), regionReadings,
                priorPlanets[it.index], now, planetSource != "cache")
        }
        val enrichedCampaigns = campaigns.map {
            it.copy(planet = RegionTelemetry.enrich(CounterTelemetry.enrich(it.planet, counterReadings),
                regionReadings, priorPlanets[it.planet.index], now, campaignSource != "cache"))
        }
        val names = catalog.mapValues { (_, p) -> p.displayName }.filterValues { it.isNotBlank() }
        val campaignsFresh = campaignSource != "cache"
        val rates = if (campaignsFresh) updateCampaignRates(campaigns, now) else fallback?.campaignRates.orEmpty()
        val orderFresh = orderSource != "cache" && !ui.fromSnapshot
        val objectiveRates = if (ui.order != null && ui.state == "active" && orderFresh)
            objectiveTracker.update(ui.order, now) else if (ui.order?.id != null && ui.order.id == fallback?.order?.order?.id) fallback?.orderRates.orEmpty() else emptyMap()

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
            planets = enrichedPlanets,
            campaigns = enrichedCampaigns,
            campaignRates = rates,
            dss = dss,
            updatedAtMillis = dataTimestamp,
            orderRates = objectiveRates,
            planetReadAtMillis = when (planetSource) {
                "community" -> communityPlanetTime
                "direct" -> directWarTime
                else -> fallback?.planetReadAtMillis ?: 0L
            },
            campaignListReadAtMillis = when (campaignSource) {
                "community" -> communityCampaignTime
                "direct" -> directWarTime
                else -> fallback?.campaignListReadAtMillis ?: 0L
            },
            dispatchReadAtMillis = when (dispatchSource) {
                "community" -> communityDispatchTime
                "direct" -> directDispatchTime
                else -> fallback?.dispatchReadAtMillis ?: 0L
            },
            staleSources = stale,
            telemetrySource = telemetrySource,
            campaignTelemetrySource = campaignSource,
            campaignReadAtMillis = when (campaignSource) {
                "community" -> communityCampaignTime
                "direct" -> directWarTime
                else -> fallback?.campaignReadAtMillis ?: 0L
            },
        ).also { result ->
            lastGood = result
            // Não regrava um fallback puro como se ele fosse novo.
            val gotFreshNetworkData = campaignSource != "cache" || planetSource != "cache" || dispatchSource != "cache" || orderSource != "cache" || snapshot.isSuccess || !dss.stale
            if (gotFreshNetworkData) TelemetryCache.saveHome(result)
        }
    } }

    private suspend fun loadCentral(previous: HomeData?): HomeData = coroutineScope {
        val pTask = async { CentralApi.read("/api/v1/planets") }
        val cTask = async { CentralApi.read("/api/v1/campaigns") }
        val oTask = async { attempt { CentralApi.read("/api/v1/assignments") } }
        val dTask = async { attempt { CentralApi.read("/api/v1/dispatches") } }
        val sTask = async { attempt { HelldiversApi.orderSnapshot() } }
        val stationTask = async { DssRepository.load() }
        val pr = pTask.await(); val cr = cTask.await()
        require(previous == null || !previous.viaCentral ||
            (pr.time >= previous.updatedAtMillis && cr.time >= previous.campaignListReadAtMillis)) { "Central anterior ao cache persistente" }
        val rawPlanets = CentralApi.planets(pr)
        require(rawPlanets.size >= 10 && rawPlanets.map { it.index }.distinct().size == rawPlanets.size)
        val counters = CounterTelemetry.collect(previous?.planets.orEmpty(), previous?.updatedAtMillis ?: 0,
            listOf(rawPlanets to pr.source), pr.time, listOf(pr.time))
        val priorPlanets = previous?.let { it.campaigns.map { c -> c.planet } + it.planets }.orEmpty().associateBy { it.index }
        val planets = rawPlanets.map {
            RegionTelemetry.enrich(CounterTelemetry.enrich(it, counters).withCentralReading(pr),
                emptyMap(), priorPlanets[it.index], pr.time, !pr.stale)
                .let { p -> if (pr.stale) p.asSavedTelemetry() else p }
        }
        val byId = planets.associateBy { it.index }
        val campaigns = CentralApi.campaigns(cr).mapNotNull { c -> byId[c.planet.index]?.let { c.copy(planet = it) } }
        val orderReading = oTask.await().getOrNull()?.takeIf { it.time >= (previous?.order?.observedAtMillis ?: 0L) }
        val live = orderReading?.let { CentralApi.assignments(it).firstOrNull { a -> a.tasks.isNotEmpty() } }
        val snapshot = sTask.await().getOrNull()
        val order = resolveCentralOrder(live, snapshot, previous?.order, orderReading != null && !orderReading.stale, orderReading?.time ?: 0L)
        val monitorFresh = snapshot?.telemetry?.stale == false && snapshot.state == "active" &&
            order.fromSnapshot && order.observedAtMillis > 0 &&
            System.currentTimeMillis() - order.observedAtMillis in 0..300_000 &&
            snapshot.order?.id != null && snapshot.order.id == order.order?.id
        val orderTime = if (monitorFresh) snapshot?.telemetry?.assignmentsTime ?: snapshot?.telemetry?.time
            else orderReading?.takeIf { !it.stale && live?.id != null && live.id == order.order?.id }?.time
        val dispatchReading = dTask.await().getOrNull()?.takeIf { it.time >= (previous?.dispatchReadAtMillis ?: 0L) }
        val catalog = attempt { HelldiversApi.planetCatalog() }.getOrElse { previous?.planetCatalog.orEmpty() }
            .ifEmpty { previous?.planetCatalog.orEmpty() }
        val dss = stationTask.await().withPlanetReference(planets, pr)
        TelemetryCache.saveDss(dss)
        val fresh = !pr.stale && !cr.stale
        if (fresh && rateHistory.isEmpty() && previous != null && previous.campaignReadAtMillis > 0 && previous.campaignReadAtMillis < pr.time)
            updateCampaignRates(previous.campaigns, previous.campaignReadAtMillis)
        HomeData(order, dispatchReading?.let { CentralApi.dispatches(it).sortedByDescending { d -> d.published.orEmpty() }.take(10) } ?: previous?.dispatches.orEmpty(),
            catalog.mapValues { it.value.displayName }, catalog, planets, campaigns,
            if (fresh) updateCampaignRates(campaigns, pr.time) else previous?.campaignRates.orEmpty(), dss, pr.time,
            orderRates = if (order.order != null && order.state == "active" && orderTime != null && orderTime > 0)
                objectiveTracker.update(order.order, orderTime) else if (order.order?.id != null && order.order.id == previous?.order?.order?.id) previous?.orderRates.orEmpty() else emptyMap(),
            staleSources = buildList {
                if (!fresh) add("campanhas")
                if (pr.stale) add("planetas")
                if (order.observedAtMillis <= 0 || System.currentTimeMillis() - order.observedAtMillis !in 0..300_000 ||
                    (order.fromSnapshot && snapshot?.telemetry?.stale != false) ||
                    (!order.fromSnapshot && (orderReading == null || orderReading.stale))) add("Ordem Maior")
                if (dispatchReading == null || dispatchReading.stale) add("despachos")
                if (dss.stale) add("DSS")
            }, telemetrySource = if (pr.stale) "cache" else pr.source, campaignTelemetrySource = if (fresh) pr.source else "cache",
            campaignReadAtMillis = pr.time, campaignListReadAtMillis = cr.time, planetReadAtMillis = pr.time, viaCentral = true,
            dispatchReadAtMillis = dispatchReading?.time ?: previous?.dispatchReadAtMillis ?: 0L)
    }

    fun resolveCentralOrder(live: Assignment?, snapshot: OrderSnapshot?, previous: OrderUi?, absenceConfirmed: Boolean, liveTime: Long = 0L): OrderUi {
        val previousTime = previous?.observedAtMillis ?: 0L
        val snapshotTime = if (snapshot?.state == "active")
            snapshot.telemetry?.assignmentsTime ?: snapshot.telemetry?.time ?: 0L
            else snapshot?.telemetry?.time ?: 0L
        val snapshotAccepted = previousTime == 0L || snapshotTime >= previousTime
        val liveAccepted = previousTime == 0L || liveTime >= previousTime
        val snap = snapshot?.order
        val prior = previous?.order
        fun same(a: Assignment?, b: Assignment?) = a?.id != null && b?.id != null && a.id == b.id
        fun older(a: Assignment, b: Assignment): Boolean {
            val aEnd = runCatching { Instant.parse(a.expiration) }.getOrNull()
            val bEnd = runCatching { Instant.parse(b.expiration) }.getOrNull()
            return aEnd != null && bEnd != null && aEnd.isBefore(bEnd)
        }
        fun newer(a: Assignment, b: Assignment): Boolean {
            val aEnd = runCatching { Instant.parse(a.expiration) }.getOrNull()
            val bEnd = runCatching { Instant.parse(b.expiration) }.getOrNull()
            return aEnd != null && bEnd != null && aEnd.isAfter(bEnd)
        }
        // A confirmed cached result survives an old assignment or disappearance.
        if (previous?.state in setOf("completed", "failed") &&
            (live == null || same(live, prior)) && (snap == null || same(snap, prior))) return previous!!
        if (snapshotAccepted && snap != null && snapshot.state in setOf("completed", "failed") &&
            (live == null || same(live, snap)) && (prior == null || same(prior, snap) || newer(snap, prior)))
            return OrderUi(snap, snapshot.state, snapshot.final_percent ?: computePercent(snap), true, snapshotTime)
        if (snapshotAccepted && snap != null && snapshot.state == "active" && snapshot.telemetry?.stale == false &&
            (prior == null || same(prior, snap) || newer(snap, prior)) &&
            (live == null || same(live, snap) || !older(snap, live)) &&
            (!same(live, snap) || !absenceConfirmed || snapshotTime >= liveTime) &&
            !(same(prior, snap) && previous?.state in setOf("completed", "failed"))) {
            val expired = runCatching { Instant.parse(snap.expiration).toEpochMilli() <= System.currentTimeMillis() }.getOrDefault(false)
            return OrderUi(snap, if (expired) "pending" else "active", computePercent(snap), true, snapshotTime)
        }
        if (live != null && (!absenceConfirmed || !liveAccepted) && previous != null) return previous
        if (live != null && (prior == null || same(live, prior) || (absenceConfirmed && newer(live, prior)))) {
            if (same(live, prior) && previous?.state in setOf("completed", "failed")) return previous!!
            val expired = runCatching { Instant.parse(live.expiration).toEpochMilli() <= System.currentTimeMillis() }.getOrDefault(false)
            return OrderUi(live, if (expired || !absenceConfirmed) "pending" else "active", computePercent(live), false, liveTime)
        }
        if (previous != null && previous.state in setOf("completed", "failed")) return previous
        if (snapshotAccepted && snap != null && snapshot.state in setOf("pending", "unknown") &&
            (prior == null || same(prior, snap) || newer(snap, prior)))
            return if (same(prior, snap) && previous != null)
                previous.copy(state = "pending", fromSnapshot = true, observedAtMillis = snapshotTime)
            else OrderUi(snap, "pending", snapshot.final_percent ?: computePercent(snap), true, snapshotTime)
        if (absenceConfirmed && liveAccepted && live == null && prior?.id != null)
            return previous!!.copy(state = "pending", observedAtMillis = liveTime)
        if (absenceConfirmed && liveAccepted && live == null) return OrderUi(null, "pending", 0.0, false)
        if (previous != null) return previous
        if (snap != null) return OrderUi(snap, "pending", snapshot.final_percent ?: computePercent(snap), true, snapshotTime)
        return OrderUi(null, "pending", 0.0, false)
    }

    fun campaignDisplayRate(data: HomeData, campaign: Campaign): Double? {
        if ("campanhas" in data.staleSources) return null
        campaignRate(data, campaign)?.let { return it }
        val e = campaign.planet.event ?: return null
        val start = runCatching { Instant.parse(e.startTime).toEpochMilli() }.getOrNull() ?: return null
        val end = runCatching { Instant.parse(e.endTime).toEpochMilli() }.getOrNull() ?: return null
        val now = data.campaignReadAtMillis
        if (now - start < 300_000 || end <= now || end <= start) return null
        return (campaignPercent(campaign) / ((now - start) / 3_600_000.0)).takeIf { it.isFinite() }
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
    fun defenseEnemyProgress(event: PlanetEvent?, now: Long = System.currentTimeMillis()): Double? {
        if (event == null) return null
        val start = event.startTime?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        val end = event.endTime?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        val total = end.toEpochMilli() - start.toEpochMilli()
        if (total <= 0L) return null
        return (((now - start.toEpochMilli()).toDouble() / total) * 100.0).coerceIn(0.0, 100.0)
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

    /** Progresso do planeta ou evento de defesa; regiões são objetivos independentes. */
    fun campaignPercent(campaign: Campaign): Double {
        val planet = campaign.planet
        val event = planet.event
        if (event != null && event.maxHealth > 0) {
            return (1.0 - event.health.toDouble() / event.maxHealth.toDouble())
                .coerceIn(0.0, 1.0) * 100.0
        }
        return if (planet.maxHealth > 0) {
            (1.0 - planet.health.toDouble() / planet.maxHealth.toDouble())
                .coerceIn(0.0, 1.0) * 100.0
        } else 0.0
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
    fun remaining(expiration: String?, now: Long = System.currentTimeMillis()): String {
        if (expiration.isNullOrBlank()) return "prazo indisponível"
        val end = runCatching { Instant.parse(expiration) }.getOrNull() ?: return "prazo indisponível"
        val seconds = end.epochSecond - now / 1000
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
