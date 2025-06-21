package gg.aquatic.comet.command.physics

import org.joml.Quaterniond
import org.joml.Vector3d

interface Body {
    val pos: Vector3d
    val velocity: Vector3d

    val vertices: List<Vector3d>
    val edges: List<Pair<Vector3d, Vector3d>>

    val mass: Double
    val inverseInertia: Vector3d
    val q: Quaterniond
    val omega: Vector3d

    fun globalToLocal(vec: Vector3d): Vector3d

    fun step()

    /**
     * @return List of intersection positions and normals
     */
    fun intersect(origin: Vector3d, end: Vector3d): List<Pair<Vector3d, Vector3d>>

    fun collides(other: Body): CollisionResult?

    fun kill()

    fun applyImpulse(
        point: Vector3d,
        normal: Vector3d,
        impulse: Vector3d,
    )

    companion object {
        const val TIME_STEP = 0.005

        fun Body.support(dir: Vector3d): Vector3d {
            val vertices = vertices
            var maxDot = -Double.MAX_VALUE
            var maxVertex = vertices[0]

            for (vertex in vertices) {
                val dot = vertex.dot(dir)

                if (dot > maxDot) {
                    maxDot = dot
                    maxVertex = vertex
                }
            }

            return maxVertex
        }
    }
}

data class CollisionResult(
    val point: Vector3d,
    val norm: Vector3d,
    val depth: Double,
)