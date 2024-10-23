package com.ixume.particleemitter.particle.transformation.rotation

import com.google.gson.JsonElement
import com.ixume.particleemitter.UnrealizedComponent
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
    private val myParticleData: ParticleData
) : RotationComponent {
    companion object : ComponentParser<ExpressionRotationComponent> {
        init {
            ParticleJsonParser.rotationComponentParsers += "expression_rotation" to this
        }

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): UnrealizedComponent<ExpressionRotationComponent>? {
            val jsonObject = jsonElement.asJsonObject
            return UnrealizedExpressionRotationComponent(
                jsonObject.expression("x") ?: return null,
                jsonObject.expression("y") ?: return null,
                jsonObject.expression("z") ?: return null,
                macros
            )
        }
    }

    override fun rotation(otherParticleData: ParticleData): Matrix4f {
        myParticleData.copyFrom(otherParticleData)

        return Matrix4f()
            .rotateX((xRot.eval() as Number).toFloat())
            .rotateY((yRot.eval() as Number).toFloat())
            .rotateZ((zRot.eval() as Number).toFloat())
    }
}

class UnrealizedExpressionRotationComponent(
    private val xRot: String,
    private val yRot: String,
    private val zRot: String,
    private val macros: Map<String, Macro>?
) : UnrealizedComponent<ExpressionRotationComponent> {
    override fun realizeComponent(emitterData: EmitterData): ExpressionRotationComponent {
        val (engine, particleData) = particleEngine(emitterData)
        return ExpressionRotationComponent(
            engine.compile(xRot, macros),
            engine.compile(yRot, macros),
            engine.compile(zRot, macros),
            particleData
        )
    }
}