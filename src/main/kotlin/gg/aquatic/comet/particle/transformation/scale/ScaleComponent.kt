package gg.aquatic.comet.particle.transformation.scale

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.parsing.particleEngine
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3f

interface ScaleComponent {
    fun scale(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3f

    companion object {
        fun default(): ScaleComponent {
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ExpressionScaleComponent(
                engine.compile("1", null),
                engine.compile("1", null),
                engine.compile("1", null),
                particleData, emitterData
            )
        }
    }
}