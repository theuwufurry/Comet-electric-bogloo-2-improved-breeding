package gg.aquatic.comet.particle.display.text

import com.google.gson.JsonElement
import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.InvalidJsonException
import gg.aquatic.comet.api.parsing.asBooleanOrNull
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.sprite.SpriteComponent
import gg.aquatic.comet.api.particle.display.text.ComponentTextData
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.particle.color.addDependency
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint
import java.awt.Color

class MiniMessageTextComponent(
    private val text: Expr<String>,
    private val bg: Expr<Color>?,
    private val lineWidth: Expr<Number>?,
    private val colored: Expr<Boolean>?,
    private val myEmitterData: EmitterData,
) : ParticleComponent, SpriteComponent {
    override val priority: Int = 0
    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        val str = text.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return
        val component = try {
            AbstractParticleEmitter.MINIMESSAGE.deserialize(str)
        } catch (e: Exception) {
            AbstractParticleEmitter.INSTANCE.logger.severe("Issue converting $str to minimessage component!")
            e.printStackTrace()

            return
        }

        val c = bg?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.rgb ?: 0
        val lineWidth = lineWidth?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toInt() ?: Int.MAX_VALUE
        otherParticleData.displayData = if (otherParticleData.age == 0.0) {
            ComponentTextData(component, c, lineWidth, colored?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: true)
        } else {
            otherParticleData.displayData
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {
        override val id: String = "minimessage_text"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<MiniMessageTextComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            return Result.success(MiniMessageTextComponent(
                jsonObject.getExpr("text")
                    .fold({ it }, { return Result.failure(it) })
                    .constructExpr<String>(engine, macros)
                    .fold({ it }, { return Result.failure(it) }),
                jsonObject.getExprOrNull("bg")
                    ?.addDependency()
                    ?.constructExpr<Color>(engine, macros)
                    ?.fold({ it }, { return Result.failure(it) }),
                jsonObject.getExprOrNull("line_width")
                    ?.constructExpr<Number>(engine, macros)
                    ?.fold({ it }, { return Result.failure(it) }),
                jsonObject.getExprOrNull("colored")
                    ?.constructExpr<Boolean>(engine, macros)
                    ?.fold({ it }, { return Result.failure(it) }),
                emitterData
            ))
        }
    }
}