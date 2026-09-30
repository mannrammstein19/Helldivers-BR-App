package br.com.helldiversbr.app.notifications

import android.content.Context

enum class AlertGroup {
    PLANETS,
    REGIONS,
    NEWS,
    MAJOR_ORDER,
    DSS,
}

enum class AlertType(
    val prefKey: String,
    val group: AlertGroup,
    val label: String,
    val defaultEnabled: Boolean = false,
) {
    PLANET_NEW_CAMPAIGN("planet_new_campaign", AlertGroup.PLANETS, "Nova Campanha Planetária", true),
    PLANET_LIBERATED("planet_liberated", AlertGroup.PLANETS, "Planeta Liberado"),
    PLANET_UNDER_ATTACK("planet_under_attack", AlertGroup.PLANETS, "Planeta Sob Ataque", true),
    PLANET_DEFENDED("planet_defended", AlertGroup.PLANETS, "Planeta Defendido", true),
    PLANET_LOST("planet_lost", AlertGroup.PLANETS, "Planeta Perdido", true),

    REGION_UNDER_ATTACK("region_under_attack", AlertGroup.REGIONS, "Região Sob Ataque"),
    REGION_LIBERATED("region_liberated", AlertGroup.REGIONS, "Região Liberada"),
    REGION_DEFENSE("region_defense", AlertGroup.REGIONS, "Defesa de Região"),
    REGION_LOST("region_lost", AlertGroup.REGIONS, "Região Perdida"),

    NEWS("news", AlertGroup.NEWS, "Notícias"),

    ORDER_NEW("order_new", AlertGroup.MAJOR_ORDER, "Nova Ordem Principal", true),
    ORDER_PROGRESS("order_progress", AlertGroup.MAJOR_ORDER, "Progresso da Ordem Principal"),
    ORDER_COMPLETED("order_completed", AlertGroup.MAJOR_ORDER, "Ordem Principal Concluída", true),

    DSS_RELOCATED("dss_relocated", AlertGroup.DSS, "DSS Realocada", true),
    DSS_TACTICAL_ACTIVE("dss_tactical_active", AlertGroup.DSS, "Ação Tática Ativa", true),
    DSS_RELOCATION_VOTE("dss_relocation_vote", AlertGroup.DSS, "Votação de Realocação da DSS", true),
}

object NotificationPreferences {
    private const val PREFS = "war_alert_preferences"
    private const val KEY_MASTER = "alerts_master_enabled"
    private const val LEGACY_KEY_ENABLED = "planet_invasion_alerts"
    private const val KEY_MIGRATED = "custom_alerts_migrated_v1"

    fun isMasterEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        migrateIfNeeded(context)
        return prefs.getBoolean(KEY_MASTER, false)
    }

    fun setMasterEnabled(context: Context, enabled: Boolean) {
        migrateIfNeeded(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MASTER, enabled)
            .apply()
    }

    fun isEnabled(context: Context, type: AlertType): Boolean {
        migrateIfNeeded(context)
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(type.prefKey, type.defaultEnabled)
    }

    fun setEnabled(context: Context, type: AlertType, enabled: Boolean) {
        migrateIfNeeded(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(type.prefKey, enabled)
            .apply()
    }

    fun types(group: AlertGroup): List<AlertType> = AlertType.entries.filter { it.group == group }

    fun enabledCount(context: Context, group: AlertGroup): Int = types(group).count { isEnabled(context, it) }

    fun setGroupEnabled(context: Context, group: AlertGroup, enabled: Boolean) {
        migrateIfNeeded(context)
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        types(group).forEach { editor.putBoolean(it.prefKey, enabled) }
        editor.apply()
    }

    private fun migrateIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATED, false)) return

        val legacyEnabled = prefs.getBoolean(LEGACY_KEY_ENABLED, false)
        val editor = prefs.edit()
            .putBoolean(KEY_MASTER, legacyEnabled)
            .putBoolean(KEY_MIGRATED, true)

        AlertType.entries.forEach { type ->
            if (!prefs.contains(type.prefKey)) {
                val enabled = when (type) {
                    AlertType.PLANET_UNDER_ATTACK -> legacyEnabled || type.defaultEnabled
                    else -> type.defaultEnabled
                }
                editor.putBoolean(type.prefKey, enabled)
            }
        }
        editor.apply()
    }
}
