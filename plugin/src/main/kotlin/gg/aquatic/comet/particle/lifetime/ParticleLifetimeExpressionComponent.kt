package gg.aquatic.comet.particle.lifetime

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint

class ParticleLifetimeExpressionComponent(
    private val lifetimeExpression: Expr<Number>?,
    private val maxLife: Expr<Number>?,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : ParticleComponent, ParticleLifetimeComponent {
    override val priority = 0
    override fun execute(
        otherEmitterData: EmitterData,
        otherParticleData: ParticleData,
    ) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        val dead = !(lifetimeExpression?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble()?.let {
            it <= 0.0
        } ?: maxLife?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toInt()?.let {
            otherParticleData.maxLife = it
            otherParticleData.age <= it
        } ?: false)

        if (dead) otherParticleData.dead = true
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {

        override val id: String = "particle_lifetime_expression"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): Result<ParticleLifetimeExpressionComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return Result.success(
                ParticleLifetimeExpressionComponent(
                    jsonObject.getExprOrNull("expiration_expression")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExprOrNull("max_lifetime")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    particleData, emitterData
                )
            )
        }
    }
}