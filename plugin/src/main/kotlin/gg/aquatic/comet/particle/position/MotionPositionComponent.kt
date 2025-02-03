package gg.aquatic.comet.particle.position

import com.google.gson.JsonElement
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.action.ActionContext
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.parsing.BaseComponentParser
import gg.aquatic.comet.api.parsing.PostInit
import gg.aquatic.comet.api.parsing.compile
import gg.aquatic.comet.api.parsing.macro.Macro
import gg.aquatic.comet.api.parsing.particleEngine
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.emitter.UnrealizedEmitter
import gg.aquatic.comet.emitter.action.Action
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.expression
import gg.aquatic.comet.particle.position.direction.DirectionSubcomponent
import gg.aquatic.comet.particle.position.direction.ExpressionDirectionSubcomponent
import gg.aquatic.comet.particle.position.direction.RandomDirectionSubcomponent
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.Block
import org.joml.Vector3d
import org.joml.Vector3i
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.script.CompiledScript
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sign

class MotionPositionComponent(
    private val initialVelocityComponent: DirectionSubcomponent,
    private val accelerationScript: Triple<CompiledScript, CompiledScript, CompiledScript>?,
    private val dragScript: CompiledScript?,
    private val restitutionScript: CompiledScript?,
    private val onCollisionAction: Action?,
    private val onCollisionEmitterID: String?,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : ParticleComponent, PositionComponent, PostInit {
    private var unrealizedEmitter: UnrealizedEmitter? = null

    override fun realize() {
        onCollisionEmitterID?.let { unrealizedEmitter = ParticleJsonParser.jsonUnrealizedEmitters[it]!! }
        onCollisionAction?.subActions?.filterIsInstance<PostInit>()?.forEach { it.realize() }
    }

    val oldPositionMap: MutableMap<UUID, Vector3d> = ConcurrentHashMap()

    override fun execute(
        otherEmitterData: EmitterData,
        otherParticleData: ParticleData
    ) {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)

        if (otherParticleData.age == 0.0) {
            oldPositionMap[otherParticleData.id] = Vector3d(otherParticleData.relativePosition)
            otherParticleData.relativePosition = Vector3d(otherParticleData.relativePosition).add(
                initialVelocityComponent.dir(
                    otherEmitterData,
                    otherParticleData
                ).mul(otherEmitterData.emitter!!.environmentData.size).rotate(myEmitterData.emitter!!.emitterRotation)
            )
            return
        }

        val dragCoefficient = (dragScript?.eval() as? Number)?.toDouble() ?: 0.0
        val acceleration = (accelerationScript?.let {
            Vector3d(
                (it.first.eval() as Number).toDouble(),
                (it.second.eval() as Number).toDouble(),
                (it.third.eval() as Number).toDouble()
            ).mul(otherEmitterData.emitter!!.environmentData.size)
        } ?: Vector3d()).add(otherParticleData.acceleration)

        otherParticleData.acceleration = Vector3d()

        val oldPos = oldPositionMap[otherParticleData.id]
        val velocity = Vector3d(otherParticleData.relativePosition).sub(oldPos)

        velocity.mul(1.0 - dragCoefficient)
        acceleration.mul(1.0 - dragCoefficient)

        oldPositionMap[otherParticleData.id] = Vector3d(otherParticleData.relativePosition)

        val newPos = Vector3d(otherParticleData.relativePosition).add(velocity).add(acceleration)
        val correction = fixCollisions(newPos, otherParticleData.acceleration)
        if (correction != null) {
            newPos.add(correction.vector)

            if (velocity.lengthSquared() > 0.01) {
                onCollision(
                    ActionContext(
                        otherEmitterData,
                        otherParticleData,
                        Pose(
                            Vector3d(newPos).add(otherParticleData.origin),
                            correction.direction
                        )
                    )
                )
                otherParticleData.relativePosition = Vector3d(newPos)
            }
        }

        otherParticleData.relativePosition = Vector3d(newPos)
    }

    override fun die(otherEmitterData: EmitterData, otherParticleData: ParticleData) {}

    private fun onCollision(context: ActionContext) {
        onCollisionAction?.execute(context)
    }

    private fun fixCollisions(rawNewPos: Vector3d, acceleration: Vector3d): CorrectionResult? {
        if (restitutionScript != null) {
            val currentPos = Vector3d(myParticleData.origin).add(myParticleData.relativePosition)
            val velocity = Vector3d(rawNewPos).sub(myParticleData.relativePosition)
            val newPos = Vector3d(myParticleData.origin).add(rawNewPos)
            val block = blockAt(myEmitterData.world!!, currentPos)

            val restitution = (restitutionScript.eval() as Number).toDouble()

            val intersection = block.intersect(currentPos, velocity)
            if (intersection != null) {
                val correctionVector = Vector3d(intersection.intersection).sub(newPos)
                val direction: Vector3d
                when (intersection.direction) {
                    0 -> {
                        acceleration.add(Vector3d(velocity).mul(-restitution, restitution, restitution))
                        direction = Vector3d(-sign(velocity.x), 0.0, 0.0)
                    }

                    1 -> {
                        acceleration.add(Vector3d(velocity).mul(restitution, -restitution, restitution))
                        direction = Vector3d(0.0, -sign(velocity.y), 0.0)
                    }

                    else -> {
                        acceleration.add(Vector3d(velocity).mul(restitution, restitution, -restitution))
                        direction = Vector3d(0.0, 0.0, -sign(velocity.z))
                    }
                }

                acceleration.add(Vector3d(velocity).add(correctionVector).mul(-1.0))

                return CorrectionResult(correctionVector, direction)
            }

            val maxOffset =
                (abs(velocity.x) + 1.0) * (abs(velocity.x) + 1.0) + (abs(velocity.y) + 1.0) * (abs(velocity.y) + 1.0) + (abs(
                    velocity.z
                ) + 1.0) * (abs(velocity.z) + 1.0)

            //block-plane offset
            val offset = Vector3i()
            //only ever have 3 intersections at any given time, 1 for each axis
            while (offset.lengthSquared() <= maxOffset) {
                val yzPlane: Int = (floor(currentPos.x) + 0.5 + (offset.x + 0.5) * sign(velocity.x)).toInt()
                var yzDistance = (yzPlane.toDouble() - currentPos.x) / velocity.x
                if (yzDistance < 0.0) {
                    yzDistance = Double.MAX_VALUE
                }

                val xzPlane: Int = (floor(currentPos.y) + 0.5 + (offset.y + 0.5) * sign(velocity.y)).toInt()
                var xzDistance = (xzPlane.toDouble() - currentPos.y) / velocity.y
                if (xzDistance < 0.0) {
                    xzDistance = Double.MAX_VALUE
                }

                val xyPlane: Int = (floor(currentPos.z) + 0.5 + (offset.z + 0.5) * sign(velocity.z)).toInt()
                var xyDistance = (xyPlane.toDouble() - currentPos.z) / velocity.z
                if (xyDistance < 0.0) {
                    xyDistance = Double.MAX_VALUE
                }

                if (min(min(yzDistance, xzDistance), xyDistance) > 1.0) {
                    return null
                }

                val index = min(yzDistance, xzDistance, xyDistance)
                when (index) {
                    //yzDistance is smallest
                    0 -> {
                        val yzIntersection = Vector3d(currentPos).add(Vector3d(velocity).mul(yzDistance))
                        val yzBlock = myEmitterData.world!!.getBlockAt(
                            (yzPlane - 0.5 + 0.5 * sign(velocity.x)).toInt(),
                            floor(yzIntersection.y).toInt(),
                            floor(yzIntersection.z).toInt()
                        )
                        if (!yzBlock.isPassable) {
                            val result = yzBlock.intersect(currentPos, velocity)
                            if (result == null) {
                                offset.x++
                                continue
                            }

                            val correctionVector = Vector3d(result.intersection).sub(newPos)
                            acceleration.add(Vector3d(velocity).mul(-restitution, restitution, restitution))
                            acceleration.add(Vector3d(velocity).add(correctionVector).mul(-1.0))
                            return CorrectionResult(correctionVector, Vector3d(-sign(velocity.x), 0.0, 0.0))
                        } else {
                            //not done...
                            offset.x++
                            continue
                        }
                    }

                    1 -> {
                        //xzDistance is smallest
                        val xzIntersection = Vector3d(currentPos).add(Vector3d(velocity).mul(xzDistance))
                        val xzBlock = myEmitterData.world!!.getBlockAt(
                            floor(xzIntersection.x).toInt(),
                            (xzPlane - 0.5 + 0.5 * sign(velocity.y)).toInt(),
                            floor(xzIntersection.z).toInt()
                        )

                        if (!xzBlock.isPassable) {
                            val result = xzBlock.intersect(currentPos, velocity)
                            if (result == null) {
                                offset.y++
                                continue
                            }

                            val correctionVector = Vector3d(result.intersection).sub(newPos)
                            acceleration.add(Vector3d(velocity).mul(restitution, -restitution, restitution))
                            acceleration.add(Vector3d(velocity).add(correctionVector).mul(-1.0))
                            return CorrectionResult(correctionVector, Vector3d(0.0, -sign(velocity.y), 0.0))
                        } else {
                            //not done...
                            offset.y++
                            continue
                        }
                    }

                    2 -> {
                        //xyDistance is smallest
                        val xyIntersection = Vector3d(currentPos).add(Vector3d(velocity).mul(xyDistance))
                        val xyBlock = myEmitterData.world!!.getBlockAt(
                            floor(xyIntersection.x).toInt(),
                            floor(xyIntersection.y).toInt(),
                            (xyPlane - 0.5 + 0.5 * sign(velocity.z)).toInt()
                        )
                        if (!xyBlock.isPassable) {
                            val result = xyBlock.intersect(currentPos, velocity)
                            if (result == null) {
                                offset.z++
                                continue
                            }

                            val correctionVector = Vector3d(result.intersection).sub(newPos)
                            acceleration.add(Vector3d(velocity).mul(restitution, restitution, -restitution))
                            acceleration.add(Vector3d(velocity).add(correctionVector).mul(-1.0))
                            return CorrectionResult(correctionVector, Vector3d(0.0, 0.0, -sign(velocity.z)))
                        } else {
                            //not done...
                            offset.z++
                            continue
                        }
                    }
                }
            }
        }

        return null
    }

    private fun blockAt(world: World, vector3d: Vector3d): Block {
        return world.getBlockAt(
            floor(vector3d.x).toInt(),
            floor(vector3d.y).toInt(),
            floor(vector3d.z).toInt()
        )
    }

    private fun min(first: Double, second: Double, third: Double): Int {
        return if (first < second) {
            if (first < third) {
                0
            } else {
                2
            }
        } else {
            if (second < third) {
                1
            } else {
                2
            }
        }
    }

    private fun Block.intersect(origin: Vector3d, velocity: Vector3d): IntersectionResult? {
        val blockPos = location.toVector().toVector3d()
        var closestResult: IntersectionResult? = null

        for (boundingBox in collisionShape.boundingBoxes) {
            val yzPlane: Double = blockPos.x + (boundingBox.centerX - boundingBox.widthX * 0.5 * sign(velocity.x))
            var yzDistance: Double = (yzPlane - origin.x) / velocity.x
            val yzIntersection = Vector3d(origin).add(Vector3d(velocity).mul(yzDistance))
            if (yzDistance < 0.0
                || yzIntersection.y - blockPos.y !in boundingBox.minY..boundingBox.maxY
                || yzIntersection.z - blockPos.z !in boundingBox.minZ..boundingBox.maxZ
            ) yzDistance = Double.MAX_VALUE

            val xzPlane: Double = blockPos.y + (boundingBox.centerY - boundingBox.height * 0.5 * sign(velocity.y))
            var xzDistance: Double = (xzPlane - origin.y) / velocity.y
            val xzIntersection = Vector3d(origin).add(Vector3d(velocity).mul(xzDistance))
            if (xzDistance < 0.0
                || xzIntersection.x - blockPos.x !in boundingBox.minX..boundingBox.maxX
                || xzIntersection.z - blockPos.z !in boundingBox.minZ..boundingBox.maxZ
            ) xzDistance = Double.MAX_VALUE

            val xyPlane: Double = blockPos.z + (boundingBox.centerZ - boundingBox.widthZ * 0.5 * sign(velocity.z))
            var xyDistance: Double = (xyPlane - origin.z) / velocity.z
            val xyIntersection = Vector3d(origin).add(Vector3d(velocity).mul(xyDistance))
            if (xyDistance < 0.0
                || xyIntersection.x - blockPos.x !in boundingBox.minX..boundingBox.maxX
                || xyIntersection.y - blockPos.y !in boundingBox.minY..boundingBox.maxY
            ) xyDistance = Double.MAX_VALUE

            if (min(min(yzDistance, xzDistance), xyDistance) > 1.0) continue
            val index = min(yzDistance, xzDistance, xyDistance)
            val result = when (index) {
                0 -> IntersectionResult(0, yzDistance, yzIntersection.add(-0.0001 * sign(velocity.x), 0.0, 0.0))
                1 -> IntersectionResult(1, xzDistance, xzIntersection.add(0.0, -0.0001 * sign(velocity.y), 0.0))
                else -> IntersectionResult(2, xyDistance, xyIntersection.add(0.0, 0.0, -0.0001 * sign(velocity.z)))
            }

            closestResult = closestResult?.let { if (result.distance < it.distance) result else it } ?: result
        }

        return closestResult
    }

    companion object : BaseComponentParser {
        override val id: String = "motion_position"

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): MotionPositionComponent? {
            val jsonObject = jsonElement.asJsonObject
            val emitterData = EmitterData()
            val (engine, particleData) = particleEngine(emitterData)

            var velocityComponent: DirectionSubcomponent? = null

            if ("initial_velocity" in jsonObject.keySet()) {
                val velocityObject = jsonObject.getAsJsonObject("initial_velocity") ?: return null
                velocityComponent = ExpressionDirectionSubcomponent(
                    engine.compile(velocityObject.expression("x") ?: return null, macros),
                    engine.compile(velocityObject.expression("y") ?: return null, macros),
                    engine.compile(velocityObject.expression("z") ?: return null, macros),
                    particleData, emitterData
                )
            } else if ("random_velocity" in jsonObject.keySet()) {
                val velocityObject = jsonObject.getAsJsonObject("random_velocity") ?: return null
                val directionPair: Pair<DirectionSubcomponent, CompiledScript>? =
                    velocityObject.getAsJsonObject("bias")?.let l@{
                        if (!it.has("direction")) throw NullPointerException("bias in random_velocity missing direction!")
                        val dirObject = it.get("direction")
                        if (dirObject.isJsonArray) {
                            Bukkit.getLogger()
                                .warning("Using outdated json array for 'direction', please switch it to 'x' 'y' 'z' format!")
                            Pair(
                                ExpressionDirectionSubcomponent(
                                    engine.compile(
                                        it.getAsJsonArray("direction")[0].expression() ?: return@l null,
                                        macros
                                    ),
                                    engine.compile(
                                        it.getAsJsonArray("direction")[1].expression() ?: return@l null,
                                        macros
                                    ),
                                    engine.compile(
                                        it.getAsJsonArray("direction")[2].expression() ?: return@l null,
                                        macros
                                    ),
                                    particleData, emitterData
                                ), engine.compile(it.getAsJsonPrimitive("spread").expression() ?: return@l null, macros)
                            )
                        } else {
                            Pair(
                                ExpressionDirectionSubcomponent(
                                    engine.compile(
                                        dirObject.asJsonObject.getAsJsonPrimitive("x").expression() ?: return@l null,
                                        macros
                                    ),
                                    engine.compile(
                                        dirObject.asJsonObject.getAsJsonPrimitive("y").expression() ?: return@l null,
                                        macros
                                    ),
                                    engine.compile(
                                        dirObject.asJsonObject.getAsJsonPrimitive("z").expression() ?: return@l null,
                                        macros
                                    ),
                                    particleData, emitterData
                                ), engine.compile(it.getAsJsonPrimitive("spread").expression() ?: return@l null, macros)
                            )
                        }
                    }

                velocityComponent = RandomDirectionSubcomponent(
                    engine.compile(velocityObject.expression("magnitude")!!, macros),
                    directionPair,
                    particleData, emitterData
                )
            }

            val accelerationObject =
                if (jsonObject.has("acceleration")) jsonObject.getAsJsonObject("acceleration") else null
            val accelerationScript: Triple<CompiledScript, CompiledScript, CompiledScript> = Triple(
                engine.compile(accelerationObject?.expression("x") ?: "0", macros),
                engine.compile(accelerationObject?.expression("y") ?: "0", macros),
                engine.compile(accelerationObject?.expression("z") ?: "0", macros)
            )

            val actions = jsonObject.getAsJsonArray("on_collision")?.let { Action.parse(it, macros) }

            return MotionPositionComponent(
                velocityComponent ?: return null,
                accelerationScript,
                jsonObject.expression("drag")?.let { engine.compile(it, macros) },
                jsonObject.expression("restitution")?.let { engine.compile(it, macros) },
                actions,
                jsonObject.expression("on_collision_emitter"),
                emitterData, particleData
            )
        }
    }

}

class IntersectionResult(val direction: Int, val distance: Double, val intersection: Vector3d)
class CorrectionResult(val vector: Vector3d, val direction: Vector3d)