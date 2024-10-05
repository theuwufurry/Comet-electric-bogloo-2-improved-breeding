package com.ixume.particleemitter.emitter.lifetime

import com.google.gson.JsonElement
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import javax.script.CompiledScript

class TimedEmitterLifetimeComponent(private val lifetimeScript: CompiledScript) : EmitterLifetimeComponent {
    companion object : ComponentParser<TimedEmitterLifetimeComponent> {
        init {
            ParticleJsonParser.emitterLifetimeComponentParsers += "timed_emitter_lifetime" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): UnrealizedComponent<TimedEmitterLifetimeComponent>? {
            val jsonObject = jsonElement.asJsonObject
            return UnrealizedTimedEmitterComponent(jsonObject.expression("expiration_expression") ?: return null, macros)
        }
    }

    override fun keepAlive(): Boolean {
        return (lifetimeScript.eval() as Double) <= 0
    }
}

class UnrealizedTimedEmitterComponent(private val lifetimeScript: String, private val macros: Map<String, Macro>?) : UnrealizedComponent<TimedEmitterLifetimeComponent> {
    override fun realizeComponent(emitterData: EmitterData): TimedEmitterLifetimeComponent {
        val engine = emitterEngine(emitterData)
        return TimedEmitterLifetimeComponent(engine.compile(lifetimeScript, macros))
    }
}