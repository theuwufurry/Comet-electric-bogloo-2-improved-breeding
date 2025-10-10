package gg.aquatic.comet.api.particle.data

import gg.aquatic.comet.api.particle.UpdateFlags

abstract class AbstractEntityDataBuilder {

    abstract fun getDataFor(
        entityData: EntityData,
        flags: UpdateFlags,
        initial: Boolean,
        usePUA: Boolean,
    ): List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<*>>?

}