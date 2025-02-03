package gg.aquatic.comet.particle.action.event

import com.google.gson.JsonElement
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.PostInit
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.emitter.action.Action
import gg.aquatic.comet.parsing.ParticleJsonParser

class ParticleInitComponent(
    private val action: Action
) : ParticleComponent, PostInit {
    override fun realize() {
        action.subActions.filterIsInstance<PostInit>().forEach { it.realize() }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        if (otherParticleData.age == 0.0) {
            action.execute(
                ActionContext(
                    otherEmitterData, otherParticleData,
                    otherParticleData.particle!!.pose
                )
            )
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "on_particle_init" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component {
            return ParticleInitComponent(Action.parse(jsonElement.asJsonArray, macros))
        }
    }
}