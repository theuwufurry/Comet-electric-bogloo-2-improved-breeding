package com.ixume.particleemitter.emitter

import com.ixume.particleemitter.emitter.lifetime.LifetimeComponent
import com.ixume.particleemitter.emitter.rate.RateComponent
import com.ixume.particleemitter.emitter.shape.ShapeComponent
import org.bukkit.Location

data class UnrealizedEmitter(val unrealizedRateComponent: RateComponent, val unrealizedLifetimeComponent: LifetimeComponent, val unrealizedShapeComponent: ShapeComponent) {
    fun realize(location: Location): Emitter {
        return Emitter(unrealizedRateComponent, unrealizedLifetimeComponent, unrealizedShapeComponent, location)
    }
}