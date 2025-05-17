package gg.aquatic.comet.particle.transformation.translation

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.expression
import org.joml.Vector3f
import javax.script.CompiledScript

class ExpressionTranslationComponent(
    private val xOffset: CompiledScript?,
    private val yOffset: CompiledScript?,
    private val zOffset: CompiledScript?,
    private val xAbsoluteOffset: CompiledScript?,
    private val yAbsoluteOffset: CompiledScript?,
    private val zAbsolulteOffset: CompiledScript?,
    private val myParticleData: ParticleData,
    private val myEmitterData: EmitterData,
) : ParticleComponent {
    override val priority = 0

    companion object : BaseComponentParser {
        override val id: String = "expression_translation"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): ExpressionTranslationComponent {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ExpressionTranslationComponent(
                jsonObject.expression("x")?.let { engine.compile((it)) },
                jsonObject.expression("y")?.let { engine.compile((it)) },
                jsonObject.expression("z")?.let { engine.compile((it)) },
                jsonObject.expression("abs_x")?.let { engine.compile((it)) },
                jsonObject.expression("abs_y")?.let { engine.compile((it)) },
                jsonObject.expression("abs_z")?.let { engine.compile((it)) },
                particleData, emitterData
            )
        }
    }

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        otherParticleData.translation = Vector3f(
            (xOffset?.eval() as? Number)?.toFloat()?.let { it * otherEmitterData.emitter!!.environmentData.size.toFloat() } ?: 0f,
            (yOffset?.eval() as? Number)?.toFloat()?.let { it * otherEmitterData.emitter!!.environmentData.size.toFloat() } ?: 0f,
            (zOffset?.eval() as? Number)?.toFloat()?.let { it * otherEmitterData.emitter!!.environmentData.size.toFloat() } ?: 0f,
        ).rotate(otherParticleData.rotation).add(
            (xAbsoluteOffset?.eval() as? Number)?.toFloat()?.let { it * otherEmitterData.emitter!!.environmentData.size.toFloat() } ?: 0f,
            (yAbsoluteOffset?.eval() as? Number)?.toFloat()?.let { it * otherEmitterData.emitter!!.environmentData.size.toFloat() } ?: 0f,
            (zAbsolulteOffset?.eval() as? Number)?.toFloat()?.let { it * otherEmitterData.emitter!!.environmentData.size.toFloat() } ?: 0f,
        )
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}
}