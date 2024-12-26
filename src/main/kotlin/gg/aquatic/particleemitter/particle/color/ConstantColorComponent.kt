package gg.aquatic.particleemitter.particle.color

import com.google.gson.JsonElement
import com.ixume.particleemitter.ParticleEmitter
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.ComponentParser
import com.ixume.particleemitter.parsing.ParticleJsonParser
import com.ixume.particleemitter.parsing.expression
import com.ixume.particleemitter.particle.ParticleData
import java.awt.Color
import javax.script.Compilable
import javax.script.CompiledScript
import javax.script.ScriptContext

class ConstantColorComponent(private val colorScript: CompiledScript, private val myEmitterData: EmitterData) : ColorComponent {
    companion object : ComponentParser<ColorComponent> {
        init {
            ParticleJsonParser.colorComponentParsers += "constant_color" to this
        }

        override fun parse(jsonElement: JsonElement): ConstantColorComponent {
            val engine = ParticleEmitter.scriptEngineFactory.scriptEngine
            val emitterData = EmitterData(0.0)
            engine.getBindings(ScriptContext.ENGINE_SCOPE) += ("emitter" to emitterData)
            val jsonObject = jsonElement.asJsonObject
            return ConstantColorComponent(
                (engine as Compilable).compile(jsonObject.expression("color")?.addDependency()),
                emitterData
            )
        }
    }

    override fun color(otherEmitterData: EmitterData, otherParticleData: ParticleData): Int {
        return if (otherParticleData.age == 0.0) {
            myEmitterData.copyFrom(otherEmitterData)
            (colorScript.eval() as Color).rgb
        } else {
            otherParticleData.color
        }
    }
}