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
import br.com.helldiversbr.app.data.DssTacticalAction
import br.com.helldiversbr.app.data.HomeData
import br.com.helldiversbr.app.data.Planet
import br.com.helldiversbr.app.data.localizedText
import java.util.concurrent.TimeUnit
import kotlin.math.floor

/**
 * Central de alertas configuráveis do HELLDIVERS-BR.
 *
 * A primeira leitura após ativar os alertas vira referência e nunca gera uma enxurrada de
 * notificações antigas. Depois disso, somente transições detectadas em telemetria fresca
 * podem disparar avisos.
 */
object WarAlertManager {
    private const val PREFS = "war_alert_preferences"
    private const val WORK_NAME = "war-alert-watch"
    private const val LEGACY_WORK_NAME = "war-invasion-watch"

    const val CHANNEL_ID = "war_invasion_alerts"

    private const val BASE_CAMPAIGNS_READY = "baseline_campaigns_ready_v2"
    private const val BASE_PLANETS_READY = "baseline_planets_ready_v2"
    private const val BASE_DISPATCH_READY = "baseline_dispatch_ready_v2"
    private const val BASE_ORDER_READY = "baseline_order_ready_v2"
    private const val BASE_DSS_READY = "baseline_dss_ready_v2"

    private const val BASE_CAMPAIGNS = "baseline_campaign_ids_v2"
    private const val BASE_DEFENSES = "baseline_defense_ids_v2"
    private const val BASE_PLANET_OWNERS = "baseline_planet_owners_v2"
    private const val BASE_REGION_OWNERS = "baseline_region_owners_v2"
    private const val BASE_REGION_ATTACKS = "baseline_region_attacks_v2"
    private const val BASE_REGION_DEFENSES = "baseline_region_defenses_v2"
    private const val BASE_DISPATCHES = "baseline_dispatch_ids_v2"
    private const val BASE_ORDER_ID = "baseline_order_id_v2"
    private const val BASE_ORDER_STATE = "baseline_order_state_v2"
    private const val BASE_ORDER_BUCKET = "baseline_order_bucket_v2"
    private const val BASE_DSS_PLANET = "baseline_dss_planet_v2"
    private const val BASE_DSS_ACTIONS = "baseline_dss_actions_v2"
    private const val BASE_DSS_ELECTION = "baseline_dss_election_v2"

    /** Compatibilidade com a preferência antiga de invasão. */
    fun isEnabled(context: Context): Boolean = NotificationPreferences.isMasterEnabled(context)

    /** Compatibilidade com chamadas antigas: agora controla o interruptor geral. */
    fun setEnabled(context: Context, enabled: Boolean) = setMasterEnabled(context, enabled)

