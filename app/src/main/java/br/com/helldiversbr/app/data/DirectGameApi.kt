package br.com.helldiversbr.app.data

import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Segunda linha do paredão: leitura bruta do backend usado pelo jogo.
 * Ela só é consultada quando a API comunitária falha, reduzindo carga sobre a fonte original.
 */
object DirectGameApi {
    private const val BASE = "https://api.live.prod.thehelldiversgame.com"
    private const val BUNDLE_TTL = 30_000L
    private const val WAR_ID_TTL = 6 * 60 * 60 * 1000L

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    @Volatile private var cachedWarId: Pair<Long, Long>? = null
    @Volatile private var cachedBundle: Pair<Long, RawWarBundle>? = null

    data class DirectWarData(
        val planets: List<Planet>,
        val campaigns: List<Campaign>,
    )

    private data class RawWarBundle(
        val warId: Long,
        val status: JsonObject,
        val info: JsonObject,
        val startMillis: Long?,
    )

    private fun get(path: String): String {
        val request = Request.Builder()
            .url("$BASE$path")
            .header("Accept", "application/json")
            .header("Accept-Language", "pt-BR")
            .header("User-Agent", "Helldivers-BR-App/22")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("API direta HTTP ${response.code} em $path")
            return response.body?.string() ?: error("API direta sem corpo em $path")
        }
    }

    private fun rootObject(body: String): JsonObject {
        val el = json.parseToJsonElement(body)
        return when (el) {
            is JsonObject -> (element(el, "data") as? JsonObject) ?: el
            else -> error("Objeto inválido da API direta")
        }
    }

    private fun rootArray(body: String): JsonArray {
        val el = json.parseToJsonElement(body)
        return when (el) {
            is JsonArray -> el
            is JsonObject -> (element(el, "data") as? JsonArray) ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
    }

    private suspend fun currentWarId(): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedWarId?.takeIf { now - it.first < WAR_ID_TTL }?.second?.let { return@withContext it }
        val el = json.parseToJsonElement(get("/api/WarSeason/current/WarID"))
        val id = when (el) {
            is JsonPrimitive -> el.longOrNull
            is JsonObject -> {
                val data = element(el, "data")
                when (data) {
                    is JsonPrimitive -> data.longOrNull
                    is JsonObject -> long(data, "id", "warId", "warID")
                    else -> long(el, "id", "warId", "warID")
                }
            }
            else -> null
        } ?: error("WarID inválido na API direta")
        cachedWarId = now to id
        id
    }

    private suspend fun bundle(): RawWarBundle = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedBundle?.takeIf { now - it.first < BUNDLE_TTL }?.second?.let { return@withContext it }
        val warId = currentWarId()
        val status = rootObject(get("/api/WarSeason/$warId/Status"))
        val info = runCatching { rootObject(get("/api/WarSeason/$warId/WarInfo")) }
            .getOrElse { rootObject(get("/api/WarSeason/$warId/Info")) }
        val startMillis = epochMillis(long(info, "startDate"))
        RawWarBundle(warId, status, info, startMillis).also { cachedBundle = now to it }
    }

    suspend fun regionStatus(): String = withContext(Dispatchers.IO) {
        cachedBundle?.takeIf { System.currentTimeMillis() - it.first < BUNDLE_TTL }
            ?.second?.status?.toString() ?: get("/api/WarSeason/${currentWarId()}/Status")
    }

    suspend fun assignment(): Assignment? = withContext(Dispatchers.IO) {
        val warId = currentWarId()
        val raw = rootArray(get("/api/v2/Assignment/War/$warId")).firstOrNull() as? JsonObject ?: return@withContext null
        val setting = element(raw, "setting") as? JsonObject ?: JsonObject(emptyMap())
        val tasks = array(setting, "tasks").mapNotNull { taskEl ->
            val t = taskEl as? JsonObject ?: return@mapNotNull null
            OrderTask(
                type = int(t, "type") ?: 0,
                values = array(t, "values").mapNotNull { (it as? JsonPrimitive)?.longOrNull },
                valueTypes = array(t, "valueTypes").mapNotNull { (it as? JsonPrimitive)?.intOrNull },
            )
        }
        val rewardObj = element(setting, "reward") as? JsonObject
        val reward = rewardObj?.let {
            Reward(
                type = element(it, "type"),
                amount = long(it, "amount") ?: 0L,
                id32 = element(it, "id32"),
            )
        }
        val expiresIn = long(raw, "expiresIn") ?: 0L
        Assignment(
            id = element(raw, "id32", "id"),
            progress = array(raw, "progress").mapNotNull { (it as? JsonPrimitive)?.longOrNull },
            title = element(setting, "overrideTitle") ?: JsonPrimitive("MAJOR ORDER"),
            briefing = element(setting, "overrideBrief"),
            description = element(setting, "taskDescription"),
            tasks = tasks,
            reward = reward,
            expiration = if (expiresIn > 0) Instant.now().plusSeconds(expiresIn).toString() else null,
        )
    }

    suspend fun dispatches(): List<Dispatch> = withContext(Dispatchers.IO) {
        val war = bundle()
        rootArray(get("/api/NewsFeed/${war.warId}?maxEntries=1024"))
            .mapNotNull { it as? JsonObject }
            .map { item ->
                val publishedWarSeconds = long(item, "published")
                Dispatch(
                    id = long(item, "id") ?: 0L,
                    published = publishedWarSeconds?.let { warSecondsToIso(war.startMillis, it) },
                    type = int(item, "type") ?: 0,
                    message = element(item, "message"),
                )
            }
            .sortedByDescending { it.published.orEmpty() }
    }

    suspend fun warData(catalog: Map<Long, PlanetCatalogEntry>): DirectWarData = withContext(Dispatchers.IO) {
        val war = bundle()
        val infoByIndex = array(war.info, "planetInfos")
            .mapNotNull { it as? JsonObject }
            .associateBy { long(it, "index") ?: -1L }
        val statusByIndex = array(war.status, "planetStatus")
            .mapNotNull { it as? JsonObject }
            .associateBy { long(it, "index") ?: -1L }
        val eventsByIndex = array(war.status, "planetEvents")
            .mapNotNull { it as? JsonObject }
            .associateBy { long(it, "planetIndex") ?: -1L }
        val attacksBySource = array(war.status, "planetAttacks")
            .mapNotNull { it as? JsonObject }
            .groupBy { long(it, "source") ?: -1L }
        val regionInfo = array(war.info, "planetRegions")
            .mapNotNull { it as? JsonObject }
            .associateBy { Pair(long(it, "planetIndex") ?: -1L, int(it, "regionIndex") ?: -1) }
        val regionsByPlanet = array(war.status, "planetRegions")
            .mapNotNull { it as? JsonObject }
            .groupBy { long(it, "planetIndex") ?: -1L }

        val indices = (infoByIndex.keys + statusByIndex.keys).filter { it >= 0L }.toSortedSet()
        val planets = indices.map { index ->
            val info = infoByIndex[index] ?: JsonObject(emptyMap())
            val status = statusByIndex[index] ?: JsonObject(emptyMap())
            val cat = catalog[index]
            val pos = (element(status, "position") as? JsonObject) ?: (element(info, "position") as? JsonObject)
            val eventRaw = eventsByIndex[index]
            val regions = regionsByPlanet[index].orEmpty().map { region ->
                val regionIndex = int(region, "regionIndex") ?: 0
                val ri = regionInfo[index to regionIndex]
                PlanetRegion(
                    id = regionIndex,
                    name = JsonPrimitive("REGIÃO ${regionIndex + 1}"),
                    hash = long(ri, "settingsHash"),
                    size = int(ri, "regionSize")?.toString(),
                    owner = int(region, "owner")?.let { JsonPrimitive(raceName(it)) },
                    health = long(region, "health"),
                    maxHealth = long(ri, "maxHealth") ?: 0L,
                    regenPerSecond = double(region, "regenPerSecond", "regerPerSecond"),
                    isAvailable = bool(region, "isAvailable"),
                    availabilityFactor = double(region, "availabilityFactor"),
                    telemetrySource = "direct",
                    telemetryReadAtMillis = System.currentTimeMillis(),
                    players = long(region, "players"),
                )
            }
            val maxHealth = long(info, "maxHealth") ?: long(status, "health") ?: 0L
            Planet(
                index = index,
                name = cat?.name ?: cat?.names ?: JsonPrimitive("PLANETA #$index"),
                sector = cat?.sector.orEmpty(),
                health = long(status, "health") ?: maxHealth,
                maxHealth = maxHealth,
                regenPerSecond = double(status, "regenPerSecond") ?: 0.0,
                currentOwner = raceName(int(status, "owner")),
                initialOwner = raceName(int(info, "initialOwner")),
                position = pos?.let { PlanetPosition(double(it, "x") ?: 0.0, double(it, "y") ?: 0.0) },
                waypoints = array(info, "waypoints").mapNotNull { (it as? JsonPrimitive)?.longOrNull },
                attacking = attacksBySource[index].orEmpty().mapNotNull { long(it, "target") },
                disabled = bool(info, "disabled") ?: false,
                statistics = PlanetStatistics(playerCount = long(status, "players") ?: 0L),
                event = eventRaw?.let { event ->
                    PlanetEvent(
                        id = long(event, "id") ?: 0L,
                        eventType = int(event, "eventType") ?: 0,
                        faction = raceName(int(event, "race")),
                        health = long(event, "health") ?: 0L,
                        maxHealth = long(event, "maxHealth") ?: 0L,
                        startTime = long(event, "startTime")?.let { warSecondsToIso(war.startMillis, it) },
                        endTime = long(event, "expireTime")?.let { warSecondsToIso(war.startMillis, it) },
                    )
                },
                regions = regions,
            )
        }
        val planetByIndex = planets.associateBy { it.index }
        val campaigns = array(war.status, "campaigns")
            .mapNotNull { it as? JsonObject }
            .mapNotNull { raw ->
                val planetIndex = long(raw, "planetIndex") ?: return@mapNotNull null
                val planet = planetByIndex[planetIndex] ?: return@mapNotNull null
                Campaign(
                    id = element(raw, "id", "id32"),
                    planet = planet,
                    faction = raceName(int(raw, "race")),
                )
            }
        DirectWarData(planets, campaigns)
    }

    suspend fun dss(
        catalog: Map<Long, PlanetCatalogEntry>,
        previous: DssReading? = null,
    ): DssReading = withContext(Dispatchers.IO) {
        val war = bundle()
        val raw = array(war.status, "spaceStations").firstOrNull() as? JsonObject
            ?: return@withContext DssReading(
                station = null,
                availability = DssAvailability.ABSENT,
                fetchedAtMillis = System.currentTimeMillis(),
                source = "direct",
            )
        val planetIndex = long(raw, "planetIndex") ?: 0L
        val cat = catalog[planetIndex]
        val richer = previous?.station?.takeIf { old ->
            (long(raw, "id32") ?: 0L) == old.id32 || (old.planet.index == planetIndex && planetIndex > 0)
        }
        val election = long(raw, "currentElectionEndWarTime")?.let { warSecondsToIso(war.startMillis, it) }.orEmpty()
        val station = SpaceStation(
            id32 = long(raw, "id32") ?: richer?.id32 ?: 0L,
            planet = Planet(
                index = planetIndex,
                name = cat?.name ?: cat?.names ?: richer?.planet?.name,
                sector = cat?.sector ?: richer?.planet?.sector.orEmpty(),
            ),
            electionEnd = election.ifBlank { richer?.electionEnd.orEmpty() },
            flags = long(raw, "flags") ?: richer?.flags ?: 0L,
            // A fonte bruta traz o estado da estação, mas nem sempre o catálogo rico das ações.
            tacticalActions = richer?.tacticalActions.orEmpty(),
        )
        DssReading(
            station = station,
            availability = if (planetIndex > 0L || localizedText(station.planet.name).isNotBlank()) DssAvailability.LIVE else DssAvailability.LOCATION_UNKNOWN,
            fetchedAtMillis = System.currentTimeMillis(),
            stale = false,
            source = "direct",
        )
    }

    private fun raceName(id: Int?): String = when (id) {
        1 -> "Humans"
        2 -> "Terminids"
        3 -> "Automatons"
        4 -> "Illuminate"
        else -> ""
    }

    private fun epochMillis(raw: Long?): Long? = when {
        raw == null || raw <= 0L -> null
        raw > 10_000_000_000L -> raw
        raw > 1_000_000_000L -> raw * 1000L
        else -> null
    }

    private fun warSecondsToIso(startMillis: Long?, seconds: Long): String? {
        val base = startMillis ?: return null
        return runCatching { Instant.ofEpochMilli(base + seconds * 1000L).toString() }.getOrNull()
    }

    /** ArrowHead alterna entre camelCase e PascalCase em respostas brutas. */
    private fun element(obj: JsonObject?, vararg keys: String): JsonElement? {
        if (obj == null) return null
        keys.forEach { key -> obj[key]?.let { return it } }
        val wanted = keys.map { it.lowercase() }.toSet()
        return obj.entries.firstOrNull { it.key.lowercase() in wanted }?.value
    }

    private fun array(obj: JsonObject?, key: String): JsonArray = element(obj, key) as? JsonArray ?: JsonArray(emptyList())
    private fun long(obj: JsonObject?, vararg keys: String): Long? = keys.firstNotNullOfOrNull { (element(obj, it) as? JsonPrimitive)?.longOrNull }
    private fun int(obj: JsonObject?, vararg keys: String): Int? = keys.firstNotNullOfOrNull { (element(obj, it) as? JsonPrimitive)?.intOrNull }
    private fun double(obj: JsonObject?, vararg keys: String): Double? = keys.firstNotNullOfOrNull { (element(obj, it) as? JsonPrimitive)?.doubleOrNull }
    private fun bool(obj: JsonObject?, vararg keys: String): Boolean? = keys.firstNotNullOfOrNull { (element(obj, it) as? JsonPrimitive)?.booleanOrNull }
}
