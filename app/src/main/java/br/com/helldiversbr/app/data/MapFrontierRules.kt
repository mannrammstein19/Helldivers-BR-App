package br.com.helldiversbr.app.data

/** Supply links define borders. Proximity, sector names and faction presences do not. */
internal data class MapBorderNode(val index: Long, val human: Boolean, val links: List<Long>)
internal fun enemyBorderPlanets(nodes: List<MapBorderNode>): Set<Long> {
    val indexed = nodes.associateBy { it.index }
    val result = mutableSetOf<Long>()
    nodes.forEach { source -> source.links.forEach link@ { id ->
        val target = indexed[id] ?: return@link
        if (source.human != target.human) result += if (source.human) target.index else source.index
    } }
    return result
}
