package gg.aquatic.comet.particle.position

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.PostInit
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.emitter.action.Action
import gg.aquatic.comet.parsing.getExpr
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class AttractorPositionComponent(
    private val attractorScripts: List<Attractor>,
    private val factor: Expr<Number>?,
    private val type: String,
    private val onHitAction: Action?,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, PositionComponent, PostInit {
    override val priority = 0

    class Attractor(
        val x: Expr<Number>,
        val y: Expr<Number>,
        val z: Expr<Number>,
        val mass: Expr<Number>?,
        val radius: Expr<Number>?
    )

    override fun realize(unrealizedEmitter: AbstractUnrealizedEmitter) {
        onHitAction?.subActions?.filterIsInstance<PostInit>()?.forEach { it.realize(unrealizedEmitter) }
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
                            (script.x.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble()
                                ?: return) * otherEmitterData.emitter!!.environmentData.size,
                            (script.y.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble()
                                ?: return) * otherEmitterData.emitter!!.environmentData.size,
                            (script.z.eval().getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble()
                                ?: return) * otherEmitterData.emitter!!.environmentData.size,
                        ),
                        (script.mass?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: 1.0),
                        (script.radius?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble()
                            ?.let { it * otherEmitterData.emitter!!.environmentData.size })
                    )
                }


                currentEvaluatedAttractors to (factor?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: 1.0)
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
                                    otherParticleData.particle!!.pose
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
                                    otherParticleData.particle!!.pose
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

                    val totalFactor = factor * attractor.mass
                    attractor.radius?.let {
                        if (totalFactor >= length - it) {
                            onHit(
                                ActionContext(
                                    otherEmitterData,
                                    otherParticleData,
                                    otherParticleData.particle!!.pose
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
        override val id: String = "attractor_position"

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<AttractorPositionComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)
            val attractorScripts: MutableList<Attractor> = mutableListOf()
            val attractorsArray = jsonObject.getAsJsonArray("attractors")
            for (attractorElement in attractorsArray) {
                val attractorObject = attractorElement.asJsonObject
                attractorScripts += Attractor(
                    attractorObject.getExpr("x")
                        .fold({ it }, { return Result.failure(it) })
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) }),
                    attractorObject.getExpr("y")
                        .fold({ it }, { return Result.failure(it) })
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) }),
                    attractorObject.getExpr("z")
                        .fold({ it }, { return Result.failure(it) })
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) }),
                    attractorObject.getExpr("mass")
                        .fold({ it }, { return Result.failure(it) })
                        .constructExpr<Number>(engine, macros)
                        .fold({ it }, { return Result.failure(it) }),
                    attractorObject.getExprOrNull("radius")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) })
                )
            }

            val actions = jsonObject.getAsJsonArray("on_hit_attractor")
                ?.let { Action.parse(it, macros).fold({ it }, { return Result.failure(it) }) }

            return Result.success(
                AttractorPositionComponent(
                    attractorScripts,
                    jsonObject.getExprOrNull("factor")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    jsonObject.getAsJsonPrimitive("type")?.asString ?: "gravity_linear",
                    actions,
                    emitterData, particleData
                )
            )
        }
    }
}