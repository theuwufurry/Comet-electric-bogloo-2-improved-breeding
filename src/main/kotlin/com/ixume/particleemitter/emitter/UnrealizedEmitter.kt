package com.ixume.particleemitter.emitter

import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.lifetime.EmitterLifetimeComponent
import com.ixume.particleemitter.particle.color.ColorComponent
import com.ixume.particleemitter.particle.lifetime.ParticleLifetimeComponent
import com.ixume.particleemitter.emitter.rate.RateComponent
import com.ixume.particleemitter.emitter.shape.ShapeComponent
import com.ixume.particleemitter.particle.position.PositionComponent
import com.ixume.particleemitter.particle.sprite.SpriteComponent
import com.ixume.particleemitter.particle.transformation.scale.ScaleComponent
import org.bukkit.Bukkit
import org.bukkit.Location

data class UnrealizedEmitter(val unrealizedRateComponent: UnrealizedComponent<out RateComponent>,
                             val unrealizedParticleLifetimeComponent: UnrealizedComponent<out ParticleLifetimeComponent>,
                             val unrealizedSpriteComponent: UnrealizedComponent<out SpriteComponent>,
                             val unrealizedShapeComponent: UnrealizedComponent<out ShapeComponent>,
                             val unrealizedColorComponent: UnrealizedComponent<out ColorComponent>,
                             val unrealizedEmitterLifetimeComponent: UnrealizedComponent<out EmitterLifetimeComponent>,
                             val unrealizedPositionComponent: UnrealizedComponent<out PositionComponent>,
                             val unrealizedScaleComponent: UnrealizedComponent<out ScaleComponent>) {
    fun realize(location: Location) {
        val emitterData = EmitterData(world = location.world)
        val emitter = Emitter(unrealizedRateComponent.realizeComponent(emitterData),
            unrealizedParticleLifetimeComponent.realizeComponent(emitterData),
            unrealizedShapeComponent.realizeComponent(emitterData),
            unrealizedSpriteComponent.realizeComponent(emitterData),
            unrealizedColorComponent.realizeComponent(emitterData),
            unrealizedEmitterLifetimeComponent.realizeComponent(emitterData),
            unrealizedPositionComponent.realizeComponent(emitterData),
            unrealizedScaleComponent.realizeComponent(emitterData),
            location, emitterData,null)
        emitter.task = Bukkit.getScheduler().runTaskTimerAsynchronously(ParticleEmitter.INSTANCE, Runnable {
            emitter.tick()
        }, 0, 1)
    }
}