package br.com.helldiversbr.app.ui.screens

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Movie

/** Movie decodes GIF disposal/timing; a software bitmap is safe on the Compose hardware canvas. */
@Suppress("DEPRECATION")
internal class MapGifSprite private constructor(private val movie: Movie) {
    private val bitmap = Bitmap.createBitmap(movie.width(), movie.height(), Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)
    private var lastTime = -1

    fun frameAt(seconds: Float, moving: Boolean): Bitmap {
        val time = if (moving) mapGifTimeMillis(seconds, movie.duration()) else 0
        if (time != lastTime) {
            movie.setTime(time)
            bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            movie.draw(canvas, 0f, 0f)
            lastTime = time
        }
        return bitmap
    }

    companion object {
        fun load(assets: AssetManager, path: String): MapGifSprite? {
            val movie = try { assets.open(path).use { Movie.decodeStream(it) } }
                catch (_: java.io.IOException) { null }
            return movie?.takeIf { it.width() > 0 && it.height() > 0 }?.let(::MapGifSprite)
        }
    }
}
