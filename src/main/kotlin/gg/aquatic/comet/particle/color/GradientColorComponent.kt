package gg.aquatic.comet.particle.color

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import java.awt.Color
import javax.script.CompiledScript

class GradientColorComponent(
    private val interpolantScript: CompiledScript,
    private val gradient: List<Pair<Double, CompiledScript>>,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ColorComponent {
    companion object : ComponentParser<GradientColorComponent> {
        init {
            ParticleJsonParser.colorComponentParsers += "gradient_color" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): GradientColorComponent? {
            val jsonObject = jsonElement.asJsonObject
            val gradient: MutableList<Pair<Double, CompiledScript>> = mutableListOf()
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)
            for (element in jsonObject.getAsJsonArray("data")) {
                gradient += (element as JsonObject).getAsJsonPrimitive("index").asNumber.toDouble() to engine.compile(
                    element.expression("color")?.addDependency() ?: return null,
                    macros
                )
            }

            return GradientColorComponent(
                engine.compile(jsonObject.expression("interpolant") ?: return null, macros),
                gradient,
                emitterData,
                particleData
            )
        }
    }

    override fun color(otherEmitterData: EmitterData, otherParticleData: ParticleData): Int {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        val interpolantResult = (interpolantScript.eval() as Number).toDouble()
        if (interpolantResult <= gradient.first().first) return (gradient.first().second.eval() as Color).rgb
        if (interpolantResult >= gradient.last().first) return (gradient.last().second.eval() as Color).rgb

        var (prevIndex, prevScript: CompiledScript) = gradient[0]
        for ((index, script) in gradient) {
            if (index > interpolantResult) {
                val interpolationFactor = (interpolantResult - prevIndex) / (index - prevIndex)
                val prevColor = prevScript.eval() as Color
                val endColor = script.eval() as Color
                val interpolatedAlpha = (((endColor.alpha * interpolationFactor + prevColor.alpha * (1.0 - interpolationFactor)).toInt()) and 0xFF) shl 24
                return (
                        interpolatedAlpha +
                                ((((endColor.red * interpolationFactor + prevColor.red * (1.0 - interpolationFactor)).toInt()) and 0xFF) shl 16) +
                                ((((endColor.green * interpolationFactor + prevColor.green * (1.0 - interpolationFactor)).toInt()) and 0xFF) shl 8) +
                                (((endColor.blue * interpolationFactor + prevColor.blue * (1.0 - interpolationFactor)).toInt()) and 0xFF))
            }

            prevIndex = index
            prevScript = script
        }

        return (gradient.last().second.eval() as Color).rgb
    }
}