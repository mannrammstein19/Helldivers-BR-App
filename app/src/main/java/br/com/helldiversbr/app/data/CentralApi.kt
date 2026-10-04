package br.com.helldiversbr.app.data

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Shared server collection; downloading a cached response never changes its observation time. */
object CentralApi {
    const val BASE = "https://helldivers-br-central.daryldixon19.workers.dev"
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    data class Reading(val data: JsonArray, val time: Long, val source: String, val stale: Boolean, val next: Long)
    private val gates = mutableMapOf<String, Mutex>()
    private val readings = java.util.concurrent.ConcurrentHashMap<String, Reading>()
    private val retries = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS).callTimeout(22, TimeUnit.SECONDS).build()

    fun decode(body: String, now: Long = System.currentTimeMillis()): Reading {
        val o = json.parseToJsonElement(body) as? JsonObject ?: error("Resposta central inválida")
        val data = o["data"] as? JsonArray ?: error("Dados centrais ausentes")
        val time = (o["time"] as? JsonPrimitive)?.longOrNull ?: error("Horário ausente")
        require(time > 0 && time <= now + 60_000 && now - time <= 86_400_000) { "Horário central inválido ou expirado" }
        val source = (o["source"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        require(source in setOf("direct", "community")) { "Origem central inválida" }
        val stale = (o["stale"] as? JsonPrimitive)?.booleanOrNull ?: true
        val next = ((o["next"] as? JsonPrimitive)?.longOrNull ?: now + 30_000).coerceIn(now + 1_000, now + 120_000)
        return Reading(data, time, source, stale, next)
    }

    suspend fun read(path: String): Reading {
        require(path in setOf("/api/v1/planets", "/api/v1/campaigns", "/api/v1/assignments", "/api/v1/dispatches", "/api/v2/space-stations"))
        val gate = synchronized(gates) { gates.getOrPut(path) { Mutex() } }
        return gate.withLock {
            val now = System.currentTimeMillis()
            val old = readings[path]?.takeIf { now - it.time <= 86_400_000 }
            if (old != null && now < old.next && now >= (retries[path] ?: 0)) return@withLock old
            if (now < (retries[path] ?: 0)) return@withLock old?.copy(stale = true) ?: error("Central em nova tentativa")
            try {
                withContext(Dispatchers.IO) {
                    client.newCall(Request.Builder().url(BASE + path).build()).execute().use {
                        check(it.isSuccessful) { "Central HTTP ${it.code}" }
                        decode(it.body?.string() ?: error("Central sem dados"))
                    }
                }.also { readings[path] = it; retries.remove(path) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                retries[path] = now + 30_000
                old?.copy(stale = true) ?: throw e
            }
        }
    }
    fun planets(r: Reading) = json.decodeFromJsonElement(ListSerializer(Planet.serializer()), r.data)
    fun campaigns(r: Reading) = json.decodeFromJsonElement(ListSerializer(Campaign.serializer()), r.data)
    fun assignments(r: Reading) = json.decodeFromJsonElement(ListSerializer(Assignment.serializer()), r.data)
    fun dispatches(r: Reading) = json.decodeFromJsonElement(ListSerializer(Dispatch.serializer()), r.data)
    fun stations(r: Reading) = json.decodeFromJsonElement(ListSerializer(SpaceStation.serializer()), r.data)
}

internal suspend fun <T> attempt(block: suspend () -> T): Result<T> = try { Result.success(block()) }
catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }

fun Planet.withCentralReading(reading: CentralApi.Reading): Planet = copy(regions = regions.map {
    it.copy(telemetryReadAtMillis = it.telemetryReadAtMillis.takeIf { t -> t > 0 } ?: reading.time,
        telemetrySource = reading.source, telemetryStale = it.telemetryStale || reading.stale)
})
