package br.com.helldiversbr.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Acesso às mesmas fontes públicas usadas pelo HELLDIVERS-BR:
 *  - api.helldivers2.dev/api/v1 (telemetria ao vivo)
 *  - dados/major-order.json no GitHub (snapshot que sobrevive ao fim da ordem)
 */
object HelldiversApi {
    private const val API = "https://api.helldivers2.dev/api/v1"
    private const val SNAPSHOT_URL =
        "https://raw.githubusercontent.com/mannrammstein19/Helldivers-BR/main/dados/major-order.json"
    private const val PLANETS_URL =
        "https://raw.githubusercontent.com/helldivers-2/json/master/planets/planets.json"

    // Site público atual. As telas nativas usam este endereço apenas em links externos/fallbacks.
    const val SITE_BASE = "https://helldivers-br.pages.dev"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun get(url: String, withHeaders: Boolean): String {
        val builder = Request.Builder().url(url)
        if (withHeaders) {
            // Identificação solicitada pela API comunitária.
            builder.header("X-Super-Client", "helldivers-br.pages.dev")
            builder.header("X-Super-Contact", "https://github.com/mannrammstein19/Helldivers-BR-App")
            builder.header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.5")
        }
        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code} em $url")
            return resp.body?.string() ?: error("Resposta vazia em $url")
        }
    }

    /** A API às vezes devolve lista direta, às vezes {"data": [...]}. */
    private fun <T> parseList(body: String, decode: (JsonArray) -> List<T>): List<T> {
        val el = json.parseToJsonElement(body)
        val array = when (el) {
            is JsonArray -> el
            is JsonObject -> el["data"] as? JsonArray ?: JsonArray(emptyList())
            else -> JsonArray(emptyList())
        }
        return decode(array)
    }

    suspend fun liveAssignments(): List<Assignment> = withContext(Dispatchers.IO) {
        parseList(get("$API/assignments", true)) {
            json.decodeFromJsonElement(ListSerializer(Assignment.serializer()), it)
        }
    }

    suspend fun dispatches(): List<Dispatch> = withContext(Dispatchers.IO) {
        parseList(get("https://api.helldivers2.dev/api/v2/dispatches", true)) {
            json.decodeFromJsonElement(ListSerializer(Dispatch.serializer()), it)
        }
    }

    suspend fun campaigns(): List<Campaign> = withContext(Dispatchers.IO) {
        parseList(get("$API/campaigns", true)) {
            json.decodeFromJsonElement(ListSerializer(Campaign.serializer()), it)
        }
    }

    suspend fun orderSnapshot(): OrderSnapshot = withContext(Dispatchers.IO) {
        json.decodeFromString(OrderSnapshot.serializer(), get(SNAPSHOT_URL, false))
    }

    @Volatile
    private var planetCatalogCache: Map<Long, PlanetCatalogEntry>? = null

    /**
     * Catálogo completo de planetas usado também pelo site. Além do nome, traz
     * bioma e condições ambientais para que o app possa renderizar os mesmos
     * fundos e chips visuais da Central de Guerra web.
     */
    suspend fun planetCatalog(): Map<Long, PlanetCatalogEntry> {
        planetCatalogCache?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val root = json.parseToJsonElement(get(PLANETS_URL, false)) as JsonObject
                root.mapNotNull { (id, value) ->
                    val key = id.toLongOrNull() ?: return@mapNotNull null
                    val item = runCatching { json.decodeFromJsonElement(PlanetCatalogEntry.serializer(), value) }.getOrNull()
                    item?.let { key to it }
                }.toMap()
            }.getOrDefault(emptyMap()).also { if (it.isNotEmpty()) planetCatalogCache = it }
        }
    }

    /** Compatibilidade para a Ordem Maior, que só precisa de id -> nome. */
    suspend fun planetNames(): Map<Long, String> = planetCatalog()
        .mapValues { (_, p) -> p.displayName }
        .filterValues { it.isNotBlank() }
}
