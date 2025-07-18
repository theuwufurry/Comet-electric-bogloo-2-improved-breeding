package gg.aquatic.comet.command

import gg.aquatic.comet.command.PhysicsCommand.DEBUG_FREQUENCY
import gg.aquatic.comet.command.PhysicsCommand.DEBUG_LEVEL
import gg.aquatic.comet.command.PhysicsCommand.DEBUG_MESH_LEVEL
import gg.aquatic.comet.command.PhysicsCommand.enhancement
import gg.aquatic.comet.command.PhysicsCommand.frozen
import gg.aquatic.comet.command.PhysicsCommand.steps
import gg.aquatic.comet.command.PhysicsCommand.untilCollision
import gg.aquatic.comet.command.physics.*
import gg.aquatic.comet.command.physics.ActiveBody.Companion.TIME_STEP
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.joml.Vector3d
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.random.Random

class PhysicsWorld(
    val world: World
) {
    val bodies: MutableList<ActiveBody> = mutableListOf()
    var contacts: MutableList<Contact> = mutableListOf()
    var meshes: MutableList<Mesh> = mutableListOf()

    private var time = 0

    fun tick() {
        repeat((0.05 / TIME_STEP).roundToInt()) {
            var doTick = true
            if (frozen) {
                if (!untilCollision && --steps < 0) doTick = false
            }

            if (doTick) {
                contacts.clear()
                meshes.clear()

                for (i in 0..<bodies.size) {
                    val first = bodies[i]
                    first.ensureNonAligned()
                    val firstBoundingBox = first.boundingBox
                    if (bodies.size > 1) {
                        for (j in (i + 1)..<bodies.size) {
                            val second = bodies[j]

                            if (!firstBoundingBox.overlaps(second.boundingBox)) continue

                            val result = first.collidesBody(second) ?: continue
                            val contact = Contact(second, first, result)
//
                            val ourContacts = first.previousContacts.filter { it.first == second || it.second == second }

                            first.contacts += contact
                            second.contacts += contact

                            for (ourContact in ourContacts) {
                                if (ourContact.result.point.distance(result.point) < 1e-2) {
                                    contact.lambdaSum += ourContact.lambdaSum * LAMBDA_CARRYOVER

                                    break
                                }
                            }

                            contacts += contact
                        }
                    }

                    val mesh = MeshBody(world)
                    val result = first.collidesEnvironment()

                    for (r in result) {
                        val c = Contact(first, mesh, r)

                        val ourContacts = first.previousContacts.filter { it.first.type == BodyType.PASSIVE || it.second.type == BodyType.PASSIVE }

                        for (ourContact in ourContacts) {
                            if (ourContact.result.point.distance(r.point) < 1e-2) {
                                c.lambdaSum += ourContact.lambdaSum * LAMBDA_CARRYOVER

                                break

                            }
                        }

                        first.contacts += c
                        contacts += c
                    }
                }

                for (body in bodies) {
                    if (body.hasGravity) body.velocity.add(Vector3d(GRAVITY).mul(TIME_STEP))
                }

                ContactsSolver.solve(contacts)

                for (body in bodies) {
                    body.step()
                }

                if (untilCollision && contacts.isNotEmpty()) {
                    untilCollision = false
                    frozen = true
                }
            }

            if (time % DEBUG_FREQUENCY == 0) {
                for (body in bodies) {
                    body.visualize()
                }

                for (contact in contacts) {
                    val (point,
                        norm,
                        _,
                        minkowski,
                        closest,
                        originals) = contact.result

                    world.debugConnect(
                        point,
                        Vector3d(point).add(Vector3d(norm).mul(0.5)),
                        Particle.DustOptions(Color.BLUE, 0.15f)
                    )

                    world.spawnParticle(
                        Particle.REDSTONE,
                        Location(
                            world,
                            point.x, point.y, point.z,
                        ),
                        1, Particle.DustOptions(Color.RED, 0.3f)
                    )


//                        world.debugConnect(
//                            point,
//                            Vector3d(point).add(contact.t1),
//                            DustOptions(Color.YELLOW, 0.2f)
//                        )
//
//                        world.spawnParticle(
//                            Particle.REDSTONE,
//                            Location(
//                                world,
//                                point.x, point.y, point.z,
//                            ),
//                            1, DustOptions(Color.FUCHSIA, 0.4f)
//                        )
//
//                        world.debugConnect(
//                            point,
//                            Vector3d(point).add(contact.t2),
//                            DustOptions(Color.YELLOW, 0.2f)
//                        )
//
//                        world.spawnParticle(
//                            Particle.REDSTONE,
//                            Location(
//                                world,
//                                point.x, point.y, point.z,
//                            ),
//                            1, DustOptions(Color.FUCHSIA, 0.4f)
//                        )


                    val minkowskiDebugOrigin = Vector3d(point).add(0.0, 3.0, 0.0)
                    val vertices = mutableSetOf<Vector3d>()
                    if (minkowski != null) {
                        originals!!
                        closest!!
                        for ((a, b, c) in minkowski) {
                            vertices += a
                            vertices += b
                            vertices += c

                            if (DEBUG_LEVEL > 2) {
                                val color =
                                    choices[Vector3d(a).add(b).add(c).hashCode().absoluteValue % (choices.size)]

                                world.debugConnect(
                                    Vector3d(minkowskiDebugOrigin).add(Vector3d(a).mul(enhancement)),
                                    Vector3d(minkowskiDebugOrigin).add(Vector3d(b).mul(enhancement)),
                                    Particle.DustOptions(Color.BLACK, 0.2f)
                                )

                                world.debugConnect(
                                    Vector3d(minkowskiDebugOrigin).add(Vector3d(a).mul(enhancement)),
                                    Vector3d(minkowskiDebugOrigin).add(Vector3d(c).mul(enhancement)),
                                    Particle.DustOptions(Color.BLACK, 0.2f)
                                )

                                world.debugConnect(
                                    Vector3d(minkowskiDebugOrigin).add(Vector3d(c).mul(enhancement)),
                                    Vector3d(minkowskiDebugOrigin).add(Vector3d(b).mul(enhancement)),
                                    Particle.DustOptions(Color.BLACK, 0.2f)
                                )

                                repeat(50) {
                                    var cA = Random.nextDouble()
                                    var cB = Random.nextDouble()
                                    var cC = Random.nextDouble()

                                    val t = cA + cB + cC

                                    cA /= t
                                    cB /= t
                                    cC /= t

                                    val bp =
                                        Vector3d(a).mul(cA).add(Vector3d(b).mul(cB)).add(Vector3d(c).mul(cC))

                                    world.spawnParticle(
                                        Particle.REDSTONE,
                                        Location(
                                            world,
                                            minkowskiDebugOrigin.x + bp.x * enhancement.x,
                                            minkowskiDebugOrigin.y + bp.y * enhancement.y,
                                            minkowskiDebugOrigin.z + bp.z * enhancement.z,
                                        ),
                                        1,
                                        Particle.DustOptions(color, 0.2f)
                                    )
                                }
                            }
                        }

                        for (vertex in vertices) {
                            val (start, end) = originals[vertex]!!

                            val newPos = Vector3d(minkowskiDebugOrigin).add(vertex)

                            if (DEBUG_LEVEL > 2) {
                                world.debugConnect(
                                    start,
                                    end,
                                    Particle.DustOptions(Color.BLUE, 0.1f)
                                )

                                world.debugConnect(
                                    newPos,
                                    end,
                                    Particle.DustOptions(Color.YELLOW, 0.1f)
                                )

                                world.debugConnect(
                                    start,
                                    newPos,
                                    Particle.DustOptions(Color.YELLOW, 0.1f)
                                )

                                world.spawnParticle(
                                    Particle.REDSTONE,
                                    Location(
                                        world,
                                        newPos.x,
                                        newPos.y,
                                        newPos.z,
                                    ),
                                    1, Particle.DustOptions(Color.WHITE, 0.5f)
                                )
                            }

                            if (DEBUG_LEVEL > 2) {
                                world.spawnParticle(
                                    Particle.REDSTONE,
                                    Location(
                                        world,
                                        minkowskiDebugOrigin.x,
                                        minkowskiDebugOrigin.y,
                                        minkowskiDebugOrigin.z,
                                    ),
                                    1, Particle.DustOptions(Color.ORANGE, 0.5f)
                                )

                                world.spawnParticle(
                                    Particle.REDSTONE,
                                    Location(
                                        world,
                                        minkowskiDebugOrigin.x + closest.first.x,
                                        minkowskiDebugOrigin.y + closest.first.y,
                                        minkowskiDebugOrigin.z + closest.first.z,
                                    ),
                                    1, Particle.DustOptions(Color.RED, 0.5f)
                                )

                                world.spawnParticle(
                                    Particle.REDSTONE,
                                    Location(
                                        world,
                                        minkowskiDebugOrigin.x + closest.second.x,
                                        minkowskiDebugOrigin.y + closest.second.y,
                                        minkowskiDebugOrigin.z + closest.second.z,
                                    ),
                                    1, Particle.DustOptions(Color.RED, 0.5f)
                                )

                                world.spawnParticle(
                                    Particle.REDSTONE,
                                    Location(
                                        world,
                                        minkowskiDebugOrigin.x + closest.third.x,
                                        minkowskiDebugOrigin.y + closest.third.y,
                                        minkowskiDebugOrigin.z + closest.third.z,
                                    ),
                                    1, Particle.DustOptions(Color.RED, 0.5f)
                                )
                            }
                        }
                    }
                }
            }

//                    if (DEBUG_LEVEL > 0) {
//                        for (body in bodies) {
//                            val boundingBox = body.boundingBox
//
//                            if (DEBUG_LEVEL > 1) {
//                                val blocks = boundingBox.overlappingBlocks(world)
//                                for (block in blocks) {
//                                    world.debugBoundingBox(block.boundingBox, DustOptions(Color.RED, 0.4f), 0.24)
//                                }
//                            }
//
//                            world.debugBoundingBox(boundingBox, DustOptions(Color.BLUE, 0.4f))
//                        }
//                    }

            if (DEBUG_MESH_LEVEL > 2) {
                PhysicsListener.mesh?.visualize(world, visualizeFaces = false, visualizeEdges = true)
                for (mesh in meshes) {
                    mesh.visualize(world, visualizeFaces = false, visualizeEdges = true)
                }
            }
        }
    }

    companion object {
        val GRAVITY = Vector3d(0.0, -5.0, 0.0)
        const val BIAS = 0.15
        const val PASSIVE_SLOP = 0.0001
        const val ACTIVE_SLOP = 0.001
        const val FRICTION = 0.3
        const val LAMBDA_CARRYOVER = 0.3
    }
}