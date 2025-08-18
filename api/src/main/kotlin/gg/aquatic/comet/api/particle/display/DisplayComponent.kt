package gg.aquatic.comet.api.particle.display

import org.bukkit.entity.EntityType


interface DisplayComponent

interface DisplayData {
    val entityType: EntityType
    fun copy(): DisplayData
}