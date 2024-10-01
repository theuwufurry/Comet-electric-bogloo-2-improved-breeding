package com.ixume.particleemitter.particle.color

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import java.awt.Color
import javax.script.CompiledScript

class GradientColorComponent(private val interpolantScript: CompiledScript, private val gradient: List<Pair<Double, CompiledScript>>, private val myEmitterData: EmitterData, private val myParticleData: ParticleData) : ColorComponent {
    companion object : ComponentParser<ColorComponent> {
        init {
            ParticleJsonParser.colorComponentParsers += "gradient_color" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): GradientColorComponent? {
            val (engine, emitterData, particleData) = particleEngine()
            val jsonObject = jsonElement.asJsonObject
            val gradient: MutableList<Pair<Double, CompiledScript>> = mutableListOf()
            for (element in jsonObject.getAsJsonArray("data")) {
                gradient += (element as JsonObject).getAsJsonPrimitive("index").asNumber.toDouble() to engine.compile(element.expression("color")?.addDependency() ?: return null, macros)
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
        val interpolantResult = interpolantScript.eval() as Double

        var (prevIndex, prevScript: CompiledScript) = gradient[0]
        for ((index, script) in gradient) {
            if (index == interpolantResult) {
                return (script.eval() as Color).rgb
            }

            if (index > interpolantResult) {
                val interpolationFactor = (interpolantResult - prevIndex) / (index - prevIndex)
                val prevColor = prevScript.eval() as Color
                val endColor = script.eval() as Color
                return (((endColor.red * interpolationFactor + prevColor.red * (1 - interpolationFactor)).toInt() shl 16) +
                        ((endColor.green * interpolationFactor + prevColor.green * (1 - interpolationFactor)).toInt() shl 8) +
                        (endColor.blue * interpolationFactor + prevColor.blue * (1 - interpolationFactor)).toInt())
            }

            prevIndex = index
            prevScript = script
        }

        return (gradient.last().second.eval() as Color).rgb
    }
}