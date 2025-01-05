package gg.aquatic.comet.particle.transformation.rotation

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleComponent
import gg.aquatic.comet.particle.ParticleData
import org.joml.Quaternionf
import javax.script.CompiledScript

class ExpressionRotationComponent(
    private val xRot: CompiledScript,
    private val yRot: CompiledScript,
    private val zRot: CompiledScript,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData
) : ParticleComponent, RotationComponent {
    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "expression_rotation" to this
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

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        otherParticleData.rotation = otherEmitterData.emitter!!.applyEmitterRotation(
            Quaternionf()
                .rotateX((xRot.eval() as Number).toFloat())
                .rotateY((yRot.eval() as Number).toFloat())
                .rotateZ((zRot.eval() as Number).toFloat())
        )
    }
}