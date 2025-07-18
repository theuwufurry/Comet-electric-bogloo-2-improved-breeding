package gg.aquatic.comet.api.particle.data

import gg.aquatic.comet.api.particle.UpdateFlags
import gg.aquatic.waves.api.nms.entity.EntityDataValue

abstract class AbstractEntityDataBuilder {

    abstract fun getDataFor(
        entityData: EntityData,
        flags: UpdateFlags,
        initial: Boolean
    ): List<EntityDataValue>?

}