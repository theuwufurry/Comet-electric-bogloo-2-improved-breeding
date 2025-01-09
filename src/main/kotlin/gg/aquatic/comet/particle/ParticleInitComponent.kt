package gg.aquatic.comet.particle

import com.google.gson.JsonElement
import gg.aquatic.comet.Component
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.action.Action
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.parsing.BaseComponentParser
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.PostInit
import gg.aquatic.comet.parsing.macro.Macro
import org.joml.Vector3d

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
                    otherEmitterData.location.toVector().toVector3d(),
                    Vector3d()
                )
            )
        }
    }

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "on_particle_init" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Component {
            return ParticleInitComponent(Action.parse(jsonElement.asJsonArray, macros))
        }
    }
}