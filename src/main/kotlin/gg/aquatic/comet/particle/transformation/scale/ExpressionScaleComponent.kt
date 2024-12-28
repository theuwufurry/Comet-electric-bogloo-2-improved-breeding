package gg.aquatic.comet.particle.transformation.scale

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3f
import javax.script.CompiledScript

class ExpressionScaleComponent(
    private val xScale: CompiledScript,
    private val yScale: CompiledScript,
    private val zScale: CompiledScript,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : ScaleComponent {
    companion object : ComponentParser<ExpressionScaleComponent> {
        init {
            ParticleJsonParser.scaleComponentParsers += "expression_scale" to this
        }

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

    override fun scale(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3f {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        return Vector3f(
            (xScale.eval() as Number).toFloat(),
            (yScale.eval() as Number).toFloat(),
            (zScale.eval() as Number).toFloat()
        )
    }
}