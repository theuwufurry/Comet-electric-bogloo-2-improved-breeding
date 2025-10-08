package gg.aquatic.comet.udar

import com.ixume.udar.body.active.ActiveBody
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import java.util.concurrent.atomic.AtomicBoolean

class UdarBodyParent(val body: ActiveBody) : Parent {
    override val pose: Pose
        get() = Pose(
            world = body.world,
            pos = body.pos,
            rot = body.q,
        )
    override val dead: AtomicBoolean
        get() = body.dead
}