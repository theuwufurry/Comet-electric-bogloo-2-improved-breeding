package gg.aquatic.comet.api.emitter.parent

import org.bukkit.entity.Entity
import java.util.concurrent.atomic.AtomicBoolean

class EntityParent(val entity: Entity) : Parent {
    override val pose: Pose
        get() {
            return entity.location.pose()
        }

    override val dead: AtomicBoolean = AtomicBoolean(entity.isDead)
        get() {
            dead.set(entity.isDead)
            return field
        }
}