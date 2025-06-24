package gg.aquatic.comet.command

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.command.physics.Body
import gg.aquatic.comet.command.physics.Body.Companion.TIME_STEP
import gg.aquatic.comet.command.physics.Contact
import gg.aquatic.comet.command.physics.Cuboid
import gg.aquatic.waves.command.ICommand
import org.bukkit.*
import org.bukkit.Particle.DustOptions
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.random.Random

object PhysicsCommand : ICommand {
    private var task: BukkitTask? = null
    val bodies = mutableListOf<Body>()
    private var frozen = false
    private var steps = 0
    private var untilCollision = false
    private var enhancement = Vector3d(1.0)

    /**
     * point, direction
     */
    private var contacts = mutableListOf<Contact>()

    override fun run(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) return

        if (args[1] == "freeze") {
            frozen = !frozen
            return
        }

        if (args[1] == "enhance") {
            enhancement = Vector3d(args[2].toDouble(), args[3].toDouble(), args[4].toDouble())
        }

        if (args[1] == "step") {
            if (args.size > 2) {
                if (args[2] == "collision") {
                    untilCollision = true
                } else {
                    steps = args[2].toInt()
                }
            } else {
                steps = 1
            }

            return
        }

        if (args[1] == "clear") {
            bodies.forEach { it.kill() }
            bodies.clear()

            return
        }

        if (args.size < 16) {
            sender.sendMessage("Usage: /comet physics cube <vx> <vy> <vz> <width> <height> <length> <lx> <ly> <lz> <ax> <ay> <az> <density> <gravity>")
            return
        }

        if (args[1] != "cube") {
            sender.sendMessage("Usage: /comet physics cube <vx> <vy> <vz> <width> <height> <length> <lx> <ly> <lz> <density> <gravity>")
            return
        }

        val v0 = Vector3d(
            args[2].toDouble(),
            args[3].toDouble(),
            args[4].toDouble(),
        )

        val dims = Vector3d(
            args[5].toDouble(),
            args[6].toDouble(),
            args[7].toDouble(),
        )

        val l = Vector3d(
            args[8].toDouble(),
            args[9].toDouble(),
            args[10].toDouble(),
        )

        val rot0 = Vector3d(
            args[11].toDouble(),
            args[12].toDouble(),
            args[13].toDouble(),
        )

        val density = args[14].toDouble()
        val hasGravity = args[15].toBoolean()

        val origin = sender.location.toVector().toVector3d()
        val rb = Cuboid(
            world = sender.world,
            pos = Vector3d(origin),
            velocity = Vector3d(v0),
            width = dims.x,
            height = dims.y,
            length = dims.z,
            q = Quaterniond().rotateXYZ(rot0.x, rot0.y, rot0.z),
            omega = Vector3d(l),
            density = density,
            hasGravity = hasGravity,
        )

