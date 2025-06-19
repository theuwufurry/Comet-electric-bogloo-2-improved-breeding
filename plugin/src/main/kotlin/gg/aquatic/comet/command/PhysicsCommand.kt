package gg.aquatic.comet.command

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.command.CubeQuatMethod.Companion.TIME_STEP
import gg.aquatic.waves.command.ICommand
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.joml.*
import kotlin.math.roundToInt

object PhysicsCommand : ICommand {
    private var task: BukkitTask? = null

    override fun run(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) return

        val world = sender.world

        if (args.size < 11) {
            sender.sendMessage("Usage: /comet physics cube <vx> <vy> <vz> <width> <height> <length> <lx> <ly> <lz>")
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

        val origin = sender.location.toVector().toVector3d()
        val qrb = CubeQuatMethod(
            pos = Vector3d(origin),
            v = Vector3d(v0),
            width = dims.x,
            height = dims.y,
            length = dims.z,
            q = Quaterniond(),
            omega = Vector3d(l),
        )

        val qrb2 = CubeQuat2Method(
            pos = Vector3d(origin),
            v = Vector3d(v0),
            width = dims.x,
            height = dims.y,
            length = dims.z,
            q = Quaterniond(),
            omega = Vector3d(l),
        )


        task?.cancel()

        task = Bukkit.getScheduler().runTaskTimer(AbstractParticleEmitter.INSTANCE, Runnable {
            repeat ((0.05 / TIME_STEP).roundToInt()) { qrb.step() }
            repeat ((0.05 / TIME_STEP).roundToInt()) { qrb2.step() }

            val transformedVerticesQ = qrb.vertices.map(qrb::transformedVertex)
            val edgesQ = CubeQuatMethod.edges(transformedVerticesQ)

            for (vertex in transformedVerticesQ) {
                world.spawnParticle(
                    Particle.REDSTONE, Location(
                        world,
                        qrb.pos.x + vertex.x,
                        qrb.pos.y + vertex.y,
                        qrb.pos.z + vertex.z,
                    ),
                    5, Particle.DustOptions(Color.RED, 0.3f)
                )
            }

            for ((start, end) in edgesQ) {
                val d = end.distance(start)
                val delta = Vector3d(end).sub(start).normalize()
                var t = 0.0
                while (t < d) {
                    world.spawnParticle(
                        Particle.REDSTONE, Location(
                            world,
                            qrb.pos.x + start.x + delta.x * t,
                            qrb.pos.y + start.y + delta.y * t,
                            qrb.pos.z + start.z + delta.z * t,
                        ),
                        5, Particle.DustOptions(Color.BLUE, 0.2f)
                    )
                    t += 0.1
                }
            }

            val transformedVerticesQ2 = qrb2.vertices.map(qrb2::transformedVertex)
            val edgesQ2 = CubeQuat2Method.edges(transformedVerticesQ2)

            for (vertex in transformedVerticesQ2) {
                world.spawnParticle(
                    Particle.REDSTONE, Location(
                        world,
                        qrb2.pos.x + vertex.x,
                        qrb2.pos.y + vertex.y,
                        qrb2.pos.z + vertex.z,
                    ),
                    5, Particle.DustOptions(Color.YELLOW, 0.3f)
                )
            }

            for ((start, end) in edgesQ2) {
                val d = end.distance(start)
                val delta = Vector3d(end).sub(start).normalize()
                var t = 0.0
                while (t < d) {
                    world.spawnParticle(
                        Particle.REDSTONE, Location(
                            world,
                            qrb2.pos.x + start.x + delta.x * t,
                            qrb2.pos.y + start.y + delta.y * t,
                            qrb2.pos.z + start.z + delta.z * t,
                        ),
                        5, Particle.DustOptions(Color.PURPLE, 0.2f)
                    )
                    t += 0.1
                }
            }
        }, 1, 1)
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

class CubeQuatMethod(
    var pos: Vector3d,
    var v: Vector3d,

    val q: Quaterniond,

    val omega: Vector3d,

    val width: Double,
    val height: Double,
    val length: Double,
) {
    /**
     * Local space
     */
    val vertices: List<Vector3d> = listOf(
        Vector3d(-width / 2, -height / 2, -length / 2),
        Vector3d(-width / 2, -height / 2, length / 2),
        Vector3d(width / 2, -height / 2, length / 2),
        Vector3d(width / 2, -height / 2, -length / 2),

        Vector3d(-width / 2, height / 2, -length / 2),
        Vector3d(-width / 2, height / 2, length / 2),
        Vector3d(width / 2, height / 2, length / 2),
        Vector3d(width / 2, height / 2, -length / 2),
    )

    fun transformedVertex(vertex: Vector3d): Vector3d {
        return Vector3d(vertex).rotate(q)
    }

    val volume = width * height * length

    val inertia: Vector3d = Vector3d(
        height * height + length * length,
        width * width + length * length,
        width * width + height * height,
    ).mul(volume / 12.0)

    private var i = 0

    fun step() {
//        println("${i++}: omega n: ${omega.length()}")
//
        pos.add(Vector3d(v).mul(TIME_STEP))

        val h2 = TIME_STEP / 2.0

        val (dK1Q, dK1O) = calcDerivatives(q, omega)

        val k2Q = Quaterniond(q).add(Quaterniond(dK1Q).scale(h2)).normalize()
        val k2O = Vector3d(omega).add(Vector3d(dK1O).mul(h2))
        val (dK2Q, dK2O) = calcDerivatives(k2Q, k2O)

        val k3Q = Quaterniond(q).add(Quaterniond(dK2Q).scale(h2)).normalize()
        val k3O = Vector3d(omega).add(Vector3d(dK2O).mul(h2))
        val (dK3Q, dK3O) = calcDerivatives(k3Q, k3O)

        val k4Q = Quaterniond(q).add(Quaterniond(dK3Q).scale(TIME_STEP)).normalize()
        val k4O = Vector3d(omega).add(Vector3d(dK3O).mul(TIME_STEP))
        val (dK4Q, dK4O) = calcDerivatives(k4Q, k4O)

        val fDO = Vector3d(dK1O)
            .add(Vector3d(dK2O).mul(2.0))
            .add(Vector3d(dK3O).mul(2.0))
            .add(dK4O)
            .mul(TIME_STEP / 6.0)

        val fDQ = Quaterniond(dK1Q)
            .add(Quaterniond(dK2Q).scale(2.0))
            .add(Quaterniond(dK3Q).scale(2.0))
            .add(dK4Q)
            .scale(TIME_STEP / 6.0)

        println("dq: ${fDQ.lengthSquared()} : 1")

        omega.add(fDO)
        q.add(fDQ)

        q.normalize()
    }

    /**
     * @return dq/dt and dOmega/dt
     */
    private fun calcDerivatives(
        q: Quaterniond,
        o: Vector3d
    ): Pair<Quaterniond, Vector3d> {
        val dO = Vector3d(
            (inertia.y - inertia.z) / inertia.x * o.y * o.z,/* + T.x/inertia.x*/
            (inertia.z - inertia.x) / inertia.y * o.z * o.x,/* + T.y/inertia.y*/
            (inertia.x - inertia.y) / inertia.z * o.x * o.y,/* + T.z/inertia.z*/
        )

        val dQ = Quaterniond(q).mul(Quaterniond(o.x, o.y, o.z, 0.0)).mul(0.5)

        return Quaterniond(dQ.x, dQ.y, dQ.z, dQ.w) to dO
    }

    companion object {
        fun edges(vertices: List<Vector3d>): List<Pair<Vector3d, Vector3d>> {
            assert(vertices.size == 8)

            return listOf(
                vertices[0] to vertices[1],
                vertices[1] to vertices[2],
                vertices[2] to vertices[3],
                vertices[3] to vertices[0],
                vertices[4] to vertices[5],
                vertices[5] to vertices[6],
                vertices[6] to vertices[7],
                vertices[7] to vertices[4],
                vertices[0] to vertices[4],
                vertices[1] to vertices[5],
                vertices[2] to vertices[6],
                vertices[3] to vertices[7],
            )
        }

        const val TIME_STEP = 0.05
    }
}

class CubeQuat2Method(
    var pos: Vector3d,
    var v: Vector3d,

    val q: Quaterniond,

    val omega: Vector3d,

    val width: Double,
    val height: Double,
    val length: Double,
) {
    /**
     * Local space
     */
    val vertices: List<Vector3d> = listOf(
        Vector3d(-width / 2, -height / 2, -length / 2),
        Vector3d(-width / 2, -height / 2, length / 2),
        Vector3d(width / 2, -height / 2, length / 2),
        Vector3d(width / 2, -height / 2, -length / 2),

        Vector3d(-width / 2, height / 2, -length / 2),
        Vector3d(-width / 2, height / 2, length / 2),
        Vector3d(width / 2, height / 2, length / 2),
        Vector3d(width / 2, height / 2, -length / 2),
    )

    fun transformedVertex(vertex: Vector3d): Vector3d {
        return Vector3d(vertex).rotate(q)
    }

    val volume = width * height * length

    val inertia: Vector3d = Vector3d(
        height * height + length * length,
        width * width + length * length,
        width * width + height * height,
    ).mul(volume / 12.0)

    private var i = 0

    fun step() {
//        println("2: ${i++}: omega n: ${omega.length()}")
//
        pos.add(Vector3d(v).mul(TIME_STEP))

        val dO = Vector3d(
            (inertia.y - inertia.z) / inertia.x * omega.y * omega.z,/* + T.x/inertia.x*/
            (inertia.z - inertia.x) / inertia.y * omega.z * omega.x,/* + T.y/inertia.y*/
            (inertia.x - inertia.y) / inertia.z * omega.x * omega.y,/* + T.z/inertia.z*/
        ).mul(TIME_STEP)

        omega.add(dO)



        val dq = Quaterniond(q).mul(Quaterniond(omega.x, omega.y, omega.z, 0.0)).mul(0.5).mul(TIME_STEP)
//        println("2: do: ${dO.length()} fdq: ${dq.length()}")
        println("dq: ${dq.lengthSquared()} : 2")

        q.add(dq.x, dq.y, dq.z, dq.w)

        q.normalize()
    }

    companion object {
        fun edges(vertices: List<Vector3d>): List<Pair<Vector3d, Vector3d>> {
            assert(vertices.size == 8)

            return listOf(
                vertices[0] to vertices[1],
                vertices[1] to vertices[2],
                vertices[2] to vertices[3],
                vertices[3] to vertices[0],
                vertices[4] to vertices[5],
                vertices[5] to vertices[6],
                vertices[6] to vertices[7],
                vertices[7] to vertices[4],
                vertices[0] to vertices[4],
                vertices[1] to vertices[5],
                vertices[2] to vertices[6],
                vertices[3] to vertices[7],
            )
        }
    }
}
