package com.ixume.particlesTesting.emitter.lifetime

import com.google.gson.JsonElement
import com.ixume.particlesTesting.emitter.EmitterMochaData
import com.ixume.particlesTesting.emitter.EmitterMochaFunction
import com.ixume.particlesTesting.parsing.expression
import com.ixume.particlesTesting.particle.ParticleMochaData
import com.ixume.particlesTesting.particle.ParticleMochaFunction
import javassist.LoaderClassPath
import team.unnamed.mocha.MochaEngine

class LifetimeExpressionComponent(private val lifetimeExpression: ParticleMochaFunction) : LifetimeComponent {
    companion object {
        fun parse(jsonElement: JsonElement): LifetimeExpressionComponent? {
            val mochaEngine = MochaEngine.createStandard()
            mochaEngine.classPool().appendClassPath(LoaderClassPath(EmitterMochaFunction::class.java.classLoader))
            val jsonObject = jsonElement.asJsonObject
            return LifetimeExpressionComponent(mochaEngine.compile(jsonObject.expression("expiration_expression") ?: (jsonObject.expression("max_lifetime") ?: return null).fromMaxLife(), ParticleMochaFunction::class.java))
        }

        private fun String.fromMaxLife(): String {
            return "particle_age - $this"
        }
    }

    override fun live(emitterData: EmitterMochaData, particleData: ParticleMochaData): Boolean {
        val evaluatedLifetime = lifetimeExpression.eval(emitterData.age, particleData.age)
        return evaluatedLifetime <= 0.0
    }
}