package gg.aquatic.comet.particle.position

import com.google.gson.JsonElement
import gg.aquatic.comet.emitter.EmitterData
import gg.aquatic.comet.emitter.action.Action
import gg.aquatic.comet.emitter.action.ActionContext
import gg.aquatic.comet.parsing.*
import gg.aquatic.comet.parsing.macro.Macro
import gg.aquatic.comet.particle.ParticleComponent
import gg.aquatic.comet.particle.ParticleData
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.script.CompiledScript
import kotlin.math.min

class AttractorPositionComponent(
    private val attractorScripts: List<Attractor>,
    private val factor: CompiledScript,
    private val type: String,
    private val onHitAction: Action?,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, PositionComponent, PostInit {
    class Attractor(
        val x: CompiledScript,
        val y: CompiledScript,
        val z: CompiledScript,
        val mass: CompiledScript,
        val radius: CompiledScript?
    )

    override fun realize() {
        onHitAction?.subActions?.filterIsInstance<PostInit>()?.forEach { it.realize() }
    }

    private class EvaluatedAttractor(val pos: Vector3d, val mass: Double, val radius: Double?)
    private class EmitterAttractorData(
        val evaluatedAttractors: List<EvaluatedAttractor>,
        val evaluatedFactor: Double,
        val age: Double
    )

    private val emitterAttractorMap: MutableMap<UUID, EmitterAttractorData> = ConcurrentHashMap()

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        val savedData = emitterAttractorMap[otherEmitterData.id]
        val (attractors, factor) =
            if (savedData == null || savedData.age != otherEmitterData.age) {
                val currentEvaluatedAttractors: MutableList<EvaluatedAttractor> = mutableListOf()
                for (script in attractorScripts) {
                    currentEvaluatedAttractors += EvaluatedAttractor(
                        Vector3d(
                            (script.x.eval() as Number).toDouble() * otherEmitterData.emitter!!.environmentData.size,
                            (script.y.eval() as Number).toDouble() * otherEmitterData.emitter!!.environmentData.size,
                            (script.z.eval() as Number).toDouble() * otherEmitterData.emitter!!.environmentData.size,
                        ),
                        (script.mass.eval() as Number).toDouble(),
                        (script.radius?.eval() as? Number)?.toDouble()
                            ?.let { it * otherEmitterData.emitter!!.environmentData.size }
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
                    val totalFactor = factor * attractor.mass / distanceSquared
                    attractor.radius?.let {
                        if (totalFactor >= length - it) {
                            onHit(
                                ActionContext(
                                    otherEmitterData,
                                    otherParticleData,
                                    otherParticleData.particle!!.pose()
                                )
                            )
                        }
                    }

                    velocity.add(delta.normalize(min(totalFactor, length)))
                }
            }

            "gravity_linear" -> {
                for (attractor in attractors) { //formula = factor * m / d^2
                    val delta = Vector3d(attractor.pos).sub(otherParticleData.relativePosition)
                    val length = delta.length()
                    if (length < 0.00001) continue

                    val distance = attractor.pos.distance(otherParticleData.relativePosition)
                    val totalFactor = factor * attractor.mass / distance
                    attractor.radius?.let {
                        if (totalFactor >= length - it) {
                            onHit(
                                ActionContext(
                                    otherEmitterData,
                                    otherParticleData,
                                    otherParticleData.particle!!.pose()
                                )
                            )
                        }
                    }

                    velocity.add(delta.normalize(min(totalFactor, length)))
                }
            }

            "flat" -> {
                for (attractor in attractors) { //formula = factor * m / d^2
                    val delta = Vector3d(attractor.pos).sub(otherParticleData.relativePosition)
                    val length = delta.length()
                    if (length < 0.00001) continue

                    val totalFactor = attractor.mass
                    attractor.radius?.let {
                        if (totalFactor >= length - it) {
                            onHit(
                                ActionContext(
                                    otherEmitterData,
                                    otherParticleData,
                                    otherParticleData.particle!!.pose()
                                )
                            )
                        }
                    }

                    velocity.add(delta.normalize(min(totalFactor, length)))
                }
            }
        }

        otherParticleData.relativePosition.add(velocity.mul(otherEmitterData.emitter!!.environmentData.size))
        return
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    private fun onHit(context: ActionContext) {
        onHitAction?.execute(context)
    }

    companion object : BaseComponentParser {
        init {
            ParticleJsonParser.componentParsers += "attractor_position" to this
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
                    attractorObject.expression("radius")?.let { engine.compile(it, macros) },
                )
            }

            val actions = jsonObject.getAsJsonArray("on_hit_attractor")?.let { Action.parse(it, macros) }

            return AttractorPositionComponent(
                attractorScripts,
                engine.compile(jsonObject.expression("factor") ?: "1", macros),
                jsonObject.getAsJsonPrimitive("type")?.asString ?: "gravity_linear",
                actions,
                emitterData, particleData
            )
        }
    }
}