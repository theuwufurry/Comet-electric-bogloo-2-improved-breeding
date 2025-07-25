package gg.aquatic.comet.particle.position.direction

import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.particle.ParticleData
import org.joml.Quaterniond
import org.joml.Vector3d
import javax.script.CompiledScript
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RandomDirectionSubcomponent(
    private val magnitudeScript: CompiledScript? = null,
    private val directionScript: Pair<DirectionSubcomponent, CompiledScript?>? = null,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : DirectionSubcomponent {
    override fun dir(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        if (myParticleData.age == 0.0) {
            val dir: Vector3d? = directionScript?.first?.dir(otherEmitterData, otherParticleData)
            val spread: Double? = directionScript?.second?.eval() as? Double
            return randomVector(otherEmitterData.emitter!!, magnitudeScript?.let { it.eval() as Number }?.toDouble() ?: 0.0, dir, spread)
        }

        return myParticleData.velocity
    }

    private fun randomVector(emitter: AbstractEmitter, magnitude: Double): Vector3d {
        return Vector3d(emitter.random.kotlinRandom.nextDouble() * 2.0 - 1.0, emitter.random.kotlinRandom.nextDouble() * 2.0 - 1.0, emitter.random.kotlinRandom.nextDouble() * 2.0 - 1.0).normalize(
            magnitude
        )
    }

    private fun randomVector(emitter: AbstractEmitter, magnitude: Double, dir: Vector3d?, spread: Double?): Vector3d {
        if (dir == null || spread == null) return randomVector(emitter, magnitude)
        val randomAngle = emitter.random.kotlinRandom.nextDouble() * Math.PI * 2.0
        val randomRadius = sqrt(emitter.random.kotlinRandom.nextDouble()) * spread
        val point = Vector3d(cos(randomAngle) * randomRadius, 1.0, sin(randomAngle) * randomRadius)
        return point.rotate(Quaterniond().rotateTo(Vector3d(0.0, 1.0, 0.0), dir)).normalize(magnitude)
    }
}