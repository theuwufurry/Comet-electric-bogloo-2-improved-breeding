package gg.aquatic.comet.particle.lifetime

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr

interface ParticleLifetimeComponent {
    companion object {
        fun default(): Component {
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ParticleLifetimeExpressionComponent(
                null,
                "20".constructExpr<Number>(engine, null).getOrThrow(),
                particleData, emitterData
            )
        }
    }
}