package gg.aquatic.comet.particle.action.event

import com.google.gson.JsonElement
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.PostInit
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.emitter.action.Action

class ParticleTickComponent(
    private val action: Action
) : ParticleComponent, PostInit {
    override val priority = 0
    override fun realize(unrealizedEmitter: AbstractUnrealizedEmitter) {
        action.subActions.filterIsInstance<PostInit>().forEach { it.realize(unrealizedEmitter) }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        action.execute(
            ActionContext(
                otherEmitterData, otherParticleData,
                otherParticleData.particle!!.pose
            )
        )
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {
        override val id: String = "on_particle_tick"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component {
            return ParticleTickComponent(Action.parse(jsonElement.asJsonArray, macros))
        }
    }
}