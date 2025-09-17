package gg.aquatic.comet.api.particle.data

import gg.aquatic.comet.api.particle.UpdateFlags
import gg.aquatic.waves.api.nms.entity.EntityDataValue

abstract class AbstractEntityDataBuilder {

    abstract fun getDataFor(
        entityData: EntityData,
        flags: UpdateFlags,
        initial: Boolean,
        useUAP: Boolean,
    ): List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<*>>?

}