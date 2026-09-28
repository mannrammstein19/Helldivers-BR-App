package br.com.helldiversbr.app.update

import br.com.helldiversbr.app.BuildConfig
import br.com.helldiversbr.app.data.HelldiversApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Formato de versao-app.json (publicado na raiz do site, junto do APK):
 * {
 *   "versionCode": 2,
 *   "versionName": "1.1.0",
 *   "apkUrl": "https://.../Helldivers-BR.apk",
 *   "notes": "O que mudou nesta versão"
 * }
 */
@Serializable
data class RemoteVersion(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val notes: String = "",
)

object UpdateChecker {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Retorna a versão remota se ela for mais nova que a instalada; senão null. Nunca lança exceção. */
    suspend fun check(): RemoteVersion? = withContext(Dispatchers.IO) {
        runCatching {
            // "?t=" evita cache de CDN/GitHub Pages devolver um arquivo antigo.
            val url = "${HelldiversApi.SITE_BASE}/versao-app.json?t=${System.currentTimeMillis() / 60000}"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val remote = json.decodeFromString(RemoteVersion.serializer(), resp.body?.string().orEmpty())
                if (remote.versionCode > BuildConfig.VERSION_CODE && remote.apkUrl.startsWith("https://")) remote else null
            }
        }.getOrNull()
    }
}
