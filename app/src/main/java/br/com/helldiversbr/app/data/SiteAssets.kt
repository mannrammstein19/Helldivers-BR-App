package br.com.helldiversbr.app.data

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object SiteAssets {
    @Volatile private var cache: Map<String, List<String>>? = null
    fun all(context: Context): Map<String, List<String>> = cache ?: synchronized(this) {
        cache ?: context.assets.open("site-assets.json").bufferedReader().use {
            Json.decodeFromString(MapSerializer(String.serializer(), ListSerializer(String.serializer())), it.readText())
        }.also { cache = it }
    }
    fun urls(context: Context, key: String): List<String> = MapAssets.file(key)?.let { listOf(it) }
        ?: all(context)[key].orEmpty().map { "${HelldiversApi.SITE_BASE}/$it" }
    fun orderKey(state: String): String = when (state) {
        "completed" -> "order_completed"
        "failed" -> "order_failed"
        else -> "order_active"
    }
}
