package br.com.helldiversbr.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.text.Normalizer

@Serializable
data class StratagemEntry(
    val name: String, val category: String, val permission: String,
    val icon: String, val path: String, val code: String,
    val cooldown: String, val cost: String, val level: String, val source: String,
    val localIcon: String = "",
)

object StratagemCatalog {
    fun load(context: Context): List<StratagemEntry> = context.assets.open("stratagems.json")
        .bufferedReader().use { Json.decodeFromString(ListSerializer(StratagemEntry.serializer()), it.readText()) }
}

fun searchKey(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(java.util.Locale.ROOT)

@Serializable
data class StratagemImage(val url: String, val label: String = "")
@Serializable
data class StratagemCell(val text: String = "", val images: List<StratagemImage> = emptyList())
@Serializable
data class StratagemSection(val title: String, val rows: List<List<StratagemCell>> = emptyList())
@Serializable
data class StratagemDetail(val path: String, val image: String, val paragraphs: List<String> = emptyList(),
                           val sections: List<StratagemSection> = emptyList(), val available: Boolean = false)

object StratagemDetails {
    @Volatile private var cache: List<StratagemDetail>? = null
    fun load(context: Context): List<StratagemDetail> = cache ?: context.assets.open("stratagem-details.json")
        .bufferedReader().use { Json.decodeFromString(ListSerializer(StratagemDetail.serializer()), it.readText()) }
        .also { cache = it }
}

fun StratagemEntry.imageModel(): String = if (localIcon.isNotBlank()) "file:///android_asset/$localIcon" else "${HelldiversApi.SITE_BASE}/$icon"
