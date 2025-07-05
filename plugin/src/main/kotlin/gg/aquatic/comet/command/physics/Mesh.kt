package gg.aquatic.comet.command.physics

import gg.aquatic.comet.command.debugConnect
import org.bukkit.Color
import org.bukkit.Color.*
import org.bukkit.Particle
import org.bukkit.Particle.DustOptions
import org.bukkit.World
import org.bukkit.util.BoundingBox
import org.joml.Quaterniond
import org.joml.Vector3d
import java.util.*

data class Mesh(
    val cheesyFaces: List<CheesyAAFace>,
    val edges: Set<Edge>
) {
    fun visualize(world: World) {
//        for (boundedFace in cheesyFaces) {
//            when (boundedFace.axis) {
//                Axis.X -> for (hole in boundedFace.holes) {
//                    world.debugConnect(
//                        Vector3d(hole.first.x, hole.first.y, hole.first.z),
//                        Vector3d(hole.first.x, hole.second.y, hole.first.z),
//                        DustOptions(RED, 0.5f),
//                    )
//
//                    world.debugConnect(
//                        Vector3d(hole.first.x, hole.second.y, hole.first.z),
//                        Vector3d(hole.first.x, hole.second.y, hole.second.z),
//                        DustOptions(RED, 0.5f),
//                    )
//
//                    world.debugConnect(
//                        Vector3d(hole.first.x, hole.second.y, hole.second.z),
//                        Vector3d(hole.first.x, hole.first.y, hole.second.z),
//                        DustOptions(RED, 0.5f),
//                    )
//
//                    world.debugConnect(
//                        Vector3d(hole.first.x, hole.first.y, hole.second.z),
//                        Vector3d(hole.first.x, hole.first.y, hole.first.z),
//                        DustOptions(RED, 0.5f),
//                    )
//                }
//
//                Axis.Y -> for (hole in boundedFace.holes) {
//                    world.debugConnect(
//                        Vector3d(hole.first.x, hole.first.y, hole.first.z),
//                        Vector3d(hole.second.x, hole.first.y, hole.first.z),
//                        DustOptions(GREEN, 0.5f),
//                    )
//
//                    world.debugConnect(
//                        Vector3d(hole.second.x, hole.first.y, hole.first.z),
//                        Vector3d(hole.second.x, hole.first.y, hole.second.z),
//                        DustOptions(GREEN, 0.5f),
//                    )
//
//                    world.debugConnect(
//                        Vector3d(hole.second.x, hole.first.y, hole.second.z),
//                        Vector3d(hole.first.x, hole.first.y, hole.second.z),
//                        DustOptions(GREEN, 0.5f),
//                    )
//
//                    world.debugConnect(
//                        Vector3d(hole.first.x, hole.first.y, hole.second.z),
//                        Vector3d(hole.first.x, hole.first.y, hole.first.z),
//                        DustOptions(GREEN, 0.5f),
//                    )
//                }
//
//                Axis.Z -> for (hole in boundedFace.holes) {
//                    world.debugConnect(
//                        Vector3d(hole.first.x, hole.first.y, hole.first.z),
//                        Vector3d(hole.second.x, hole.first.y, hole.first.z),
//                        DustOptions(BLUE, 0.5f),
//                    )
//
//                    world.debugConnect(
//                        Vector3d(hole.second.x, hole.first.y, hole.first.z),
//                        Vector3d(hole.second.x, hole.second.y, hole.first.z),
//                        DustOptions(BLUE, 0.5f),
//                    )
//
//                    world.debugConnect(
//                        Vector3d(hole.second.x, hole.second.y, hole.first.z),
//                        Vector3d(hole.first.x, hole.second.y, hole.first.z),
//                        DustOptions(BLUE, 0.5f),
//                    )
//
//                    world.debugConnect(
//                        Vector3d(hole.first.x, hole.second.y, hole.first.z),
//                        Vector3d(hole.first.x, hole.first.y, hole.first.z),
//                        DustOptions(BLUE, 0.5f),
//                    )
//                }
//            }
//        }

        for (edge in edges) {
            world.debugConnect(edge.start, edge.end, DustOptions(YELLOW, 0.4f), 0.0999)
        }
    }
}

class MeshBody(
    override val world: World
) : Body {
    override val id: UUID = UUID.randomUUID()
    override val velocity: Vector3d = Vector3d()
    override val vertices: List<Vector3d> = listOf()
    override val edges: List<Pair<Vector3d, Vector3d>> = listOf()
    override val boundingBox: BoundingBox = BoundingBox()
    override val inverseMass: Double = 0.0
    override val inverseInertia: Vector3d = Vector3d(0.0)
    override val q: Quaterniond = Quaterniond()
    override val omega: Vector3d = Vector3d()
    override val hasGravity: Boolean = false
    override val pos: Vector3d = Vector3d()

    override fun support(dir: Vector3d): Vector3d {
        return Vector3d()
    }

    override fun globalToLocal(vec: Vector3d): Vector3d {
        return Vector3d()
    }

    override fun step() {}

    override fun intersect(origin: Vector3d, end: Vector3d): List<Pair<Vector3d, Vector3d>> {
        return listOf()
    }

    override fun collidesBody(other: Body): CollisionResult? {
        return null
    }

    override fun collidesMesh(mesh: Mesh): List<CollisionResult> {
        return listOf()
    }

    override fun kill() {}

    override fun applyImpulse(point: Vector3d, normal: Vector3d, impulse: Vector3d) {}
}