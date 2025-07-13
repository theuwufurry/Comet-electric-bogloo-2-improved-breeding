package gg.aquatic.comet.command.physics

import org.joml.Vector3d

data class Edge(
    val start: Vector3d,
    val end: Vector3d,
    val mount: EdgeMount? = null,
    val axis: Axis? = null,
) {
    val vec = Vector3d(end).sub(start)
}