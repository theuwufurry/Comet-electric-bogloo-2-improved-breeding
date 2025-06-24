package gg.aquatic.comet.command.physics

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.waves.command.ICommand
import org.bukkit.*
import org.bukkit.Particle.DustOptions
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.joml.Vector3d
import kotlin.random.Random

object EPACommand : ICommand {
    private var task: BukkitTask? = null

    /**
     * Stores vertices, 3 for each face
     */
    private val shape = mutableListOf<Triple<Vector3d, Vector3d, Vector3d>>()
    private var origin: Vector3d? = null
    private val verticesBuffer = mutableListOf<Vector3d>()

    override fun run(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) return
        if (sender !is Player) return

        val world = sender.world

        if (args[1] == "anew") {
            shape.clear()
            verticesBuffer.clear()
            task?.cancel()
            task = null

            origin = sender.eyeLocation.toVector().toVector3d()

            val a0 = Vector3d(origin).add(-2.0, 0.0, 0.0)
            val b0 = Vector3d(origin).add(2.0, 0.0, 0.0)
            val c0 = Vector3d(origin).add(0.0, 0.0, -2.0)
            val d0 = Vector3d(origin).add(0.0, 2.0, 0.0)

            shape += Triple(a0, c0, b0)
            shape += Triple(a0, b0, d0)
            shape += Triple(b0, c0, d0)
            shape += Triple(c0, a0, d0)

            task = Bukkit.getScheduler().runTaskTimer(AbstractParticleEmitter.INSTANCE, Runnable {
                run {
                    val (distance, normal, faces) = Cuboid.closestNormal(origin!!, shape)!!
                    val face = faces.first()
                    val (a, b, c) = face
                    val coefficients = Cuboid.toBarycentric(origin!!, a, b, c)!!
                    val aC = coefficients.x
                    val bC = coefficients.y
                    val cC = coefficients.z
                    val loc = Vector3d(a).mul(aC).add(Vector3d(b).mul(bC)).add(Vector3d(c).mul(cC))

                    world.spawnParticle(
                        Particle.REDSTONE,
                        Location(
                            world,
                            origin!!.x,
                            origin!!.y,
                            origin!!.z
                        ), 1, DustOptions(Color.WHITE, 1f)
                    )

                    world.spawnParticle(
                        Particle.REDSTONE,
                        Location(
                            world,
                            loc.x,
                            loc.y,
                            loc.z
                        ), 1, DustOptions(Color.YELLOW, 1f)
                    )
                }

                for ((a, b, c) in shape) {
                    world.spawnParticle(
                        Particle.REDSTONE,
                        Location(world, a.x, a.y, a.z),
                        1,
                        DustOptions(Color.RED, 0.5f)
                    )

                    world.spawnParticle(
                        Particle.REDSTONE,
                        Location(world, b.x, b.y, b.z),
                        1,
                        DustOptions(Color.RED, 0.5f)
                    )

                    world.spawnParticle(
                        Particle.REDSTONE,
                        Location(world, c.x, c.y, c.z),
                        1,
                        DustOptions(Color.RED, 0.5f)
                    )

                    world.connect(a, b, DustOptions(Color.BLUE, 0.25f))
                    world.connect(b, c, DustOptions(Color.BLUE, 0.25f))
                    world.connect(c, a, DustOptions(Color.BLUE, 0.25f))

                    val ab = Vector3d(b).sub(a)
                    val ac = Vector3d(c).sub(a)

                    val n = Vector3d(ab).cross(ac)
                    val relP = Vector3d(sender.eyeLocation.toVector().toVector3d()).sub(a)
                    if (relP.dot(n) > 0.0) {
                        repeat(50) {
                            var cA = Random.nextDouble()
                            var cB = Random.nextDouble()
                            var cC = Random.nextDouble()

                            val t = cA + cB + cC

                            cA /= t
                            cB /= t
                            cC /= t

                            val bp = Vector3d(a).mul(cA).add(Vector3d(b).mul(cB)).add(Vector3d(c).mul(cC))

                            world.spawnParticle(
                                Particle.REDSTONE,
                                Location(world, bp.x, bp.y, bp.z),
                                1,
                                DustOptions(Color.GREEN, 0.3f)
                            )
                        }
                    }
                }
            }, 1, 1)
        } else if (args[1] == "next") {
            verticesBuffer += sender.eyeLocation.toVector().toVector3d()
            if (task == null) {
                if (verticesBuffer.size == 4) {
                    shape += Triple(verticesBuffer[2], verticesBuffer[1], verticesBuffer[0])
                    shape += Triple(verticesBuffer[0], verticesBuffer[1], verticesBuffer[3])
                    shape += Triple(verticesBuffer[1], verticesBuffer[2], verticesBuffer[3])
                    shape += Triple(verticesBuffer[2], verticesBuffer[0], verticesBuffer[3])

                    verticesBuffer.clear()

                    task = Bukkit.getScheduler().runTaskTimer(AbstractParticleEmitter.INSTANCE, Runnable {
                        for ((a, b, c) in shape) {
                            world.spawnParticle(
                                Particle.REDSTONE,
                                Location(world, a.x, a.y, a.z),
                                5,
                                DustOptions(Color.RED, 0.5f)
                            )

                            world.spawnParticle(
                                Particle.REDSTONE,
                                Location(world, b.x, b.y, b.z),
                                5,
                                DustOptions(Color.RED, 0.5f)
                            )

                            world.spawnParticle(
                                Particle.REDSTONE,
                                Location(world, c.x, c.y, c.z),
                                5,
                                DustOptions(Color.RED, 0.5f)
                            )

                            world.connect(a, b, DustOptions(Color.BLUE, 0.25f))
                            world.connect(b, c, DustOptions(Color.BLUE, 0.25f))
                            world.connect(c, a, DustOptions(Color.BLUE, 0.25f))

                            val ab = Vector3d(b).sub(a)
                            val ac = Vector3d(c).sub(a)

                            val n = Vector3d(ab).cross(ac)
                            val relP = Vector3d(sender.eyeLocation.toVector().toVector3d()).sub(a)
                            if (relP.dot(n) > 0.0) {
                                repeat(50) {
                                    var cA = Random.nextDouble()
                                    var cB = Random.nextDouble()
                                    var cC = Random.nextDouble()

                                    val t = cA + cB + cC

                                    cA /= t
                                    cB /= t
                                    cC /= t

                                    val bp = Vector3d(a).mul(cA).add(Vector3d(b).mul(cB)).add(Vector3d(c).mul(cC))

                                    world.spawnParticle(
                                        Particle.REDSTONE,
                                        Location(world, bp.x, bp.y, bp.z),
                                        5,
                                        DustOptions(Color.GREEN, 0.5f)
                                    )
                                }
                            }
                        }
                    }, 1, 1)
                }
            } else {
//                val newP = sender.eyeLocation.toVector().toVector3d()

                repeat(10) {
                    addPoint(
                        shape, Vector3d(origin).add(
                            Vector3d(
                                Random.nextDouble(-1.0, 1.0),
                                Random.nextDouble(-1.0, 1.0),
                                Random.nextDouble(-1.0, 1.0)
                            ).normalize(2.0)
                        ), world
                    )
                }
            }
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}

private fun addPoint(shape: MutableList<Triple<Vector3d, Vector3d, Vector3d>>, point: Vector3d, world: World) {
    // holds vertices associated with edges
    // non uniques will always be wound 2 ways, and will be removed
    // so unique leftovers are all wound correctly
    val uniqueEdges = mutableListOf<Pair<Vector3d, Vector3d>>()
    val facesToRemove = mutableListOf<Triple<Vector3d, Vector3d, Vector3d>>()

    for (face in shape) {
        val (a, b, c) = face
        val ab = Vector3d(b).sub(a)
        val ac = Vector3d(c).sub(a)

        val n = Vector3d(ab).cross(ac)
        val relP = Vector3d(point).sub(a)
        if (relP.dot(n) > 0.0) {
            repeat(500) {
                var cA = Random.nextDouble()
                var cB = Random.nextDouble()
                var cC = Random.nextDouble()

                val t = cA + cB + cC

                cA /= t
                cB /= t
                cC /= t

                val bp = Vector3d(a).mul(cA).add(Vector3d(b).mul(cB)).add(Vector3d(c).mul(cC))

                world.spawnParticle(
                    Particle.REDSTONE,
                    Location(world, bp.x, bp.y, bp.z),
                    5,
                    DustOptions(Color.ORANGE, 0.5f)
                )
            }
            //if any edge in 'uniqueEdges' is the same or the reverse of ab, ac, or cb, remove it
            //otherwise, add edge in wound order
            var foundAB = false
            var foundBC = false
            var foundCA = false

            val edgesToRemove = mutableListOf<Pair<Vector3d, Vector3d>>()
            for (uniqueEdge in uniqueEdges) {
                if (
                    (uniqueEdge.first.distanceSquared(a) < EPSILON && uniqueEdge.second.distanceSquared(b) < EPSILON)
                    || (uniqueEdge.first.distanceSquared(b) < EPSILON && uniqueEdge.second.distanceSquared(a) < EPSILON)
                ) {
                    foundAB = true
                    edgesToRemove += uniqueEdge
                    continue
                }

                if (
                    (uniqueEdge.first.distanceSquared(b) < EPSILON && uniqueEdge.second.distanceSquared(c) < EPSILON)
                    || (uniqueEdge.first.distanceSquared(c) < EPSILON && uniqueEdge.second.distanceSquared(b) < EPSILON)
                ) {
                    foundBC = true
                    edgesToRemove += uniqueEdge
                    continue
                }

                if (
                    (uniqueEdge.first.distanceSquared(c) < EPSILON && uniqueEdge.second.distanceSquared(a) < EPSILON)
                    || (uniqueEdge.first.distanceSquared(a) < EPSILON && uniqueEdge.second.distanceSquared(c) < EPSILON)
                ) {
                    foundCA = true
                    edgesToRemove += uniqueEdge
                    continue
                }
            }

            uniqueEdges.removeAll(edgesToRemove)

            if (!foundAB) {
                uniqueEdges += a to b
            }

            if (!foundBC) {
                uniqueEdges += b to c
            }

            if (!foundCA) {
                uniqueEdges += c to a
            }

            facesToRemove += face

        }
    }

    shape.removeAll(facesToRemove)
    facesToRemove.clear()

    for ((uniqueStart, uniqueEnd) in uniqueEdges) {
        world.connect(uniqueStart, uniqueEnd, DustOptions(Color.PURPLE, 0.5f))

        shape += Triple(uniqueStart, uniqueEnd, point)
    }
}

private const val EPSILON = 0.0001

private fun World.connect(start: Vector3d, end: Vector3d, options: DustOptions) {
    val delta = Vector3d(end).sub(start).normalize()
    var t = 0.0
    while (t < end.distance(start)) {
        spawnParticle(
            Particle.REDSTONE, Location(
                this,
                start.x + delta.x * t,
                start.y + delta.y * t,
                start.z + delta.z * t,
            ),
            1, options
        )
        t += 0.1
    }
}