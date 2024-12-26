package gg.aquatic.particleemitter.particle.transformation.scale

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3f

interface ScaleComponent {
    fun scale(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3f
}