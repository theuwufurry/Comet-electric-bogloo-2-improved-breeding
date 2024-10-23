package com.ixume.particleemitter.particle.color

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import java.awt.Color
import javax.script.CompiledScript

class GradientColorComponent(private val interpolantScript: CompiledScript, private val gradient: List<Pair<Double, CompiledScript>>, private val myParticleData: ParticleData) : ColorComponent {
    companion object : ComponentParser<GradientColorComponent> {
        init {
            ParticleJsonParser.colorComponentParsers += "gradient_color" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): UnrealizedComponent<GradientColorComponent>? {
            val jsonObject = jsonElement.asJsonObject
            val gradient: MutableList<Pair<Double, String>> = mutableListOf()
            for (element in jsonObject.getAsJsonArray("data")) {
                gradient += (element as JsonObject).getAsJsonPrimitive("index").asNumber.toDouble() to (element.expression("color")?.addDependency() ?: return null)
            }

            return UnrealizedGradientColorComponent(
                jsonObject.expression("interpolant") ?: return null,
                gradient,
                macros
            )
        }
    }

    override fun color(otherParticleData: ParticleData): Int {
        myParticleData.copyFrom(otherParticleData)
        val interpolantResult = interpolantScript.eval() as Double

        var (prevIndex, prevScript: CompiledScript) = gradient[0]
        for ((index, script) in gradient) {
            if (index == interpolantResult) {
                return (script.eval() as Color).argb()
            }

            if (index > interpolantResult) {
                val interpolationFactor = (interpolantResult - prevIndex) / (index - prevIndex)
                val prevColor = prevScript.eval() as Color
                val endColor = script.eval() as Color
                return (((endColor.red * interpolationFactor + prevColor.red * (1 - interpolationFactor)).toInt() shl 16) +
                        ((endColor.green * interpolationFactor + prevColor.green * (1 - interpolationFactor)).toInt() shl 8) +
                        (endColor.blue * interpolationFactor + prevColor.blue * (1 - interpolationFactor)).toInt() +
                        ((endColor.alpha * interpolationFactor + prevColor.alpha * (1 - interpolationFactor)).toInt() shl 24))
            }

            prevIndex = index
            prevScript = script
        }

        return (gradient.last().second.eval() as Color).argb()
    }
}

class UnrealizedGradientColorComponent(private val interpolation: String, private val gradient: List<Pair<Double, String>>, private val macros: Map<String, Macro>?) : UnrealizedComponent<GradientColorComponent> {
    override fun realizeComponent(emitterData: EmitterData): GradientColorComponent {
        val (engine, particleData) = particleEngine(emitterData)
        return GradientColorComponent(engine.compile(interpolation, macros),
            gradient.map { Pair(it.first, engine.compile(it.second, macros)) },
            particleData)
    }
}