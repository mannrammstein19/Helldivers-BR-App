package br.com.helldiversbr.app.update

import android.content.Context

object UpdatePreferences {
    private const val PREFS = "update_preferences"
    private const val KEY_AUTO_CHECK = "auto_check_on_start"

    fun isAutoCheckEnabled(context: Context): Boolean = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_AUTO_CHECK, true)

    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_CHECK, enabled)
            .apply()
    }
}
