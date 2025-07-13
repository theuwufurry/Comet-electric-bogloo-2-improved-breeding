package gg.aquatic.comet.command.physics

import org.joml.Vector2d
import org.joml.Vector3d
import org.joml.Vector3i

data class CheesyAAFace(
    val axis: Axis,
    val start: Vector3i,
    val end: Vector3i,
    /**
     * start, end
     */
    val holes: MutableList<Pair<Vector3d, Vector3d>>,
)

data class MeshFace(
    val axis: Axis,
    val start: Vector3d,
    val end: Vector3d,
    val valid: MutableList<MeshFacePass>,
    val invalid: MutableList<Pair<Vector2d, Vector2d>>,
)

data class MeshFacePass(
    val start: Vector2d,
    val end: Vector2d,
    val inAxisDir: Boolean
)
