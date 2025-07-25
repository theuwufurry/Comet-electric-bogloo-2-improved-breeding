package gg.aquatic.comet.particle.position

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.expression
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.script.CompiledScript

class ExpressionPositionComponent(
    private val xOffset: CompiledScript,
    private val yOffset: CompiledScript,
    private val zOffset: CompiledScript,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, PositionComponent {
    override val priority = 0
    private val cachedOldOutput: MutableMap<UUID, Vector3d> = ConcurrentHashMap()
    private val oldOutput: MutableMap<UUID, Vector3d> = ConcurrentHashMap()

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        val om = if (otherEmitterData.emitter!!.isPregen) cachedOldOutput else oldOutput

        val oldResult = om.getOrPut(otherParticleData.id) { Vector3d() }
        val newResult = Vector3d(
            (xOffset.eval() as Number).toDouble() * otherEmitterData.emitter!!.environmentData.size,
            (yOffset.eval() as Number).toDouble() * otherEmitterData.emitter!!.environmentData.size,
            (zOffset.eval() as Number).toDouble() * otherEmitterData.emitter!!.environmentData.size
        )
        om[otherParticleData.id] = newResult.rotate(myEmitterData.emitter!!.pose.rot)
        val newPos = Vector3d(otherParticleData.relativePosition).add(Vector3d(newResult).sub(oldResult))
        otherParticleData.relativePosition = newPos
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {
        override val id: String = "expression_position"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): ExpressionPositionComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            return ExpressionPositionComponent(
                engine.compile(jsonObject.expression("x") ?: return null, macros) ?: return null,
                engine.compile(jsonObject.expression("y") ?: return null, macros) ?: return null,
                engine.compile(jsonObject.expression("z") ?: return null, macros) ?: return null,
                emitterData, particleData
            )
        }
    }
}