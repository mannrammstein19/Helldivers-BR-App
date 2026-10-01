package br.com.helldiversbr.app.ui.presentation

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import br.com.helldiversbr.app.data.Campaign
import br.com.helldiversbr.app.data.HomeData
import br.com.helldiversbr.app.data.OrderRepository
import br.com.helldiversbr.app.data.Planet
import kotlinx.coroutines.delay

private class VisualClock {
    var now by mutableLongStateOf(System.currentTimeMillis())
}
private data class VisualNumbers(val frame: NumberFrame, val enabled: Boolean, val clock: VisualClock)
private val LocalNumbers = staticCompositionLocalOf { VisualNumbers(NumberFrame(), false, VisualClock()) }

@Composable
fun ProvideVisualNumbers(frame: NumberFrame, enabled: Boolean, content: @Composable () -> Unit) {
    val clock = remember { VisualClock() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val moving = frame.entries.values.any { it.perSecond != 0.0 }
    LaunchedEffect(lifecycle, enabled, moving) {
        clock.now = System.currentTimeMillis()
        if (enabled && moving) lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                clock.now = System.currentTimeMillis()
                delay(250)
            }
        }
    }
    CompositionLocalProvider(LocalNumbers provides VisualNumbers(frame, enabled, clock), content = content)
}

@Composable
fun visualCampaignPercent(campaign: Campaign): Double {
    val actual = OrderRepository.campaignPercent(campaign)
    val numbers = LocalNumbers.current
    return if (numbers.enabled) numbers.frame.value(progressKey(campaign), actual, numbers.clock.now) else actual
}

@Composable
fun visualPlanetCounter(planet: Planet, field: String, actual: Long): Long {
    val numbers = LocalNumbers.current
    if (!numbers.enabled) return actual
    val shown = numbers.frame.value(counterKey(planet.index, field), actual.toDouble(), numbers.clock.now)
    // Conserva a parte inteira original inclusive para contadores grandes demais para Double.
    val delta = (shown - actual.toDouble()).coerceAtLeast(0.0).toLong()
    return actual + delta.coerceAtMost(Long.MAX_VALUE - actual.coerceAtLeast(0))
}

fun progressKey(campaign: Campaign): String = "progress:${OrderRepository.campaignKey(campaign)}"
fun counterKey(planet: Long, field: String): String = "stats:$planet:$field"

fun observations(data: HomeData): List<NumberObservation> = buildList {
    if (data.telemetrySource == "cache") return@buildList
    if ("campanhas" !in data.staleSources) data.campaigns.forEach { campaign ->
        val p = campaign.planet
        add(NumberObservation(progressKey(campaign), OrderRepository.campaignPercent(campaign), true,
            "${data.telemetrySource}:${p.currentOwner}:${p.maxHealth}:${p.event?.maxHealth}"))
    }
    val planets = (if ("planetas" !in data.staleSources) data.planets else emptyList()) +
        (if ("campanhas" !in data.staleSources) data.campaigns.map { it.planet } else emptyList())
    planets.associateBy { it.index }.values.forEach { p ->
        val s = p.statistics
        listOf("bulletsFired" to s.bulletsFired, "bulletsHit" to s.bulletsHit,
            "terminidKills" to s.terminidKills, "automatonKills" to s.automatonKills,
            "illuminateKills" to s.illuminateKills).forEach { (field, value) ->
            if (value != null && value >= 0 && (field.startsWith("bullets") || field == killCounterField(p))) add(NumberObservation(counterKey(p.index, field), value.toDouble(), false, data.telemetrySource))
        }
    }
}

/** A facção atacante prevalece sobre Super Terra em planetas sob defesa. */
fun killCounterField(planet: Planet): String? = when (OrderRepository.factionKey(
    planet.event?.faction?.takeIf { it.isNotBlank() } ?: planet.currentOwner,
)) {
    "terminids" -> "terminidKills"
    "automatons" -> "automatonKills"
    "illuminates" -> "illuminateKills"
    else -> null
}
