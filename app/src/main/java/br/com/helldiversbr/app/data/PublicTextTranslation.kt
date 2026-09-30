package br.com.helldiversbr.app.data

import android.content.Context
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Same provider as ptbr.js. Only public order/dispatch text is sent. */
object PublicTextTranslation {
    private val lock = Mutex()
    private val failedUntil = mutableMapOf<String, Long>()
    private val client = OkHttpClient.Builder().callTimeout(12, TimeUnit.SECONDS).build()
    private val englishWords = setOf("the", "and", "of", "to", "our", "must", "have", "has", "with", "from", "will", "are", "their", "this", "that", "order", "major", "liberate", "defend", "against", "kill", "enemies", "hold", "designated")
    private val portugueseWords = setOf("o", "a", "os", "as", "de", "da", "do", "dos", "das", "para", "uma", "um", "em", "com", "não", "ordem", "maior", "libertar", "defender", "terra")
    fun needsTranslation(text: String): Boolean {
        val words = Regex("[\\p{L}]+").findAll(text.lowercase()).map { it.value }.toList()
        return words.count { it in englishWords } > words.count { it in portugueseWords }
    }
    fun cached(context: Context, text: String): String? = context.getSharedPreferences("hdbr_public_ptbr", 0).getString(key(text), null)
    private fun key(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    suspend fun translate(context: Context, text: String, retry: Boolean = false): String = withContext(Dispatchers.IO) {
        val known = translateKnown(text)
        if (known != text || !needsTranslation(text)) return@withContext known
        lock.withLock {
            cached(context, text)?.let { return@withLock it }
            val key = key(text)
            if (!retry && (failedUntil[key] ?: 0L) > System.currentTimeMillis()) error("Tradução temporariamente indisponível")
            try {
                // UTF-8 safe chunks, comfortably below MyMemory's 500-byte query limit.
                val chunks = mutableListOf<String>()
                var chunk = ""
                text.split(Regex("\\s+")).forEach { word ->
                    if ((chunk + " " + word).toByteArray().size > 450 && chunk.isNotEmpty()) {
                        chunks += chunk; chunk = ""
                    }
                    for (ch in word) {
                        if ((chunk + ch).toByteArray().size > 450) { chunks += chunk; chunk = "" }
                        chunk += ch
                    }
                    chunk += " "
                }
                if (chunk.isNotBlank()) chunks += chunk.trim()
                val translated = chunks.map { part ->
                    val url = "https://api.mymemory.translated.net/get".toHttpUrl().newBuilder()
                        .addQueryParameter("q", part).addQueryParameter("langpair", "en|pt-br").build()
                    client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                        check(response.isSuccessful)
                        val data = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                        check(data["responseStatus"]?.jsonPrimitive?.intOrNull == 200)
                        check(data["quotaFinished"]?.jsonPrimitive?.booleanOrNull != true)
                        val value = data["responseData"]?.jsonObject?.get("translatedText")?.jsonPrimitive?.content.orEmpty()
                        check(value.isNotBlank())
                        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
                    }
                }.joinToString(" ")
                val prefs = context.getSharedPreferences("hdbr_public_ptbr", 0)
                val edit = prefs.edit()
                if (prefs.all.size >= 200) prefs.all.keys.take(prefs.all.size - 199).forEach { edit.remove(it) }
                edit.putString(key, translated).apply()
                failedUntil.remove(key)
                translated
            } catch (e: Exception) {
                failedUntil[key] = System.currentTimeMillis() + 300_000L
                throw e
            }
        }
    }
}
