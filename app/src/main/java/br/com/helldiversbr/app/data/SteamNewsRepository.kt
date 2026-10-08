package br.com.helldiversbr.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.time.Instant
import java.util.concurrent.TimeUnit

@Serializable
data class SteamNews(val id: String, val title: String, val url: String, val publishedAtMillis: Long = 0L)
@Serializable
data class SteamReading(val items: List<SteamNews> = emptyList(), val readAtMillis: Long = 0L,
    val source: String = "community", val stale: Boolean = false, val loading: Boolean = false,
    val message: String? = null)

/** News are independent of war telemetry, order results and notification baselines. */
object SteamNewsRepository {
    const val NEWS_URL = "https://store.steampowered.com/news/app/553850"
    private const val DIRECT_URL = "https://api.steampowered.com/ISteamNews/GetNewsForApp/v2/?appid=553850&count=3&maxlength=1&feeds=steam_community_announcements&format=json"
    private const val INTERVAL = 15 * 60_000L
    private val gate = Mutex()
    private var lastAttempt = 0L
    private var latest: SteamReading? = null
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()

    fun validUrl(raw: String): Boolean {
        val uri = runCatching { URI(raw) }.getOrNull() ?: return false
        return uri.scheme == "https" && uri.host?.lowercase() in setOf(
            "store.steampowered.com", "steamcommunity.com", "steamstore-a.akamaihd.net")
    }
    fun parse(body: String): List<SteamNews> {
        val root = Json.parseToJsonElement(body)
        val rows = when (root) {
            is JsonArray -> root
            is JsonObject -> {
                val news = root["appnews"] as? JsonObject ?: error("Resposta Steam inválida")
                require((news["appid"] as? JsonPrimitive)?.longOrNull == 553850L) { "Notícias de outro aplicativo" }
                news["newsitems"] as? JsonArray ?: error("Resposta sem notícias Steam")
            }
            else -> error("Resposta Steam inválida")
        }
        return rows.mapNotNull { element ->
            val row = element as? JsonObject ?: return@mapNotNull null
            fun text(key: String) = (row[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
            val title = text("title").trim()
            val url = text("url").trim()
            if (title.isBlank() || !validUrl(url)) return@mapNotNull null
            val numeric = (row["date"] as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 }
            val published = numeric?.let { if (it < 10_000_000_000L) it * 1000 else it }
                ?: runCatching { Instant.parse(text("publishedAt").ifBlank { text("date") }).toEpochMilli() }.getOrDefault(0L)
            SteamNews(text("gid").ifBlank { text("id").ifBlank { url } }, title, url, published)
        }.distinctBy { it.id }.sortedByDescending { it.publishedAtMillis }.take(3)
    }
    suspend fun load(): SteamReading = gate.withLock {
        val previous = latest ?: TelemetryCache.loadSteam()?.copy(stale = true, source = "cache")
        val now = System.currentTimeMillis()
        if (now - lastAttempt < INTERVAL) return@withLock previous ?: SteamReading(message = "Notícias temporariamente indisponíveis.")
        lastAttempt = now
        try {
            val central = attempt {
                CentralApi.read("/api/v1/steam").let { reading ->
                    require(reading.time >= (previous?.readAtMillis ?: 0L))
                    val items = parse(reading.data.toString()).also { require(it.isNotEmpty()) }
                    SteamReading(items, reading.time, reading.source, reading.stale)
                }
            }.getOrNull()
            if (central != null) {
                latest = central
                TelemetryCache.saveSteam(central)
                return@withLock central
            }
            val (items, source) = try {
                parse(HelldiversApi.steamNews()).also { require(it.isNotEmpty()) } to "community"
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                val body = withContext(Dispatchers.IO) {
                    client.newCall(Request.Builder().url(DIRECT_URL).build()).execute().use {
                        check(it.isSuccessful) { "Steam HTTP ${it.code}" }
                        it.body?.string().orEmpty()
                    }
                }
                parse(body).also { require(it.isNotEmpty()) } to "steam"
            }
            SteamReading(items, System.currentTimeMillis(), source).also { latest = it; TelemetryCache.saveSteam(it) }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            (previous?.copy(stale = true, source = "cache", loading = false)
                ?: SteamReading(message = "Notícias temporariamente indisponíveis.")).also { latest = it }
        }
    }
}
