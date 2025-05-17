package gg.aquatic.comet.particle.light

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.LightData
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.expression
import javax.script.CompiledScript

class ConstantLightComponent(
    private val skylightScript: CompiledScript?,
    private val blocklightScript: CompiledScript?,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData,
) : ParticleComponent {
    override val priority = 0

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        if (skylightScript == null && blocklightScript == null) return
        if (otherParticleData.light != null) return

        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        val skylight = if (skylightScript == null) 0 else {
            (skylightScript.eval() as Number).toInt()
        }

        val blocklight = if (blocklightScript == null) 0 else {
            (blocklightScript.eval() as Number).toInt()
        }

        otherParticleData.light = LightData(skylight, blocklight)
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {
        override val id: String = "constant_lightdata"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ConstantLightComponent {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ConstantLightComponent(
                jsonObject.expression("sky")?.let { engine.compile((it)) },
                jsonObject.expression("block")?.let { engine.compile((it)) },
                particleData, emitterData
            )
        }
    }
}