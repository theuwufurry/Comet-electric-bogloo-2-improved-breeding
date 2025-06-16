package gg.aquatic.comet.particle.position.initial

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.particle.position.PositionComponent
import org.joml.Vector3d
import javax.script.CompiledScript

class InitialExpressionPositionComponent(
    private val xOffset: CompiledScript?,
    private val yOffset: CompiledScript?,
    private val zOffset: CompiledScript?,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, PositionComponent {
    override val priority = 0
    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        if (otherParticleData.age == 0.0) {
            myEmitterData.copyFrom(otherEmitterData)
            myParticleData.copyFrom(otherParticleData)
            otherParticleData.relativePosition.add(
                Vector3d(
                    (xOffset?.eval() as? Number)?.toDouble() ?: 0.0,
                    (yOffset?.eval() as? Number)?.toDouble() ?: 0.0,
                    (zOffset?.eval() as? Number)?.toDouble() ?: 0.0,
                ).rotate(otherEmitterData.emitter!!.pose.rot)
            )
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {

        override val id: String = "initial_expression_position"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): InitialExpressionPositionComponent {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return InitialExpressionPositionComponent(
                jsonObject.expression("x")?.let { engine.compile(it, macros) },
                jsonObject.expression("y")?.let { engine.compile(it, macros) },
                jsonObject.expression("z")?.let { engine.compile(it, macros) },
                emitterData, particleData
            )
        }
    }
}