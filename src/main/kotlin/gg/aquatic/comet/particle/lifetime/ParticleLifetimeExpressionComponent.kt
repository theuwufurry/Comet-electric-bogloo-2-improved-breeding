package gg.aquatic.comet.particle.lifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleComponent
import gg.aquatic.comet.particle.ParticleData
import javax.script.CompiledScript

class ParticleLifetimeExpressionComponent(
    private val lifetimeExpression: CompiledScript?,
    private val maxLife: CompiledScript?,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : ParticleComponent, ParticleLifetimeComponent {
    override fun execute(
        otherEmitterData: EmitterData,
        otherParticleData: ParticleData,
    ) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        otherParticleData.dead = !(lifetimeExpression?.run {
            (eval() as Number).toDouble() <= 0.0
        } ?: maxLife?.run {
            val evaluated = (eval() as Number).toInt()
            otherParticleData.maxLife = evaluated
            otherParticleData.age <= evaluated
        } ?: false)
    }

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "particle_lifetime_expression" to this
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
}