package gg.aquatic.comet.particle.optimization

import com.google.gson.JsonElement
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.asBooleanOrNull
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData

class SensitiveCenteringComponent(
    val isSensitive: Boolean,
) : ParticleComponent {
    override val priority: Int = 0

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        otherParticleData.sensitiveCentering = isSensitive
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {
        override val id: String = "sensitive_centering"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?,
        ): Result<Component> {
            val jsonObject = jsonElement.asJsonObject
            return Result.success(SensitiveCenteringComponent(jsonObject["sensitive"].asBooleanOrNull() ?: true))
        }
    }
}