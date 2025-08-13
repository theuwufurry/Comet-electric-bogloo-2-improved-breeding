package gg.aquatic.comet.api.particle.display

import gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.type.EntityType

interface DisplayComponent

interface DisplayData {
    val entityType: EntityType
    fun copy(): DisplayData
}