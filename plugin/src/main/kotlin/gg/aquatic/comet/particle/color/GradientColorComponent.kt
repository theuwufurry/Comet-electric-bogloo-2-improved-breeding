package gg.aquatic.comet.particle.color

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.expression
import java.awt.Color
import javax.script.CompiledScript
import kotlin.math.roundToInt

class GradientColorComponent(
    private val interpolantScript: CompiledScript,
    private val gradient: List<Pair<Double, CompiledScript>>,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, ColorComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "gradient_color"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): GradientColorComponent? {
            val jsonObject = jsonElement.asJsonObject
            val gradient: MutableList<Pair<Double, CompiledScript>> = mutableListOf()
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)
            if (jsonObject["data"].isJsonArray) {
                for (element in jsonObject.getAsJsonArray("data")) {
                    gradient += (element as JsonObject).getAsJsonPrimitive("index").asNumber.toDouble() to (engine.compile(
                        element.expression("color")?.addDependency() ?: return null,
                        macros
                    ) ?: continue)
                }
            } else if (jsonObject["data"].isJsonObject) {
                for ((index, colorStr) in jsonObject.getAsJsonObject("data").entrySet()) {
                    gradient += index.toDouble() to (engine.compile(
                        colorStr.asString.addDependency(),
                        macros
                    ) ?: continue)
                }
            } else {
                return null
            }

            return GradientColorComponent(
                engine.compile(jsonObject.expression("interpolant") ?: return null, macros) ?: return null,
                gradient,
                emitterData,
                particleData
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        val interpolantResult = (interpolantScript.eval() as Number).toDouble()
        if (interpolantResult <= gradient.first().first) {
            otherParticleData.color = (gradient.first().second.eval() as Color).rgb
            return
        }

        if (interpolantResult >= gradient.last().first) {
            otherParticleData.color = (gradient.last().second.eval() as Color).rgb
        }

        var (prevIndex, prevScript: CompiledScript) = gradient[0]
        for ((index, script) in gradient) {
            if (index > interpolantResult) {
                val interpolationFactor = (interpolantResult - prevIndex) / (index - prevIndex)
                val prevColor = prevScript.eval() as Color
                val endColor = script.eval() as Color
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

        otherParticleData.color = (gradient.last().second.eval() as Color).rgb
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}