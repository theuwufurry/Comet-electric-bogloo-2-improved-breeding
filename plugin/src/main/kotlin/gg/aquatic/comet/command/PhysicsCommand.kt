package gg.aquatic.comet.command

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.command.physics.BlockBody
import gg.aquatic.comet.command.physics.Body
import gg.aquatic.comet.command.physics.Body.Companion.TIME_STEP
import gg.aquatic.comet.command.physics.Contact
import gg.aquatic.comet.command.physics.Cuboid
import gg.aquatic.waves.command.ICommand
import org.bukkit.*
import org.bukkit.Particle.DustOptions
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.BoundingBox
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

object PhysicsCommand : ICommand {
    private lateinit var task: BukkitTask
    private var time = 0
    private val DEBUG_FREQUENCY = 2
    val globalBodies = mutableMapOf<World, MutableList<Body>>()
    private var frozen = false
    private var steps = 0
    private var untilCollision = false
    private var enhancement = Vector3d(1.0)

    /**
     * point, direction
     */
    private var globalContacts = mutableMapOf<World, MutableList<Contact>>()
    var DEBUG_LEVEL = 0

    fun init() {
        task = Bukkit.getScheduler().runTaskTimer(AbstractParticleEmitter.INSTANCE, Runnable {
            time++
            for ((world, localBodies) in globalBodies.entries) {
                val contacts = mutableListOf<Contact>()
                globalContacts[world] = contacts
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
                        for (body in localBodies) {
                            if (body.hasGravity) body.velocity.add(Vector3d(GRAVITY).mul(TIME_STEP))
                        }

                        contacts.clear()

                        for (i in 0..<localBodies.size) {
                            val firstBody = localBodies[i]
                            val firstBoundingBox = firstBody.boundingBox
                            if (localBodies.size > 1) {
                                for (j in (i + 1)..<localBodies.size) {
                                    val second = localBodies[j]

                                    if (!firstBoundingBox.overlaps(second.boundingBox)) continue

                                    val result = firstBody.collidesBody(second) ?: continue

                                    contacts += Contact(firstBody, second, result)
                                }
                            }

                            val blocks = firstBoundingBox.overlappingBlocks(world)
                            for (block in blocks) {
                                val body = BlockBody(block)
                                val result = firstBody.collidesBlock(body) ?: continue
                                contacts += Contact(body, firstBody, result)
                                break
                            }

//                            for (debugBody in debugBodies) {
//                                val result = firstBody.collidesBlock(debugBody) ?: continue
//                                contacts += Contact(debugBody, firstBody, result)
//                            }
                        }

                        for (itr in 1..5) {
                            for (contact in contacts) {
                                val first = contact.first
                                val second = contact.second

                                val point = contact.result.point
                                val norm = contact.result.norm
                                val depth = contact.result.depth

                                val massImpact = first.inverseMass + second.inverseMass

                                val firstLocalPoint = first.globalToLocal(point)
                                val firstLocalNorm =
                                    Vector3d(norm).negate().rotate(Quaterniond(first.q).conjugate()).normalize()
                                val secondLocalPoint = second.globalToLocal(point)
                                val secondLocalNorm =
                                    Vector3d(norm).rotate(Quaterniond(second.q).conjugate()).normalize()

                                val vr = Vector3d(second.velocity).sub(first.velocity).dot(norm) +
                                        Vector3d(second.omega).cross(secondLocalPoint).dot(secondLocalNorm) -
                                        Vector3d(first.omega).cross(firstLocalPoint).dot(firstLocalNorm)

                                val firstAngularImpact =
                                    Vector3d(firstLocalPoint)
                                        .cross(firstLocalNorm)
                                        .mul(first.inverseInertia)
                                        .cross(firstLocalPoint)
                                        .dot(firstLocalNorm)
//                                    println("first angular impact: $firstAngularImpact")
                                val secondAngularImpact =
                                    Vector3d(secondLocalPoint)
                                        .cross(secondLocalNorm)
                                        .mul(second.inverseInertia)
                                        .cross(secondLocalPoint)
                                        .dot(secondLocalNorm)
//                                    println("first angular impact: $secondAngularImpact")
                                val angularImpact = firstAngularImpact + secondAngularImpact

                                val bias = BIAS / TIME_STEP * (abs(depth) - SLOP).coerceAtLeast(0.0)

                                var J =
                                    (bias + (vr * -(1.0)) / (massImpact + angularImpact)).coerceAtLeast(
                                        0.0
                                    )


                                val curJSum = contact.jSum
                                contact.jSum = (curJSum + J).coerceAtLeast(0.0)
                                J = contact.jSum - curJSum

                                val firstDV = Vector3d(norm).mul(J * first.inverseMass)
                                first.velocity.sub(firstDV)
                                first.omega.add(
                                    Vector3d(firstLocalPoint).cross(Vector3d(firstLocalNorm).mul(J))
                                        .mul(first.inverseInertia)
                                )

                                val secondDV = Vector3d(norm).mul(J * second.inverseMass)
                                second.velocity.add(secondDV)
                                second.omega.add(
                                    Vector3d(secondLocalPoint).cross(Vector3d(secondLocalNorm).mul(J))
                                        .mul(second.inverseInertia)
                                )

//                                    if (J > 1.0) {
//                                        println("STRONG COLLISION! ITR: $itr")
//                                        println("  - NORM: $norm DEPTH: $depth POINT: $point")
//                                        println("  - J: $J")
//                                        println("    * VR: $vr")
//                                        println("    * BIAS: $bias")
//                                        println("    * MASSIMPACT: $massImpact")
//                                        println("    * ANGULARIMPACT: $angularImpact")
//                                        println("  - firstDV: $firstDV")
//                                        println("  - secondDV: $secondDV")
//                                    }
                            }
                        }

                        for (body in localBodies) {
                            body.step()
                        }
                    }


                    for (contact in contacts) {
                        val (point,
                            norm,
                            _,
                            minkowski,
                            closest,
                            originals) = contact.result

                        world.debugConnect(
                            point,
                            Vector3d(point).add(norm),
                            DustOptions(Color.BLUE, 0.2f)
                        )

                        world.spawnParticle(
                            Particle.REDSTONE,
                            Location(
                                world,
                                point.x, point.y, point.z,
                            ),
                            1, Particle.DustOptions(Color.RED, 0.4f)
                        )

                        val minkowskiDebugOrigin = Vector3d(point).add(0.0, 3.0, 0.0)
                        val vertices = mutableSetOf<Vector3d>()
                        if (minkowski != null) {
                            originals!!
                            closest!!
                            for ((a, b, c) in minkowski) {
                                vertices += a
                                vertices += b
                                vertices += c

                                if (DEBUG_LEVEL > 2) {
                                    val color =
                                        choices[Vector3d(a).add(b).add(c).hashCode().absoluteValue % (choices.size)]

                                    world.debugConnect(
                                        Vector3d(minkowskiDebugOrigin).add(Vector3d(a).mul(enhancement)),
                                        Vector3d(minkowskiDebugOrigin).add(Vector3d(b).mul(enhancement)),
                                        DustOptions(Color.BLACK, 0.2f)
                                    )

                                    world.debugConnect(
                                        Vector3d(minkowskiDebugOrigin).add(Vector3d(a).mul(enhancement)),
                                        Vector3d(minkowskiDebugOrigin).add(Vector3d(c).mul(enhancement)),
                                        DustOptions(Color.BLACK, 0.2f)
                                    )

                                    world.debugConnect(
                                        Vector3d(minkowskiDebugOrigin).add(Vector3d(c).mul(enhancement)),
                                        Vector3d(minkowskiDebugOrigin).add(Vector3d(b).mul(enhancement)),
                                        DustOptions(Color.BLACK, 0.2f)
                                    )

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
                                            Location(
                                                world,
                                                minkowskiDebugOrigin.x + bp.x * enhancement.x,
                                                minkowskiDebugOrigin.y + bp.y * enhancement.y,
                                                minkowskiDebugOrigin.z + bp.z * enhancement.z,
                                            ),
                                            1,
                                            DustOptions(color, 0.2f)
                                        )
                                    }
                                }
                            }

                            for (vertex in vertices) {
                                val (start, end) = originals[vertex]!!

                                val newPos = Vector3d(minkowskiDebugOrigin).add(vertex)

                                if (DEBUG_LEVEL > 2) {
                                    world.debugConnect(
                                        start,
                                        end,
                                        DustOptions(Color.BLUE, 0.1f)
                                    )

                                    world.debugConnect(
                                        newPos,
                                        end,
                                        DustOptions(Color.YELLOW, 0.1f)
                                    )

                                    world.debugConnect(
                                        start,
                                        newPos,
                                        DustOptions(Color.YELLOW, 0.1f)
                                    )

                                    world.spawnParticle(
                                        Particle.REDSTONE,
                                        Location(
                                            world,
                                            newPos.x,
                                            newPos.y,
                                            newPos.z,
                                        ),
                                        1, Particle.DustOptions(Color.WHITE, 0.5f)
                                    )
                                }

                                if (DEBUG_LEVEL > 2) {
                                    world.spawnParticle(
                                        Particle.REDSTONE,
                                        Location(
                                            world,
                                            minkowskiDebugOrigin.x,
                                            minkowskiDebugOrigin.y,
                                            minkowskiDebugOrigin.z,
                                        ),
                                        1, Particle.DustOptions(Color.ORANGE, 0.5f)
                                    )

                                    world.spawnParticle(
                                        Particle.REDSTONE,
                                        Location(
                                            world,
                                            minkowskiDebugOrigin.x + closest.first.x,
                                            minkowskiDebugOrigin.y + closest.first.y,
                                            minkowskiDebugOrigin.z + closest.first.z,
                                        ),
                                        1, Particle.DustOptions(Color.RED, 0.5f)
                                    )

                                    world.spawnParticle(
                                        Particle.REDSTONE,
                                        Location(
                                            world,
                                            minkowskiDebugOrigin.x + closest.second.x,
                                            minkowskiDebugOrigin.y + closest.second.y,
                                            minkowskiDebugOrigin.z + closest.second.z,
                                        ),
                                        1, Particle.DustOptions(Color.RED, 0.5f)
                                    )

                                    world.spawnParticle(
                                        Particle.REDSTONE,
                                        Location(
                                            world,
                                            minkowskiDebugOrigin.x + closest.third.x,
                                            minkowskiDebugOrigin.y + closest.third.y,
                                            minkowskiDebugOrigin.z + closest.third.z,
                                        ),
                                        1, Particle.DustOptions(Color.RED, 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (time % DEBUG_FREQUENCY == 0) {
                    if (DEBUG_LEVEL > 0) {
                        for (body in localBodies) {
                            val boundingBox = body.boundingBox

                            if (DEBUG_LEVEL > 1) {
                                val blocks = boundingBox.overlappingBlocks(world)
                                for (block in blocks) {
                                    world.debugBoundingBox(block.boundingBox, DustOptions(Color.RED, 0.4f), 0.24)
                                }
                            }

                            world.debugBoundingBox(boundingBox, DustOptions(Color.BLUE, 0.4f))
                        }
                    }
                }
            }
        }, 1, 1)
    }

    override fun run(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) return

        if (args[1] == "debug") {
            DEBUG_LEVEL = args[2].toInt()
            return
        }

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
            clear()
            return
        }

        if (args[1] == "cube") {
            var v0 = Vector3d(0.0)
            var dims = Vector3d(1.0)
            var l = Vector3d(0.0)
            var rot0 = Vector3d(0.0)

            var density = 1.0
            var hasGravity = true
            var SAT = false

            var i = 2
            while (i < args.size) {
                val arg = args[i]

                if (arg == "--velocity" || arg == "-v") {
                    val buf = mutableListOf<Double>()
                    i++
                    while (i < args.size) {
                        val arg2 = args[i]
                        val d = arg2.toDoubleOrNull()
                        if (d == null) {
                            i--;
                            break
                        }

                        buf += d

                        if (buf.size == 3) {
                            v0 = Vector3d(buf[0], buf[1], buf[2])
                            break
                        }

                        i++
                    }
                }

                if (arg == "--dims" || arg == "-d") {
                    val buf = mutableListOf<Double>()
                    i++
                    while (i < args.size) {
                        val arg2 = args[i]
                        val d = arg2.toDoubleOrNull()
                        if (d == null) {
                            i--;
                            break
                        }

                        buf += d

                        if (buf.size == 3) {
                            dims = Vector3d(buf[0], buf[1], buf[2])
                            break
                        }

                        i++
                    }
                }

                if (arg == "--omega" || arg == "-o") {
                    val buf = mutableListOf<Double>()
                    i++
                    while (i < args.size) {
                        val arg2 = args[i]
                        val d = arg2.toDoubleOrNull()
                        if (d == null) {
                            i--;
                            break
                        }

                        buf += d

                        if (buf.size == 3) {
                            l = Vector3d(buf[0], buf[1], buf[2])
                            break
                        }

                        i++
                    }
                }

                if (arg == "--rot" || arg == "-r") {
                    val buf = mutableListOf<Double>()
                    i++
                    while (i < args.size) {
                        val arg2 = args[i]
                        val d = arg2.toDoubleOrNull()
                        if (d == null) {
                            i--;
                            break
                        }

                        buf += d

                        if (buf.size == 3) {
                            rot0 = Vector3d(buf[0], buf[1], buf[2])
                            break
                        }

                        i++
                    }
                }

                if (arg == "--density") {
                    i++
                    val arg2 = args[i]
                    val d = arg2.toDoubleOrNull()
                    if (d == null) {
                        i--
                    } else {
                        density = d
                    }
                }

                if (arg == "--gravity" || arg == "-g") {
                    hasGravity = true
                }

                if (arg == "--no-gravity" || arg == "-ng") {
                    hasGravity = false
                }

                if (arg == "--sat") {
                    SAT = true
                }

                i++
            }

            val origin = sender.location.toVector().toVector3d()
            val rb = if (SAT) Cuboid(
                world = sender.world,
                pos = Vector3d(origin),
                velocity = Vector3d(),
                width = dims.x,
                height = dims.y,
                length = dims.z,
                q = Quaterniond(),
                omega = Vector3d(),
                density = density,
                hasGravity = hasGravity,
            ) else Cuboid(
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

            val ls = globalBodies.getOrPut(sender.world) { mutableListOf() }

            ls += rb
        }
    }

    fun clear() {
        globalBodies.values.forEach { it.forEach { body -> body.kill() } }
        globalBodies.clear()
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }

    fun onDisable() {
        clear()
    }
}

fun World.debugConnect(start: Vector3d, end: Vector3d, options: DustOptions, interval: Double = 0.1) {
    val dir = Vector3d(end).sub(start).normalize()!!
    if (!dir.isFinite) return
    var t = 0.0
    while (t < end.distance(start)) {
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

        t += interval
    }
}

private fun BoundingBox.overlappingBlocks(world: World): List<Block> {
    val blocks = mutableListOf<Block>()
    var x = minX
    while (x < maxX + 1.0) {
        var y = minY
        while (y < maxY + 1.0) {
            var z = minZ
            while (z < maxZ + 1.0) {
                val block = world.getBlockAt(
                    floor(x).toInt(),
                    floor(y).toInt(),
                    floor(z).toInt()
                )

                if (block.isPassable) {
                    z++
                    continue
                }

                if (block.boundingBox.overlaps(this)
                ) {
                    val local = this.clone().shift(-block.location.x, -block.location.y, -block.location.z)
                    if (block.collisionShape.overlaps(local)) {
                        blocks += block
                    }
                }

                z++
            }
            y++
        }
        x++
    }

    return blocks
}

private fun World.debugBoundingBox(box: BoundingBox, options: DustOptions, interval: Double = 0.1) {
    this.debugConnect(
        Vector3d(box.minX, box.minY, box.minZ),
        Vector3d(box.maxX, box.minY, box.minZ),
        options, interval
    )

    this.debugConnect(
        Vector3d(box.maxX, box.minY, box.minZ),
        Vector3d(box.maxX, box.minY, box.maxZ),
        options, interval
    )

    this.debugConnect(
        Vector3d(box.maxX, box.minY, box.maxZ),
        Vector3d(box.minX, box.minY, box.maxZ),
        options, interval
    )

    this.debugConnect(
        Vector3d(box.minX, box.minY, box.maxZ),
        Vector3d(box.minX, box.minY, box.minZ),
        options, interval
    )

    this.debugConnect(
        Vector3d(box.minX, box.maxY, box.minZ),
        Vector3d(box.maxX, box.maxY, box.minZ),
        options, interval
    )

    this.debugConnect(
        Vector3d(box.maxX, box.maxY, box.minZ),
        Vector3d(box.maxX, box.maxY, box.maxZ),
        options, interval
    )

    this.debugConnect(
        Vector3d(box.maxX, box.maxY, box.maxZ),
        Vector3d(box.minX, box.maxY, box.maxZ),
        options, interval
    )

    this.debugConnect(
        Vector3d(box.minX, box.maxY, box.maxZ),
        Vector3d(box.minX, box.maxY, box.minZ),
        options, interval
    )

    this.debugConnect(
        Vector3d(box.minX, box.minY, box.minZ),
        Vector3d(box.minX, box.maxY, box.minZ),
        options, interval
    )

    this.debugConnect(
        Vector3d(box.maxX, box.minY, box.minZ),
        Vector3d(box.maxX, box.maxY, box.minZ),
        options, interval
    )

    this.debugConnect(
        Vector3d(box.maxX, box.minY, box.maxZ),
        Vector3d(box.maxX, box.maxY, box.maxZ),
        options, interval
    )

    this.debugConnect(
        Vector3d(box.minX, box.minY, box.maxZ),
        Vector3d(box.minX, box.maxY, box.maxZ),
        options, interval
    )
}

private const val BIAS = 0.1
private const val SLOP = 0.05
private val GRAVITY = Vector3d(0.0, -5.0, 0.0)

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
