package com.ixume.particleemitter.particle.position.direction

import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Quaterniond
import org.joml.Vector3d
import javax.script.CompiledScript

class RandomDirectionSubcomponent(private val magnitudeScript: CompiledScript?, private val directionScript: Pair<DirectionSubcomponent, CompiledScript>?, private val myEmitterData: EmitterData, private val myParticleData: ParticleData) : DirectionSubcomponent {
    override fun dir(): Vector3d {
        if (myParticleData.age == 0.0) {
            val dir: Vector3d? = directionScript?.first?.dir()
            val spread: Double? = directionScript?.second?.eval() as? Double
            return randomVector(magnitudeScript?.let { it.eval() as Double } ?: 1.0, dir, spread)
        }

        return myParticleData.velocity
    }

    private fun randomVector(magnitude: Double): Vector3d {
        return Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0).normalize(magnitude)
    }

    private fun randomVector(magnitude: Double, dir: Vector3d?, spread: Double?): Vector3d {
        if (dir == null || spread == null) return randomVector(magnitude)
        val point = Vector3d((Math.random() - 0.5) * spread, 1.0, (Math.random() - 0.5) * spread)
        return point.rotate(Quaterniond().rotateTo(Vector3d(0.0, 1.0, 0.0), dir)).normalize(magnitude)
    }
}