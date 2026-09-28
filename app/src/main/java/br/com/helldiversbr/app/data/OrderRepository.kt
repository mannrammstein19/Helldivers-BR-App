package br.com.helldiversbr.app.data

import java.time.Instant

/** Estado de tela da Ordem Maior. */
data class OrderUi(
    val order: Assignment?,
    /** "active" | "completed" | "failed" | "pending" */
    val state: String,
    val percent: Double,
    val fromSnapshot: Boolean,
)

data class HomeData(
    val order: OrderUi,
    val dispatches: List<Dispatch>,
    val planetNames: Map<Long, String>,
    val updatedAtMillis: Long,
)

object OrderRepository {

    /**
     * Mesma estratégia do site: tenta a API ao vivo; se a ordem sumiu (terminou),
     * usa o snapshot salvo pelo GitHub Actions.
     */
    suspend fun load(): HomeData {
        val live = runCatching { HelldiversApi.liveAssignments().firstOrNull { it.tasks.isNotEmpty() } }
        val snapshot = runCatching { HelldiversApi.orderSnapshot() }
        val dispatches = runCatching { HelldiversApi.dispatches().sortedByDescending { it.published.orEmpty() }.take(10) }

        val liveOrder = live.getOrNull()
        val snap = snapshot.getOrNull()

        // Se as duas fontes falharam, propaga o erro para a tela mostrar "sem conexão".
        if (liveOrder == null && snap == null && (live.isFailure || snapshot.isFailure)) {
            throw (live.exceptionOrNull() ?: snapshot.exceptionOrNull() ?: IllegalStateException("Sem dados"))
        }

        val snapOrder = snap?.order
        val ui = when {
            liveOrder != null -> OrderUi(
                order = liveOrder,
                state = "active",
                percent = computePercent(liveOrder),
                fromSnapshot = false,
            )
            snap != null && snapOrder != null -> OrderUi(
                order = snapOrder,
                state = snap.state.ifBlank { "pending" },
                percent = snap.final_percent ?: computePercent(snapOrder),
                fromSnapshot = true,
            )
            else -> OrderUi(order = null, state = "pending", percent = 0.0, fromSnapshot = false)
        }

        return HomeData(
            order = ui,
            dispatches = dispatches.getOrDefault(emptyList()),
            planetNames = HelldiversApi.planetNames(),
            updatedAtMillis = System.currentTimeMillis(),
        )
    }

    /** Progresso médio das tarefas em % (progress[i] / meta da tarefa i). */
    fun computePercent(order: Assignment): Double {
        if (order.tasks.isEmpty()) return 0.0
        val parts = order.tasks.mapIndexed { i, task ->
            val goal = task.goal
            val progress = order.progress.getOrNull(i) ?: 0L
            when {
                goal != null && goal > 0 -> (progress.toDouble() / goal).coerceIn(0.0, 1.0)
                // Tarefas de libertar/defender planeta: progresso 1 = concluída.
                progress > 0 -> 1.0
                else -> 0.0
            }
        }
        return parts.average() * 100.0
    }

    /** Tempo restante formatado como no site (ex.: "3d 20h", "5h 12min"). */
    fun remaining(expiration: String?): String {
        if (expiration.isNullOrBlank()) return "prazo indisponível"
        val end = runCatching { Instant.parse(expiration) }.getOrNull() ?: return "prazo indisponível"
        val seconds = end.epochSecond - Instant.now().epochSecond
        if (seconds <= 0) return "prazo esgotado"
        val d = seconds / 86400
        val h = seconds % 86400 / 3600
        val m = seconds % 3600 / 60
        return when {
            d > 0 -> "${d}d ${h}h"
            h > 0 -> "${h}h ${m}min"
            else -> "${m}min"
        }
    }
}
