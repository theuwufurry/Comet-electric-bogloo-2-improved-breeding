package com.ixume.particleemitter.particle.position.direction

import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.compile
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.parsing.particleEngine
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Quaterniond
import org.joml.Vector3d
import javax.script.CompiledScript
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RandomDirectionSubcomponent(private val magnitudeScript: CompiledScript?, private val directionScript: Pair<DirectionSubcomponent, CompiledScript>?, private val myParticleData: ParticleData) : DirectionSubcomponent {
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
        val randomAngle = Math.random() * Math.PI * 2.0
        val randomRadius = sqrt(Math.random()) * spread
        val point = Vector3d(cos(randomAngle) * randomRadius, 1.0, sin(randomAngle) * randomRadius)
        return point.rotate(Quaterniond().rotateTo(Vector3d(0.0, 1.0, 0.0), dir)).normalize(magnitude)
    }
}

class UnrealizedRandomDirectionSubcomponent(private val magnitude: String?, private val direction: Pair<UnrealizedComponent<out DirectionSubcomponent>, String>?, private val macros: Map<String, Macro>?) : UnrealizedComponent<RandomDirectionSubcomponent> {
    override fun realizeComponent(emitterData: EmitterData): RandomDirectionSubcomponent {
        val (engine, particleData) = particleEngine(emitterData)
        return RandomDirectionSubcomponent(magnitude?.let { engine.compile(it, macros) }, direction?.let {
            Pair(it.first.realizeComponent(emitterData), engine.compile(it.second, macros))
        }, particleData)
    }
}