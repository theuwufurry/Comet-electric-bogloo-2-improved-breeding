package gg.aquatic.comet.particle.light

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.LightData
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint

class ConstantLightComponent(
    private val skylightScript: Expr<Number>?,
    private val blocklightScript: Expr<Number>?,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData,
) : ParticleComponent {
    override val priority = 0

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        if (skylightScript == null && blocklightScript == null) return
        if (otherParticleData.light != null) return

        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        val skylight = skylightScript?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toInt() ?: 0
        val blocklight = blocklightScript?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toInt() ?: 0

        otherParticleData.light = LightData(skylight, blocklight)
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {
        override val id: String = "constant_lightdata"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<ConstantLightComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return Result.success(
                ConstantLightComponent(
                    jsonObject.getExprOrNull("sky")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    jsonObject.getExprOrNull("block")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    particleData, emitterData
                )
            )
        }
    }
}