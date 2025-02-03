package gg.aquatic.comet.api.emitter.parent

import org.bukkit.entity.Entity

class EntityParent(val entity: Entity) : Parent {
    override val pose: Pose
        get() {
            return entity.location.pose()
        }
}