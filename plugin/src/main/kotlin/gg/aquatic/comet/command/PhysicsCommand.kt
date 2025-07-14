package gg.aquatic.comet.command

import gg.aquatic.comet.api.AbstractParticleEmitter
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
import kotlin.math.floor

object PhysicsCommand : ICommand {
    private lateinit var task: BukkitTask
    val DEBUG_FREQUENCY = 2
    val physicsWorlds = mutableMapOf<World, PhysicsWorld>()
    var frozen = false
    var steps = 0
    var untilCollision = false
    var enhancement = Vector3d(1.0)

    /**
     * point, direction
     */
    var DEBUG_LEVEL = 0
    var DEBUG_SAT_LEVEL = 0
    var PUSH_V = 2.5
    var DEBUG_MESH_LEVEL = 0

    fun init() {
        task = Bukkit.getScheduler().runTaskTimer(AbstractParticleEmitter.INSTANCE, Runnable {
            physicsWorlds.forEach { it.value.tick() }
        }, 1, 1)
    }

    override fun run(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) return

        if (args[1] == "debug") {
            val nl = args[2].toInt()
            DEBUG_LEVEL = nl
            sender.sendMessage("DEBUG LEVEL IS NOW $DEBUG_LEVEL")
            return
        }

        if (args[1] == "debug-sat-level") {
            val nl = args[2].toInt()
            DEBUG_SAT_LEVEL = nl
            sender.sendMessage("DEBUG SAT LEVEL IS NOW $DEBUG_SAT_LEVEL")
            return
        }

        if (args[1] == "push-v") {
            val nl = args[2].toDouble()
            PUSH_V = nl
            sender.sendMessage("PUSH_V IS NOW $PUSH_V")
            return
        }

        if (args[1] == "debug-mesh-level") {
            val nl = args[2].toInt()
            DEBUG_MESH_LEVEL = nl
            sender.sendMessage("DEBUG MESH LEVEL IS NOW $DEBUG_MESH_LEVEL")
            return
        }

        if (args[1] == "freeze") {
            if (frozen) sender.sendMessage("UNFROZEN") else println("FROZEN")
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
            var dims = Vector3d(0.99)
            var l = Vector3d(0.0)
            var rot0 = Vector3d(0.0)

            var density = 1.0
            var hasGravity = true
            var SAT = false
            var trueOmega = false

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

                if (arg == "--true-omega" || arg == "-to") {
                    trueOmega = true
                }

                i++
            }

            val origin = sender.location.toVector().toVector3d()
            val q = Quaterniond().rotateXYZ(rot0.x, rot0.y, rot0.z)
            val o = if (trueOmega) l.rotate(Quaterniond(q).conjugate()) else l
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
                q = q,
                omega = o,
                density = density,
                hasGravity = hasGravity,
            )

            val ls = physicsWorlds.getOrPut(sender.world) { PhysicsWorld((sender.world)) }

            ls.bodies += rb
        }
    }

    fun clear() {
        physicsWorlds.values.forEach { it.bodies.forEach { body -> body.kill() } }
        physicsWorlds.values.forEach { it.bodies.clear() }
        physicsWorlds.values.forEach { it.meshes.clear() }
        physicsWorlds.values.forEach { it.contacts.clear() }
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

val choices = listOf(
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
