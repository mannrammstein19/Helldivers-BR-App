package br.com.helldiversbr.app.ui.screens

import br.com.helldiversbr.app.data.HelldiversApi
import br.com.helldiversbr.app.data.PlanetCatalogEntry

/**
 * Catálogo visual compartilhado pela Central de Guerra nativa.
 * Os arquivos são os mesmos já publicados pelo HELLDIVERS-BR web.
 */
object PlanetVisuals {
    private val biomeImages = mapOf(
        "sandy_base" to "Sandy_base_Landscape.png",
        "sandy_spiky" to "Sandy_spiky_Landscape.png",
        "sandy_acid" to "Sandy_acid_Landscape.png",
        "sandy_mineral" to "Sandy_mineral_Landscape.png",
        "sandy_moon" to "Sandy_moon_Landscape.png",
        "primordial_base" to "Primordial_base_Landscape.png",
        "primordial_dead" to "Primordial_dead_Landscape.png",
        "primordial_purple" to "Primordial_purple_Landscape.png",
        "primordial_blue" to "Primordial_blue_Landscape.png",
        "arctic_glacier_base" to "Arctic_glacier_base_Landscape.png",
        "arctic_glacier_coldrocky" to "Arctic_glacier_coldrocky_Landscape.png",
        "moor_baseplanet" to "Moor_baseplanet_Landscape.png",
        "moor_tundra" to "Moor_tundra_Landscape.png",
        "moor_arid" to "Moor_arid_Landscape.png",
        "moor_red" to "Moor_red_Landscape.png",
        "swamp_base" to "Swamp_base_Landscape.png",
        "swamp_haunted" to "Swamp_haunted_Landscape.png",
        "bug_hiveworld" to "Bug_hiveworld_Landscape.png",
        "supercolony" to "Supercolony_Landscape.png",
        "magma_base" to "Magma_Base_Landscape.png",
        "cyberstan" to "Cyberstan_landscape.png",
        "super_earth" to "Super_Earth_landscape.png",
        "void_source" to "Void_Source_Planet_Landscape_Void_Header.png",
    )

    private val planetSpecific = mapOf(
        262L to "Magma_Base_Landscape.png",
        269L to "Brilliance_Planet_Landscape_Header.jpg",
    )

    private val planetSpecificByName = mapOf(
        "k" to "Magma_Base_Landscape.png",
        "brilliance" to "Brilliance_Planet_Landscape_Header.jpg",
        "luxuriant" to "Luxuriant_Planet_Landscape_Header.png",
        "fronteria" to "Tropical_Oasis_Biome_Header.png",
    )

    data class HazardVisual(
        val key: String,
        val label: String,
        val iconFile: String?,
        val symbol: String,
    )

    private val hazards = mapOf(
        "normal_temp" to HazardVisual("normal_temp", "Temperatura normal", "Normal Temp.png", "♨"),
        "extreme_cold" to HazardVisual("extreme_cold", "Frio extremo", "Extreme Cold.png", "❄"),
        "blizzards" to HazardVisual("blizzards", "Tempestades de neve", "Blizzards.png", "❄"),
        "meteor_storms" to HazardVisual("meteor_storms", "Tempestades de meteoros", "Meteor Storms.png", "☄"),
        "rainstorms" to HazardVisual("rainstorms", "Tempestades de chuva", "Rainstorms.png", "☔"),
        "sandstorms" to HazardVisual("sandstorms", "Tempestades de areia", "Sandstorms.png", "≋"),
        "thick_fog" to HazardVisual("thick_fog", "Névoa densa", "Thick Fog .png", "◌"),
        "tremors" to HazardVisual("tremors", "Tremores", "Tremors .png", "≋"),
        "volcanic_activity" to HazardVisual("volcanic_activity", "Atividade vulcânica", "Volcanic Activity.png", "🌋"),
        "intense_heat" to HazardVisual("intense_heat", "Calor intenso", "Intense Heat.png", "🔥"),
        "fire_tornadoes" to HazardVisual("fire_tornadoes", "Tornados de fogo", "Fire Tornados.png", "🌪"),
        "acid_storms" to HazardVisual("acid_storms", "Tempestades ácidas", "Acid Storms.png", "☣"),
        "heavy_gloom_shroud" to HazardVisual("heavy_gloom_shroud", "Manto de escuridão", "Heavy Gloom Shroud .png", "◐"),
        "ion_storms" to HazardVisual("ion_storms", "Tempestades de íons", "Ion Storms.png", "⚡"),
    )

    fun planetImage(index: Long, name: String, catalog: PlanetCatalogEntry?): String {
        val file = planetSpecific[index]
            ?: planetSpecificByName[name.lowercase().trim()]
            ?: biomeImages[catalog?.biome?.lowercase()?.trim()]
            ?: return ""
        return br.com.helldiversbr.app.data.MapAssets.file("imagens/planetas/$file") ?: ""
    }

    fun biomeLabel(catalog: PlanetCatalogEntry?): String {
        val raw = catalog?.biome.orEmpty()
        if (raw.isBlank()) return "BIOMA DESCONHECIDO"
        return raw.replace('_', ' ').split(' ').joinToString(" ") { part ->
            part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }.uppercase()
    }

    fun hazards(catalog: PlanetCatalogEntry?): List<HazardVisual> {
        val keys = buildList {
            addAll(catalog?.environmentals.orEmpty())
            addAll(catalog?.weather_effects.orEmpty())
        }.map { it.lowercase().trim().replace(' ', '_').replace('-', '_') }
            .filter { it.isNotBlank() && it != "none" }
            .distinct()
        return keys.map { hazards[it] ?: HazardVisual(it, pretty(it), null, "◆") }
    }

    fun hazardIconUrl(item: HazardVisual): String? = item.iconFile?.let {
        br.com.helldiversbr.app.data.MapAssets.file("imagens/ui/efeito-planeta/$it")
    }

    fun factionLogo(raw: String, defense: Boolean = false): String {
        val key = when (br.com.helldiversbr.app.data.OrderRepository.factionKey(raw)) {
            "terminids" -> "terminid"
            "automatons" -> "automaton"
            "illuminates" -> "illuminate"
            else -> "human"
        }
        return br.com.helldiversbr.app.data.MapAssets.file(key).orEmpty()
    }

    private fun pretty(value: String): String = value.replace('_', ' ').split(' ').joinToString(" ") { part ->
        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
