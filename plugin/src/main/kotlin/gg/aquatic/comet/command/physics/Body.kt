package gg.aquatic.comet.command.physics

import org.bukkit.block.Block
import org.bukkit.util.BoundingBox
import org.joml.Quaterniond
import org.joml.Vector3d
import java.util.UUID

interface Body  {
    val id: UUID
    val velocity: Vector3d

    val vertices: List<Vector3d>
    val edges: List<Pair<Vector3d, Vector3d>>
    val boundingBox: BoundingBox

    val inverseMass: Double
    val inverseInertia: Vector3d
    val q: Quaterniond
    val omega: Vector3d

    val hasGravity: Boolean

    val pos: Vector3d
    fun support(dir: Vector3d): Vector3d

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

    }
}

data class CollisionResult(
    val firstPoint: Vector3d,
    val secondPoint: Vector3d,
    val norm: Vector3d,
    val depth: Double,
    val minkowski: List<Triple<Vector3d, Vector3d, Vector3d>>,
    val closest: Triple<Vector3d, Vector3d, Vector3d>,
    val originals: Map<Vector3d, Pair<Vector3d, Vector3d>>,
)

class BlockBody(val block: Block) : Body {
    override val id: UUID = UUID.randomUUID()
    override val velocity: Vector3d = Vector3d()
    override val vertices = block.collisionShape.boundingBoxes.flatMap {
        listOf(
            Vector3d(block.x + it.minX, block.y + it.minY, block.z + it.maxZ),
            Vector3d(block.x + it.minX, block.y + it.minY, block.z + it.minZ),
            Vector3d(block.x + it.minX, block.y + it.maxY, block.z + it.maxZ),
            Vector3d(block.x + it.minX, block.y + it.maxY, block.z + it.minZ),
            Vector3d(block.x + it.maxX, block.y + it.minY, block.z + it.maxZ),
            Vector3d(block.x + it.maxX, block.y + it.minY, block.z + it.minZ),
            Vector3d(block.x + it.maxX, block.y + it.maxY, block.z + it.maxZ),
            Vector3d(block.x + it.maxX, block.y + it.maxY, block.z + it.minZ),
        )
    }
    override val edges: List<Pair<Vector3d, Vector3d>> = listOf()
    override val boundingBox: BoundingBox = block.boundingBox
    override val inverseMass: Double = 0.0
    override val inverseInertia: Vector3d = Vector3d()
    override val q: Quaterniond = Quaterniond()
    override val omega: Vector3d = Vector3d()
    override val hasGravity: Boolean = false

    override fun intersect(origin: Vector3d, end: Vector3d): List<Pair<Vector3d, Vector3d>> { return listOf() }

    override fun globalToLocal(vec: Vector3d): Vector3d {
        return Vector3d(vec).sub(block.x.toDouble(), block.y.toDouble(), block.z.toDouble())
    }

    override fun step() { }


    override fun collides(other: Body): CollisionResult? { return null }

    override fun kill() { }

    override fun applyImpulse(point: Vector3d, normal: Vector3d, impulse: Vector3d) { }

    override val pos: Vector3d = block.boundingBox.center.toVector3d()

    override fun support(dir: Vector3d): Vector3d {
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
