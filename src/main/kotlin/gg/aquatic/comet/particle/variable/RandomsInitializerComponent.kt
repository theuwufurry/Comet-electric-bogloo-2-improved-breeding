package gg.aquatic.comet.particle.variable

import com.google.gson.JsonElement
import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterComponent
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.BaseComponentParser
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleComponent
import gg.aquatic.comet.particle.ParticleData

/*
"randoms": {
    "emitter": 5
    "particle" 3
}
 */
class RandomsInitializerComponent(private val emitterRandoms: Int, private val particleRandoms: Int): EmitterComponent, ParticleComponent {
    override fun init(otherEmitterData: EmitterData) {
        for (i in 1..emitterRandoms) {
            if (i == 1) otherEmitterData.variable["random"] = Math.random()
            else otherEmitterData.variable["random$i"] = Math.random()
        }
    }

    override fun execute(otherEmitterData: EmitterData) {
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        if (otherParticleData.age == 0.0) {
            for (i in 1..particleRandoms) {
                if (i == 1) otherParticleData.variable["random"] = Math.random()
                else otherParticleData.variable["random$i"] = Math.random()
            }
        }
    }

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "randoms" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component? {
            val jsonObject = jsonElement.asJsonObject
            return RandomsInitializerComponent(
                if (jsonObject.has("emitter")) jsonObject.getAsJsonPrimitive("emitter").asInt else 0,
                if (jsonObject.has("particle")) jsonObject.getAsJsonPrimitive("particle").asInt else 0
            )
        }
    }
}