package gg.aquatic.comet.emitter.lifetime

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr

interface EmitterLifetimeComponent {
    companion object {
        fun default(): Component {
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            engine.compile("emitter.age - 100.0", null)

            return TimedEmitterLifetimeComponent(
                "emitter.age - 100.0".constructExpr<Number>(engine, null).getOrThrow(), null, emitterData
            )
        }
    }
}