    fun setMasterEnabled(context: Context, enabled: Boolean) {
        NotificationPreferences.setMasterEnabled(context, enabled)
        if (enabled) {
            resetBaselines(context)
            schedule(context)
        } else {
            cancel(context)
        }
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas da Guerra Galáctica",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Ordens, campanhas, regiões, notícias e eventos da DSS escolhidos no HELLDIVERS-BR."
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun schedule(context: Context) {
        // Remove o agendamento antigo da V22 para não duplicar verificações após a atualização.
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_WORK_NAME)
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
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_WORK_NAME)
    }

    /**
     * Processa um snapshot completo. Cada família só avança o baseline quando sua fonte não
     * está marcada como cache/stale, impedindo dado velho de virar evento novo.
     */
    @Synchronized
    fun processHomeData(context: Context, data: HomeData) {
        if (!NotificationPreferences.isMasterEnabled(context)) return

        val freshCampaigns = "campanhas" !in data.staleSources
        val freshPlanets = "planetas" !in data.staleSources && data.planets.isNotEmpty()
        val freshDispatches = "despachos" !in data.staleSources
        val freshOrder = "Ordem Maior" !in data.staleSources ||
            (data.order.fromSnapshot && data.order.state in setOf("completed", "failed"))
        val freshDss = !data.dss.stale

        if (freshCampaigns) processCampaignAndPlanetEvents(context, data.campaigns, if (freshPlanets) data.planets else emptyList())
        if (freshPlanets) processPlanetAndRegionOwnership(context, data.planets, data.campaigns, freshCampaigns)
        if (freshDispatches) processDispatches(context, data)
        if (freshOrder) processOrder(context, data)
        if (freshDss) processDss(context, data)
    }

    /** Mantido para compatibilidade; novos chamadores devem usar [processHomeData]. */
    fun processCampaigns(context: Context, campaigns: List<Campaign>) {
        if (!NotificationPreferences.isMasterEnabled(context) || campaigns.isEmpty()) return
        processCampaignAndPlanetEvents(context, campaigns, emptyList())
    }

    private fun processCampaignAndPlanetEvents(context: Context, campaigns: List<Campaign>, planets: List<Planet>) {
        if (campaigns.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val currentCampaigns = campaigns.associateBy { campaignIdentity(it) }
        val currentCampaignKeys = currentCampaigns.keys
        val currentDefenses = campaigns
            .filter { it.planet.event != null }
            .associateBy { defenseIdentity(it) }
        val currentDefenseKeys = currentDefenses.keys

        if (!prefs.getBoolean(BASE_CAMPAIGNS_READY, false)) {
            prefs.edit()
                .putStringSet(BASE_CAMPAIGNS, currentCampaignKeys)
                .putStringSet(BASE_DEFENSES, currentDefenseKeys)
                .putBoolean(BASE_CAMPAIGNS_READY, true)
                .apply()
            return
        }

        val previousCampaigns = prefs.getStringSet(BASE_CAMPAIGNS, emptySet()).orEmpty().toSet()
        val previousDefenses = prefs.getStringSet(BASE_DEFENSES, emptySet()).orEmpty().toSet()

        val newCampaignKeys = currentCampaignKeys - previousCampaigns
        if (NotificationPreferences.isEnabled(context, AlertType.PLANET_NEW_CAMPAIGN)) {
            newCampaignKeys.mapNotNull(currentCampaigns::get).take(3).forEach { campaign ->
                sendAlert(
                    context,
                    AlertType.PLANET_NEW_CAMPAIGN,
                    "NOVA CAMPANHA // ${campaign.planet.nameText.uppercase()}",
                    "Uma nova campanha planetária entrou na Guerra Galáctica.",
                    "campaign-${campaign.planet.index}",
                )
            }
        }

        val newDefenseKeys = currentDefenseKeys - previousDefenses
        if (NotificationPreferences.isEnabled(context, AlertType.PLANET_UNDER_ATTACK)) {
            newDefenseKeys.mapNotNull(currentDefenses::get).take(3).forEach { campaign ->
                sendAlert(
                    context,
                    AlertType.PLANET_UNDER_ATTACK,
                    "PLANETA SOB ATAQUE // ${campaign.planet.nameText.uppercase()}",
                    "Uma nova defesa da Super Terra foi detectada.",
                    "defense-${campaign.planet.index}",
                )
            }
        }

        // Uma defesa que sumiu e continua com posse humana foi concluída com sucesso.
        if (planets.isNotEmpty() && NotificationPreferences.isEnabled(context, AlertType.PLANET_DEFENDED)) {
            val currentDefensePlanetIds = currentDefenses.values.map { it.planet.index }.toSet()
            val previousDefensePlanetIds = previousDefenses.mapNotNull(::planetIdFromDefenseKey).toSet()
            val ended = previousDefensePlanetIds - currentDefensePlanetIds
            val byId = planets.associateBy { it.index }
            ended.mapNotNull(byId::get)
                .filter { isHumanOwner(it.currentOwner) }
                .take(3)
                .forEach { planet ->
                    sendAlert(
                        context,
                        AlertType.PLANET_DEFENDED,
                        "PLANETA DEFENDIDO // ${planet.nameText.uppercase()}",
                        "A defesa foi encerrada com o planeta sob controle da Super Terra.",
                        "defended-${planet.index}",
                    )
                }
        }

        prefs.edit()
            .putStringSet(BASE_CAMPAIGNS, currentCampaignKeys)
            .putStringSet(BASE_DEFENSES, currentDefenseKeys)
            .apply()
    }

    private fun processPlanetAndRegionOwnership(context: Context, planets: List<Planet>, campaigns: List<Campaign>, freshCampaigns: Boolean) {
        if (planets.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val currentOwners = planets.associate { it.index.toString() to normalizedOwner(it.currentOwner) }
            .filterValues { it in setOf("humans", "terminids", "automatons", "illuminate") }
        val regionOwners = linkedMapOf<String, String>()
        val regionAttacks = linkedSetOf<String>()
        val regionDefenses = linkedSetOf<String>()
        val defensePlanetIds = campaigns.filter { it.planet.event != null }.map { it.planet.index }.toSet()

        planets.forEach { planet ->
            planet.regions.forEachIndexed { index, region ->
                if (region.telemetryStale || br.com.helldiversbr.app.data.RegionTelemetry.ownerId(region.owner) == null) return@forEachIndexed
                val key = regionKey(planet, index)
                val owner = normalizedOwner(localizedText(region.owner))
                regionOwners[key] = owner
                val max = region.maxHealth
                val health = region.health ?: max
                if (isHumanOwner(owner) && max > 0L && health > 0L && health < max) regionAttacks += key
                if (freshCampaigns && planet.index in defensePlanetIds && isHumanOwner(owner) && region.isAvailable != false) regionDefenses += key
            }
        }

        if (!prefs.getBoolean(BASE_PLANETS_READY, false)) {
            prefs.edit()
                .putStringSet(BASE_PLANET_OWNERS, encodeMap(currentOwners))
                .putStringSet(BASE_REGION_OWNERS, encodeMap(regionOwners))
                .putStringSet(BASE_REGION_ATTACKS, regionAttacks)
                .putStringSet(BASE_REGION_DEFENSES, regionDefenses)
                .putBoolean(BASE_PLANETS_READY, true)
                .apply()
            return
        }

        val previousOwners = decodeMap(prefs.getStringSet(BASE_PLANET_OWNERS, emptySet()).orEmpty())
        val previousRegionOwners = decodeMap(prefs.getStringSet(BASE_REGION_OWNERS, emptySet()).orEmpty())
        val previousRegionAttacks = prefs.getStringSet(BASE_REGION_ATTACKS, emptySet()).orEmpty().toSet()
        val previousRegionDefenses = prefs.getStringSet(BASE_REGION_DEFENSES, emptySet()).orEmpty().toSet()

        val planetById = planets.associateBy { it.index.toString() }
        currentOwners.forEach { (id, currentOwner) ->
            val previousOwner = previousOwners[id] ?: return@forEach
            val planet = planetById[id] ?: return@forEach
            if (previousOwner in setOf("terminids", "automatons", "illuminate") && isHumanOwner(currentOwner) &&
                NotificationPreferences.isEnabled(context, AlertType.PLANET_LIBERATED)) {
                sendAlert(
                    context,
                    AlertType.PLANET_LIBERATED,
                    "PLANETA LIBERADO // ${planet.nameText.uppercase()}",
                    "O controle do planeta passou para a Super Terra.",
                    "liberated-${planet.index}",
                )
            }
            if (isHumanOwner(previousOwner) && currentOwner in setOf("terminids", "automatons", "illuminate") &&
                NotificationPreferences.isEnabled(context, AlertType.PLANET_LOST)) {
                sendAlert(
                    context,
                    AlertType.PLANET_LOST,
                    "PLANETA PERDIDO // ${planet.nameText.uppercase()}",
                    "O controle do planeta deixou as mãos da Super Terra.",
                    "lost-${planet.index}",
                )
            }
        }

        currentRegionChanges(context, planets, previousRegionOwners, regionOwners)

        if (NotificationPreferences.isEnabled(context, AlertType.REGION_UNDER_ATTACK)) {
            (regionAttacks - previousRegionAttacks).take(3).forEach { key ->
                val label = regionLabel(planets, key)
                sendAlert(context, AlertType.REGION_UNDER_ATTACK, "REGIÃO SOB ATAQUE", label, "region-attack-$key")
            }
        }
        if (NotificationPreferences.isEnabled(context, AlertType.REGION_DEFENSE)) {
            (regionDefenses - previousRegionDefenses).take(3).forEach { key ->
                val label = regionLabel(planets, key)
                sendAlert(context, AlertType.REGION_DEFENSE, "DEFESA DE REGIÃO", label, "region-defense-$key")
            }
        }

        prefs.edit()
            .putStringSet(BASE_PLANET_OWNERS, encodeMap(previousOwners + currentOwners))
            .putStringSet(BASE_REGION_OWNERS, encodeMap(previousRegionOwners + regionOwners))
            .putStringSet(BASE_REGION_ATTACKS, retainUnobserved(previousRegionAttacks, regionOwners.keys, regionAttacks))
            .putStringSet(BASE_REGION_DEFENSES, if (freshCampaigns)
                retainUnobserved(previousRegionDefenses, regionOwners.keys, regionDefenses) else previousRegionDefenses)
            .apply()
    }

    private fun currentRegionChanges(
        context: Context,
        planets: List<Planet>,
        previous: Map<String, String>,
        current: Map<String, String>,
    ) {
        current.forEach { (key, currentOwner) ->
            val previousOwner = previous[key] ?: return@forEach
            if (previousOwner in setOf("terminids", "automatons", "illuminate") && isHumanOwner(currentOwner) &&
                NotificationPreferences.isEnabled(context, AlertType.REGION_LIBERATED)) {
                sendAlert(
                    context,
                    AlertType.REGION_LIBERATED,
                    "REGIÃO LIBERADA",
                    regionLabel(planets, key),
                    "region-liberated-$key",
                )
            }
            if (isHumanOwner(previousOwner) && currentOwner in setOf("terminids", "automatons", "illuminate") &&
                NotificationPreferences.isEnabled(context, AlertType.REGION_LOST)) {
                sendAlert(
                    context,
                    AlertType.REGION_LOST,
                    "REGIÃO PERDIDA",
                    regionLabel(planets, key),
                    "region-lost-$key",
                )
            }
        }
    }

    private fun processDispatches(context: Context, data: HomeData) {
        val current = data.dispatches.map { it.id.toString() }.filter { it != "0" }.toSet()
        if (current.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(BASE_DISPATCH_READY, false)) {
            prefs.edit().putStringSet(BASE_DISPATCHES, current).putBoolean(BASE_DISPATCH_READY, true).apply()
            return
        }
        val previous = prefs.getStringSet(BASE_DISPATCHES, emptySet()).orEmpty().toSet()
        val newIds = current - previous
        if (newIds.isNotEmpty() && NotificationPreferences.isEnabled(context, AlertType.NEWS)) {
            val newest = data.dispatches.firstOrNull { it.id.toString() in newIds }
            if (newest != null) {
                sendAlert(
                    context,
                    AlertType.NEWS,
                    "NOVO DESPACHO DO MINISTÉRIO",
                    newest.text.ifBlank { "Uma nova transmissão foi publicada." }.take(220),
                    "dispatch-${newest.id}",
                )
            }
        }
        prefs.edit().putStringSet(BASE_DISPATCHES, current).apply()
    }

    private fun processOrder(context: Context, data: HomeData) {
        val order = data.order.order ?: return
        val id = order.id?.toString().orEmpty()
        if (id.isBlank()) return
        val state = data.order.state
        val bucket = progressBucket(data.order.percent)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        if (!prefs.getBoolean(BASE_ORDER_READY, false)) {
            prefs.edit()
                .putString(BASE_ORDER_ID, id)
                .putString(BASE_ORDER_STATE, state)
                .putInt(BASE_ORDER_BUCKET, bucket)
                .putBoolean(BASE_ORDER_READY, true)
                .apply()
            return
        }

        val previousId = prefs.getString(BASE_ORDER_ID, "").orEmpty()
        val previousState = prefs.getString(BASE_ORDER_STATE, "").orEmpty()
        val previousBucket = prefs.getInt(BASE_ORDER_BUCKET, 0)

        if (id != previousId && NotificationPreferences.isEnabled(context, AlertType.ORDER_NEW)) {
            sendAlert(
                context,
                AlertType.ORDER_NEW,
                "NOVA ORDEM PRINCIPAL",
                order.titleText.ifBlank { order.briefingText }.ifBlank { "O Alto Comando emitiu uma nova Ordem Maior." }.take(220),
                "order-new-$id",
            )
        } else if (id == previousId && bucket > previousBucket && bucket in 10..90 &&
            NotificationPreferences.isEnabled(context, AlertType.ORDER_PROGRESS)) {
            sendAlert(
                context,
                AlertType.ORDER_PROGRESS,
                "ORDEM PRINCIPAL // PROGRESSO",
                "A Ordem Principal alcançou aproximadamente $bucket% de progresso.",
                "order-progress-$id-$bucket",
            )
        }

        val completedNow = state == "completed" || data.order.percent >= 100.0
        val completedBefore = previousState == "completed" || previousBucket >= 100
        if (id == previousId && completedNow && !completedBefore &&
            NotificationPreferences.isEnabled(context, AlertType.ORDER_COMPLETED)) {
            sendAlert(
                context,
                AlertType.ORDER_COMPLETED,
                "ORDEM PRINCIPAL CONCLUÍDA",
                "Objetivos concluídos. Abra o HELLDIVERS-BR para ver o resultado.",
                "order-complete-$id",
            )
        }

        prefs.edit()
            .putString(BASE_ORDER_ID, id)
            .putString(BASE_ORDER_STATE, state)
            .putInt(BASE_ORDER_BUCKET, bucket)
            .apply()
    }

    private fun processDss(context: Context, data: HomeData) {
        val station = data.dss.station ?: return
        val planetId = station.planet.index
        val activeActions = station.tacticalActions.filter(::isDssActionActive).map { it.id32.toString() }.toSet()
        val election = station.electionEnd
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        if (!prefs.getBoolean(BASE_DSS_READY, false)) {
            prefs.edit()
                .putLong(BASE_DSS_PLANET, planetId)
                .putStringSet(BASE_DSS_ACTIONS, activeActions)
                .putString(BASE_DSS_ELECTION, election)
                .putBoolean(BASE_DSS_READY, true)
                .apply()
            return
        }

        val previousPlanet = prefs.getLong(BASE_DSS_PLANET, 0L)
        val previousActions = prefs.getStringSet(BASE_DSS_ACTIONS, emptySet()).orEmpty().toSet()
        val previousElection = prefs.getString(BASE_DSS_ELECTION, "").orEmpty()

        if (planetId > 0L && previousPlanet > 0L && planetId != previousPlanet &&
            NotificationPreferences.isEnabled(context, AlertType.DSS_RELOCATED)) {
            sendAlert(
                context,
                AlertType.DSS_RELOCATED,
                "DSS REALOCADA",
                "A Estação Espacial da Democracia foi realocada para ${station.planet.nameText}.",
                "dss-move-$planetId",
            )
        }

        if (NotificationPreferences.isEnabled(context, AlertType.DSS_TACTICAL_ACTIVE)) {
            val newActions = activeActions - previousActions
            newActions.take(3).forEach { id ->
                val action = station.tacticalActions.firstOrNull { it.id32.toString() == id }
                sendAlert(
                    context,
                    AlertType.DSS_TACTICAL_ACTIVE,
                    "AÇÃO TÁTICA DA DSS ATIVA",
                    action?.name?.ifBlank { "Uma nova ação tática entrou em operação." }
                        ?: "Uma nova ação tática entrou em operação.",
                    "dss-action-$id",
                )
            }
        }

        if (election.isNotBlank() && previousElection.isNotBlank() && election != previousElection &&
            NotificationPreferences.isEnabled(context, AlertType.DSS_RELOCATION_VOTE)) {
            sendAlert(
                context,
                AlertType.DSS_RELOCATION_VOTE,
                "VOTAÇÃO DE REALOCAÇÃO DA DSS",
                "Uma nova janela de votação da Estação Espacial da Democracia foi detectada.",
                "dss-election-${election.hashCode()}",
            )
        }

        prefs.edit()
            .putLong(BASE_DSS_PLANET, planetId)
            .putStringSet(BASE_DSS_ACTIONS, activeActions)
            .putString(BASE_DSS_ELECTION, election)
            .apply()
    }

    private fun resetBaselines(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(BASE_CAMPAIGNS_READY)
            .remove(BASE_PLANETS_READY)
            .remove(BASE_DISPATCH_READY)
            .remove(BASE_ORDER_READY)
            .remove(BASE_DSS_READY)
            .remove(BASE_CAMPAIGNS)
            .remove(BASE_DEFENSES)
            .remove(BASE_PLANET_OWNERS)
            .remove(BASE_REGION_OWNERS)
            .remove(BASE_REGION_ATTACKS)
            .remove(BASE_REGION_DEFENSES)
            .remove(BASE_DISPATCHES)
            .remove(BASE_ORDER_ID)
            .remove(BASE_ORDER_STATE)
            .remove(BASE_ORDER_BUCKET)
            .remove(BASE_DSS_PLANET)
            .remove(BASE_DSS_ACTIONS)
            .remove(BASE_DSS_ELECTION)
            .apply()
    }

    private fun campaignIdentity(campaign: Campaign): String =
        "${campaign.planet.index}:${campaign.id?.toString().orEmpty()}"

    private fun defenseIdentity(campaign: Campaign): String =
        "${campaign.planet.index}:${campaign.planet.event?.id ?: 0L}"

    private fun planetIdFromDefenseKey(key: String): Long? = key.substringBefore(':').toLongOrNull()

    private fun regionKey(planet: Planet, index: Int): String {
        val region = planet.regions[index]
        val stable = region.hash?.takeIf { it != 0L }?.toString()
            ?: localizedText(region.name).ifBlank { index.toString() }
        return "${planet.index}:$stable"
    }

    private fun regionLabel(planets: List<Planet>, key: String): String {
        val planetId = key.substringBefore(':').toLongOrNull()
        val stable = key.substringAfter(':', "")
        val planet = planets.firstOrNull { it.index == planetId }
        val region = planet?.regions?.firstOrNull { region ->
            region.hash?.toString() == stable || localizedText(region.name) == stable
        }
        val regionName = region?.let { localizedText(it.name) }.orEmpty().ifBlank { "Uma região" }
        val planetName = planet?.nameText.orEmpty()
        return if (planetName.isBlank()) regionName else "$regionName em $planetName."
    }

    private fun normalizedOwner(raw: String): String = raw.trim().lowercase().let { owner ->
        when (owner) {
            "1", "human", "humans", "super earth", "super terra" -> "humans"
            "2", "terminid", "terminids", "terminídeos", "terminideos" -> "terminids"
            "3", "automaton", "automatons", "autômatos", "automatos" -> "automatons"
            "4", "illuminate", "illuminates", "iluminados" -> "illuminate"
            else -> owner
        }
    }

    private fun isHumanOwner(raw: String): Boolean = normalizedOwner(raw) == "humans"

    private fun progressBucket(percent: Double): Int =
        (floor(percent.coerceIn(0.0, 100.0) / 10.0) * 10.0).toInt()

    private fun isDssActionActive(action: DssTacticalAction): Boolean {
        if (action.status == 2) return true
        val raw = listOf(
            localizedText(action.statusName),
            localizedText(action.state),
            localizedText(action.statusText),
        ).joinToString(" ").lowercase()
        return Regex("(^|\\b)(active|ativa|activated|ativada)(\\b|$)").containsMatchIn(raw)
    }

    private fun encodeMap(map: Map<String, String>): Set<String> =
        map.mapTo(linkedSetOf()) { (key, value) -> "$key\u001F$value" }

    private fun decodeMap(values: Set<String>): Map<String, String> = values.mapNotNull { raw ->
        val split = raw.indexOf('\u001F')
        if (split <= 0) null else raw.substring(0, split) to raw.substring(split + 1)
    }.toMap()

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun sendAlert(
        context: Context,
        type: AlertType,
        title: String,
        body: String,
        eventKey: String,
    ) {
        if (!canNotify(context) || !NotificationPreferences.isEnabled(context, type)) return
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val requestCode = (type.prefKey + eventKey).hashCode()
        val pending = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
        } catch (_: SecurityException) {
            // A permissão pode ser revogada entre a verificação e o envio.
        }
    }
}
