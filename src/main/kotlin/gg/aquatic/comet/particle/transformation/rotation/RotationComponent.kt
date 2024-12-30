package gg.aquatic.comet.particle.transformation.rotation

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.parsing.particleEngine
import gg.aquatic.comet.particle.ParticleData
import org.joml.Quaternionf

interface RotationComponent {
    fun rotation(otherEmitterData: EmitterData, otherParticleData: ParticleData): Quaternionf

    companion object {
        fun default(): RotationComponent {
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ExpressionRotationComponent(
                engine.compile("0", null),
                engine.compile("0", null),
                engine.compile("0", null),
                particleData, emitterData
            )
        }
    }
}