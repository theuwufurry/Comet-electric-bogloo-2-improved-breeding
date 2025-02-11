package gg.aquatic.comet.particle.display.sprite

import com.google.gson.JsonElement
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.emitterEngine
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.display.TextDisplayComponent
import gg.aquatic.comet.api.particle.display.sprite.SpriteComponent
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.parsing.expression
import javax.script.CompiledScript

class ConstantSpriteComponent(private val sprite: CompiledScript, private val myEmitterData: EmitterData) :
    ParticleComponent, SpriteComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "constant_sprite"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ConstantSpriteComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            return ConstantSpriteComponent(
                engine.compile(jsonObject.expression("sprite") ?: return null, macros, true),
                emitterData
            )
        }

        fun default(): Component {
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            return ConstantSpriteComponent(
                engine.compile("\"particle\"", null),
                emitterData
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        otherParticleData.displayData = if (otherParticleData.age == 0.0) {
            val str = sprite.eval() as String
            if (str.first() == '\"' && str.last() == '\"') {
                TextDisplayComponent(str.substring(1, str.length - 1))
            } else {
                SpriteData(str)
            }
        } else {
            otherParticleData.displayData
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}