package gg.aquatic.comet.api.particle.data

import gg.aquatic.comet.api.particle.UpdateFlags

abstract class AbstractEntityDataBuilder {

    abstract fun getDataFor(
        entityData: EntityData,
        flags: UpdateFlags,
        initial: Boolean
    ): List<gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.entity.data.EntityData>?

}