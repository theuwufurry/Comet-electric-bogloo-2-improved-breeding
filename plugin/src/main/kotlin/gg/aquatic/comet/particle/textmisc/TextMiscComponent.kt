package gg.aquatic.comet.particle.textmisc

import com.google.gson.JsonElement
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.asBooleanOrNull
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData

class TextMiscComponent(
    val seeThrough: Boolean,
    val shadow: Boolean,
) : ParticleComponent {
    override val priority: Int = 0

    override fun execute(
        otherEmitterData: EmitterData,
        otherParticleData: ParticleData,
    ) {
        otherParticleData.seeThrough = seeThrough
        otherParticleData.shadow = shadow
    }

    override fun die(
        otherEmitterData: EmitterData,
        otherParticleData: ParticleData,
    ) {
    }

    companion object : BaseComponentParser {
        override val id: String = "text_misc"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?,
        ): Result<Component> {
            val jsonObject = jsonElement.asJsonObject

            val seeThrough = jsonObject["see_through"]?.asBooleanOrNull() ?: false
            val shadow = jsonObject["shadow"]?.asBooleanOrNull() ?: false

            return Result.success(TextMiscComponent(seeThrough, shadow))
        }
    }
}