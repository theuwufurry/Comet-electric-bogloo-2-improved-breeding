package com.ixume.particleemitter.particle.lifetime

import com.google.gson.JsonElement
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import javax.script.CompiledScript

class ParticleLifetimeExpressionComponent(private val lifetimeExpression: CompiledScript?, private val maxLife: Int?, private val myParticleData: ParticleData) : ParticleLifetimeComponent {
    companion object : ComponentParser<ParticleLifetimeExpressionComponent> {
        init {
            ParticleJsonParser.particleLifetimeComponentParsers += "particle_lifetime_expression" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): UnrealizedComponent<ParticleLifetimeExpressionComponent> {
            val jsonObject = jsonElement.asJsonObject
            return UnrealizedParticleLifetimeExpressionComponent(
                jsonObject.expression("expiration_expression"),
                jsonObject.expression("max_lifetime"),
                macros
            )
        }
    }

    override fun keepAlive(
        otherParticleData: ParticleData,
    ): Boolean {
        myParticleData.copyFrom(otherParticleData)
        return lifetimeExpression?.run {
            (eval() as Double) <= 0.0
        } ?: maxLife?.let { otherParticleData.age <= it } ?: false
    }
}

class UnrealizedParticleLifetimeExpressionComponent(private val lifetime: String?, private val maxLife: String?, private val macros: Map<String, Macro>?) : UnrealizedComponent<ParticleLifetimeExpressionComponent> {
    override fun realizeComponent(emitterData: EmitterData): ParticleLifetimeExpressionComponent {
        val (engine, particleData) = particleEngine(emitterData)
        return ParticleLifetimeExpressionComponent(
            lifetime?.let { engine.compile(lifetime, macros) },
            maxLife?.let { engine.compile(maxLife, macros) }?.eval() as? Int,
            particleData
        )
    }
}