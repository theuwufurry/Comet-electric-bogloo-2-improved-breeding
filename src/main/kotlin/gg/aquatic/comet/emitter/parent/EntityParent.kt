package gg.aquatic.comet.emitter.parent

import org.bukkit.entity.Entity
import org.joml.Vector3d

class EntityParent(val entity: Entity) : Parent {
    override fun location(): Vector3d {
        return entity.location.toVector().toVector3d()
    }
}