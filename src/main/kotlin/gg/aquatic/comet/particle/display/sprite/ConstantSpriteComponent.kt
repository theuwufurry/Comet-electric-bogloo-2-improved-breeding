package gg.aquatic.comet.particle.display.sprite

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import gg.aquatic.comet.particle.display.DisplayData
import gg.aquatic.comet.particle.display.TextDisplayComponent
import javax.script.CompiledScript

class ConstantSpriteComponent(private val sprite: CompiledScript, private val myEmitterData: EmitterData) :
    SpriteComponent {
    companion object : ComponentParser<ConstantSpriteComponent> {
        init {
            ParticleJsonParser.displayComponentParsers += "constant_sprite" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ConstantSpriteComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            return ConstantSpriteComponent(
                engine.compile(jsonObject.expression("sprite") ?: return null, macros, true),
                emitterData
            )
        }
    }

    override fun display(otherEmitterData: EmitterData, otherParticleData: ParticleData): DisplayData {
        myEmitterData.copyFrom(otherEmitterData)
        return if (otherParticleData.age == 0.0) {
            val str = sprite.eval() as String
            if (str.first() == '\"' && str.last() == '\"') {
                return TextDisplayComponent(str.substring(1, str.length - 1))
            } else {
                return SpriteData(str)
            }
        } else {
            otherParticleData.displayData
        }
    }
}