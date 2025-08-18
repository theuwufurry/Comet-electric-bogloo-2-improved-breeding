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

class ParticleDeathComponent(
    private val action: Action
) : ParticleComponent, PostInit {
    override val priority = 0
    override fun realize(unrealizedEmitter: AbstractUnrealizedEmitter) {
        action.subActions.filterIsInstance<PostInit>().forEach { it.realize(unrealizedEmitter) }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        action.execute(
            ActionContext(
                otherEmitterData, otherParticleData,
                otherParticleData.particle!!.pose
            )
        )
    }

    companion object : BaseComponentParser {
        override val id: String = "on_particle_death"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<Component> {
            return Result.success(
                ParticleDeathComponent(
                    Action.parse(jsonElement.asJsonArray, macros).fold({ it }, { return Result.failure(it) })
                )
            )
        }
    }
}