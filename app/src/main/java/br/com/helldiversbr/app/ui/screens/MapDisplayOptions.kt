package br.com.helldiversbr.app.ui.screens

import android.content.Context

data class MapDisplayOptions(
    val clean: Boolean = false, val names: Boolean = true, val players: Boolean = true,
    val progress: Boolean = true, val presences: Boolean = true, val ships: Boolean = true,
    val stripes: Boolean = true, val motion: Boolean = true,
) {
    fun save(context: Context) {
        context.getSharedPreferences("map-display", Context.MODE_PRIVATE).edit().apply {
            putBoolean("clean", clean); putBoolean("names", names); putBoolean("players", players)
            putBoolean("progress", progress); putBoolean("presences", presences); putBoolean("ships", ships)
            putBoolean("stripes", stripes); remove("bulletin"); putBoolean("motion", motion)
        }.apply()
    }
    companion object {
        fun load(context: Context): MapDisplayOptions {
            val p=context.getSharedPreferences("map-display", Context.MODE_PRIVATE)
            return MapDisplayOptions(p.getBoolean("clean",false),p.getBoolean("names",true),p.getBoolean("players",true),
                p.getBoolean("progress",true),p.getBoolean("presences",true),p.getBoolean("ships",true),
                p.getBoolean("stripes",true),p.getBoolean("motion",true))
        }
        fun preset(clean: Boolean) = MapDisplayOptions(clean, !clean, !clean, !clean, true, !clean, !clean, !clean)
    }
}
