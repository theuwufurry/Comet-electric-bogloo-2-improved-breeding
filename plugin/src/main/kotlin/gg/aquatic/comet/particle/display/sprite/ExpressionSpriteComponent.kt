package gg.aquatic.comet.particle.display.sprite

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.sprite.SpriteComponent
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint

class ExpressionSpriteComponent(
    private val sprite: Expr<String>,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : ParticleComponent, SpriteComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "expression_sprite"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<ExpressionSpriteComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return Result.success(
                ExpressionSpriteComponent(
                    jsonObject.getExpr("sprite")
                        .fold({ it }, { return Result.failure(it) })
                        .constructExpr<String>(engine, macros, tryAsSimpleString = true)
                        .fold({ it }, { return Result.failure(it) }),
                    particleData, emitterData
                )
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        otherParticleData.displayData = SpriteData(sprite.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return)
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}