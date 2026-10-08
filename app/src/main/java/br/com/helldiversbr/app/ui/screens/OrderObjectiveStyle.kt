package br.com.helldiversbr.app.ui.screens

import br.com.helldiversbr.app.data.OrderTask

internal fun objectiveKind(task: OrderTask): String = when (task.type) {
    3 -> "ERRADICAÇÃO"
    11 -> "LIBERTAÇÃO"
    12 -> "DEFESA"
    13 -> "CONTROLE"
    else -> "MISSÃO"
}
