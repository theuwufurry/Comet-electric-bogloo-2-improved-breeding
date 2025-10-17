package gg.aquatic.comet.api.emitter.parent

import org.bukkit.Location
import org.bukkit.World
import org.joml.Quaterniond
import org.joml.Vector3d
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.atan2
import kotlin.math.sqrt

enum class EmitterSpace {
    WORLD,
    PARENT_EMITTER,
    PARENT_PARTICLE
}

interface Parent {
    val pose: Pose
    val dead: AtomicBoolean
}

data class Pose(
    val world: World,
    val pos: Vector3d,
    val rot: Quaterniond,
) {
    val location: Location
        get() {
            val euler = Vector3d()
            rot.getEulerAnglesXYZ(euler)

            return Location(
                world,
                pos.x,
                pos.y,
                pos.z,
                euler.y.toFloat(), euler.x.toFloat()
            )
        }

    fun clone(): Pose {
        return Pose(world, Vector3d(pos.x, pos.y, pos.z), Quaterniond(rot.x, rot.y, rot.z, rot.w))
    }
}

fun Vector3d.noRollQuaternion(): Quaterniond {
    val yaw = atan2(x, z)
    val pitch = atan2(y, sqrt(x * x + z * z))

    return Quaterniond()
        .rotateY(yaw)
        .rotateX(pitch)
}

fun Location.pose(): Pose {
    val rot = Quaterniond()
        .rotateZ(-((pitch) * Math.PI / 180.0))
        .rotateLocalY((-yaw * Math.PI / 180.0))

    return Pose(
        world!!,
        toVector().toVector3d(),
        rot,
    )
}

val NORMAL: Vector3d
    get() {
        return Vector3d(0.0, 0.0, 1.0)
    }