package gg.aquatic.comet.particle.position.direction

import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.particle.ParticleData
import org.joml.Vector3d
import javax.script.CompiledScript

class ExpressionDirectionSubcomponent(
    private val xOffset: CompiledScript?,
    private val yOffset: CompiledScript?,
    private val zOffset: CompiledScript?,
    private val magnitude: CompiledScript?,
    val myParticleData: ParticleData,
    val myEmitterData: EmitterData
) : DirectionSubcomponent {
    override fun dir(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val dir = Vector3d(
            (xOffset?.eval() as? Number)?.toDouble() ?: 0.0,
            (yOffset?.eval() as? Number)?.toDouble() ?: 0.0,
            (zOffset?.eval() as? Number)?.toDouble() ?: 0.0,
        )

        magnitude?.let {
            dir.normalize((it.eval() as Number).toDouble())
        }

        return dir
    }
}