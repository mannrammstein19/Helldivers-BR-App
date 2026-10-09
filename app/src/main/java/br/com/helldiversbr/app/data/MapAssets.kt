package br.com.helldiversbr.app.data

import android.content.Context
import kotlinx.serialization.json.*

/** Packaged map artwork; there is no network fallback for these assets. */
object MapAssets {
    private var files: Map<String, String> = emptyMap()
    private var planets: Map<Long, String> = emptyMap()
    var catalog: Map<Long, PlanetCatalogEntry> = emptyMap(); private set
    fun init(context: Context) {
        fun objectFile(name: String) = context.assets.open(name).bufferedReader().use { CentralApi.json.parseToJsonElement(it.readText()).jsonObject }
        files = objectFile("map-assets.json").mapValues { it.value.jsonPrimitive.content }
        planets = objectFile("map-planet-icons.json").mapKeys { it.key.toLong() }.mapValues { it.value.jsonPrimitive.content }
        catalog = objectFile("planet-catalog.json").mapNotNull { (k,v) -> k.toLongOrNull()?.let { it to CentralApi.json.decodeFromJsonElement(PlanetCatalogEntry.serializer(),v) } }.toMap()
        PlanetPresences.init(context.assets.open("map-presences.json").bufferedReader().use { it.readText() })
    }
    fun file(key: String): String? = files[key]?.let { "file:///android_asset/$it" }
    fun planet(index: Long): String? = planets[index]?.let { "file:///android_asset/$it" }
    fun planetEntries(): Map<Long, String> = planets
    fun all(): Map<String, String> = files
}

data class PlanetPresence(val key: String, val name: String, val faction: String, val ids: Set<Long>, val file: String, val aliases: Set<String> = emptySet()) {
    val model: String? get() = when(key) {
        "jet", "fire", "cyborg" -> "nave-automata"
        "masses" -> "nave-iluminada"
        "snatchers" -> "nave-raptores"
        "appropriators" -> "nave-apropriadores"
        "fleet" -> "nave-frota-iluminada"
        else -> null
    }
    val formation: Int get() = if(key in setOf("masses", "snatchers")) 3 else 1
    val artwork: String? get() = MapAssets.file("imagens/guerra/presencas/$file")
}
object PlanetPresences {
    private var catalog: List<PlanetPresence> = emptyList()
    private fun normalized(value: String) = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").trim().replace(Regex("\\s+"), " ").uppercase(java.util.Locale.ROOT)
    fun init(body: String) {
        catalog = CentralApi.json.parseToJsonElement(body).jsonArray.map {
            val o=it.jsonObject
            PlanetPresence(o.getValue("key").jsonPrimitive.content, o.getValue("name").jsonPrimitive.content,
                o.getValue("faction").jsonPrimitive.content, o.getValue("ids").jsonArray.mapNotNull { i -> i.jsonPrimitive.longOrNull }.toSet(), o.getValue("file").jsonPrimitive.content,
                (o["aliases"] as? JsonArray).orEmpty().map { a -> normalized(a.jsonPrimitive.content) }.toSet())
        }
    }
    fun effectId(value: JsonElement): Long? = when(value) {
        is JsonPrimitive -> value.longOrNull
        is JsonObject -> listOf("galacticEffectId", "GalacticEffectId", "effectId", "id").firstNotNullOfOrNull { (value[it] as? JsonPrimitive)?.longOrNull }
        else -> null
    }
    fun list(planet: Planet): List<PlanetPresence> {
        val effects = PlanetEffects.values(planet)
        val observed = catalog.filter { entry -> effects.any { effect ->
            val id = effectId(effect)
            if (id != null) id in entry.ids
            else !PlanetEffects.hasId(effect) && normalized(PlanetEffects.name(effect)) in entry.aliases
        } }
        return if (!planet.presenceHistoryStale) observed
        else (observed + catalog.filter { it.key in planet.savedPresenceKeys }).distinctBy { it.key }
    }
}

/** Failure/omitted effects preserve dated visual references, never tactical modifiers. */
fun Planet.withSavedPresences(previous: Planet?, missingUnconfirmed: Boolean): Planet {
    if (!missingUnconfirmed) return copy(savedPresenceKeys = emptyList(), presenceHistoryStale = false, savedTcsPresent = false)
    val native = PlanetPresences.list(copy(savedPresenceKeys = emptyList(), presenceHistoryStale = false))
    val same = previous?.takeIf { it.index == index }
    val keys = (native.map { it.key } + same?.let { PlanetPresences.list(it).map { p -> p.key } }.orEmpty()
        + savedPresenceKeys).distinct()
    val savedTcs = savedTcsPresent || same?.let(TcsInfrastructure::has) == true
    return copy(savedPresenceKeys = keys, presenceHistoryStale = true, savedTcsPresent = savedTcs,
        effectsReadAtMillis = if (same != null && same.effectsReadAtMillis > 0L)
            same.effectsReadAtMillis else effectsReadAtMillis)
}

fun planetTitle(raw: String): String = raw.split(Regex("\\s+")).joinToString(" ") {
    if (it.matches(Regex("[IVXLCDM]+", RegexOption.IGNORE_CASE)) || it.matches(Regex("[A-Za-z]?[-]?\\d+"))) it.uppercase()
    else it.lowercase().replaceFirstChar(Char::titlecase)
}
fun campaignLabel(c: Campaign): String = when {
    c.planet.event != null -> "DEFESA"
    c.type == 1 -> "RECONHECIMENTO"
    c.type == 2 -> "CAMPANHA ESPECIAL"
    c.type == null || c.type == 0 -> "LIBERTAÇÃO"
    else -> "CAMPANHA"
}
