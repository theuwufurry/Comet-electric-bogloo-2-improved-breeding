package gg.aquatic.comet.particle.display

import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.compile
import gg.aquatic.comet.parsing.emitterEngine
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.particle.ParticleData
import gg.aquatic.comet.particle.display.sprite.ConstantSpriteComponent

interface DisplayComponent {
    fun display(otherEmitterData: EmitterData, otherParticleData: ParticleData): DisplayData

    companion object {
        fun default(): DisplayComponent {
            val emitterData = EmitterData()
            val engine = emitterEngine(emitterData)

            return ConstantSpriteComponent(
                engine.compile("\"particle\"", null),
                emitterData
            )
        }
    }
}

interface DisplayData {
    fun copy(): DisplayData
}

data class TextDisplayComponent(val string: String) : DisplayData {
    override fun copy(): DisplayData {
        return TextDisplayComponent(string)
    }
}