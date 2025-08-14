package gg.aquatic.comet.particle.color

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.InvalidJsonException
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint
import java.awt.Color
import kotlin.math.roundToInt

class GradientColorComponent(
    private val interpolantScript: Expr<Number>,
    private val gradient: List<Pair<Double, Expr<Color>>>,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, ColorComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "gradient_color"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<GradientColorComponent> {
            val jsonObject = jsonElement.asJsonObject
            val gradient: MutableList<Pair<Double, Expr<Color>>> = mutableListOf()
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)
            if (jsonObject["data"].isJsonArray) {
                for (element in jsonObject.getAsJsonArray("data")) {
                    gradient += (element as JsonObject).getAsJsonPrimitive("index").asNumber.toDouble() to (
                            element.getExpr("color")
                                .fold({ it }, { return Result.failure(it) })
                                .addDependency()
                                .constructExpr<Color>(engine, macros)
                                .fold({ it }, { return Result.failure(it) })
                            )
                }
            } else if (jsonObject["data"].isJsonObject) {
                for ((index, colorStr) in jsonObject.getAsJsonObject("data").entrySet()) {
                    gradient += (index.toDouble()) to (
                            colorStr.asString
                                .addDependency()
                                .constructExpr<Color>(engine, macros)
                                .fold({ it }, { return Result.failure(it) })
                            )
                }
            } else {
                return Result.failure(InvalidJsonException("Missing data!"))
            }

            return Result.success(GradientColorComponent(
                jsonObject.getExpr("interpolant")
                    .fold({ it }, { return Result.failure(it) })
                    .constructExpr<Number>(engine, macros)
                    .fold({ it }, { return Result.failure(it) }),
                gradient,
                emitterData,
                particleData
            ))
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        val interpolantResult = interpolantScript.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: return
        if (interpolantResult <= gradient.first().first) {
            otherParticleData.color = gradient.first().second.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.rgb ?: return
            return
        }

        if (interpolantResult >= gradient.last().first) {
            otherParticleData.color = gradient.last().second.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.rgb ?: return
        }

        var (prevIndex, prevScript: Expr<Color>) = gradient[0]
        for ((index, script) in gradient) {
            if (index > interpolantResult) {
                val interpolationFactor = (interpolantResult - prevIndex) / (index - prevIndex)
                val prevColor = prevScript.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return
                val endColor = script.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id) ?: return
                val interpolatedAlpha =
                    (((endColor.alpha * interpolationFactor + prevColor.alpha * (1.0 - interpolationFactor)).toInt()) and 0xFF) shl 24
                otherParticleData.color = (
                        interpolatedAlpha +
                                ((((endColor.red * interpolationFactor + prevColor.red * (1.0 - interpolationFactor)).roundToInt()) and 0xFF) shl 16) +
                                ((((endColor.green * interpolationFactor + prevColor.green * (1.0 - interpolationFactor)).roundToInt()) and 0xFF) shl 8) +
                                (((endColor.blue * interpolationFactor + prevColor.blue * (1.0 - interpolationFactor)).roundToInt()) and 0xFF))
                return
            }

            prevIndex = index
            prevScript = script
        }

        otherParticleData.color = gradient.last().second.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.rgb ?: return
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}