        bodies += rb

        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(AbstractParticleEmitter.INSTANCE, Runnable {
                repeat((0.05 / TIME_STEP).roundToInt()) {
                    var doTick = true
                    if (frozen) {
                        if (untilCollision && contacts.isNotEmpty()) {
                            untilCollision = false
                            frozen = true
                            doTick = false
                        }

                        if (!untilCollision && --steps < 0) doTick = false
                    }

                    if (doTick) {
                        for (body in bodies) {
                            if (body.hasGravity) body.velocity.add(0.0, -1.0 * TIME_STEP, 0.0)
                        }

                        if (bodies.size > 1) {
                            contacts.clear()
                            for (i in 0..<bodies.size) {
                                for (j in (i + 1)..<bodies.size) {
                                    val first = bodies[i]
                                    val second = bodies[j]

                                    val result = first.collides(second) ?: continue

                                    contacts += Contact(first, second, result)
                                }
                            }

                            for (itr in 1..10) {
                                for (contact in contacts) {
                                    val first = contact.first
                                    val second = contact.second

                                    val point = contact.result.firstPoint
                                    val norm = contact.result.norm
                                    val depth = contact.result.depth

                                    val massImpact = 1.0 / first.mass + 1.0 / second.mass
//                                first.pos.sub(Vector3d(norm).mul(depth / massImpact / first.mass))
//                                second.pos.add(Vector3d(norm).mul(depth / massImpact / second.mass))

                                    val firstLocalPoint = first.globalToLocal(point)
                                    val firstLocalNorm =
                                        Vector3d(norm).negate().rotate(Quaterniond(first.q).conjugate()).normalize()
                                    val secondLocalPoint = second.globalToLocal(point)
                                    val secondLocalNorm =
                                        Vector3d(norm).rotate(Quaterniond(second.q).conjugate()).normalize()

                                    val vr = Vector3d(second.velocity).sub(first.velocity).dot(norm) +
                                            Vector3d(second.omega).cross(secondLocalPoint).dot(secondLocalNorm) -
                                            Vector3d(first.omega).cross(firstLocalPoint).dot(firstLocalNorm)
//                                println("$itr VR: $vr")

                                    val firstAngularImpact =
                                        Vector3d(firstLocalPoint)
                                            .cross(firstLocalNorm)
                                            .mul(first.inverseInertia)
                                            .cross(firstLocalPoint)
                                            .dot(firstLocalNorm)
                                    val secondAngularImpact =
                                        Vector3d(secondLocalPoint)
                                            .cross(secondLocalNorm)
                                            .mul(second.inverseInertia)
                                            .cross(secondLocalPoint)
                                            .dot(secondLocalNorm)
                                    val angularImpact = firstAngularImpact + secondAngularImpact

                                    val bias = BIAS / TIME_STEP * (abs(depth) - SLOP).coerceAtLeast(0.0)

                                    var J =
                                        (bias + (vr * -(1.0)) / (massImpact + angularImpact)).coerceAtLeast(
                                            0.0
                                        )

                                    val curJSum = contact.jSum
                                    contact.jSum = (curJSum + J).coerceAtLeast(0.0)
                                    J = contact.jSum - curJSum

                                    val firstDV = Vector3d(norm).mul(J / first.mass)
                                    first.velocity.sub(firstDV)
                                    first.omega.add(
                                        Vector3d(firstLocalPoint).cross(Vector3d(firstLocalNorm).mul(J))
                                            .mul(first.inverseInertia)
                                    )

                                    val secondDV = Vector3d(norm).mul(J / second.mass)
                                    second.velocity.add(secondDV)
                                    second.omega.add(
                                        Vector3d(secondLocalPoint).cross(Vector3d(secondLocalNorm).mul(J))
                                            .mul(second.inverseInertia)
                                    )
                                }

                            }
//                        }
                        }

                        for (body in bodies) {
                            body.step()
                        }
                    }

                    for (contact in contacts) {
                        val (firstPoint,
                            secondPoint,
                            norm,
                            _,
                            minkowski,
                            closest,
                            originals) = contact.result

                        sender.world.debugConnect(
                            firstPoint,
                            Vector3d(firstPoint).add(norm),
                            DustOptions(Color.BLUE, 0.3f)
                        )

                        sender.world.spawnParticle(
                            Particle.REDSTONE,
                            Location(
                                sender.world,
                                firstPoint.x, firstPoint.y, firstPoint.z,
                            ),
                            1, Particle.DustOptions(Color.RED, 0.8f)
                        )

                        sender.world.debugConnect(
                            secondPoint,
                            Vector3d(secondPoint).add(norm),
                            DustOptions(Color.BLACK, 0.3f)
                        )

                        sender.world.spawnParticle(
                            Particle.REDSTONE,
                            Location(
                                sender.world,
                                secondPoint.x,
                                secondPoint.y,
                                secondPoint.z,
                            ),
                            1, Particle.DustOptions(Color.WHITE, 0.8f)
                        )

                        val minkowskiDebugOrigin = Vector3d(firstPoint).add(0.0, 3.0, 0.0)
                        val vertices = mutableSetOf<Vector3d>()
                        for ((a, b, c) in minkowski) {
                            vertices += a
                            vertices += b
                            vertices += c

//                            val color = choices[Vector3d(a).add(b).add(c).hashCode().absoluteValue % (choices.size)]
//
//                            sender.world.debugConnect(
//                                Vector3d(minkowskiDebugOrigin).add(Vector3d(a).mul(enhancement)),
//                                Vector3d(minkowskiDebugOrigin).add(Vector3d(b).mul(enhancement)),
//                                DustOptions(Color.BLACK, 0.2f)
//                            )
//
//                            sender.world.debugConnect(
//                                Vector3d(minkowskiDebugOrigin).add(Vector3d(a).mul(enhancement)),
//                                Vector3d(minkowskiDebugOrigin).add(Vector3d(c).mul(enhancement)),
//                                DustOptions(Color.BLACK, 0.2f)
//                            )
//
//                            sender.world.debugConnect(
//                                Vector3d(minkowskiDebugOrigin).add(Vector3d(c).mul(enhancement)),
//                                Vector3d(minkowskiDebugOrigin).add(Vector3d(b).mul(enhancement)),
//                                DustOptions(Color.BLACK, 0.2f)
//                            )
//
//                            repeat(50) {
//                                var cA = Random.nextDouble()
//                                var cB = Random.nextDouble()
//                                var cC = Random.nextDouble()
//
//                                val t = cA + cB + cC
//
//                                cA /= t
//                                cB /= t
//                                cC /= t
//
//                                val bp = Vector3d(a).mul(cA).add(Vector3d(b).mul(cB)).add(Vector3d(c).mul(cC))
//
//                                sender.world.spawnParticle(
//                                    Particle.REDSTONE,
//                                    Location(
//                                        sender.world,
//                                        minkowskiDebugOrigin.x + bp.x * enhancement.x,
//                                        minkowskiDebugOrigin.y + bp.y * enhancement.y,
//                                        minkowskiDebugOrigin.z + bp.z * enhancement.z,
//                                    ),
//                                    1,
//                                    DustOptions(color, 0.2f)
//                                )
//                            }
                        }

                        for (vertex in vertices) {
                            val (start, end) = originals[vertex]!!

                            val newPos = Vector3d(minkowskiDebugOrigin).add(vertex)

//                            sender.world.debugConnect(
//                                start,
//                                end,
//                                DustOptions(Color.BLUE, 0.1f)
//                            )
//
//                            sender.world.debugConnect(
//                                newPos,
//                                end,
//                                DustOptions(Color.YELLOW, 0.1f)
//                            )
//
//                            sender.world.debugConnect(
//                                start,
//                                newPos,
//                                DustOptions(Color.YELLOW, 0.1f)
//                            )
//
//                            sender.world.spawnParticle(
//                                Particle.REDSTONE,
//                                Location(
//                                    sender.world,
//                                    newPos.x,
//                                    newPos.y,
//                                    newPos.z,
//                                ),
//                                1, Particle.DustOptions(Color.WHITE, 0.5f)
//                            )
                        }
//
//                        sender.world.spawnParticle(
//                            Particle.REDSTONE,
//                            Location(
//                                sender.world,
//                                minkowskiDebugOrigin.x,
//                                minkowskiDebugOrigin.y,
//                                minkowskiDebugOrigin.z,
//                            ),
//                            1, Particle.DustOptions(Color.ORANGE, 0.5f)
//                        )
//
//                        sender.world.spawnParticle(
//                            Particle.REDSTONE,
//                            Location(
//                                sender.world,
//                                minkowskiDebugOrigin.x + closest.first.x,
//                                minkowskiDebugOrigin.y + closest.first.y,
//                                minkowskiDebugOrigin.z + closest.first.z,
//                            ),
//                            1, Particle.DustOptions(Color.RED, 0.5f)
//                        )
//
//                        sender.world.spawnParticle(
//                            Particle.REDSTONE,
//                            Location(
//                                sender.world,
//                                minkowskiDebugOrigin.x + closest.second.x,
//                                minkowskiDebugOrigin.y + closest.second.y,
//                                minkowskiDebugOrigin.z + closest.second.z,
//                            ),
//                            1, Particle.DustOptions(Color.RED, 0.5f)
//                        )
//
//                        sender.world.spawnParticle(
//                            Particle.REDSTONE,
//                            Location(
//                                sender.world,
//                                minkowskiDebugOrigin.x + closest.third.x,
//                                minkowskiDebugOrigin.y + closest.third.y,
//                                minkowskiDebugOrigin.z + closest.third.z,
//                            ),
//                            1, Particle.DustOptions(Color.RED, 0.5f)
//                        )
                    }
                }
//            val transformedVerticesQ = rb!!.vertices
//            val edgesQ = rb!!.edges
//
//            for (vertex in transformedVerticesQ) {
//                world.spawnParticle(
//                    Particle.REDSTONE, Location(
//                        world,
//                        vertex.x,
//                        vertex.y,
//                        vertex.z,
//                    ),
//                    5, Particle.DustOptions(Color.RED, 0.6f)
//                )
//            }
//
//            for ((start, end) in edgesQ) {
//                val d = end.distance(start)
//                val delta = Vector3d(end).sub(start).normalize()
//                var t = 0.0
//                while (t < d) {
//                    world.spawnParticle(
//                        Particle.REDSTONE, Location(
//                            world,
//                            start.x + delta.x * t,
//                            start.y + delta.y * t,
//                            start.z + delta.z * t,
//                        ),
//                        5, Particle.DustOptions(Color.BLUE, 0.4f)
//                    )
//                    t += 0.1
//                }
//            }
            }, 1, 1)
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("cube", "clear", "freeze", "step")
            2 -> listOf("<vx>")
            3 -> listOf("<vy>")
            4 -> listOf("<vz>")
            5 -> listOf("<width>")
            6 -> listOf("<height>")
            7 -> listOf("<length>")
            8 -> listOf("<lx>")
            9 -> listOf("<ly>")
            10 -> listOf("<lz>")
            else -> emptyList()
        }
    }
}

private fun World.debugConnect(start: Vector3d, end: Vector3d, options: DustOptions) {
    val dir = Vector3d(end).sub(start).normalize()!!
    if (!dir.isFinite) return
    var t = 0.0
    while (t < end.distance(start)) {
        t += 0.1

        spawnParticle(
            Particle.REDSTONE,
            Location(
                this,
                start.x + dir.x * t,
                start.y + dir.y * t,
                start.z + dir.z * t,
            ),
            1, options,
        )
    }
}

private const val BIAS = 0.1
private const val SLOP = 0.1

private val choices = listOf(
    Color.fromRGB(16777215),
    Color.fromRGB(12632256),
    Color.fromRGB(8421504),
    Color.fromRGB(0),
    Color.fromRGB(16711680),
    Color.fromRGB(8388608),
    Color.fromRGB(16776960),
    Color.fromRGB(8421376),
    Color.fromRGB(65280),
    Color.fromRGB(32768),
    Color.fromRGB(65535),
    Color.fromRGB(32896),
    Color.fromRGB(255),
    Color.fromRGB(128),
    Color.fromRGB(16711935),
    Color.fromRGB(8388736),
    Color.fromRGB(16753920),
)
