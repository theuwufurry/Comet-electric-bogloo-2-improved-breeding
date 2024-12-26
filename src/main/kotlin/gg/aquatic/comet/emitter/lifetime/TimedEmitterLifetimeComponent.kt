package gg.aquatic.comet.emitter.lifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import javax.script.CompiledScript

class TimedEmitterLifetimeComponent(
    private val lifetimeScript: CompiledScript,
    private val myEmitterData: EmitterData
) : EmitterLifetimeComponent {
    companion object : ComponentParser<TimedEmitterLifetimeComponent> {
        init {
            ParticleJsonParser.emitterLifetimeComponentParsers += "timed_emitter_lifetime" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): TimedEmitterLifetimeComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)
            return TimedEmitterLifetimeComponent(
                engine.compile(
                    jsonObject.expression("expiration_expression") ?: return null, macros
                ), emitterData
            )
        }
    }


    override fun keepAlive(otherEmitterData: EmitterData): Boolean {
        myEmitterData.copyFrom(otherEmitterData)
        return (lifetimeScript.eval() as Double) <= 0
    }
}