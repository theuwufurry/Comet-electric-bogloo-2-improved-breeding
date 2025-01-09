package gg.aquatic.comet.particle.action

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.emitter.action.SubAction
import gg.aquatic.comet.parsing.ComponentParser
import gg.aquatic.comet.parsing.macro.Macro

class ParticleDieSubAction : SubAction {
    override fun execute(context: ActionContext) {
        if (context.otherParticleData == null) {
            throw NullPointerException("Emitter")
        }

        context.otherParticleData.dead = true
    }

    companion object : ComponentParser<ParticleDieSubAction> {
        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ParticleDieSubAction? {
            if (jsonElement.isJsonPrimitive && jsonElement.asString == "particle_die") {
                return ParticleDieSubAction()
            }

            return null
        }
    }
}