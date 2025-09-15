package gg.aquatic.comet.api.particle.display

import com.github.retrooper.packetevents.protocol.entity.type.EntityType

interface DisplayComponent

interface DisplayData {
    val entityType: EntityType
    fun copy(): DisplayData
}