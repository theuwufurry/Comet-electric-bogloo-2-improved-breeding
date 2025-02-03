package gg.aquatic.comet.particle.transformation.scale

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.expression
import org.joml.Vector3f
import javax.script.CompiledScript

class ExpressionScaleComponent(
    private val xScale: CompiledScript,
    private val yScale: CompiledScript,
    private val zScale: CompiledScript,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : ParticleComponent, ScaleComponent {
    companion object : BaseComponentParser, ScaleComponent {
        override val id: String = "expression_scale"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): ExpressionScaleComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ExpressionScaleComponent(
                engine.compile(jsonObject.expression("x") ?: return null, macros),
                engine.compile(jsonObject.expression("y") ?: return null, macros),
                engine.compile(jsonObject.expression("z") ?: return null, macros),
                particleData, emitterData
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        otherParticleData.scale = Vector3f(
            (xScale.eval() as Number).toFloat() * otherEmitterData.emitter!!.environmentData.size.toFloat(),
            (yScale.eval() as Number).toFloat() * otherEmitterData.emitter!!.environmentData.size.toFloat(),
            (zScale.eval() as Number).toFloat() * otherEmitterData.emitter!!.environmentData.size.toFloat()
        )
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}