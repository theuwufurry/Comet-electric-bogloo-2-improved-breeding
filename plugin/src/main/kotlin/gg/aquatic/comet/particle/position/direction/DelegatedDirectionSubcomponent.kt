package gg.aquatic.comet.particle.position.direction

import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.particle.ParticleData
import org.joml.Vector3d
import kotlin.reflect.KClass

class DelegatedDirectionSubcomponent(
    val component: KClass<out DirectionSubcomponent>
) : DirectionSubcomponent {
    override fun dir(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d {
        return Vector3d()
    }
}