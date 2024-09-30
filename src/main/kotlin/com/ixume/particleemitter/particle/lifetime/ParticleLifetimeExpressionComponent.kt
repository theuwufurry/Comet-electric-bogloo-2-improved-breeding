package com.ixume.particleemitter.particle.lifetime

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.particle.ParticleData
import javax.script.CompiledScript

class ParticleLifetimeExpressionComponent(private val lifetimeExpression: CompiledScript?, private val maxLife: Int?, private val myParticleData: ParticleData, private val myEmitterData: EmitterData) : ParticleLifetimeComponent {
    companion object : ComponentParser<ParticleLifetimeComponent> {
        init {
            ParticleJsonParser.particleLifetimeComponentParsers += "particle_lifetime_expression" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, String>?): ParticleLifetimeExpressionComponent? {
            val (engine, emitterData, particleData) = particleEngine()
            val jsonObject = jsonElement.asJsonObject
            return ParticleLifetimeExpressionComponent(
                jsonObject.expression("expiration_expression")?.let { engine.compile(it, macros) },
                jsonObject.expression("max_lifetime")?.let { engine.compile(it, macros).eval() as Int },
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