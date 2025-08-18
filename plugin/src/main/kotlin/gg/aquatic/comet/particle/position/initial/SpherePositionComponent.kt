package gg.aquatic.comet.particle.position.initial

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.asStringOrNull
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.parsing.getExprOrNull
import gg.aquatic.comet.particle.position.PositionComponent
import gg.aquatic.comet.particle.position.direction.DirectionSubcomponent
import gg.aquatic.comet.script.expr.Expr
import gg.aquatic.comet.script.expr.JSExpr.Companion.constructExpr
import gg.aquatic.comet.script.expr.getOrPrint
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class SpherePositionComponent(
    private val radiusScript: Expr<Number>?,
    private val dir: SphereDirection?,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData,
) : ParticleComponent, PositionComponent, DirectionSubcomponent {
    override val priority = 0

    private val directionMap: MutableMap<UUID, Vector3d> = ConcurrentHashMap()

    override fun execute(otherEmitterData: EmitterData, otherParticleData: ParticleData) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        otherParticleData.relativePosition = if (otherParticleData.age == 0.0) {
            val radius = radiusScript?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: DEFAULT_RADIUS
            val radiusSquared = radius * radius
            var sphereOffset = randomVector(otherEmitterData.emitter!!, radius)
            while (sphereOffset.lengthSquared() > radiusSquared) {
                sphereOffset = randomVector(otherEmitterData.emitter!!, radius)
            }

            directionMap[otherParticleData.id] = sphereOffset

            Vector3d(otherParticleData.relativePosition).add(sphereOffset)
        } else otherParticleData.relativePosition
    }

    private fun randomVector(emitter: AbstractEmitter, radius: Double): Vector3d {
        return Vector3d(
            emitter.random.kotlinRandom.nextDouble() * 2.0 * radius - radius,
            emitter.random.kotlinRandom.nextDouble() * 2.0 * radius - radius,
            emitter.random.kotlinRandom.nextDouble() * 2.0 * radius - radius
        )
    }

    override fun dir(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d {
        if (dir == null) return Vector3d()

        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        val magnitude = dir.magnitude?.eval()?.getOrPrint(otherEmitterData.emitter!!.unrealizedEmitter.id)?.toDouble() ?: 1.0

        return when (dir.type) {
            DirType.INWARDS -> {
                directionMap[otherParticleData.id]!!.mul(-1.0).normalize(magnitude)
            }

            DirType.OUTWARDS -> {
                directionMap[otherParticleData.id]!!.normalize(magnitude)
            }
        }
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    companion object : BaseComponentParser {
        override val id: String = "sphere_position"
        private const val DEFAULT_RADIUS = 1.0

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): Result<SpherePositionComponent> {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            val dir = jsonObject["direction"]?.let top@{
                val magnitudeScript = jsonObject["magnitude"]?.getExprOrNull()
                    ?.constructExpr<Number>(engine, macros)
                    ?.fold({ it }, { return Result.failure(it) })

                it.asStringOrNull()?.let { str ->
                    when (str) {
                        "inwards" -> SphereDirection(DirType.INWARDS, magnitudeScript)
                        "outwards" -> return@top SphereDirection(DirType.OUTWARDS, magnitudeScript)
                        else -> null
                    }
                }
            }

            return Result.success(
                SpherePositionComponent(
                    jsonObject.getExprOrNull("radius")
                        ?.constructExpr<Number>(engine, macros)
                        ?.fold({ it }, { return Result.failure(it) }),
                    dir,
                    emitterData, particleData
                )
            )
        }
    }

    class SphereDirection(
        val type: DirType,
        val magnitude: Expr<Number>? = null,
    )

    enum class DirType {
        INWARDS,
        OUTWARDS
    }
}