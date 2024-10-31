package com.ixume.particleemitter.particle.lifetime

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import javax.script.CompiledScript

class ParticleLifetimeExpressionComponent(
    private val lifetimeExpression: CompiledScript?,
    private val maxLife: CompiledScript?,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : ParticleLifetimeComponent {
    companion object : ComponentParser<ParticleLifetimeExpressionComponent> {
        init {
            ParticleJsonParser.particleLifetimeComponentParsers += "particle_lifetime_expression" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ParticleLifetimeExpressionComponent {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ParticleLifetimeExpressionComponent(
                jsonObject.expression("expiration_expression")?.let { engine.compile(it, macros) },
                jsonObject.expression("max_lifetime")?.let { engine.compile(it, macros) },
                particleData, emitterData
            )
        }
    }

    override fun keepAlive(
        otherEmitterData: EmitterData,
        otherParticleData: ParticleData,
    ): Boolean {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        return lifetimeExpression?.run {
            (eval() as Double) <= 0.0
        } ?: maxLife?.run {
            val evaluated = (eval() as Number).toInt()
            otherParticleData.maxLife = evaluated
            otherParticleData.age <= evaluated
        } ?: false
    }
}