package com.ixume.particleemitter.emitter.lifetime

import com.google.gson.JsonElement
import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.expression
import com.ixume.particleemitter.particle.ParticleData
import javax.script.Bindings
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.SimpleBindings

class LifetimeExpressionComponent(private val lifetimeExpression: CompiledScript?, private val maxLife: Int?) : LifetimeComponent {
    companion object {
        fun parse(jsonElement: JsonElement): LifetimeExpressionComponent {
            val engine = ParticleEmitter.scriptEngineFactory.scriptEngine as Compilable
            val jsonObject = jsonElement.asJsonObject
            return LifetimeExpressionComponent(
                jsonObject.expression("expiration_expression")?.let { engine.compile(it) },
                jsonObject.expression("max_lifetime")?.let { engine.compile(it).eval() as Int }
            )
        }
    }

    override fun keepAlive(
        emitterData: EmitterData,
        emitterBindings: Bindings,
        particleData: ParticleData,
        particleBindings: Bindings
    ): Boolean {
        val mergedBindings = SimpleBindings()
        mergedBindings.putAll(emitterBindings)
        mergedBindings.putAll(particleBindings)
        return lifetimeExpression?.run {
            (eval(mergedBindings) as Double) <= 0.0
        } ?: maxLife?.let { particleData.age <= it } ?: false
    }
}