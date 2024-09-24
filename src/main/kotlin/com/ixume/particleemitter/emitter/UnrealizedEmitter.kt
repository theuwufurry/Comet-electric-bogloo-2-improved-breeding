package com.ixume.particlesTesting.emitter

import com.ixume.particlesTesting.emitter.lifetime.LifetimeComponent
import com.ixume.particlesTesting.emitter.rate.RateComponent
import com.ixume.particlesTesting.emitter.shape.ShapeComponent
import org.bukkit.Location

//stores unrealized components
//unrealized components have the strings which can be evaluated into mocha functions
//2 levels of mocha functions for us:
//emitter level, and particle level
//emitter level functions provide the emitter data (emitter_age)
//created on emitter creation
//particle level provide particle data that depends on particle data (particle_age)
//created on particle creation, same time data is created
//for example: color component is realized on particle creation
//for example: particle creation rate is realized on emitter creation
data class UnrealizedEmitter(val unrealizedRateComponent: RateComponent, val unrealizedLifetimeComponent: LifetimeComponent, val unrealizedShapeComponent: ShapeComponent) {
    fun realize(location: Location): Emitter {
        return Emitter(unrealizedRateComponent, unrealizedLifetimeComponent, unrealizedShapeComponent, location)
    }
}