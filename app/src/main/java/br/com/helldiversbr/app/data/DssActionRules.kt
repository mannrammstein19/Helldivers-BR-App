package br.com.helldiversbr.app.data

enum class DssActionPhase { ACTIVE, COOLDOWN, FUNDING, OFFLINE, PENDING }
object DssActionRules {
    fun deadline(action: DssTacticalAction): String = action.statusExpire.ifBlank {
        action.statusExpiresAt.ifBlank { action.statusExpiration }
    }
    fun phase(action: DssTacticalAction, now: Long): DssActionPhase {
        val raw = listOf(localizedText(action.statusName), localizedText(action.state), localizedText(action.statusText))
            .firstOrNull { it.isNotBlank() }.orEmpty()
        val rawEnd = deadline(action)
        val end = DssSupport.dateMillis(rawEnd)
        val active = action.status == 2 || Regex("\\b(active|ativa|activated|ativada)\\b", RegexOption.IGNORE_CASE).containsMatchIn(raw)
        if (active) return if (now <= 0 || (rawEnd.isNotBlank() && (end == null || end <= now)))
            DssActionPhase.PENDING else DssActionPhase.ACTIVE
        if (action.status == 3 || Regex("cooldown|recharg|recarreg", RegexOption.IGNORE_CASE).containsMatchIn(raw))
            return if (now > 0 && end != null && end > now) DssActionPhase.COOLDOWN else DssActionPhase.PENDING
        if (action.status == 1 || Regex("funding|preparando", RegexOption.IGNORE_CASE).containsMatchIn(raw)) return DssActionPhase.FUNDING
        if (action.status == 0 || Regex("inactive|unavailable|desativ|indispon", RegexOption.IGNORE_CASE).containsMatchIn(raw)) return DssActionPhase.OFFLINE
        return DssActionPhase.PENDING
    }
    fun percent(cost: DssCost): Double? {
        val current = cost.currentValue ?: return null
        val target = cost.targetValue ?: return null
        return if (current.isFinite() && current >= 0 && target.isFinite() && target > 0)
            (current / target * 100).coerceIn(0.0, 100.0) else null
    }
    fun estimateSeconds(cost: DssCost): Double? {
        val current = cost.currentValue ?: return null
        val target = cost.targetValue ?: return null
        val delta = cost.deltaPerSecond ?: return null
        return if (percent(cost) != null && delta.isFinite() && delta > 0)
            ((target - current).coerceAtLeast(0.0) / delta).takeIf { it.isFinite() } else null
    }
    fun resource(cost: DssCost): String = when (cost.itemMixId ?: cost.id.toLongOrNull()) {
        3992382197L -> "Amostra comum"
        3608481516L -> "Nota de requisição"
        2985106497L -> "Amostra rara"
        else -> "Recurso não identificado"
    }
}
