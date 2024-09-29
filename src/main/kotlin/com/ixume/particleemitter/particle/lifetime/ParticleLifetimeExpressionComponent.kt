package com.ixume.particleemitter.particle.lifetime

import com.google.gson.JsonElement
import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.ComponentParser
import com.ixume.particleemitter.parsing.ParticleJsonParser
import com.ixume.particleemitter.parsing.expression
import com.ixume.particleemitter.particle.ParticleData
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext

class ParticleLifetimeExpressionComponent(private val lifetimeExpression: CompiledScript?, private val maxLife: Int?, private val myParticleData: ParticleData, private val myEmitterData: EmitterData) : ParticleLifetimeComponent {
    companion object : ComponentParser<ParticleLifetimeComponent> {
        init {
            ParticleJsonParser.particleLifetimeComponentParsers += "particle_lifetime_expression" to this
        }

        override fun parse(jsonElement: JsonElement): ParticleLifetimeExpressionComponent {
            val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
            val particleData = ParticleData()
            val emitterData = EmitterData()
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("particle" to particleData)
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
            val jsonObject = jsonElement.asJsonObject
            return ParticleLifetimeExpressionComponent(
                jsonObject.expression("expiration_expression")?.let { (engine as Compilable).compile(it) },
                jsonObject.expression("max_lifetime")?.let { (engine as Compilable).compile(it).eval() as Int },
                particleData,
                emitterData
            )
        }
    }

    override fun keepAlive(
        otherEmitterData: EmitterData,
        otherParticleData: ParticleData,
    ): Boolean {
        myParticleData.copyFrom(otherParticleData)
        myEmitterData.copyFrom(otherEmitterData)
        return lifetimeExpression?.run {
            (eval() as Double) <= 0.0
        } ?: maxLife?.let { otherParticleData.age <= it } ?: false
    }
}