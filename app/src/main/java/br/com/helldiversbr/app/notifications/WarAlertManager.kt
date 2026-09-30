package br.com.helldiversbr.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import br.com.helldiversbr.app.MainActivity
import br.com.helldiversbr.app.R
import br.com.helldiversbr.app.data.Campaign
import java.util.concurrent.TimeUnit

object WarAlertManager {
    private const val PREFS = "war_alert_preferences"
    private const val KEY_ENABLED = "planet_invasion_alerts"
    private const val KEY_INITIALIZED = "defense_baseline_initialized"
    private const val KEY_DEFENSES = "known_defense_events"
    const val CHANNEL_ID = "war_invasion_alerts"
    private const val WORK_NAME = "war-invasion-watch"

    fun isEnabled(context: Context): Boolean = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit().putBoolean(KEY_ENABLED, enabled)
        if (enabled) {
            // A primeira leitura depois de ativar vira referência; não notificamos uma defesa antiga.
            editor.putBoolean(KEY_INITIALIZED, false).remove(KEY_DEFENSES)
        }
        editor.apply()
        if (enabled) schedule(context) else cancel(context)
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de invasão",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Avisos quando uma nova defesa planetária é detectada."
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<WarAlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Compara as defesas atuais com a última leitura conhecida e dispara um único alerta
     * para os eventos realmente novos. Também é chamado com o app aberto, então a reação
     * em primeiro plano acompanha a telemetria de 60 s.
     */
    fun processCampaigns(context: Context, campaigns: List<Campaign>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return

        val defenses = campaigns.filter { it.planet.event != null }
        val current = defenses.mapNotNull { campaign ->
            val event = campaign.planet.event ?: return@mapNotNull null
            "${campaign.planet.index}:${event.id}"
        }.toSet()

        val initialized = prefs.getBoolean(KEY_INITIALIZED, false)
        if (!initialized) {
            prefs.edit()
                .putStringSet(KEY_DEFENSES, current)
                .putBoolean(KEY_INITIALIZED, true)
                .apply()
            return
        }

        val previous = prefs.getStringSet(KEY_DEFENSES, emptySet()).orEmpty().toSet()
        val freshKeys = current - previous
        prefs.edit().putStringSet(KEY_DEFENSES, current).apply()
        if (freshKeys.isEmpty()) return

        val newDefenses = defenses.filter { campaign ->
            val event = campaign.planet.event ?: return@filter false
            "${campaign.planet.index}:${event.id}" in freshKeys
        }
        notify(context, newDefenses)
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun notify(context: Context, campaigns: List<Campaign>) {
        if (campaigns.isEmpty() || !canNotify(context)) return
        createChannel(context)

        val names = campaigns.map { it.planet.nameText }.distinct()
        val title = if (names.size == 1) "ALERTA DE INVASÃO // ${names.first().uppercase()}" else "ALERTA DE INVASÃO // ${names.size} PLANETAS"
        val body = if (names.size == 1) {
            "A Super Terra iniciou uma defesa planetária. Abra a Central de Guerra para acompanhar."
        } else {
            "Novas defesas detectadas: ${names.take(4).joinToString()}."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            1204,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        NotificationManagerCompat.from(context).notify(0x4844, notification)
    }
}
