package gg.aquatic.comet.command.physics

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.command.PhysicsCommand
import gg.aquatic.comet.command.debugConnect
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.scheduler.BukkitTask
import org.joml.Vector3d
import org.joml.Vector3i
import kotlin.math.max
import kotlin.math.min

object PhysicsListener : Listener {
    private lateinit var graphicsTask: BukkitTask
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, AbstractParticleEmitter.INSTANCE)
        graphicsTask = Bukkit.getScheduler().runTaskTimer(AbstractParticleEmitter.INSTANCE, Runnable {

        }, 2, 2)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val start = event.player.eyeLocation.toVector().toVector3d()
        val dir = event.player.location.direction.toVector3d().mul(20.0)
        val end = Vector3d(start).add(dir)

        val allIntersections = mutableListOf<Triple<Body, Vector3d, Vector3d>>()
        for (body in PhysicsCommand.globalBodies[event.player.world] ?: return) {
            allIntersections += body.intersect(start, end).map { Triple(body, it.first, it.second) }
        }

        val (body, intersection, normal) = allIntersections.minByOrNull { (_, inter, _) -> inter.distanceSquared(start) }
            ?: return

        event.player.world.spawnParticle(
            Particle.REDSTONE, Location(
                event.player.world,
                intersection.x,
                intersection.y,
                intersection.z,
            ),
            5, Particle.DustOptions(Color.YELLOW, 0.5f)
        )

        event.player.world.debugConnect(
            Vector3d(
                intersection.x,
                intersection.y,
                intersection.z,
            ),
            Vector3d(
                intersection.x + normal.x,
                intersection.y + normal.y,
                intersection.z + normal.z,
            ), Particle.DustOptions(Color.BLUE, 0.25f)
        )

        val type = event.player.inventory.itemInMainHand.type
        if (type == Material.END_ROD) {
            body.applyImpulse(intersection, normal, Vector3d(dir).normalize(2.5))
        }
    }

    private var meshRange: Pair<Location, Location?>? = null

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand
        if (item.type == Material.GOLD_NUGGET) {
            event.isCancelled = true
            meshRange = if (meshRange != null && meshRange!!.second == null) {
                mesh(
                    event.block.world,
                    Vector3i(
                        min(meshRange!!.first.blockX, event.block.location.blockX),
                        min(meshRange!!.first.blockY, event.block.location.blockY),
                        min(meshRange!!.first.blockZ, event.block.location.blockZ),
                    ),
                    Vector3i(
                        max(meshRange!!.first.blockX, event.block.location.blockX),
                        max(meshRange!!.first.blockY, event.block.location.blockY),
                        max(meshRange!!.first.blockZ, event.block.location.blockZ),
                    )
                )

                meshRange!!.first to event.block.location
            } else {
                event.block.location to null
            }
        }
    }

    var meshBlocks = listOf<Block>()
    var boundedFaces = listOf<BoundedAAFace>()
    var boundedEdges = setOf<Edge>()

    /**
     * Generate list of bounded faces and edges
     */
    private fun mesh(
        world: World,
        meshStart: Vector3i,
        meshEnd: Vector3i,
    ): Mesh {
        require(meshStart.x <= meshEnd.x && meshStart.y <= meshEnd.y && meshStart.z <= meshEnd.z)
        val xDim = meshEnd.x - meshStart.x + 2 + 2
        val yDim = meshEnd.y - meshStart.y + 2 + 2
        val zDim = meshEnd.z - meshStart.z + 2 + 2
        //(relative)[x][y][z]
        val blocks: List<List<List<Block>>> = List(xDim) { relX ->
            List(yDim) { relY ->
                List(zDim) { relZ ->
                    world.getBlockAt(
                        meshStart.x - 2 + relX,
                        meshStart.y - 2 + relY,
                        meshStart.z - 2 + relZ,
                    )
                }
            }
        }

        meshBlocks = (blocks.flatMap { yz: List<List<Block?>> -> yz.flatten() } as List<Block>)

        val xFaces = List(meshEnd.x - meshStart.x + 3) { relX ->
            BoundedAAFace(
                axis = Axis.X,
                start = Vector3i(
                    meshStart.x + relX - 1,
                    meshStart.y,
                    meshStart.z
                ),
                end = Vector3i(
                    meshStart.x + relX - 1,
                    meshEnd.y,
                    meshEnd.z
                ),
                holes = mutableListOf(),
            )
        }

        val yFaces = List(meshEnd.y - meshStart.y + 3) { relY ->
            BoundedAAFace(
                axis = Axis.Y,
                start = Vector3i(
                    meshStart.x,
                    meshStart.y + relY - 1,
                    meshStart.z
                ),
                end = Vector3i(
                    meshEnd.x,
                    meshStart.y + relY - 1,
                    meshEnd.z
                ),
                holes = mutableListOf(),
            )
        }

        val zFaces = List(meshEnd.z - meshStart.z + 3) { relZ ->
            BoundedAAFace(
                axis = Axis.Z,
                start = Vector3i(
                    meshStart.y,
                    meshStart.z,
                    meshStart.z + relZ - 1,
                ),
                end = Vector3i(
                    meshEnd.x,
                    meshEnd.y,
                    meshStart.z + relZ - 1,
                ),
                holes = mutableListOf(),
            )
        }

        //gather bounded faces
        //a face is only present between 2 blocks if one of them doesn't exist
        for (x in meshStart.x..(meshEnd.x + 2)) {
            val relX = x - meshStart.x
            val xFace = xFaces[relX]
            for (y in meshStart.y..(meshEnd.y + 2)) {
                val relY = y - meshStart.y
                val yFace = yFaces[relY]
                for (z in meshStart.z..(meshEnd.z + 2)) {
                    val relZ = z - meshStart.z
                    val zFace = zFaces[relZ]

                    val block = blocks[relX + 1][relY + 1][relZ + 1]

                    val adjacentX = blocks[relX][relY + 1][relZ + 1]
                    if (block.isPassable == adjacentX.isPassable) {
                        //hole!
                        val hole = (Vector3d(
                            x.toDouble() - 1.0,
                            y.toDouble() - 1.0,
                            z.toDouble() - 1.0,
                        ) to Vector3d(
                            x.toDouble() - 1.0,
                            y.toDouble(),
                            z.toDouble(),
                        ))

                        xFace.holes += hole
                    }

                    val adjacentY = blocks[relX + 1][relY][relZ + 1]
                    if (block.isPassable == adjacentY.isPassable) {
                        //hole!
                        val hole = (Vector3d(
                            x.toDouble() - 1.0,
                            y.toDouble() - 1.0,
                            z.toDouble() - 1.0,
                        ) to Vector3d(
                            x.toDouble(),
                            y.toDouble() - 1.0,
                            z.toDouble(),
                        ))

                        yFace.holes += hole
                    }

                    val adjacentZ = blocks[relX + 1][relY + 1][relZ]
                    if (block.isPassable == adjacentZ.isPassable) {
                        //hole!
                        val hole = (Vector3d(
                            x.toDouble() - 1.0,
                            y.toDouble() - 1.0,
                            z.toDouble() - 1.0,
                        ) to Vector3d(
                            x.toDouble(),
                            y.toDouble(),
                            z.toDouble() - 1.0,
                        ))

                        zFace.holes += hole
                    }
                }
            }
        }

        //edge is on internal edges of holes
        val edges = mutableSetOf<Edge>()

        for (face in xFaces) {
            val theseEdges = mutableListOf<Edge>()
            for (hole in face.holes) {
                val ls = mutableListOf<Edge>()
                Edge(
                    Vector3d(
                        hole.first.x,
                        hole.first.y,
                        hole.first.z,
                    ), Vector3d(
                        hole.first.x,
                        hole.second.y,
                        hole.first.z,
                    )
                ).takeUnless { hole.first.z < meshStart.z.toDouble() }?.let { ls += it }

                Edge(
                    Vector3d(
                        hole.first.x,
                        hole.first.y,
                        hole.second.z,
                    ), Vector3d(
                        hole.first.x,
                        hole.second.y,
                        hole.second.z,
                    )
                ).takeUnless { hole.first.z > meshEnd.z.toDouble() }?.let { ls += it }

                Edge(
                    Vector3d(
                        hole.first.x,
                        hole.first.y,
                        hole.first.z,
                    ), Vector3d(
                        hole.first.x,
                        hole.first.y,
                        hole.second.z,
                    )
                ).takeUnless { hole.first.y < meshStart.y.toDouble() }?.let { ls += it }

                Edge(
                    Vector3d(
                        hole.first.x,
                        hole.second.y,
                        hole.first.z,
                    ), Vector3d(
                        hole.first.x,
                        hole.second.y,
                        hole.second.z,
                    )
                ).takeUnless { hole.first.y > meshEnd.y.toDouble() }?.let { ls += it }

                val toRemove = mutableListOf<Edge>()
                val toAdd = mutableListOf<Edge>()
                for (e in ls) {
                    if (e !in theseEdges) {
                        toAdd += e
                    } else {
                        toRemove += e
                    }
                }

                theseEdges.removeAll(toRemove)
                theseEdges.addAll(toAdd)
            }

            edges += theseEdges
        }

            for (face in yFaces) {
                val theseEdges = mutableListOf<Edge>()
                for (hole in face.holes) {
                    val ls = mutableListOf<Edge>()
                    Edge(
                        Vector3d(
                            hole.first.x,
                            hole.first.y,
                            hole.first.z,
                        ), Vector3d(
                            hole.second.x,
                            hole.first.y,
                            hole.first.z,
                        )
                    ).takeUnless { hole.first.z < meshStart.z.toDouble() }?.let { ls += it }

                    Edge(
                        Vector3d(
                            hole.first.x,
                            hole.first.y,
                            hole.second.z,
                        ), Vector3d(
                            hole.second.x,
                            hole.first.y,
                            hole.second.z,
                        )
                    ).takeUnless { hole.first.z > meshEnd.z.toDouble() }?.let { ls += it }

                    if (PhysicsCommand.DEBUG_MESH_LEVEL > 0) {
                        Edge(
                            Vector3d(
                                hole.first.x,
                                hole.first.y,
                                hole.first.z,
                            ), Vector3d(
                                hole.first.x,
                                hole.first.y,
                                hole.second.z,
                            )
                        ).takeUnless { hole.first.x < meshStart.x.toDouble() }?.let { ls += it }

                        Edge(
                            Vector3d(
                                hole.second.x,
                                hole.first.y,
                                hole.first.z,
                            ), Vector3d(
                                hole.second.x,
                                hole.first.y,
                                hole.second.z,
                            )
                        ).takeUnless { hole.first.x > meshEnd.x.toDouble() }?.let { ls += it }
                    }

                    val toRemove = mutableListOf<Edge>()
                    val toAdd = mutableListOf<Edge>()
                    for (e in ls) {
                        if (e !in theseEdges) {
                            toAdd += e
                        } else {
                            toRemove += e
                        }
                    }

                    theseEdges.removeAll(toRemove)
                    theseEdges.addAll(toAdd)
                }

                edges += theseEdges
            }

        if (PhysicsCommand.DEBUG_MESH_LEVEL > 0) {
            for (face in zFaces) {
                val theseEdges = mutableListOf<Edge>()
                for (hole in face.holes) {
                    val ls = mutableListOf<Edge>()
                    Edge(
                        Vector3d(
                            hole.first.x,
                            hole.first.y,
                            hole.first.z,
                        ), Vector3d(
                            hole.second.x,
                            hole.first.y,
                            hole.first.z,
                        )
                    ).takeUnless { hole.first.y < meshStart.y.toDouble() }?.let { ls += it }

                    Edge(
                        Vector3d(
                            hole.first.x,
                            hole.second.y,
                            hole.first.z,
                        ), Vector3d(
                            hole.second.x,
                            hole.second.y,
                            hole.first.z,
                        )
                    ).takeUnless { hole.first.y > meshEnd.y.toDouble() }?.let { ls += it }

                    Edge(
                        Vector3d(
                            hole.first.x,
                            hole.first.y,
                            hole.first.z,
                        ), Vector3d(
                            hole.first.x,
                            hole.second.y,
                            hole.first.z,
                        )
                    ).takeUnless { hole.first.x < meshStart.x.toDouble() }?.let { ls += it }

                    Edge(
                        Vector3d(
                            hole.second.x,
                            hole.first.y,
                            hole.first.z,
                        ), Vector3d(
                            hole.second.x,
                            hole.second.y,
                            hole.first.z,
                        )
                    ).takeUnless { hole.first.x > meshEnd.x.toDouble() }?.let { ls += it }

                    val toRemove = mutableListOf<Edge>()
                    val toAdd = mutableListOf<Edge>()
                    for (e in ls) {
                        if (e !in theseEdges) {
                            toAdd += e
                        } else {
                            toRemove += e
                        }
                    }

                    theseEdges.removeAll(toRemove)
                    theseEdges.addAll(toAdd)
                }

                edges += theseEdges
            }
        }

        val allBoundedFaces = mutableListOf<BoundedAAFace>()
        allBoundedFaces += xFaces
        allBoundedFaces += yFaces
        allBoundedFaces += zFaces

        boundedFaces = allBoundedFaces
        boundedEdges = edges

        return Mesh(
            boundedFaces = allBoundedFaces,
            edges = edges,
        )
    }
}