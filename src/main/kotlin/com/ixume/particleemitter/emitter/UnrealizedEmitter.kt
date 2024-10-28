package com.ixume.particleemitter.emitter

import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.lifetime.EmitterLifetimeComponent
import com.ixume.particleemitter.emitter.rate.RateComponent
import com.ixume.particleemitter.emitter.shape.ShapeComponent
import com.ixume.particleemitter.particle.color.ColorComponent
import com.ixume.particleemitter.particle.display.DisplayComponent
import com.ixume.particleemitter.particle.lifetime.ParticleLifetimeComponent
import com.ixume.particleemitter.particle.position.PositionComponent
import com.ixume.particleemitter.particle.transformation.rotation.RotationComponent
import com.ixume.particleemitter.particle.transformation.scale.ScaleComponent
import net.minecraft.world.entity.Display.BillboardConstraints
import org.bukkit.Bukkit
import org.bukkit.Location
import org.joml.Quaterniond
import org.joml.Vector3d

data class UnrealizedEmitter(
    val unrealizedRateComponent: UnrealizedComponent<out RateComponent>,
    val unrealizedParticleLifetimeComponent: UnrealizedComponent<out ParticleLifetimeComponent>,
    val unrealizedSpriteComponent: UnrealizedComponent<out DisplayComponent>,
    val unrealizedShapeComponent: UnrealizedComponent<out ShapeComponent>,
    val unrealizedColorComponent: UnrealizedComponent<out ColorComponent>,
    val unrealizedEmitterLifetimeComponent: UnrealizedComponent<out EmitterLifetimeComponent>,
    val unrealizedPositionComponent: UnrealizedComponent<out PositionComponent>,
    val unrealizedScaleComponent: UnrealizedComponent<out ScaleComponent>,
    val unrealizedRotationComponent: UnrealizedComponent<out RotationComponent>,
    val billboardConstraints: BillboardConstraints,
) {

    private var cachedEmitter: CachedEmitter = cacheEmitter()

    private fun cacheEmitter(): CachedEmitter {
        val emitterData = EmitterData()
        return CachedEmitter(
            emitterData,
            unrealizedRateComponent.realizeComponent(emitterData),
            unrealizedParticleLifetimeComponent.realizeComponent(emitterData),
            unrealizedShapeComponent.realizeComponent(emitterData),
            unrealizedSpriteComponent.realizeComponent(emitterData),
            unrealizedColorComponent.realizeComponent(emitterData),
            unrealizedEmitterLifetimeComponent.realizeComponent(emitterData),
            unrealizedPositionComponent.realizeComponent(emitterData),
            unrealizedScaleComponent.realizeComponent(emitterData),
            unrealizedRotationComponent.realizeComponent(emitterData),
        )
    }

    fun realize(location: Location) {
        cachedEmitter.emitterData.world = location.world
        cachedEmitter.emitterData.rotation =
            Quaterniond().rotateTo(Vector3d(0.0, 0.0, 1.0), location.direction.toVector3d().normalize())
        val emitter = Emitter(
            cachedEmitter.rateComponent,
            cachedEmitter.particleLifetimeComponent,
            cachedEmitter.shapeComponent,
            cachedEmitter.displayComponent,
            cachedEmitter.colorComponent,
            cachedEmitter.emitterLifetimeComponent,
            cachedEmitter.positionComponent,
            cachedEmitter.scaleComponent,
            cachedEmitter.rotationComponent,
            billboardConstraints, location, cachedEmitter.emitterData, null
        )
        emitter.task = Bukkit.getScheduler().runTaskTimerAsynchronously(ParticleEmitter.INSTANCE, Runnable {
            emitter.tick()
        }, 0, 1)

        cachedEmitter = cacheEmitter()
    }
}

class CachedEmitter(
    val emitterData: EmitterData,
    val rateComponent: RateComponent,
    val particleLifetimeComponent: ParticleLifetimeComponent,
    val shapeComponent: ShapeComponent,
    val displayComponent: DisplayComponent,
    val colorComponent: ColorComponent,
    val emitterLifetimeComponent: EmitterLifetimeComponent,
    val positionComponent: PositionComponent,
    val scaleComponent: ScaleComponent,
    val rotationComponent: RotationComponent,
)