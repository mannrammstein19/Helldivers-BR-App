package br.com.helldiversbr.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import br.com.helldiversbr.app.notifications.WarAlertManager

/** One cache/decoder for the original PNG, WebP and SVG assets of the portal. */
class HelldiversApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        WarAlertManager.createChannel(this)
        if (WarAlertManager.isEnabled(this)) WarAlertManager.schedule(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(SvgDecoder.Factory()) }
        .build()
}
