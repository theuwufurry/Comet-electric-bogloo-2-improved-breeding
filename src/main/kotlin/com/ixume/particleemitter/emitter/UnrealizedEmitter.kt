package com.ixume.particleemitter.emitter

import com.ixume.particleemitter.emitter.lifetime.EmitterLifetimeComponent
import com.ixume.particleemitter.emitter.rate.RateComponent
import com.ixume.particleemitter.emitter.recursive.RecursiveEmitterComponent
import com.ixume.particleemitter.emitter.shape.ShapeComponent
import com.ixume.particleemitter.particle.color.ColorComponent
import com.ixume.particleemitter.particle.display.DisplayComponent
import com.ixume.particleemitter.particle.lifetime.ParticleLifetimeComponent
import com.ixume.particleemitter.particle.position.PositionComponent
import com.ixume.particleemitter.particle.transformation.rotation.RotationComponent
import com.ixume.particleemitter.particle.transformation.scale.ScaleComponent
import net.minecraft.world.entity.Display.BillboardConstraints
import org.bukkit.Location

data class UnrealizedEmitter(
    val rateComponent: RateComponent,
    val particleLifetimeComponent: ParticleLifetimeComponent,
    val displayComponent: DisplayComponent,
    val shapeComponent: ShapeComponent,
    val colorComponent: ColorComponent,
    val emitterLifetimeComponent: EmitterLifetimeComponent,
    val positionComponent: PositionComponent,
    val scaleComponent: ScaleComponent,
    val rotationComponent: RotationComponent,
    val recursiveEmitterComponent: RecursiveEmitterComponent?,
    val billboardConstraints: BillboardConstraints,
) {
    fun realize(location: Location): Emitter {
        val emitterData = EmitterData()
        emitterData.world = location.world
        return Emitter(
            rateComponent,
            particleLifetimeComponent,
            shapeComponent,
            displayComponent,
            colorComponent,
            emitterLifetimeComponent,
            positionComponent,
            scaleComponent,
            rotationComponent,
            recursiveEmitterComponent,
            billboardConstraints, location, emitterData
        )
    }
}