package gg.aquatic.comet.particle.display.text

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.TextData
import gg.aquatic.comet.api.particle.display.sprite.SpriteComponent
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.particle.color.addDependency
import java.awt.Color
import javax.script.CompiledScript

class ConstantTextComponent(
    private val text: CompiledScript,
    private val bg: CompiledScript?,
    private val lineWidth: CompiledScript?,
    private val myEmitterData: EmitterData,
) : ParticleComponent, SpriteComponent {
    override val priority: Int = 0
    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        val str = text.eval() as String
        val c = (bg?.eval() as? Color)?.rgb ?: 0
        val lineWidth = (lineWidth?.eval() as? Int) ?: Int.MAX_VALUE
        otherParticleData.displayData = if (otherParticleData.age == 0.0) {
            TextData(str, c, lineWidth)
        } else {
            otherParticleData.displayData
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {
        override val id: String = "constant_text"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ConstantTextComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            return ConstantTextComponent(
                engine.compile(jsonObject.expression("text") ?: return null, macros) ?: return null,
                jsonObject.expression("bg")?.addDependency()?.let { engine.compile(it, macros) },
                jsonObject.expression("line_width")?.let { engine.compile(it, macros) },
                emitterData
            )
        }
    }
}
