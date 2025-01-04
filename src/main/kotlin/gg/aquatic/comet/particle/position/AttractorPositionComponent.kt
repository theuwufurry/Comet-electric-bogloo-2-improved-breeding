package gg.aquatic.comet.particle.position

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.ComponentResult
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3d
import java.util.*
import javax.script.CompiledScript
import kotlin.math.min

class AttractorPositionComponent(
    private val attractorScripts: List<Attractor>,
    private val factor: CompiledScript,
    private val type: String,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : PositionComponent {
    class Attractor(
        val x: CompiledScript,
        val y: CompiledScript,
        val z: CompiledScript,
        val mass: CompiledScript
    )

    private class EvaluatedAttractor(val pos: Vector3d, val mass: Double)
    private class EmitterAttractorData(val evaluatedAttractors: List<EvaluatedAttractor>, val evaluatedFactor: Double, val age: Double)

    private val emitterAttractorMap: MutableMap<UUID, EmitterAttractorData> = mutableMapOf()

    override fun pos(otherEmitterData: EmitterData, otherParticleData: ParticleData): ComponentResult<Vector3d> {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val savedData = emitterAttractorMap[otherEmitterData.id]
        val (attractors, factor) =
            if (savedData == null || savedData.age != otherEmitterData.age) {
                val currentEvaluatedAttractors: MutableList<EvaluatedAttractor> = mutableListOf()
                for (script in attractorScripts) {
                    currentEvaluatedAttractors += EvaluatedAttractor(
                        Vector3d(
                            (script.x.eval() as Number).toDouble(),
                            (script.y.eval() as Number).toDouble(),
                            (script.z.eval() as Number).toDouble(),
                        ), (script.mass.eval() as Number).toDouble()
                    )
                }

                currentEvaluatedAttractors to (factor.eval() as Number).toDouble()
            } else {
                savedData.evaluatedAttractors to savedData.evaluatedFactor
            }

        emitterAttractorMap[otherEmitterData.id] = EmitterAttractorData(attractors, factor, otherEmitterData.age)
        val velocity = Vector3d()
        when (type) {
            "gravity_squared" -> {
                for (attractor in attractors) { //formula = factor * m / d^2
                    val delta = Vector3d(attractor.pos).sub(otherParticleData.relativePosition)
                    val length = delta.length()
                    if (length < 0.00001) continue
                    val distanceSquared = attractor.pos.distanceSquared(otherParticleData.relativePosition)
                    val totalFactor = min(factor * attractor.mass / distanceSquared, length)
                    velocity.add(delta.normalize(totalFactor))
                }
            }

            "gravity_linear" -> {
                for (attractor in attractors) { //formula = factor * m / d^2
                    val delta = Vector3d(attractor.pos).sub(otherParticleData.relativePosition)
                    val length = delta.length()
                    if (length < 0.00001) continue
                    val distance = attractor.pos.distance(otherParticleData.relativePosition)
                    val totalFactor = min(factor * attractor.mass / distance, length)
                    velocity.add(delta.normalize(totalFactor))
                }
            }

            "flat" -> {
                for (attractor in attractors) { //formula = factor * m / d^2
                    val delta = Vector3d(attractor.pos).sub(otherParticleData.relativePosition)
                    val length = delta.length()
                    if (length < 0.00001) continue
                    val totalFactor = min(attractor.mass, length)
                    velocity.add(delta.normalize(totalFactor))
                }
            }
        }

        return ComponentResult(Vector3d(otherParticleData.relativePosition).add(velocity))
    }

    companion object : ComponentParser<AttractorPositionComponent> {
        init {
            ParticleJsonParser.positionComponentParsers += "attractor_position" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): AttractorPositionComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)
            val attractorScripts: MutableList<Attractor> = mutableListOf()
            val attractorsArray = jsonObject.getAsJsonArray("attractors")
            for (attractorElement in attractorsArray) {
                val attractorObject = attractorElement.asJsonObject
                attractorScripts += Attractor(
                    engine.compile(attractorObject.expression("x") ?: return null, macros),
                    engine.compile(attractorObject.expression("y") ?: return null, macros),
                    engine.compile(attractorObject.expression("z") ?: return null, macros),
                    engine.compile(attractorObject.expression("mass") ?: "1", macros),
                )
            }

            return AttractorPositionComponent(
                attractorScripts,
                engine.compile(jsonObject.expression("factor") ?: "1", macros),
                jsonObject.getAsJsonPrimitive("type")?.asString ?: "gravity_linear",
                emitterData, particleData
            )
        }
    }
}