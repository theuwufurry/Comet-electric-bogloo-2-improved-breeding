package gg.aquatic.comet.emitter.parent

import org.bukkit.entity.Entity

class EntityParent(val entity: Entity) : Parent {
    override fun pose(): Pose {
        return entity.location.pose()
    }
}