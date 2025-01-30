package gg.aquatic.comet.emitter.parent

import org.bukkit.Location
import org.joml.Vector3d

enum class EmitterSpace {
    WORLD,
    PARENT_EMITTER,
    PARENT_PARTICLE
}

interface Parent {
    fun pose(): Pose
}

data class Pose(
    val pos: Vector3d,
    val dir: Vector3d
)

fun Location.pose(): Pose {
    return Pose(
        toVector().toVector3d(),
        direction.toVector3d()
    )
}