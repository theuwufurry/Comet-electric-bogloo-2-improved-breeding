package gg.aquatic.comet.particle.action.event

import com.google.gson.JsonElement
import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterComponent
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.action.Action
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.emitter.action.event.EmitterInitComponent
import gg.aquatic.comet.parsing.BaseComponentParser
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.PostInit
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleComponent
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3d

class ParticleDeathComponent(
    private val action: Action
) : ParticleComponent, PostInit {
    override fun realize() {
        action.subActions.filterIsInstance<PostInit>().forEach { it.realize() }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        action.execute(
            ActionContext(
                otherEmitterData, otherParticleData,
                Vector3d(otherParticleData.origin).add(otherParticleData.relativePosition),
                Vector3d()
            )
        )
    }

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "on_particle_death" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component {
            return ParticleDeathComponent(Action.parse(jsonElement.asJsonArray, macros))
        }
    }
}