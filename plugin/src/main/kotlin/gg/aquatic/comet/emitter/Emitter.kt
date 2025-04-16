package gg.aquatic.comet.emitter

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.*
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.optimization.updatefrequency.UpdateFrequencyComponent
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.parent.pose
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.particle.ParticleComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.UpdateFlags
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.api.particle.display.DisplayData
import gg.aquatic.comet.emitter.optimization.TimestampedEmitterData
import gg.aquatic.comet.emitter.optimization.VirtualRuntime
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.comet.particle.Particle
import gg.aquatic.comet.particle.data.EntityDataBuilder
import gg.aquatic.waves.chunk.trackedByPlayers
import gg.aquatic.waves.shadow.com.retrooper.packetevents.PacketEvents
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import gg.aquatic.waves.util.audience.AquaticAudience
import gg.aquatic.waves.util.toUser
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle.DustOptions
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.system.measureNanoTime

class Emitter(
    val parent: Parent? = null,
    val components: List<Component>,
    val rateComponent: RateComponent,
    private val distanceCullingComponent: DistanceCullingComponent,
    private val updateFrequencyComponent: UpdateFrequencyComponent,
    val billboardConstraints: BillboardConstraints,
    location: Location,
    val emitterData: EmitterData,
    val unrealizedEmitter: AbstractUnrealizedEmitter,
    override val forwardVector: Vector3d,
    override val environmentData: EnvironmentData,
    override val audience: AquaticAudience,
    internal: Boolean,
    val seed: Int = Random.nextInt(),

    ) : AbstractEmitter() {
    private val dustOptions =
        DustOptions(Color.fromRGB(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255)), 0.5f)

    val optimized = environmentData.data["optimize"] != null && environmentData.data["optimize"] == true
    val static = ((environmentData.data["static"] as? Boolean) ?: true) && optimized

    override val id: UUID = emitterData.id
    val emitterComponents: List<EmitterComponent> =
        components.filterIsInstance<EmitterComponent>().sortedBy { it.priority }
    val particleComponents: List<ParticleComponent> =
        components.filterIsInstance<ParticleComponent>().sortedBy { it.priority }

    override val random: DeterministicRandom = DeterministicRandom(seed)

    override var location = location
        private set

    //origin can change, rotation can change
    override val particles: MutableList<Particle> = mutableListOf()
    private val deadParticles: MutableList<Particle> = mutableListOf()
    private val blocked = AtomicBoolean(false)
    override var emitterRotation: Quaterniond = calculateEmitterRotation()

    fun calculateEmitterRotation(): Quaterniond {
        return Quaterniond().rotateTo(forwardVector, pose.dir)
    }

    private val currentViewers = ConcurrentHashMap.newKeySet<Player>()

    init {
        blocked.set(true)
        emitterData.emitter = this

        if (!internal) {
            if (optimized) {
                val t = "${unrealizedEmitter.id} took: ${
                    measureNanoTime {
                        VirtualRuntime(this).generateCaches()
                    }.toDouble() / 1_000_000.0
                }"

                if (DEBUG_DISPLAY_DATA >= 1 || DEBUG_LOCS >= 1) {
                    println("${unrealizedEmitter.id} took ${t.toDouble() / 1_000_000.0}ms to pregen!")
                }
            }
        }

        if (!static) {
            emitterComponents.forEach { it.init(emitterData) }
        }

        if (optimized) {
            val c = GlobalTicker.emitterCache[emitterData.id]
            if (c != null) {
                c.emitterActions.firstOrNull { it.time == 0 }?.let { emData ->
                    emData.actions.forEach { a ->
                        a(this)
                    }
                }
            }
        }

        blocked.set(false)
    }

    private var locMisses = 0
    private var displayMisses = 0
    private var particleMisses = 0
    private var emitterMisses = 0

    override fun tick(): EmitterTickResult {
        if (blocked.get()) {
            println("${unrealizedEmitter.id} blocked!")
            return EmitterTickResult(true)
        }

        blocked.set(true)
        val r = if (static) staticTick() else nonStaticTick()
        blocked.set(false)

        return r
    }

    private fun nonStaticTick(): EmitterTickResult {
        parent?.pose?.let { setPose(it) }
        emitterRotation = calculateEmitterRotation()
        emitterComponents.forEach { it.execute(emitterData) }

        if (emitterData.dead || (parent != null && parent.dead)) {
            dead = true
            emitterComponents.forEach { it.die(emitterData) }
        }

        if (dead && particles.size == 0) {
            if (locMisses > 0 || particleMisses > 0 || emitterMisses > 0 || displayMisses > 0) {
                println(
                    """
                -- ${unrealizedEmitter.id} --
                $locMisses loc misses
                $displayMisses display misses
                $particleMisses particle misses
                $emitterMisses emitter misses
            """.trimIndent()
                )
            }

            return EmitterTickResult(false)
        }

        val dataPackets: MutableList<PacketWrapper<*>> = mutableListOf()

        for (particle in particles) {
            fun die() {
                deadParticles += particle
                particle.data.emitter?.dead = true
            }

            particle.tick()

            val shouldUpdate = updateFrequencyComponent.shouldSendUpdate(emitterData, particle.data)
            shouldUpdate.interpolationDuration?.let { particle.data.transformationInterpolationDuration = it }

            val initialPos = Vector3d(particle.data.origin).add(particle.data.relativePosition)

            particleComponents.forEach { it.execute(emitterData, particle.data) }

            if (optimized) {
                run optimized@{
                    val c = GlobalTicker.emitterCache[emitterData.id]
                    if (c == null) {
                        emitterMisses++
                        return@optimized
                    }

                    val p = c.hashes[particle.data.id]

                    if (p == null) {
                        particleMisses++
                        return@optimized
                    }

                    if (!static && particle.data.locHash() !in p.first) {
                        locMisses++
                        return@optimized
                    }

                    var teleportationDuration: Int? = null
                    //match!!
                    val locs = c.locations[particle.data.id]!!
//                        if (particle.data.age.toInt() > locs.last().time)  handle path end
                    locs.withIndex().firstOrNull { it.value.time == particle.data.age.toInt() }
                        ?.let { (i, cp) ->
                            if (i == locs.size - 1) {
                                //last
                                if (DEBUG_LOCS >= 1 || DEBUG_DISPLAY_DATA >= 1) {
                                    println("==> MATCHED END")
                                }
                            }

                            if (DEBUG_LOCS >= 1) println("=> MATCH TP : ${particle.data.age.toInt()}")
                            val nextPos = locs.getOrNull(i + 1)
                            if (nextPos != null) {
                                if (DEBUG_LOCS >= 2) println("TP | ${nextPos.vec.vec.x} ${nextPos.vec.vec.y} ${nextPos.vec.vec.z}")
                                dataPackets += WrapperPlayServerEntityTeleport(
                                    particle.id,
                                    gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.world.Location(
                                        gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3d(
                                            nextPos.vec.vec.x,
                                            nextPos.vec.vec.y,
                                            nextPos.vec.vec.z,
                                        ), 0f, 0f
                                    ),
                                    true
                                )

                                val nnextPos = locs.getOrNull(i + 2)
                                if (nnextPos != null) {
                                    val dt = nnextPos.time - nextPos.time
                                    if (dt != nextPos.time - cp.time) {
                                        if (DEBUG_LOCS >= 1) println("-> TP DURATION : ${dt + 1}")
                                        teleportationDuration = dt + 1
                                    }
                                }
                            }
                        } ?: run {
                        if (particle.data.age.toInt() == 1) {
                            val first = locs.first()
                            val second = locs[1]

                            if (DEBUG_LOCS >= 1) println("INIT TP | POS: ${second.vec.vec}")

                            dataPackets += WrapperPlayServerEntityTeleport(
                                particle.id,
                                gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.world.Location(
                                    gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3d(
                                        second.vec.vec.x,
                                        second.vec.vec.y,
                                        second.vec.vec.z,
                                    ), 0f, 0f
                                ),
                                true
                            )

                            if (locs.size > 2) {
                                val third = locs[2]
                                val dt = third.time - second.time
                                if (dt != second.time - first.time) {
                                    if (DEBUG_LOCS >= 1) println("I -> TP DURATION : ${dt + 1}")
                                    teleportationDuration = dt + 1
                                }
                            }
                        } else {
                        }
                    }

                    if (!static && particle.data.displayHash() !in p.second) {
                        displayMisses++
                        return@optimized
                    }
//                            if (DEBUG_DISPLAY_DATA >= 1) println("T : ${particle.data.age}")

                    val (color: Int?, dD: DisplayData?) =
                        c.coloredTextureData[particle.data.id]!!.firstOrNull { it.time == particle.data.age.toInt() }
                            ?.let { it.color to it.displayData } ?: (null to null)
                    val transformableData = c.transformableData[particle.data.id]!!

                    transformableData.withIndex().firstOrNull { it.value.time == particle.data.age.toInt() }
                        ?.let { (i, cp) ->
                            if (DEBUG_DISPLAY_DATA >= 1) println("=> MATCH DISPLAY : ${particle.data.age.toInt()} $i")
                            val prevDatum = transformableData[i - 1]
                            val nextDatum = transformableData.getOrNull(i + 1)
                            if (nextDatum != null) {
                                val nd = ParticleData(particle.data.id)
                                color?.let { nd.color = it }
                                nd.color = nd.color or ((nextDatum.vec.alpha * 255.0).toInt() shl 24)
                                dD?.let { nd.displayData = dD }
                                nd.scale = nextDatum.vec.scale
                                nd.rotation = nextDatum.vec.rot

                                val prevDt = nextDatum.time - cp.time

                                nd.transformationInterpolationDuration = prevDt

                                //TODO: make sure transparency works
                                val flags = UpdateFlags()

                                if (teleportationDuration != null) {
                                    nd.teleportationDuration = teleportationDuration!!
                                    flags.teleportationDuration = true
                                }

                                flags.display = color != null
                                flags.rotation = (nextDatum.vec.rot != cp.vec.rot)
                                flags.scale = (nextDatum.vec.scale != cp.vec.scale)
                                flags.transformationInterpolation = (prevDt != cp.time - prevDatum.time)
                                flags.transparency = (nextDatum.vec.alpha != cp.vec.alpha)
                                if (DEBUG_DISPLAY_DATA >= 1) println("  | TRANSPARENCY: ${nextDatum.vec.alpha * 255.0}")

                                particle.updatePacket(EntityDataBuilder, true, nd, flags)
                                    ?.let { dataPackets += it }

                                val nnextDatum = transformableData.getOrNull(i + 2)

                                if (nnextDatum != null) {
                                    val dt = nnextDatum.time - nextDatum.time
                                    particle.data.transformationInterpolationDuration = dt
                                }
                            }
                        } ?: run {
                        if (particle.data.age.toInt() == 1) {
                            val first = transformableData.first()
                            val second = transformableData[1]

                            val dt = second.time - first.time
                            particle.data.transformationInterpolationDuration = dt

                            val nd = ParticleData(particle.data.id)
                            color?.let { nd.color = it }
                            nd.color = nd.color or ((second.vec.alpha * 255.0).toInt() shl 24)
                            dD?.let { nd.displayData = dD }
                            nd.scale = second.vec.scale
                            nd.rotation = second.vec.rot

                            val flags = UpdateFlags()
                            flags.display = color != null
                            flags.rotation = (second.vec.rot != first.vec.rot)
                            flags.scale = (second.vec.scale != first.vec.scale)
                            flags.transparency = (second.vec.alpha != first.vec.alpha)

                            if (teleportationDuration != null) {
                                nd.teleportationDuration = teleportationDuration!!
                                flags.teleportationDuration = true
                            }

                            if (DEBUG_DISPLAY_DATA >= 1) println("DD | INIT")

                            particle.updatePacket(EntityDataBuilder, true, nd, flags)
                                ?.let { dataPackets += it }
                        } else if (color != null) {
                            val nd = ParticleData(particle.data.id)
                            nd.color = color
                            nd.displayData = dD!!
                            // use correct display data

                            if (DEBUG_DISPLAY_DATA >= 1) println("COLOR | $color")

                            if (teleportationDuration != null) {
                                nd.teleportationDuration = teleportationDuration!!
                            }

                            particle.updatePacket(
                                EntityDataBuilder,
                                true,
                                nd,
                                UpdateFlags(
                                    display = true,
                                    teleportationDuration = teleportationDuration != null
                                )
                            )
                                ?.let { dataPackets += it }
                        } else {
                            if (teleportationDuration != null) {
                                val nd = ParticleData(particle.data.id)
                                nd.teleportationDuration = teleportationDuration!!

                                particle.updatePacket(
                                    EntityDataBuilder,
                                    true,
                                    nd,
                                    UpdateFlags(teleportationDuration = true)
                                )
                                    ?.let { dataPackets += it }
                            } else {
                            }
                        }
                    }
                }

                if (particle.data.dead) {
                    particleComponents.forEach { it.die(emitterData, particle.data) }
                    die()
                    continue
                }
            } else {
                if (initialPos != particle.data.relativePosition) {
                    if (shouldUpdate.shouldUpdate) {
                        particle.getMovementPacket().let { dataPackets += it }
                    }
                }

                particle.updatePacket(
                    entityDataBuilder = EntityDataBuilder,
                    shouldUpdate = shouldUpdate.shouldUpdate,
                    flagOverride = null
                )?.let { dataPackets += it }

                if (particle.data.dead) {
                    particleComponents.forEach { it.die(emitterData, particle.data) }
                    die()
                    continue
                }
            }
        }

        val playerManager = PacketEvents.getAPI().playerManager

        particles.removeAll(deadParticles)
        val rawDeadParticleIDs = deadParticles.map { it.id }.toMutableList()
        val deadParticleIDs: MutableList<Pair<Player, MutableList<Int>>> = mutableListOf()
        val particleIDs: MutableList<Int> by lazy {
            particles.map { it.id }.toMutableList().also { it.addAll(rawDeadParticleIDs) }
        }
        val chunkViewers = location.chunk.trackedByPlayers()

        val playersToRemove = HashSet<Player>()
        for (currentViewer in currentViewers) {
            if (currentViewer !in chunkViewers || !currentViewer.isOnline) {
                playersToRemove += currentViewer
            }
        }

        for (player in playersToRemove) {
            currentViewers -= player
        }

        for (player in location.chunk.trackedByPlayers()) {
            if (player in playersToRemove) continue
            val distanceSquared = player.eyeLocation.distanceSquared(location)
            if (currentViewers.contains(player)) {
                if (distanceSquared > distanceCullingComponent.viewDistance || !audience.canBeApplied(player)) {
                    deadParticleIDs += player to particleIDs
                    currentViewers -= player
                }
            }

            if (!audience.canBeApplied(player) || player !in currentViewers) continue

            if (distanceSquared <= distanceCullingComponent.viewDistance) {
                deadParticleIDs += player to rawDeadParticleIDs
                for (packet in dataPackets) {
                    try {
                        player.toUser().sendPacketSilently(packet)
                    } catch (ignored: NullPointerException) {
                    }
                }
            }
        }

        deadParticles.clear()

        if (!dead && emitterData.isActive) nonStaticSpawnParticles()

        return EmitterTickResult(true, deadParticleIDs)

    }

    private var time = 0
    private fun staticTick(): EmitterTickResult {
        if (time == -1 && particles.size == 0) {
            if (locMisses > 0 || particleMisses > 0 || emitterMisses > 0 || displayMisses > 0) {
                println(
                    """
                -- ${unrealizedEmitter.id} --
                $locMisses loc misses
                $displayMisses display misses
                $particleMisses particle misses
                $emitterMisses emitter misses
            """.trimIndent()
                )
            }

            return EmitterTickResult(false)
        }

        if (time != -1) time++

        val dataPackets: MutableList<PacketWrapper<*>> = mutableListOf()

        val c = GlobalTicker.emitterCache[emitterData.id]
        if (c == null) {
            emitterMisses++
            AbstractParticleEmitter.INSTANCE.logger.severe("${unrealizedEmitter.id} emitter id not found in cache! Please submit a bug report.")
            return EmitterTickResult(false)
        }

        c.emitterActions.firstOrNull { it.time == time }?.let { emData ->
            emData.actions.forEach { a ->
                a(this)
            }
        }

        for (particle in particles) {
            particle.data.age++

            run optimized@{
                fun die() {
                    deadParticles += particle
                }

                val p = c.hashes[particle.data.id]

                if (p == null) {
                    particleMisses++
                    return@optimized
                }

                c.particleActions[particle.data.id]?.firstOrNull { it.time == particle.data.age.toInt() }?.actions?.forEach {
                    it(
                        this@Emitter,
                        particle
                    )
                }

                var teleportationDuration: Int? = null
                //match!!
                val locs = c.locations[particle.data.id]!!
//                        if (particle.data.age.toInt() > locs.last().time)  handle path end
                locs.withIndex().firstOrNull { it.value.time == particle.data.age.toInt() }
                    ?.let { (i, cp) ->
                        if (i == locs.size - 1) {
                            //last
                            if (DEBUG_LOCS >= 1 || DEBUG_DISPLAY_DATA >= 1) {
                                println("==> MATCHED END")
                            }
                            die()
                            return@optimized
                        }

                        if (DEBUG_LOCS >= 1) println("=> MATCH TP : ${particle.data.age.toInt()}")
                        val nextPos = locs.getOrNull(i + 1)
                        if (nextPos != null) {
                            if (DEBUG_LOCS >= 2) println("TP | ${nextPos.vec.vec.x} ${nextPos.vec.vec.y} ${nextPos.vec.vec.z}")
                            dataPackets += WrapperPlayServerEntityTeleport(
                                particle.id,
                                gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.world.Location(
                                    gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3d(
                                        nextPos.vec.vec.x,
                                        nextPos.vec.vec.y,
                                        nextPos.vec.vec.z,
                                    ), 0f, 0f
                                ),
                                true
                            )

                            val nnextPos = locs.getOrNull(i + 2)
                            if (nnextPos != null) {
                                val dt = nnextPos.time - nextPos.time
                                if (dt != nextPos.time - cp.time) {
                                    if (DEBUG_LOCS >= 1) println("-> TP DURATION : ${dt + 1}")
                                    teleportationDuration = dt + 1
                                }
                            }
                        }
                    } ?: run {
                    if (particle.data.age.toInt() == 1) {
                        val first = locs.first()
                        val second = locs[1]

                        if (DEBUG_LOCS >= 1) println("INIT TP | POS: ${second.vec.vec}")

                        dataPackets += WrapperPlayServerEntityTeleport(
                            particle.id,
                            gg.aquatic.waves.shadow.com.retrooper.packetevents.protocol.world.Location(
                                gg.aquatic.waves.shadow.com.retrooper.packetevents.util.Vector3d(
                                    second.vec.vec.x,
                                    second.vec.vec.y,
                                    second.vec.vec.z,
                                ), 0f, 0f
                            ),
                            true
                        )

                        if (locs.size > 2) {
                            val third = locs[2]
                            val dt = third.time - second.time
                            if (dt != second.time - first.time) {
                                if (DEBUG_LOCS >= 1) println("I -> TP DURATION : ${dt + 1}")
                                teleportationDuration = dt + 1
                            }
                        }
                    } else {
                    }
                }
//                            if (DEBUG_DISPLAY_DATA >= 1) println("T : ${particle.data.age}")

                val (color: Int?, dD: DisplayData?) =
                    c.coloredTextureData[particle.data.id]!!.firstOrNull { it.time == particle.data.age.toInt() }
                        ?.let { it.color to it.displayData } ?: (null to null)
                val transformableData = c.transformableData[particle.data.id]!!

                transformableData.withIndex().firstOrNull { it.value.time == particle.data.age.toInt() }
                    ?.let { (i, cp) ->
                        if (DEBUG_DISPLAY_DATA >= 1) println("=> MATCH DISPLAY : ${particle.data.age.toInt()} $i")
                        val prevDatum = transformableData[i - 1]
                        val nextDatum = transformableData.getOrNull(i + 1)
                        if (nextDatum != null) {
                            val nd = ParticleData(particle.data.id)
                            color?.let { nd.color = it }
                            nd.color = nd.color or ((nextDatum.vec.alpha * 255.0).toInt() shl 24)
                            dD?.let { nd.displayData = dD }
                            nd.scale = nextDatum.vec.scale
                            nd.rotation = nextDatum.vec.rot

                            val prevDt = nextDatum.time - cp.time

                            nd.transformationInterpolationDuration = prevDt

                            //TODO: make sure transparency works
                            val flags = UpdateFlags()

                            if (teleportationDuration != null) {
                                nd.teleportationDuration = teleportationDuration!!
                                flags.teleportationDuration = true
                            }

                            flags.display = color != null
                            flags.rotation = (nextDatum.vec.rot != cp.vec.rot)
                            flags.scale = (nextDatum.vec.scale != cp.vec.scale)
                            flags.transformationInterpolation = (prevDt != cp.time - prevDatum.time)
                            flags.transparency = (nextDatum.vec.alpha != cp.vec.alpha)
                            if (DEBUG_DISPLAY_DATA >= 1) println("  | TRANSPARENCY: ${nextDatum.vec.alpha * 255.0}")

                            particle.updatePacket(EntityDataBuilder, true, nd, flags)
                                ?.let { dataPackets += it }

//                            val nnextDatum = transformableData.getOrNull(i + 2)
//
//                            if (nnextDatum != null) {
//                                val dt = nnextDatum.time - nextDatum.time
//                                particle.data.transformationInterpolationDuration = dt
//                            }
                        }
                    } ?: run {
                    if (particle.data.age.toInt() == 1) {
                        val first = transformableData.first()
                        val second = transformableData[1]

                        val dt = second.time - first.time
                        particle.data.transformationInterpolationDuration = dt

                        val nd = ParticleData(particle.data.id)
                        color?.let { nd.color = it }
                        nd.color = nd.color or ((second.vec.alpha * 255.0).toInt() shl 24)
                        dD?.let { nd.displayData = dD }
                        nd.scale = second.vec.scale
                        nd.rotation = second.vec.rot

                        val flags = UpdateFlags()
                        flags.display = color != null
                        flags.rotation = (second.vec.rot != first.vec.rot)
                        flags.scale = (second.vec.scale != first.vec.scale)
                        flags.transparency = (second.vec.alpha != first.vec.alpha)

                        if (teleportationDuration != null) {
                            nd.teleportationDuration = teleportationDuration!!
                            flags.teleportationDuration = true
                        }

                        if (DEBUG_DISPLAY_DATA >= 1) println("DD | INIT")

                        particle.updatePacket(EntityDataBuilder, true, nd, flags)
                            ?.let { dataPackets += it }
                    } else if (color != null) {
                        val nd = ParticleData(particle.data.id)
                        nd.color = color
                        nd.displayData = dD!!
                        // use correct display data

                        if (DEBUG_DISPLAY_DATA >= 1) println("COLOR | $color")

                        if (teleportationDuration != null) {
                            nd.teleportationDuration = teleportationDuration!!
                        }

                        particle.updatePacket(
                            EntityDataBuilder,
                            true,
                            nd,
                            UpdateFlags(
                                display = true,
                                teleportationDuration = teleportationDuration != null
                            )
                        )
                            ?.let { dataPackets += it }
                    } else {
                        if (teleportationDuration != null) {
                            val nd = ParticleData(particle.data.id)
                            nd.teleportationDuration = teleportationDuration!!

                            particle.updatePacket(
                                EntityDataBuilder,
                                true,
                                nd,
                                UpdateFlags(teleportationDuration = true)
                            )
                                ?.let { dataPackets += it }
                        } else {
                        }
                    }
                }
            }
        }

        val data = c.emitterData.firstOrNull { it.time == time }
        if (data != null) {
            if (data.dead) {
                time = -1
            }

            staticSpawnParticles(data)
        }

        val playerManager = PacketEvents.getAPI().playerManager

        particles.removeAll(deadParticles)
        val rawDeadParticleIDs = deadParticles.map { it.id }.toMutableList()
        val deadParticleIDs: MutableList<Pair<Player, MutableList<Int>>> = mutableListOf()
        val particleIDs: MutableList<Int> by lazy {
            particles.map { it.id }.toMutableList().also { it.addAll(rawDeadParticleIDs) }
        }
        val chunkViewers = location.chunk.trackedByPlayers()

        val playersToRemove = HashSet<Player>()
        for (currentViewer in currentViewers) {
            if (currentViewer !in chunkViewers || !currentViewer.isOnline) {
                playersToRemove += currentViewer
            }
        }

        for (player in playersToRemove) {
            currentViewers -= player
        }

        for (player in location.chunk.trackedByPlayers()) {
            if (player in playersToRemove) continue
            val distanceSquared = player.eyeLocation.distanceSquared(location)
            if (currentViewers.contains(player)) {
                if (distanceSquared > distanceCullingComponent.viewDistance || !audience.canBeApplied(player)) {
                    deadParticleIDs += player to particleIDs
                    currentViewers -= player
                }
            }

            if (!audience.canBeApplied(player) || player !in currentViewers) continue

            if (distanceSquared <= distanceCullingComponent.viewDistance) {
                deadParticleIDs += player to rawDeadParticleIDs
                for (packet in dataPackets) {
                    try {
                        playerManager.sendPacketSilently(player, packet)
                    } catch (ignored: NullPointerException) {
                    }
                }
            }
        }

        deadParticles.clear()

        return EmitterTickResult(true, deadParticleIDs)

    }

    private fun killParticles(particlesToKill: List<Particle>) {
        if (particlesToKill.isEmpty()) return
        val ids = particlesToKill.map { it.id }.toIntArray()
        for (player in currentViewers) {
            try {
                player.toUser().sendPacketSilently(WrapperPlayServerDestroyEntities(*ids))
            } catch (ignored: NullPointerException) {
            }
        }
    }

    private fun nonStaticSpawnParticles() {
        val bundle: MutableList<PacketWrapper<*>> = mutableListOf()
        repeat(rateComponent.toEmit(emitterData)) {
            val particleData = ParticleData(random.uuid())
            val particle = Particle(particleData)
            particleData.particle = particle
            particleData.origin = location.toVector().toVector3d()
            particleData.billboardConstraints = billboardConstraints
            particleData.interpolationDelay = updateFrequencyComponent.interpolationDelay
            particleData.transformationInterpolationDuration = updateFrequencyComponent.initialInterpolationDuration

            particleComponents.forEach { it.execute(emitterData, particleData) }

            particle.init()

            fun end(d: ParticleData = particle.data) {
                val packets = particle.getAddPacket(d)
                bundle.addAll(packets)

                particles += particle
            }

            if (!optimized) {
                end()
                return@repeat
            }

            val locs = GlobalTicker.emitterCache[id]?.locations?.get(particleData.id)
            if (locs == null) {
                end()
                return@repeat
            }

            val displayData = GlobalTicker.emitterCache[id]?.transformableData?.get(particleData.id)
            if (displayData == null) {
                end()
                return@repeat
            }

            val texturedColor = GlobalTicker.emitterCache[id]?.coloredTextureData?.get(particleData.id)
            if (texturedColor == null) {
                end()
                return@repeat
            }

            val pd = ParticleData(particle.data.id)

            val firstLoc = locs.first()
            pd.teleportationDuration = ((locs[1].time - firstLoc.time)) + 1
            pd.relativePosition = Vector3d(firstLoc.vec.vec.x, firstLoc.vec.vec.y, firstLoc.vec.vec.z)

            val firstDisp = displayData.first()
            val secondDisp = displayData[1]

            pd.transformationInterpolationDuration = (secondDisp.time - firstDisp.time) + 1

            pd.scale = firstDisp.vec.scale
            pd.rotation = firstDisp.vec.rot
            pd.rotation = firstDisp.vec.rot

            val firstTex = texturedColor.first()

            pd.color = firstTex.color
            pd.color = pd.color or ((firstDisp.vec.alpha * 255.0).toInt() shl 24)
            pd.displayData = firstTex.displayData

            val packets = particle.getAddPacket(pd)
            bundle.addAll(packets)

            particles += particle
        }

        for (player in location.chunk.trackedByPlayers()) {
            if (!audience.canBeApplied(player)) continue
            val distanceSquared = player.eyeLocation.distanceSquared(location)
            if (distanceSquared < distanceCullingComponent.viewDistance) {
                currentViewers += player
                for (packet in bundle) {
                    try {
                        player.toUser().sendPacketSilently(packet)
                    } catch (ignored: NullPointerException) {
                    }
                }
            }
        }
    }


    private fun staticSpawnParticles(timestampedEmitterData: TimestampedEmitterData) {
        val c = GlobalTicker.emitterCache[emitterData.id]
        if (c == null) {
            emitterMisses++
            AbstractParticleEmitter.INSTANCE.logger.severe("${unrealizedEmitter.id} emitter id not found in cache! Please submit a bug report.")
            return
        }

        val bundle: MutableList<PacketWrapper<*>> = mutableListOf()
        for (spawn in timestampedEmitterData.spawns) {
            val particleData = ParticleData(spawn)
            val particle = Particle(particleData)

            c.particleActions[particle.data.id]?.firstOrNull { it.time == 0 }?.actions?.forEach {
                it(
                    this@Emitter,
                    particle
                )
            }

            fun end(d: ParticleData = particle.data) {
                val packets = particle.getAddPacket(d)
                bundle.addAll(packets)

                particles += particle
            }

            if (!(environmentData.data["optimize"] != null && environmentData.data["optimize"] == true)) {
                end()
                continue
            }

            val locs = GlobalTicker.emitterCache[id]?.locations?.get(particleData.id)
            if (locs == null) {
                end()
                continue
            }

            val displayData = GlobalTicker.emitterCache[id]?.transformableData?.get(particleData.id)
            if (displayData == null) {
                end()
                continue
            }

            val texturedColor = GlobalTicker.emitterCache[id]?.coloredTextureData?.get(particleData.id)
            if (texturedColor == null) {
                end()
                continue
            }

            val pd = ParticleData(particle.data.id)

            val firstLoc = locs.first()
            pd.teleportationDuration = ((locs[1].time - firstLoc.time)) + 1
            pd.relativePosition = Vector3d(firstLoc.vec.vec.x, firstLoc.vec.vec.y, firstLoc.vec.vec.z)

            val firstDisp = displayData.first()
            val secondDisp = displayData[1]

            pd.transformationInterpolationDuration = (secondDisp.time - firstDisp.time) + 1

            pd.scale = firstDisp.vec.scale
            pd.rotation = firstDisp.vec.rot
            pd.rotation = firstDisp.vec.rot

            val firstTex = texturedColor.first()

            pd.color = firstTex.color
            pd.color = pd.color or ((firstDisp.vec.alpha * 255.0).toInt() shl 24)
            pd.displayData = firstTex.displayData

            val packets = particle.getAddPacket(pd)
            bundle.addAll(packets)

            particles += particle
        }

        for (player in location.chunk.trackedByPlayers()) {
            if (!audience.canBeApplied(player)) continue
            val distanceSquared = player.eyeLocation.distanceSquared(location)
            if (distanceSquared < distanceCullingComponent.viewDistance) {
                currentViewers += player
                for (packet in bundle) {
                    try {
                        player.toUser().sendPacketSilently(packet)
                    } catch (ignored: NullPointerException) {
                    }
                }
            }
        }
    }

    override val players: List<Player>
        get() {
            val maxDistance = distanceCullingComponent.viewDistance
            return location.chunk.trackedByPlayers()
                .filter { audience.canBeApplied(it) }
                .filter { it.eyeLocation.distanceSquared(location) < maxDistance }
        }

    override fun setPose(pose: Pose) {
        location.x = pose.pos.x
        location.y = pose.pos.y
        location.z = pose.pos.z

        location.direction = Vector(
            pose.dir.x,
            pose.dir.y,
            pose.dir.z
        )
    }

    override val pose: Pose
        get() {
            return location.pose()
        }

    override val isPregen: Boolean = false
    override fun realize(
        unrealizedEmitter: AbstractUnrealizedEmitter,
        parent: Parent?,
        location: Location,
        environmentData: EnvironmentData,
        audience: AquaticAudience,
        random: DeterministicRandom,
        uuid: UUID
    ) {
        unrealizedEmitter.internalRealize(
            parent,
            location,
            environmentData,
            audience,
            random,
            uuid
        )
    }

    override fun kill() {
        dead = true
        killParticles(particles)
        particles.clear()
    }

    override fun applyEmitterRotation(input: Quaternionf): Quaternionf {
        return if (billboardConstraints == BillboardConstraints.FIXED) Quaternionf(emitterRotation).mul(input) else input
    }

    companion object {
        val DEBUG_LOCS = 0
        val DEBUG_DISPLAY_DATA = 0
        val DEBUG_TEXTURE_DATA = 0
    }
}