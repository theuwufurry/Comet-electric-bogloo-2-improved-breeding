package com.ixume.particleemitter.particle.transformation.rotation

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import org.joml.Matrix4f
import javax.script.CompiledScript

class ExpressionRotationComponent(
    private val xRot: CompiledScript,
    private val yRot: CompiledScript,
    private val zRot: CompiledScript,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : RotationComponent {
    companion object : ComponentParser<ExpressionRotationComponent> {
        init {
            ParticleJsonParser.rotationComponentParsers += "expression_rotation" to this
        }

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): ExpressionRotationComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ExpressionRotationComponent(
                engine.compile(jsonObject.expression("x") ?: return null, macros),
                engine.compile(jsonObject.expression("y") ?: return null, macros),
                engine.compile(jsonObject.expression("z") ?: return null, macros),
                particleData, emitterData
            )
        }
    }

    override fun rotation(otherEmitterData: EmitterData, otherParticleData: ParticleData): Matrix4f {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        return Matrix4f()
            .rotateX((xRot.eval() as Number).toFloat())
            .rotateY((yRot.eval() as Number).toFloat())
            .rotateZ((zRot.eval() as Number).toFloat())
    }
}