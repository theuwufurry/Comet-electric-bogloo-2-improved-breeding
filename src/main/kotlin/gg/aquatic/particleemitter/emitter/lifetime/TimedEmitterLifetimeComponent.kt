package gg.aquatic.particleemitter.emitter.lifetime

import com.google.gson.JsonElement
import gg.aquatic.particleemitter.ParticleEmitter
import gg.aquatic.particleemitter.emitter.EmitterData
import gg.aquatic.particleemitter.parsing.ComponentParser
import gg.aquatic.particleemitter.parsing.ParticleJsonParser
import gg.aquatic.particleemitter.parsing.expression
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext

class TimedEmitterLifetimeComponent(private val lifetimeScript: CompiledScript, private val myEmitterData: EmitterData) : EmitterLifetimeComponent {
    companion object : ComponentParser<TimedEmitterLifetimeComponent> {
        init {
            ParticleJsonParser.emitterLifetimeComponentParsers += "timed_emitter_lifetime" to this
        }

        override fun parse(jsonElement: JsonElement): TimedEmitterLifetimeComponent? {
            val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
            val emitterData = EmitterData(0.0)
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
            val jsonObject = jsonElement.asJsonObject
            return TimedEmitterLifetimeComponent((engine as Compilable).compile(jsonObject.expression("expiration_expression") ?: return null), emitterData)
        }
    }

    override fun keepAlive(otherEmitterData: EmitterData): Boolean {
        myEmitterData.copyFrom(otherEmitterData)
        return (lifetimeScript.eval() as Double) <= 0
    }
}