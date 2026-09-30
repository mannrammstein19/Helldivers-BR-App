package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.OrderTask
import br.com.helldiversbr.app.data.OrderTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderTargetsTest {
    @Test fun resolvesUnitWithShuffledFieldsAndPreservesLargeGoal() {
        val task = OrderTask(type = 3, valueTypes = listOf(4, 3, 1), values = listOf(2651633799L, 25000000000L, 2L))
        assertEquals("Atropeladores", OrderTargets.label(task, "Terminídeos"))
        assertTrue(task.goal == 25000000000L)
        assertTrue(task.copy(values = listOf(2651633799L, 25000000L, 2L)).goal == 25000000L)
    }

    @Test fun resolvesCatalogAcrossAllThreeFactions() {
        val cases = listOf(
            Triple(20706814L, 3L, "Batedores Andantes"),
            Triple(2664856027L, 3L, "Tanques Autômatos"),
            Triple(471929602L, 3L, "Hulks"),
            Triple(4276710272L, 3L, "Devastadores"),
            Triple(878778730L, 3L, "Soldados Autômatos"),
            Triple(3330362068L, 2L, "Caçadores"),
            Triple(2058088313L, 2L, "Guerreiros"),
            Triple(2387277009L, 2L, "Espreitadores"),
            Triple(2651633799L, 2L, "Atropeladores"),
            Triple(2514244534L, 2L, "Titãs de Bile"),
            Triple(1379865898L, 2L, "Cuspidores de Bile"),
            Triple(4211847317L, 4L, "Sem-voto"),
        )
        for ((id, faction, name) in cases) {
            val task = OrderTask(type = 3, valueTypes = listOf(1, 3, 4), values = listOf(faction, 10000000L, id))
            assertEquals("ID $id", name, OrderTargets.label(task, "facção genérica"))
        }
    }

    @Test fun distinguishesEnemyIdFromItemAndPlanetIds() {
        val task = OrderTask(type = 3, valueTypes = listOf(5, 12, 4, 1, 3),
            values = listOf(2651633799L, 4211847317L, 471929602L, 3L, 10000000L))
        assertEquals("Hulks", OrderTargets.label(task, "Autômatos"))
        assertTrue(task.goal == 10000000L)
    }

    @Test fun acceptsKnownUnitWithoutFactionOrWithUnrestrictedFaction() {
        assertEquals("Hulks", OrderTargets.label(OrderTask(valueTypes = listOf(4), values = listOf(471929602L)), ""))
        assertEquals("Hulks", OrderTargets.label(OrderTask(valueTypes = listOf(4, 1), values = listOf(471929602L, 0L)), ""))
    }

    @Test fun refusesUnknownOrWrongFaction() {
        assertEquals("alvo específico não identificado", OrderTargets.label(OrderTask(valueTypes = listOf(1, 4), values = listOf(3L, 2651633799L)), "Autômatos"))
        assertEquals("alvo específico não identificado", OrderTargets.label(OrderTask(valueTypes = listOf(4), values = listOf(999L)), "Terminídeos"))
    }

    @Test fun handlesGenericOrdersAndIncompleteArraysWithoutGuessingAnId() {
        assertEquals("Terminídeos", OrderTargets.label(OrderTask(), "Terminídeos"))
        assertEquals("inimigos", OrderTargets.label(OrderTask(), ""))
        assertEquals("Terminídeos", OrderTargets.label(OrderTask(valueTypes = listOf(4), values = listOf(0L)), "Terminídeos"))
        assertEquals("Autômatos", OrderTargets.label(OrderTask(valueTypes = listOf(1, 4), values = listOf(3L)), "Autômatos"))
        assertEquals("Autômatos", OrderTargets.label(OrderTask(valueTypes = listOf(5), values = listOf(471929602L)), "Autômatos"))
    }
}
