package gg.aquatic.comet.command

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.command.physics.Body
import gg.aquatic.comet.command.physics.Body.Companion.TIME_STEP
import gg.aquatic.comet.command.physics.Cuboid
import gg.aquatic.waves.command.ICommand
import io.ktor.network.sockets.*
import org.bukkit.*
import org.bukkit.Particle.DustOptions
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.roundToInt

object PhysicsCommand : ICommand {
    private var task: BukkitTask? = null
    val bodies = mutableListOf<Body>()

    override fun run(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) return

        if (args[1] == "clear") {
            bodies.forEach { it.kill() }
            bodies.clear()

            return
        }

        if (args.size < 14) {
            sender.sendMessage("Usage: /comet physics cube <vx> <vy> <vz> <width> <height> <length> <lx> <ly> <lz> <ax> <ay> <az>")
            return
        }

        if (args[1] != "cube") {
            sender.sendMessage("Usage: /comet physics cube <vx> <vy> <vz> <width> <height> <length> <lx> <ly> <lz>")
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
        )

        bodies += rb

        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(AbstractParticleEmitter.INSTANCE, Runnable {
                repeat((0.05 / TIME_STEP).roundToInt()) {
                    for (body in bodies) {
                        body.step()
                    }

                    if (bodies.size > 1) {
                        for (i in 0..<bodies.size) {
                            for (j in (i + 1)..<bodies.size) {
                                val first = bodies[i]
                                val second = bodies[j]

                                val result = first.collides(second) ?: continue
                                val (point, norm, depth) = result
                                //depth * M1
                                //depth * M2
                                //M1 + M2 = T
                                //depth * M1 + depth * M2 = 1

                                val massImpact = 1.0 / first.mass + 1.0 / second.mass
                                first.pos.sub(Vector3d(norm).mul(depth / massImpact / first.mass))
                                second.pos.add(Vector3d(norm).mul(depth / massImpact / second.mass))

                                sender.world.debugConnect(point, Vector3d(point).add(norm), DustOptions(Color.BLUE, 0.3f))

                                val vr = Vector3d(second.velocity).sub(first.velocity)
                                println(vr.dot(norm))
//                                println("between $i and $j point: $point norm: $norm depth: $depth vr: $vr")

                                val firstLocalPoint = first.globalToLocal(point)
                                val firstLocalNorm = Vector3d(norm).negate().rotate(Quaterniond(first.q).conjugate()).normalize()
                                val secondLocalPoint = second.globalToLocal(point)
                                val secondLocalNorm = Vector3d(norm).rotate(Quaterniond(second.q).conjugate()).normalize()

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

////
                                val J =
                                    vr.dot(norm) * -(1.0 + 1.0) / (massImpact + angularImpact)

                                val firstDV = Vector3d(norm).mul(J / first.mass)
                                first.velocity.sub(firstDV)
                                first.omega.add(Vector3d(firstLocalPoint).cross(Vector3d(firstLocalNorm).mul(J)).mul(first.inverseInertia))

                                val secondDV = Vector3d(norm).mul(J / second.mass)
                                second.velocity.add(secondDV)
                                second.omega.add(Vector3d(secondLocalPoint).cross(Vector3d(secondLocalNorm).mul(J)).mul(second.inverseInertia))

//                                println("mass impact: $massImpact angular impact: $angularImpact firstDV: $firstDV secondDV: $secondDV")

                                sender.world.spawnParticle(
                                    Particle.REDSTONE,
                                    Location(
                                        sender.world,
                                        point.x, point.y, point.z,
                                    ),
                                    5, Particle.DustOptions(Color.RED, 0.8f)
                                )
                            }
                        }
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
        return listOf(
            when (args.size) {
                1 -> "cube"
                2 -> "<vx>"
                3 -> "<vy>"
                4 -> "<vz>"
                5 -> "<width>"
                6 -> "<height>"
                7 -> "<length>"
                8 -> "<lx>"
                9 -> "<ly>"
                10 -> "<lz>"
                else -> ""
            }
        )
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
            5, options,
        )
    }
}