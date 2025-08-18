package gg.aquatic.comet.particle.display.sprite

import com.google.gson.JsonElement
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.sprite.SpriteComponent
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint

class ConstantSpriteComponent(private val sprite: Expr<String>, private val myEmitterData: EmitterData) :
    ParticleComponent, SpriteComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "constant_sprite"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<ConstantSpriteComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            return Result.success(
                ConstantSpriteComponent(
                    jsonObject.getExpr("sprite")
                        .fold({ it }, { return Result.failure(it) })
                        .constructExpr<String>(engine, macros, tryAsSimpleString = true)
                        .fold({ it }, { return Result.failure(it) }),
                    emitterData
                )
            )
        }

        fun default(): Component {
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            return ConstantSpriteComponent(
                "\"particle\"".constructExpr<String>(engine, null).getOrThrow(),
                emitterData
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        otherParticleData.displayData = if (otherParticleData.age == 0.0) {
            val str = sprite.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return
            SpriteData(str)
        } else {
            otherParticleData.displayData
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}