package com.ixume.particleemitter.particle.position.direction

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3d
import javax.script.CompiledScript

class RandomDirectionSubcomponent(private val magnitudeScript: CompiledScript?, private val myEmitterData: EmitterData, private val myParticleData: ParticleData) : DirectionSubcomponent {
    override fun dir(): Vector3d {
        if (myParticleData.age == 0.0) {
            return randomVector(magnitudeScript?.let { it.eval() as Double } ?: 1.0)
        }

        return myParticleData.velocity
    }

    private fun randomVector(magnitude: Double): Vector3d {
        return Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0).normalize(magnitude)
    }
}