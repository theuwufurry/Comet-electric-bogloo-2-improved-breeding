package com.ixume.particleemitter.emitter.action.sub

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.action.ActionContext
import com.ixume.particleemitter.emitter.action.SubAction
import com.ixume.particleemitter.parsing.ComponentParser
import com.ixume.particleemitter.parsing.macro.Macro

class ParticleDieSubAction : SubAction{
    companion object : ComponentParser<ParticleDieSubAction> {
        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ParticleDieSubAction? {
            if (jsonElement.isJsonPrimitive && jsonElement.asString == "particle_die") {
                return ParticleDieSubAction()
            }

            return null
        }
    }

    override fun execute(context: ActionContext) {
        if (context.otherParticleData == null) {
            throw NullPointerException("Calling Particle Death SubAction on Emitter!")
        }

        context.otherParticleData.dead = true
    }
}