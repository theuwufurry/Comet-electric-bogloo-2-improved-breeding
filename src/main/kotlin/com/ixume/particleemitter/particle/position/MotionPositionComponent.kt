package com.ixume.particleemitter.particle.position

import com.google.gson.JsonElement
import com.ixume.particleemitter.emitter.EmitterData
import com.ixume.particleemitter.parsing.*
import com.ixume.particleemitter.parsing.macro.Macro
import com.ixume.particleemitter.particle.ParticleData
import com.ixume.particleemitter.particle.position.direction.DirectionSubcomponent
import com.ixume.particleemitter.particle.position.direction.ExpressionDirectionSubcomponent
import com.ixume.particleemitter.particle.position.direction.RandomDirectionSubcomponent
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.joml.Vector3d
import org.joml.Vector3i
import javax.script.CompiledScript
import kotlin.math.floor
import kotlin.math.sign

class MotionPositionComponent(
    private val initialVelocityComponent: DirectionSubcomponent,
    private val accelerationScript: Triple<CompiledScript, CompiledScript, CompiledScript>,
    private val dragScript: CompiledScript,
    private val restitutionScript: CompiledScript?,
    private val myEmitterData: EmitterData,
    private val myParticleData: ParticleData
) : PositionComponent {
    companion object : ComponentParser<PositionComponent> {
        init {
            ParticleJsonParser.positionComponentParsers += "motion_position" to this
        }

        override fun parse(jsonElement: JsonElement, macros: Map<String, Macro>?): PositionComponent? {
            val (engine, emitterData, particleData) = particleEngine()
            val jsonObject = jsonElement.asJsonObject

            var velocityComponent: DirectionSubcomponent? = null

            if ("initial_velocity" in jsonObject.keySet()) {
                val velocityObject = jsonObject.getAsJsonObject("initial_velocity") ?: return null
                velocityComponent = ExpressionDirectionSubcomponent(
                    engine.compile(velocityObject.expression("x") ?: return null, macros),
                    engine.compile(velocityObject.expression("y") ?: return null, macros),
                    engine.compile(velocityObject.expression("z") ?: return null, macros),
                    emitterData,
                    particleData
                )
            } else if ("random_velocity" in jsonObject.keySet()) {
                val velocityObject = jsonObject.getAsJsonObject("random_velocity") ?: return null
                val directionPair: Pair<DirectionSubcomponent, CompiledScript>? =
                    velocityObject.getAsJsonObject("bias")?.let l@{
                        Pair(
                            ExpressionDirectionSubcomponent(
                                engine.compile(it.getAsJsonArray("direction")[0].expression() ?: return@l null, macros),
                                engine.compile(it.getAsJsonArray("direction")[1].expression() ?: return@l null, macros),
                                engine.compile(it.getAsJsonArray("direction")[2].expression() ?: return@l null, macros),
                                emitterData,
                                particleData
                            ), engine.compile(it.getAsJsonPrimitive("spread").expression() ?: return@l null, macros)
                        )
                    }

                velocityComponent = RandomDirectionSubcomponent(
                    velocityObject.expression("magnitude")?.let { engine.compile(it, macros) },
                    directionPair,
                    emitterData,
                    particleData
                )
            }

            val accelerationObject = jsonObject.getAsJsonObject("acceleration") ?: return null
            val accelerationScript: Triple<CompiledScript, CompiledScript, CompiledScript> = Triple(
                engine.compile(accelerationObject.expression("x") ?: return null, macros),
                engine.compile(accelerationObject.expression("y") ?: return null, macros),
                engine.compile(accelerationObject.expression("z") ?: return null, macros)
            )
            return MotionPositionComponent(
                velocityComponent ?: return null,
                accelerationScript,
                engine.compile(jsonObject.expression("drag") ?: return null, macros),
                jsonObject.expression("restitution")?.let{ engine.compile(it, macros) },
                emitterData,
                particleData
            )
        }
    }

    override fun pos(otherEmitterData: EmitterData, otherParticleData: ParticleData): Vector3d {
        myEmitterData.copyFrom(otherEmitterData)
        myParticleData.copyFrom(otherParticleData)
        if (otherParticleData.age == 0.0) {
            return initialVelocityComponent.dir()
        }

        val dragCoefficient = dragScript.eval() as Double
        val acceleration = Vector3d(accelerationScript.first.eval() as Double, accelerationScript.second.eval() as Double, accelerationScript.third.eval() as Double)

        val correctionVector = Vector3d()
        if (restitutionScript != null) {
            val restitution = restitutionScript.eval() as Double
            val absolutePos = Vector3d(otherParticleData.origin).add(otherParticleData.relativePosition)
            val oldPoint = Vector3d(otherParticleData.origin).add(otherParticleData.oldRelativePosition)

            val oldVelocity = Vector3d(absolutePos).sub(oldPoint)
            val maxOffset =
                (oldVelocity.x + 1.0) * (oldVelocity.x + 1.0) + (oldVelocity.y + 1.0) * (oldVelocity.y + 1.0) + (oldVelocity.z + 1.0) * (oldVelocity.z + 1.0)
            if (blockAt(otherEmitterData.world!!, absolutePos).type != Material.AIR) {
                //block-plane offset
                val offset = Vector3i()
                //only ever have 3 intersections at any given time, 1 for each axis
                while (offset.lengthSquared() <= maxOffset) {
                    val yzPlane: Int = (floor(oldPoint.x) + 0.5 + (offset.x + 0.5) * sign(oldVelocity.x)).toInt()
                    val yzDistance = (yzPlane.toDouble() - oldPoint.x) / oldVelocity.x

                    val xzPlane: Int = (floor(oldPoint.y) + 0.5 + (offset.y + 0.5) * sign(oldVelocity.y)).toInt()
                    val xzDistance = (xzPlane.toDouble() - oldPoint.y) / oldVelocity.y

                    val xyPlane: Int = (floor(oldPoint.z) + 0.5 + (offset.z + 0.5) * sign(oldVelocity.z)).toInt()
                    val xyDistance = (xyPlane.toDouble() - oldPoint.z) / oldVelocity.z

                    if (yzDistance < xzDistance) {
                        if (yzDistance < xyDistance) {
                            //yzDistance is smallest
                            val yzIntersection = Vector3d(oldPoint).add(Vector3d(oldVelocity).mul(yzDistance))
                            if (otherEmitterData.world!!.getBlockAt(
                                    (yzPlane - 0.5 + 0.5 * sign(oldVelocity.x)).toInt(),
                                    floor(yzIntersection.y).toInt(),
                                    floor(yzIntersection.z).toInt()
                                ).type != Material.AIR
                            ) {
                                //done!
                                val correction = yzPlane - absolutePos.x - 0.00001 * sign(oldVelocity.x)
                                correctionVector
                                    .add(
                                        Vector3d(
                                            correction,
                                            yzIntersection.y - absolutePos.y,
                                            yzIntersection.z - absolutePos.z
                                        )
                                    )
                                acceleration.add(Vector3d(oldVelocity).mul(-restitution, restitution, restitution))
                                //remove into-bounce velocity
                                acceleration.add(Vector3d(oldVelocity).add(correctionVector).mul(-1.0))
                                break
                            } else {
                                //not done...
                                offset.x++
                                continue
                            }
                        } else {
                            //xyDistance is smallest
                            val xyIntersection = Vector3d(oldPoint).add(Vector3d(oldVelocity).mul(xyDistance))
                            if (otherEmitterData.world!!.getBlockAt(
                                    floor(xyIntersection.x).toInt(),
                                    floor(xyIntersection.y).toInt(),
                                    (xyPlane - 0.5 + 0.5 * sign(oldVelocity.z)).toInt()
                                ).type != Material.AIR
                            ) {
                                //done!
                                val correction = xyPlane - absolutePos.z - 0.00001 * sign(oldVelocity.z)
                                correctionVector
                                    .add(
                                        Vector3d(
                                            xyIntersection.x - absolutePos.x,
                                            xyIntersection.y - absolutePos.y,
                                            correction
                                        )
                                    )
                                acceleration.add(Vector3d(oldVelocity).mul(restitution, restitution, -restitution))
                                acceleration.add(Vector3d(oldVelocity).add(correctionVector).mul(-1.0))
                                break
                            } else {
                                //not done...
                                offset.z++
                                continue
                            }
                        }
                    } else {
                        if (xzDistance < xyDistance) {
                            //xzDistance is smallest
                            val xzIntersection = Vector3d(oldPoint).add(Vector3d(oldVelocity).mul(xzDistance))
                            if (otherEmitterData.world!!.getBlockAt(
                                    floor(xzIntersection.x).toInt(),
                                    (xzPlane - 0.5 + 0.5 * sign(oldVelocity.y)).toInt(),
                                    floor(xzIntersection.z).toInt()
                                ).type != Material.AIR
                            ) {
                                //done!
                                val correction = xzPlane - absolutePos.y - 0.00001 * sign(oldVelocity.y)
                                correctionVector
                                    .add(
                                        Vector3d(
                                            xzIntersection.x - absolutePos.x,
                                            correction,
                                            xzIntersection.z - absolutePos.z
                                        )
                                    )
                                acceleration.add(Vector3d(oldVelocity).mul(restitution, -restitution, restitution))
                                acceleration.add(Vector3d(oldVelocity).add(correctionVector).mul(-1.0))
                                break
                            } else {
                                //not done...
                                offset.y++
                                continue
                            }
                        } else {
                            //xyDistance is smallest
                            val xyIntersection = Vector3d(oldPoint).add(Vector3d(oldVelocity).mul(xyDistance))
                            if (otherEmitterData.world!!.getBlockAt(
                                    floor(xyIntersection.x).toInt(),
                                    floor(xyIntersection.y).toInt(),
                                    (xyPlane - 0.5 + 0.5 * sign(oldVelocity.z)).toInt()
                                ).type != Material.AIR
                            ) {
                                //done!
                                val correction = xyPlane - absolutePos.z - 0.00001 * sign(oldVelocity.z)
                                correctionVector
                                    .add(
                                        Vector3d(
                                            xyIntersection.x - absolutePos.x,
                                            xyIntersection.y - absolutePos.y,
                                            correction
                                        )
                                    )
                                acceleration.add(Vector3d(oldVelocity).mul(restitution, restitution, -restitution))
                                acceleration.add(Vector3d(oldVelocity).add(correctionVector).mul(-1.0))
                                break
                            } else {
                                //not done...
                                offset.z++
                                continue
                            }
                        }
                    }
                }
            }
        }

        val updatedPos = Vector3d(otherParticleData.relativePosition).add(correctionVector)
        val velocity = Vector3d(updatedPos).sub(otherParticleData.oldRelativePosition)
        velocity.mul(1.0 - dragCoefficient)

        otherParticleData.oldRelativePosition = Vector3d(updatedPos)

        return Vector3d(updatedPos).add(velocity).add(acceleration)
    }

    private fun blockAt(world: World, vector3d: Vector3d): Block {
        return world.getBlockAt(
            floor(vector3d.x).toInt(),
            floor(vector3d.y).toInt(),
            floor(vector3d.z).toInt()
        )
    }
}