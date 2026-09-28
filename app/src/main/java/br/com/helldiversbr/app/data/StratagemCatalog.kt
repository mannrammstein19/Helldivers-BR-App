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
)

object StratagemCatalog {
    fun load(context: Context): List<StratagemEntry> = context.assets.open("stratagems.json")
        .bufferedReader().use { Json.decodeFromString(ListSerializer(StratagemEntry.serializer()), it.readText()) }
}

fun searchKey(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(java.util.Locale.ROOT)
