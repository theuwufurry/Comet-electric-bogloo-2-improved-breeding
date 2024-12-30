package gg.aquatic.comet.particle.position

import gg.aquatic.comet.emitter.ComponentResult
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.parsing.particleEngine
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3d

interface PositionComponent {
    fun pos(otherEmitterData: EmitterData, otherParticleData: ParticleData): ComponentResult<Vector3d>

    companion object {
        fun default(): PositionComponent {
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ExpressionPositionComponent(
                engine.compile("0", null),
                engine.compile("0", null),
                engine.compile("0", null),
                emitterData, particleData
            )
        }
    }
}