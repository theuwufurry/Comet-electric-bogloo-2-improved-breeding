package gg.aquatic.comet.api

import org.bukkit.entity.Entity
import org.joml.Vector3d

class EntityMount(
    private val entity: Entity
) : Mount {
    private val offset = entity.type.let {
        Vector3d(
            PassengerOffsets.offsets[it]?.x ?: 0.0,
            PassengerOffsets.offsets[it]?.y ?: 0.0,
            PassengerOffsets.offsets[it]?.z ?: 0.0,
        )
    }

    override val entityID: Int = entity.entityId
    override val pos: Vector3d
        get() {
            return entity.location.toVector().toVector3d().add(offset)
        }
}