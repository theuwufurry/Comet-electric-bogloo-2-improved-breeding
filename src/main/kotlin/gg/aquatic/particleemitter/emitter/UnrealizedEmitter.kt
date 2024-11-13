package gg.aquatic.particleemitter.emitter

import gg.aquatic.aquaticseries.lib.audience.AquaticAudience
import gg.aquatic.particleemitter.ParticleEmitter
import gg.aquatic.particleemitter.emitter.lifetime.EmitterLifetimeComponent
import gg.aquatic.particleemitter.emitter.rate.RateComponent
import gg.aquatic.particleemitter.emitter.shape.ShapeComponent
import gg.aquatic.particleemitter.particle.color.ColorComponent
import gg.aquatic.particleemitter.particle.lifetime.ParticleLifetimeComponent
import gg.aquatic.particleemitter.particle.position.PositionComponent
import gg.aquatic.particleemitter.particle.texture.SpriteComponent
import gg.aquatic.particleemitter.particle.transformation.scale.ScaleComponent
import org.bukkit.Bukkit
import org.bukkit.Location

data class UnrealizedEmitter(
    val unrealizedRateComponent: RateComponent,
    val unrealizedParticleLifetimeComponent: ParticleLifetimeComponent,
    val unrealizedSpriteComponent: SpriteComponent,
    val unrealizedShapeComponent: ShapeComponent,
    val unrealizedColorComponent: ColorComponent,
    val unrealizedEmitterLifetimeComponent: EmitterLifetimeComponent,
    val unrealizedPositionComponent: PositionComponent,
    val unrealizedScaleComponent: ScaleComponent
) {
    fun realize(location: Location, audience: AquaticAudience) {
        val emitter = Emitter(
            unrealizedRateComponent,
            unrealizedParticleLifetimeComponent,
            unrealizedShapeComponent,
            unrealizedSpriteComponent,
            unrealizedColorComponent,
            unrealizedEmitterLifetimeComponent,
            unrealizedPositionComponent,
            unrealizedScaleComponent,
            location,
            null,
            audience
        )
        emitter.task = Bukkit.getScheduler().runTaskTimerAsynchronously(ParticleEmitter.INSTANCE, Runnable {
            emitter.tick()
        }, 0, 1)
    }
}