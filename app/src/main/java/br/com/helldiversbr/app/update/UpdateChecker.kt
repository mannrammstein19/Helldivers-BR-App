package br.com.helldiversbr.app.update

import br.com.helldiversbr.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Metadados da versão publicada do app. */
@Serializable
data class RemoteVersion(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val notes: String = "",
)

sealed interface UpdateCheckResult {
    data class Available(val version: RemoteVersion) : UpdateCheckResult
    data object Latest : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

object UpdateChecker {
    private const val VERSION_URL =
        "https://raw.githubusercontent.com/mannrammstein19/Helldivers-BR-App/main/versao-app.json"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun checkResult(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = "$VERSION_URL?t=${System.currentTimeMillis() / 60000}"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext UpdateCheckResult.Failed("Servidor respondeu HTTP ${resp.code}.")
                val remote = json.decodeFromString(RemoteVersion.serializer(), resp.body?.string().orEmpty())
                if (remote.versionCode > BuildConfig.VERSION_CODE && remote.apkUrl.startsWith("https://")) {
                    UpdateCheckResult.Available(remote)
                } else {
                    UpdateCheckResult.Latest
                }
            }
        } catch (error: Exception) {
            UpdateCheckResult.Failed(error.message ?: "Não foi possível verificar a atualização.")
        }
    }

    /** Retorna a versão remota se ela for mais nova que a instalada; senão null. Nunca lança exceção. */
    suspend fun check(): RemoteVersion? = when (val result = checkResult()) {
        is UpdateCheckResult.Available -> result.version
        UpdateCheckResult.Latest -> null
        is UpdateCheckResult.Failed -> null
    }
}
