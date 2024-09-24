package com.ixume.particlesTesting.emitter

import com.ixume.particlesTesting.emitter.lifetime.LifetimeComponent
import com.ixume.particlesTesting.emitter.rate.RateComponent
import com.ixume.particlesTesting.emitter.shape.ShapeComponent
import com.ixume.particlesTesting.particle.Particle
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay

data class Emitter(val rateComponent: RateComponent, val lifetimeComponent: LifetimeComponent, val shapeComponent: ShapeComponent, var location: Location) {
    private val data: EmitterMochaData = EmitterMochaData(0.0)
    private val particles: MutableList<Particle> = mutableListOf()

    fun tick() {
        val world = location.world

        data.age++

        val deadParticles: MutableList<Particle> = mutableListOf()
        for (particle in particles) {
            particle.tick()
            if (!lifetimeComponent.live(data, particle.data)) {
                particle.entity?.remove()
                deadParticles += particle
            }
        }

        particles.removeAll(deadParticles)

        repeat(rateComponent.toEmit(data)) {
            val spawnOffset = shapeComponent.offset(data)
            val entity: TextDisplay = world.spawn(Location(location.world, location.x + spawnOffset.x, location.y + spawnOffset.y, location.z + spawnOffset.z), TextDisplay::class.java)
            entity.billboard = Display.Billboard.CENTER
            entity.text(Component.text("P"))
            entity.backgroundColor = Color.fromARGB(0, 0, 0, 0)
            particles += Particle(entity)
        }
    }
}