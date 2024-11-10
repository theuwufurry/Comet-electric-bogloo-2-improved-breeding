package gg.aquatic.particleemitter.particle.color

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import gg.aquatic.particleemitter.ParticleEmitter
import gg.aquatic.particleemitter.emitter.EmitterData
import gg.aquatic.particleemitter.parsing.ComponentParser
import gg.aquatic.particleemitter.parsing.ParticleJsonParser
import gg.aquatic.particleemitter.parsing.expression
import gg.aquatic.particleemitter.particle.ParticleData
import java.awt.Color
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext

class GradientColorComponent(private val interpolantScript: CompiledScript, private val gradient: List<Pair<Double, CompiledScript>>, private val myEmitterData: EmitterData, private val myParticleData: ParticleData) : ColorComponent {
    companion object : ComponentParser<ColorComponent> {
        init {
            ParticleJsonParser.colorComponentParsers += "gradient_color" to this
        }

        override fun parse(jsonElement: JsonElement): GradientColorComponent {
            val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
            val emitterData = EmitterData()
            val particleData = ParticleData()
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("particle" to particleData)
            val jsonObject = jsonElement.asJsonObject
            val gradient: MutableList<Pair<Double, CompiledScript>> = mutableListOf()
            for (element in jsonObject.getAsJsonArray("data")) {
                gradient += (element as JsonObject).getAsJsonPrimitive("index").asNumber.toDouble() to (engine as Compilable).compile(element.expression("color")?.addDependency())
            }

            return GradientColorComponent(
                (engine as Compilable).compile(jsonObject.expression("interpolant")),
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