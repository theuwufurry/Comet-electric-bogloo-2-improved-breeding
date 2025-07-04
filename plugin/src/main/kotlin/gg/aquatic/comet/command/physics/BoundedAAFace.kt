package gg.aquatic.comet.command.physics

import org.joml.Vector3d
import org.joml.Vector3i

data class BoundedAAFace(
    val axis: Axis,
    val start: Vector3i,
    val end: Vector3i,
    /**
     * start, end
     */
    val holes: MutableList<Pair<Vector3d, Vector3d>>,
)
