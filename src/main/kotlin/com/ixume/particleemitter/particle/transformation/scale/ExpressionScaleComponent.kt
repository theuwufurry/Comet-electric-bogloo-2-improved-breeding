package com.ixume.particleemitter.particle.transformation.scale

import com.google.gson.JsonElement
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Vector3f
import javax.script.CompiledScript

class ExpressionScaleComponent(private val xScale: CompiledScript, private val yScale: CompiledScript, private val zScale: CompiledScript, private val myEmitterData: EmitterData, private val myParticleData: ParticleData) : ScaleComponent {
    companion object : ComponentParser<ExpressionScaleComponent> {
        init {
            ParticleJsonParser.scaleComponentParsers += "expression_scale" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): UnrealizedComponent<ExpressionScaleComponent>? {
            val jsonObject = jsonElement.asJsonObject
            return UnrealizedExpressionScaleComponent(
                jsonObject.expression("x") ?: return null,
                jsonObject.expression("y") ?: return null,
                jsonObject.expression("z") ?: return null,
                macros)
        }
    }

    override fun scale(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3f {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        return Vector3f((xScale.eval() as Double).toFloat(), (yScale.eval() as Double).toFloat(), (zScale.eval() as Double).toFloat())
    }
}

class UnrealizedExpressionScaleComponent(private val xScale: String, private val yScale: String, private val zScale: String, private val macros: Map<String, Macro>?) : UnrealizedComponent<ExpressionScaleComponent> {
    override fun realizeComponent(emitterData: EmitterData): ExpressionScaleComponent {
        val (engine, particleData) = particleEngine(emitterData)
        return ExpressionScaleComponent(
            engine.compile(xScale, macros),
            engine.compile(yScale, macros),
            engine.compile(zScale, macros),
            emitterData,
            particleData
        )
    }
}