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
    private val edgeMask = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.RadialGradient(movie.width() / 2f, movie.height() / 2f, movie.height() / 2f,
            intArrayOf(android.graphics.Color.WHITE, android.graphics.Color.WHITE, android.graphics.Color.TRANSPARENT),
            floatArrayOf(0f, .48f, 1f), android.graphics.Shader.TileMode.CLAMP)
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
    }
    private var softenEdges = false

    fun frameAt(seconds: Float, moving: Boolean): Bitmap {
        val time = if (moving) mapGifTimeMillis(seconds, movie.duration()) else 0
        if (time != lastTime) {
            movie.setTime(time)
            bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            movie.draw(canvas, 0f, 0f)
            if (softenEdges) canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), edgeMask)
            lastTime = time
        }
        return bitmap
    }

    companion object {
        fun load(assets: AssetManager, path: String): MapGifSprite? {
            val movie = try { assets.open(path).use { Movie.decodeStream(it) } }
                catch (_: java.io.IOException) { null }
            return movie?.takeIf { it.width() > 0 && it.height() > 0 }?.let { MapGifSprite(it).apply { softenEdges = path.endsWith("meridia.gif") } }
        }
    }
}
