package br.com.helldiversbr.app.data

/** The older Home campaign must not overwrite a newer map's ownership or invasion. */
internal fun mergeMapPlanets(map: List<Planet>, campaigns: List<Campaign>, preferCampaigns: Boolean, campaignPresencesUnconfirmed: Boolean = false): List<Planet> {
    val byId = campaigns.associateBy { it.planet.index }
    return (map.map { planet ->
        if (preferCampaigns) byId[planet.index]?.planet?.copy(position = planet.mapPosition,
            waypoints = planet.waypoints, attacking = planet.attacking, disabled = planet.disabled)?.withSavedPresences(planet, campaignPresencesUnconfirmed) ?: planet
        else planet
    } + (if (preferCampaigns || map.isEmpty()) campaigns.map { it.planet } else emptyList())).distinctBy { it.index }
}
