package gg.aquatic.comet.particle.lifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.expression
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

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

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