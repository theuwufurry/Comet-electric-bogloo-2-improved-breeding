package gg.aquatic.comet.particle.action

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.emitter.action.SubAction
import gg.aquatic.comet.api.parsing.ComponentParser
import gg.aquatic.comet.api.parsing.NotMyType
import gg.aquatic.comet.api.parsing.macro.Macro

class ParticleDieSubAction : SubAction {
    override fun execute(context: ActionContext) {
        val otherParticleData = context.otherParticleData ?: throw NullPointerException("Emitter")
        otherParticleData.dead = true
    }

    companion object : ComponentParser<ParticleDieSubAction> {
        override val id: String = "particle_die"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<ParticleDieSubAction> {
            if (jsonElement.isJsonPrimitive && jsonElement.asString == "particle_die") {
                return Result.success(ParticleDieSubAction())
            }

            return Result.failure(NotMyType())
        }
    }
}