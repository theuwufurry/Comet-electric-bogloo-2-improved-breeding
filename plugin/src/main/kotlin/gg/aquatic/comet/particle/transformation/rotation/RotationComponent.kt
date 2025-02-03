package gg.aquatic.comet.particle.transformation.rotation

import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.particleEngine

interface RotationComponent {
    companion object {
        fun default(): Component {
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ExpressionRotationComponent(
                listOf(),
                particleData, emitterData
            )
        }
    }
}