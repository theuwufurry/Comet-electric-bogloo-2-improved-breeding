package gg.aquatic.comet.particle.transformation.rotation

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.particleEngine


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