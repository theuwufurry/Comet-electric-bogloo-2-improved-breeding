package gg.aquatic.comet.command.physics

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.command.debugConnect
import gg.aquatic.waves.command.ICommand
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.joml.Vector3d

object ClosestLinesCommand : ICommand {
    private var task: BukkitTask? = null
    private val lines = mutableListOf<Pair<Vector3d, Vector3d>>()
    private var originBuffer: Vector3d? = null
    override fun run(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) return

        if (originBuffer == null) {
            originBuffer = sender.eyeLocation.toVector().toVector3d()
            return
        } else {
            lines += sender.eyeLocation.toVector().toVector3d() to originBuffer!!
            originBuffer = null
            if (lines.size > 2) {
                lines.removeFirst()
            }
        }

        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(AbstractParticleEmitter.INSTANCE, Runnable {
                for ((origin, end) in lines) {
                    sender.world.debugConnect(
                        origin,
                        end,
                        Particle.DustOptions(Color.BLUE, 0.3f), 0.05
                    )
                }

                if (lines.size > 1) {
                    val (onFirst, onSecond, distance) = Cuboid.closestPointsBetweenSegments(
                        a0 = lines[0].first,
                        a1 = lines[0].second,
                        b0 = lines[1].first,
                        b1 = lines[1].second,
                    ) ?: return@Runnable

                    sender.world.debugConnect(
                        onFirst,
                        onSecond,
                        Particle.DustOptions(Color.RED, 0.5f),
                        0.01
                    )

                    println("distance: $distance")
                }
            }, 2, 2)
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}