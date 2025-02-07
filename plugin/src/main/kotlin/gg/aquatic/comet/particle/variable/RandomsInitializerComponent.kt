package gg.aquatic.comet.particle.variable

import com.google.gson.JsonElement
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterComponent
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.ParticleJsonParser

class RandomsInitializerComponent(private val emitterRandoms: Int, private val particleRandoms: Int) :
    EmitterComponent,
    ParticleComponent {
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

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    override fun die(otherEmitterData: EmitterData) {}

    companion object : BaseComponentParser {
        override val id: String = "randoms"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component {
            val jsonObject = jsonElement.asJsonObject
            return RandomsInitializerComponent(
                if (jsonObject.has("emitter")) jsonObject.getAsJsonPrimitive("emitter").asInt else 0,
                if (jsonObject.has("particle")) jsonObject.getAsJsonPrimitive("particle").asInt else 0
            )
        }
    }
}