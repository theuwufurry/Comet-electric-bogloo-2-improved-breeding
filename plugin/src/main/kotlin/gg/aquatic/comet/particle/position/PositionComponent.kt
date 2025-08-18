package gg.aquatic.comet.particle.position

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.particle.position.initial.InitialExpressionPositionComponent

interface PositionComponent {
    companion object {
        fun default(): Component {
            val emitterData = EmitterData()
            val (_, particleData) = particleEngine(emitterData)

            return InitialExpressionPositionComponent(
                null, null, null,
                emitterData, particleData
            )
        }
    }
}