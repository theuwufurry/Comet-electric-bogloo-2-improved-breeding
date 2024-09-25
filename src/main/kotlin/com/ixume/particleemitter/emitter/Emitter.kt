package com.ixume.particleemitter.emitter

import com.ixume.particleemitter.emitter.lifetime.LifetimeComponent
import com.ixume.particleemitter.emitter.rate.RateComponent
import com.ixume.particleemitter.emitter.shape.ShapeComponent
import com.ixume.particleemitter.particle.Particle
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import org.bukkit.Location
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.joml.Vector3d
import javax.script.Bindings
import javax.script.SimpleBindings

data class Emitter(val rateComponent: RateComponent, val lifetimeComponent: LifetimeComponent, val shapeComponent: ShapeComponent, var location: Location) {
    private val data: EmitterData = EmitterData(0.0)
    private val emitterBindings: Bindings = SimpleBindings(mutableMapOf("emitter" to data) as Map<String, Any>?)
    private val particles: MutableList<Particle> = mutableListOf()
    private val deadParticles: MutableList<Particle> = mutableListOf()

    fun tick() {
        val world = location.world

        data.age++
        for (particle in particles) {
            particle.tick()
            if (!lifetimeComponent.keepAlive(data, emitterBindings, particle.data, particle.bindings)) {
                deadParticles += particle
            }
        }

        particles.removeAll(deadParticles)
        val ids = IntArrayList(deadParticles.map {it.id})
        for (player in world.players) {
            (player as CraftPlayer).handle.connection.send(ClientboundRemoveEntitiesPacket(ids))
        }

        deadParticles.clear()

        repeat(rateComponent.toEmit(data, emitterBindings)) {
            val spawnOffset = shapeComponent.offset(data, emitterBindings)
            val particle = Particle(Vector3d(location.x + spawnOffset.x, location.y + spawnOffset.y, location.z + spawnOffset.z))
                val packet = particle.getAddPacket(world)
                for (player in world.players) {
                    (player as CraftPlayer).handle.connection.send(packet)
                }

            particles += particle
        }
    }
}