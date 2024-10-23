package com.ixume.particleemitter.particle.position

import com.google.gson.JsonElement
import com.ixume.particleemitter.UnrealizedComponent
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import com.ixume.particleemitter.particle.position.direction.DirectionSubcomponent
import com.ixume.particleemitter.particle.position.direction.UnrealizedExpressionDirectionSubcomponent
import com.ixume.particleemitter.particle.position.direction.UnrealizedRandomDirectionSubcomponent
import org.bukkit.World
import org.bukkit.block.Block
import org.joml.Vector3d
import org.joml.Vector3i
import javax.script.CompiledScript
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sign

class MotionPositionComponent(
    private val initialVelocityComponent: DirectionSubcomponent,
    private val accelerationScript: Triple<CompiledScript, CompiledScript, CompiledScript>,
    private val dragScript: CompiledScript,
    private val restitutionScript: CompiledScript?,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : PositionComponent {
    companion object : ComponentParser<MotionPositionComponent> {
        init {
            ParticleJsonParser.positionComponentParsers += "motion_position" to this
        }

        override fun parse(
            jsonElement: JsonElement,
            macros: Map<String, Macro>?
        ): UnrealizedComponent<MotionPositionComponent>? {
            val jsonObject = jsonElement.asJsonObject

            var velocityComponent: UnrealizedComponent<out DirectionSubcomponent>? = null

            if ("initial_velocity" in jsonObject.keySet()) {
                val velocityObject = jsonObject.getAsJsonObject("initial_velocity") ?: return null
                velocityComponent = UnrealizedExpressionDirectionSubcomponent(
                    velocityObject.expression("x") ?: return null,
                    velocityObject.expression("y") ?: return null,
                    velocityObject.expression("z") ?: return null,
                    macros
                )
            } else if ("random_velocity" in jsonObject.keySet()) {
                val velocityObject = jsonObject.getAsJsonObject("random_velocity") ?: return null
                val directionPair: Pair<UnrealizedComponent<out DirectionSubcomponent>, String>? =
                    velocityObject.getAsJsonObject("bias")?.let l@{
                        Pair(
                            UnrealizedExpressionDirectionSubcomponent(
                                it.getAsJsonArray("direction")[0].expression() ?: return@l null,
                                it.getAsJsonArray("direction")[1].expression() ?: return@l null,
                                it.getAsJsonArray("direction")[2].expression() ?: return@l null,
                                macros
                            ), it.getAsJsonPrimitive("spread").expression() ?: return@l null
                        )
                    }

                velocityComponent = UnrealizedRandomDirectionSubcomponent(
                    velocityObject.expression("magnitude"),
                    directionPair,
                    macros
                )
            }

            val accelerationObject = jsonObject.getAsJsonObject("acceleration") ?: return null
            val accelerationScript: Triple<String, String, String> = Triple(
                accelerationObject.expression("x") ?: return null,
                accelerationObject.expression("y") ?: return null,
                accelerationObject.expression("z") ?: return null,
            )
            return UnrealizedMotionPositionComponent(
                velocityComponent ?: return null,
                accelerationScript,
                jsonObject.expression("drag") ?: return null,
                jsonObject.expression("restitution"),
                macros
            )
        }
    }

    override fun pos(otherParticleData: ParticleData): Vector3d {
        myParticleData.copyFrom(otherParticleData)
        if (otherParticleData.age == 0.0) {
            return initialVelocityComponent.dir(otherParticleData).rotate(myEmitterData.rotation)
        }

        val dragCoefficient = dragScript.eval() as Double
        val acceleration = Vector3d(
            accelerationScript.first.eval() as Double,
            accelerationScript.second.eval() as Double,
            accelerationScript.third.eval() as Double
        )

        val correctionVector = fixCollisions(acceleration)

        val updatedPos = Vector3d(otherParticleData.relativePosition).add(correctionVector)

        val velocity = Vector3d(updatedPos).sub(otherParticleData.oldRelativePosition)

        velocity.mul(1.0 - dragCoefficient)
        acceleration.mul(1.0 - dragCoefficient)

        otherParticleData.oldRelativePosition = Vector3d(updatedPos)

        return Vector3d(updatedPos).add(velocity).add(acceleration)
    }

    private fun fixCollisions(acceleration: Vector3d): Vector3d {
        if (restitutionScript != null) {
            val oldPoint = Vector3d(myParticleData.origin).add(myParticleData.oldRelativePosition)
            val absolutePos = Vector3d(myParticleData.origin).add(myParticleData.relativePosition)
            val oldVelocity = Vector3d(absolutePos).sub(oldPoint)
            val block = blockAt(myEmitterData.world!!, oldPoint)

            val restitution = restitutionScript.eval() as Double

            val intersection = block.intersect(oldPoint, oldVelocity)
            if (intersection != null) {
                val correctionVector = Vector3d(intersection.intersection).sub(absolutePos)
                when (intersection.direction) {
                    0 -> acceleration.add(Vector3d(oldVelocity).mul(-restitution, restitution, restitution))
                    1 -> acceleration.add(Vector3d(oldVelocity).mul(restitution, -restitution, restitution))
                    else -> acceleration.add(Vector3d(oldVelocity).mul(restitution, restitution, -restitution))
                }

                acceleration.add(Vector3d(oldVelocity).add(correctionVector).mul(-1.0))

                return correctionVector
            }

            val maxOffset =
                (abs(oldVelocity.x) + 1.0) * (abs(oldVelocity.x) + 1.0) + (abs(oldVelocity.y) + 1.0) * (abs(oldVelocity.y) + 1.0) + (abs(
                    oldVelocity.z
                ) + 1.0) * (abs(oldVelocity.z) + 1.0)

            //block-plane offset
            val offset = Vector3i()
            //only ever have 3 intersections at any given time, 1 for each axis
            while (offset.lengthSquared() <= maxOffset) {
                val yzPlane: Int = (floor(oldPoint.x) + 0.5 + (offset.x + 0.5) * sign(oldVelocity.x)).toInt()
                var yzDistance = (yzPlane.toDouble() - oldPoint.x) / oldVelocity.x
                if (yzDistance < 0.0) {
                    yzDistance = Double.MAX_VALUE
                }

                val xzPlane: Int = (floor(oldPoint.y) + 0.5 + (offset.y + 0.5) * sign(oldVelocity.y)).toInt()
                var xzDistance = (xzPlane.toDouble() - oldPoint.y) / oldVelocity.y
                if (xzDistance < 0.0) {
                    xzDistance = Double.MAX_VALUE
                }

                val xyPlane: Int = (floor(oldPoint.z) + 0.5 + (offset.z + 0.5) * sign(oldVelocity.z)).toInt()
                var xyDistance = (xyPlane.toDouble() - oldPoint.z) / oldVelocity.z
                if (xyDistance < 0.0) {
                    xyDistance = Double.MAX_VALUE
                }

                if (min(min(yzDistance, xzDistance), xyDistance) > 1.0) {
                    return Vector3d()
                }

                val index = min(yzDistance, xzDistance, xyDistance)
                when (index) {
                    //yzDistance is smallest
                    0 -> {
                        val yzIntersection = Vector3d(oldPoint).add(Vector3d(oldVelocity).mul(yzDistance))
                        val yzBlock = myEmitterData.world!!.getBlockAt(
                            (yzPlane - 0.5 + 0.5 * sign(oldVelocity.x)).toInt(),
                            floor(yzIntersection.y).toInt(),
                            floor(yzIntersection.z).toInt()
                        )
                        if (yzBlock.isCollidable) {
                            val result = yzBlock.intersect(oldPoint, oldVelocity)
                            if (result == null) {
                                offset.x++
                                continue
                            }

                            val correctionVector = Vector3d(result.intersection).sub(absolutePos)
                            acceleration.add(Vector3d(oldVelocity).mul(-restitution, restitution, restitution))
                            acceleration.add(Vector3d(oldVelocity).add(correctionVector).mul(-1.0))
                            return correctionVector
                        } else {
                            //not done...
                            offset.x++
                            continue
                        }
                    }

                    1 -> {
                        //xzDistance is smallest
                        val xzIntersection = Vector3d(oldPoint).add(Vector3d(oldVelocity).mul(xzDistance))
                        val xzBlock = myEmitterData.world!!.getBlockAt(
                            floor(xzIntersection.x).toInt(),
                            (xzPlane - 0.5 + 0.5 * sign(oldVelocity.y)).toInt(),
                            floor(xzIntersection.z).toInt()
                        )

                        if (xzBlock.isCollidable) {
                            val result = xzBlock.intersect(oldPoint, oldVelocity)
                            if (result == null) {
                                offset.y++
                                continue
                            }

                            val correctionVector = Vector3d(result.intersection).sub(absolutePos)
                            acceleration.add(Vector3d(oldVelocity).mul(restitution, -restitution, restitution))
                            acceleration.add(Vector3d(oldVelocity).add(correctionVector).mul(-1.0))
                            return correctionVector
                        } else {
                            //not done...
                            offset.y++
                            continue
                        }
                    }

                    2 -> {
                        //xyDistance is smallest
                        val xyIntersection = Vector3d(oldPoint).add(Vector3d(oldVelocity).mul(xyDistance))
                        val xyBlock = myEmitterData.world!!.getBlockAt(
                            floor(xyIntersection.x).toInt(),
                            floor(xyIntersection.y).toInt(),
                            (xyPlane - 0.5 + 0.5 * sign(oldVelocity.z)).toInt()
                        )
                        if (xyBlock.isCollidable) {
                            val result = xyBlock.intersect(oldPoint, oldVelocity)
                            if (result == null) {
                                offset.z++
                                continue
                            }

                            val correctionVector = Vector3d(result.intersection).sub(absolutePos)
                            acceleration.add(Vector3d(oldVelocity).mul(restitution, restitution, -restitution))
                            acceleration.add(Vector3d(oldVelocity).add(correctionVector).mul(-1.0))
                            return correctionVector
                        } else {
                            //not done...
                            offset.z++
                            continue
                        }
                    }
                }
            }
        }

        return Vector3d()
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
                0 -> IntersectionResult(0, yzDistance, yzIntersection/*.add(.00000 * sign(velocity.x), 0.0, 0.0)*/)
                1 -> IntersectionResult(1, xzDistance, xzIntersection/*.add(0.0, -0.00000 * sign(velocity.y), 0.0)*/)
                else -> IntersectionResult(2, xyDistance, xyIntersection/*.add(0.0, 0.0, -0.00000 * sign(velocity.z))*/)
            }

            closestResult = closestResult?.let { if (result.distance < it.distance) result else it} ?: result
        }

        return closestResult
    }
}

class IntersectionResult(val direction: Int, val distance: Double, val intersection: Vector3d)

class UnrealizedMotionPositionComponent(
    private val initialVelocityComponent: UnrealizedComponent<out DirectionSubcomponent>,
    private val acceleration: Triple<String, String, String>,
    private val dragScript: String,
    private val restitutionScript: String?,
    private val macros: Map<String, Macro>?
) : UnrealizedComponent<MotionPositionComponent> {
    override fun realizeComponent(emitterData: EmitterData): MotionPositionComponent {
        val (engine, particleData) = particleEngine(emitterData)
        return MotionPositionComponent(
            initialVelocityComponent.realizeComponent(emitterData),
            Triple(
                engine.compile(acceleration.first, macros),
                engine.compile(acceleration.second, macros),
                engine.compile(acceleration.third, macros)
            ),
            engine.compile(dragScript, macros),
            restitutionScript?.let { engine.compile(it, macros) },
            emitterData,
            particleData
        )
    }
}