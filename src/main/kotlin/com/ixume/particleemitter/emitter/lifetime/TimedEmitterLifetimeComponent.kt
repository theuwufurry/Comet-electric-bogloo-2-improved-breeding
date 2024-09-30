package com.ixume.particleemitter.emitter.lifetime

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import javax.script.CompiledScript

class TimedEmitterLifetimeComponent(private val lifetimeScript: CompiledScript, private val myEmitterData: EmitterData) : EmitterLifetimeComponent {
    companion object : ComponentParser<TimedEmitterLifetimeComponent> {
        init {
            ParticleJsonParser.emitterLifetimeComponentParsers += "timed_emitter_lifetime" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, String>?): TimedEmitterLifetimeComponent? {
            val (engine, emitterData) = emitterEngine()
            val jsonObject = jsonElement.asJsonObject
            return TimedEmitterLifetimeComponent(engine.compile(jsonObject.expression("expiration_expression") ?: return null, macros), emitterData)
        }
    }

    override fun keepAlive(otherEmitterData: EmitterData): Boolean {
        myEmitterData.copyFrom(otherEmitterData)
        return (lifetimeScript.eval() as Double) <= 0
    }
}