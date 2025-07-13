package gg.aquatic.comet.command.physics

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.command.PhysicsCommand
import gg.aquatic.comet.command.debugConnect
import org.bukkit.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.scheduler.BukkitTask
import org.joml.Vector3d
import org.joml.Vector3i
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureNanoTime

object PhysicsListener : Listener {
    private lateinit var graphicsTask: BukkitTask
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, AbstractParticleEmitter.INSTANCE)
        graphicsTask = Bukkit.getScheduler().runTaskTimer(AbstractParticleEmitter.INSTANCE, Runnable {

        }, 2, 2)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val start = event.player.eyeLocation.toVector().toVector3d()
        val dir = event.player.location.direction.toVector3d().mul(20.0)
        val end = Vector3d(start).add(dir)

        val allIntersections = mutableListOf<Triple<Body, Vector3d, Vector3d>>()
        for (body in PhysicsCommand.physicsWorlds[event.player.world]?.bodies ?: return) {
            allIntersections += body.intersect(start, end).map { Triple(body, it.first, it.second) }
        }

        val (body, intersection, normal) = allIntersections.minByOrNull { (_, inter, _) -> inter.distanceSquared(start) }
            ?: return

        event.player.world.spawnParticle(
            Particle.REDSTONE, Location(
                event.player.world,
                intersection.x,
                intersection.y,
                intersection.z,
            ),
            5, Particle.DustOptions(Color.YELLOW, 0.5f)
        )

        event.player.world.debugConnect(
            Vector3d(
                intersection.x,
                intersection.y,
                intersection.z,
            ),
            Vector3d(
                intersection.x + normal.x,
                intersection.y + normal.y,
                intersection.z + normal.z,
            ), Particle.DustOptions(Color.BLUE, 0.25f)
        )

        val type = event.player.inventory.itemInMainHand.type
        if (type == Material.END_ROD) {
            body.applyImpulse(intersection, normal, Vector3d(dir).normalize(PhysicsCommand.PUSH_V))
        }
    }

    private var meshRange: Pair<Location, Location?>? = null
    var mesh: Mesh2? = null

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand
        if (item.type == Material.END_ROD) {
            event.isCancelled = true
            return
        }

        if (item.type == Material.GOLD_NUGGET) {
            event.isCancelled = true
            meshRange = if (meshRange != null && meshRange!!.second == null) {
                val time = measureNanoTime {
                    mesh = Mesh.mesh2(
                        event.block.world,
                        Vector3i(
                            min(meshRange!!.first.blockX, event.block.location.blockX),
                            min(meshRange!!.first.blockY, event.block.location.blockY),
                            min(meshRange!!.first.blockZ, event.block.location.blockZ),
                        ),
                        Vector3i(
                            max(meshRange!!.first.blockX, event.block.location.blockX),
                            max(meshRange!!.first.blockY, event.block.location.blockY),
                            max(meshRange!!.first.blockZ, event.block.location.blockZ),
                        )
                    )
                }

                println("TOOK: ${time.toDouble() / 1_000_000.toDouble()}")

                meshRange!!.first to event.block.location
            } else {
                event.block.location to null
            }
        }
    }
}
