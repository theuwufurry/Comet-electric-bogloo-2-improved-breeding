package gg.aquatic.comet.api.particle.display

import com.github.retrooper.packetevents.protocol.entity.type.EntityType

interface DisplayComponent

interface DisplayData<T : Any> {
    val entityType: EntityType
    fun copy(): DisplayData<T>
    val content: T
}