package br.com.helldiversbr.app.data

/**
 * Assignment unit IDs (valueType 4), not item IDs or assignment IDs.
 * Sources, translations and scope: CATALOGO-ALVOS-ORDEM.md.
 * Data reference: CrosswaveOmega/hd2api.py, MIT; see assets/licenses/hd2api.txt.
 */
object OrderTargets {
    private val units: Map<Long, Pair<String, Int>> = mapOf(
        20706814L to ("Batedores Andantes" to 3), // Scout Strider
        2664856027L to ("Tanques Autômatos" to 3), // Shredder Tank
        471929602L to ("Hulks" to 3), // Hulk
        4276710272L to ("Devastadores" to 3), // Devastator
        878778730L to ("Soldados Autômatos" to 3), // Trooper
        3330362068L to ("Caçadores" to 2), // Hunter
        2058088313L to ("Guerreiros" to 2), // Warrior
        2387277009L to ("Espreitadores" to 2), // Stalker
        2651633799L to ("Atropeladores" to 2), // Charger
        2514244534L to ("Titãs de Bile" to 2), // Bile Titan
        1379865898L to ("Cuspidores de Bile" to 2), // Bile Spewer
        4211847317L to ("Sem-voto" to 4), // Voteless
    )

    fun label(task: OrderTask, factionLabel: String): String {
        val id = task.targetUnitId ?: return factionLabel.ifBlank { "inimigos" }
        val unit = units[id]
        return if (unit != null && (task.factionId == null || task.factionId == 0 || task.factionId == unit.second)) unit.first
        else "alvo específico não identificado"
    }
}
