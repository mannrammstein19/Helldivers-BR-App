package br.com.helldiversbr.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.com.helldiversbr.app.data.HelldiversApi

/** Foreground-only, opt-in anthem. Never starts automatically or changes system volume. */
class AnthemPlayer(context: Context) {
    private val prefs = context.getSharedPreferences("hdbr_anthem", 0)
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private var media: MediaPlayer? = null
    private var ready = false
    private var ducked = false
    var playing by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var volume by mutableStateOf(prefs.getFloat("volume", .30f).coerceIn(0f, 1f)); private set
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { ducked = true; applyVolume() }
                AudioManager.AUDIOFOCUS_GAIN -> { ducked = false; applyVolume() }
            }
        }.build()

    fun updateVolume(value: Float) { volume = value.coerceIn(0f, 1f); applyVolume() }
    fun saveVolume() { prefs.edit().putFloat("volume", volume).apply() }
    private fun applyVolume() { val v = volume * if (ducked) .2f else 1f; media?.setVolume(v, v) }
    fun toggle() { if (playing || loading) pause() else play() }
    private fun startPrepared() {
        if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            error = "Áudio ocupado. Tente tocar novamente."; return
        }
        try { applyVolume(); media?.start(); playing = true; error = null }
        catch (_: Exception) { fail() }
    }
    private fun play() {
        error = null
        if (ready) { startPrepared(); return }
        loading = true
        val player = MediaPlayer()
        media = player
        try {
            player.setAudioAttributes(attributes)
            player.isLooping = true
            player.setOnPreparedListener {
                if (media === it) { loading = false; ready = true; startPrepared() }
            }
            player.setOnErrorListener { failed, _, _ -> if (media === failed) fail(); true }
            player.setDataSource("${HelldiversApi.SITE_BASE}/audio/hino-super-terra.mp3")
            player.prepareAsync()
        } catch (_: Exception) { fail() }
    }
    private fun fail() { release(); error = "Não foi possível carregar o hino. Confira a conexão e tente novamente." }
    fun pause() {
        if (loading) { release(); return }
        if (playing) runCatching { media?.pause() }
        playing = false; ducked = false
        manager.abandonAudioFocusRequest(focus)
    }
    fun release() {
        val old = media; media = null
        old?.setOnPreparedListener(null); old?.setOnErrorListener(null)
        old?.release()
        ready = false; loading = false; playing = false; ducked = false
        manager.abandonAudioFocusRequest(focus)
    }
}
