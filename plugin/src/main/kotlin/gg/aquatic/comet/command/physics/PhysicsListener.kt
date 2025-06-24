package gg.aquatic.comet.command.physics

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.command.PhysicsCommand
import org.bukkit.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.joml.Vector3d

object PhysicsListener : Listener {
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, AbstractParticleEmitter.INSTANCE)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val start = event.player.eyeLocation.toVector().toVector3d()
        val dir = event.player.location.direction.toVector3d().mul(20.0)
        val end = Vector3d(start).add(dir)

        val allIntersections = mutableListOf<Triple<Body, Vector3d, Vector3d>>()
        for (body in PhysicsCommand.bodies) {
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

        val type = event.player.inventory.itemInMainHand.type
        if (type == Material.END_ROD) {
            body.applyImpulse(intersection, normal, Vector3d(dir).normalize(2.5))
        }
    }
}