package br.com.helldiversbr.app.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Cache persistente da telemetria. A hora contida nos objetos é sempre a hora da
 * leitura real; salvar novamente um fallback nunca transforma dado velho em dado novo.
 */
object TelemetryCache {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Volatile private var root: File? = null

    fun init(context: Context) {
        root = File(context.applicationContext.filesDir, "telemetry-cache").apply { mkdirs() }
    }

    private fun file(name: String): File? = root?.let { File(it, name) }

    suspend fun loadHome(): HomeData? = read("home.json") { json.decodeFromString(HomeData.serializer(), it) }
    suspend fun saveHome(data: HomeData) = write("home.json", json.encodeToString(HomeData.serializer(), data))

    suspend fun loadDss(): DssReading? = read("dss.json") { json.decodeFromString(DssReading.serializer(), it) }
    suspend fun saveDss(data: DssReading) = write("dss.json", json.encodeToString(DssReading.serializer(), data))

    suspend fun loadGalaxy(): GalaxyCache? = read("galaxy.json") { json.decodeFromString(GalaxyCache.serializer(), it) }
    suspend fun saveGalaxy(data: GalaxyCache) = write("galaxy.json", json.encodeToString(GalaxyCache.serializer(), data))

    private suspend fun <T> read(name: String, decode: (String) -> T): T? = withContext(Dispatchers.IO) {
        val target = file(name) ?: return@withContext null
        if (!target.isFile) return@withContext null
        runCatching { decode(target.readText()) }.getOrNull()
    }

    private suspend fun write(name: String, body: String) = withContext(Dispatchers.IO) {
        val target = file(name) ?: return@withContext
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, "$name.tmp")
        runCatching {
            temp.writeText(body)
            if (!temp.renameTo(target)) {
                target.writeText(body)
                temp.delete()
            }
        }.onFailure { temp.delete() }
        Unit
    }
}

@Serializable
data class GalaxyCache(
    val planets: List<Planet> = emptyList(),
    val dss: DssReading = DssReading(),
    val planetCatalog: Map<Long, PlanetCatalogEntry> = emptyMap(),
    val updatedAtMillis: Long = 0L,
    val telemetrySource: String = "cache",
)
