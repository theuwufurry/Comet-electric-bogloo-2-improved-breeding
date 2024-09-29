package com.ixume.particleemitter.emitter

import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.emitter.lifetime.EmitterLifetimeComponent
import com.ixume.particleemitter.particle.color.ColorComponent
import com.ixume.particleemitter.particle.lifetime.ParticleLifetimeComponent
import com.ixume.particleemitter.emitter.rate.RateComponent
import com.ixume.particleemitter.emitter.shape.ShapeComponent
import com.ixume.particleemitter.particle.position.PositionComponent
import com.ixume.particleemitter.particle.texture.SpriteComponent
import com.ixume.particleemitter.particle.transformation.scale.ScaleComponent
import org.bukkit.Bukkit
import org.bukkit.Location

data class UnrealizedEmitter(val unrealizedRateComponent: RateComponent,
                             val unrealizedParticleLifetimeComponent: ParticleLifetimeComponent,
                             val unrealizedSpriteComponent: SpriteComponent,
                             val unrealizedShapeComponent: ShapeComponent,
                             val unrealizedColorComponent: ColorComponent,
                             val unrealizedEmitterLifetimeComponent: EmitterLifetimeComponent,
                             val unrealizedPositionComponent: PositionComponent,
                             val unrealizedScaleComponent: ScaleComponent) {
    fun realize(location: Location) {
        val emitter = Emitter(unrealizedRateComponent, unrealizedParticleLifetimeComponent, unrealizedShapeComponent, unrealizedSpriteComponent, unrealizedColorComponent, unrealizedEmitterLifetimeComponent, unrealizedPositionComponent, unrealizedScaleComponent,location, null)
        emitter.task = Bukkit.getScheduler().runTaskTimerAsynchronously(ParticleEmitter.INSTANCE, Runnable {
            emitter.tick()
        }, 0, 1)
    }
}