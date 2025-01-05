package gg.aquatic.comet.particle.position

import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.particleEngine

interface PositionComponent {
    companion object {
        fun default(): Component {
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