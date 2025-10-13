package gg.aquatic.comet.udar

import com.ixume.udar.body.active.ActiveBody
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import org.joml.Vector3d
import java.util.concurrent.atomic.AtomicBoolean

class UdarBodyParent(
    val body: ActiveBody,
    val relPos: Vector3d,
) : Parent {
    override val pose: Pose
        get() = Pose(
            world = body.world,
            pos = Vector3d(relPos).rotate(body.q).add(body.pos),
            rot = body.q,
        )
    override val dead: AtomicBoolean
        get() = body.dead
}