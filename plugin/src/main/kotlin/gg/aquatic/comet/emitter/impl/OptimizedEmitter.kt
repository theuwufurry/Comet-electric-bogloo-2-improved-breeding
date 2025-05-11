package gg.aquatic.comet.emitter.impl

import gg.aquatic.comet.api.Component
import gg.aquatic.comet.api.emitter.AbstractEmitter
import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter
import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.EmitterTickResult
import gg.aquatic.comet.api.emitter.environment.EnvironmentData
import gg.aquatic.comet.api.emitter.parent.Parent
import gg.aquatic.comet.api.emitter.parent.Pose
import gg.aquatic.comet.api.emitter.parent.pose
import gg.aquatic.comet.api.emitter.random.DeterministicRandom
import gg.aquatic.comet.api.emitter.rate.RateComponent
import gg.aquatic.comet.api.particle.ParticleData
import gg.aquatic.comet.api.particle.UpdateFlags
import gg.aquatic.comet.api.particle.data.BillboardConstraints
import gg.aquatic.comet.api.particle.display.DisplayData
import gg.aquatic.comet.emitter.GlobalTicker
import gg.aquatic.comet.emitter.SpawningProcessor
import gg.aquatic.comet.emitter.optimization.CachedPath
import gg.aquatic.comet.emitter.optimization.TimestampedEmitterData
import gg.aquatic.comet.emitter.optimization.VirtualRuntime
import gg.aquatic.comet.emitter.optimization.distanceculling.DistanceCullingComponent
import gg.aquatic.comet.particle.Particle
import gg.aquatic.comet.particle.data.EntityDataBuilder
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.waves.shadow.com.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import gg.aquatic.waves.util.audience.AquaticAudience
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.system.measureNanoTime

class OptimizedEmitter(
    val parent: Parent? = null,
    val components: List<Component>,
    val rateComponent: RateComponent,
    distanceCullingComponent: DistanceCullingComponent,
    val billboardConstraints: BillboardConstraints,
    location: Location,
    val emitterData: EmitterData,
    override val unrealizedEmitter: AbstractUnrealizedEmitter,
    override val forwardVector: Vector3d,
    override val environmentData: EnvironmentData,
    override val audience: AquaticAudience,
    private val internal: Boolean,
    seed: Int = Random.nextInt(),
) : AbstractEmitter() {
    override val id: UUID = emitterData.id
    override val random: DeterministicRandom = DeterministicRandom(seed)
    override var location = location
        private set

    //origin can change, rotation can change
    override val particles: MutableList<Particle> = mutableListOf()
    private val spawningProcessor = SpawningProcessor(this, distanceCullingComponent)
    private val blocked = AtomicBoolean(false)
    private val killed = AtomicBoolean(false)
    override var emitterRotation: Quaterniond = calculateEmitterRotation()

    fun calculateEmitterRotation(): Quaterniond {
        return Quaterniond().rotateTo(forwardVector, pose.dir)
    }

    private val cachedEmitterPath: CachedPath
    private val runtime: VirtualRuntime?

    init {
        blocked.set(true)
        emitterData.emitter = this

        if (!internal) {
            val t =
                measureNanoTime {
                    runtime = VirtualRuntime(this)
                    runtime.step(0)
                }.toDouble() / 1_000_000.0

            if (DEBUG_DISPLAY_DATA >= 0.5 || DEBUG_LOCS >= 0.5) {
                println("${unrealizedEmitter.id} took ${t}ms to pregen!")
            }
        } else {
            runtime = null
        }

        cachedEmitterPath = GlobalTicker.emitterCache[emitterData.id]!!

        cachedEmitterPath.emitterActions.firstOrNull { it.time == 0 }?.let { emData ->
            emData.actions.forEach { a ->
                a(this)
            }
        }

        blocked.set(false)
    }

    private var time = 0

    override fun tick(): EmitterTickResult {
        if (blocked.get()) {
            println("${unrealizedEmitter.id} blocked!")
            return EmitterTickResult(true)
        }

        blocked.set(true)

//        println("O.${unrealizedEmitter.id}: TICK t:$time")

        if (killed.get()) return EmitterTickResult(false)

        time++

        if (!internal) {
            val shouldLive: Boolean
            val t =
                measureNanoTime {
                    shouldLive = runtime!!.step(time)
                }.toDouble() / 1_000_000.0

            if (DEBUG_DISPLAY_DATA >= 0.5 || DEBUG_LOCS >= 0.5) {
                println("${unrealizedEmitter.id} took ${t}ms to step!")
            }

            if (!shouldLive) {
//                println("O.${unrealizedEmitter.id} RUNTIME DEAD at $time")
                if (particles.size == 0) {
//                    println("O.${unrealizedEmitter.id} RUNTIME DEAD SUCCESSFUL at $time")
                    dead = true
                    kill()
                    return EmitterTickResult(false)
                }
            }
        } else {
            if (dead && particles.size == 0) {
                return EmitterTickResult(false)
            }
        }

        val dataPackets: MutableList<PacketWrapper<*>> = mutableListOf()

        cachedEmitterPath.emitterActions.firstOrNull { it.time == time }?.let { emData ->
            emData.actions.forEach { a ->
                a(this)
            }
        }

        for (particle in particles) {
            run optimized@{
                fun die() {
                    spawningProcessor.die(particle)
                }

                cachedEmitterPath.particleActions[particle.data.id]?.firstOrNull { it.time == particle.data.age.toInt() }?.actions?.forEach {
                    it(
                        this@OptimizedEmitter,
                        particle
                    )
                }

                var teleportationDuration: Int? = null
                val locs = cachedEmitterPath.locations[particle.data.id] ?: return@optimized
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
                    }
                val (color: Int?, dD: DisplayData?) =
                    (cachedEmitterPath.coloredTextureData[particle.data.id]
                        ?: return@optimized).firstOrNull { it.time == particle.data.age.toInt() }
                        ?.let { it.color to it.displayData } ?: (null to null)
                val transformableData = cachedEmitterPath.transformableData[particle.data.id] ?: return@optimized

                transformableData.withIndex().firstOrNull { it.value.time == particle.data.age.toInt() }
                    ?.let { (i, cp) ->
                        if (DEBUG_DISPLAY_DATA >= 1) println("=> MATCH DISPLAY : ${particle.data.age.toInt()} $i")
                        val prevDatum = if (particle.data.age == 0.0) null else transformableData[i - 1]
                        val nextDatum = transformableData.getOrNull(i + 1)
                        if (nextDatum != null) {
                            val nd = ParticleData(particle.data.id)
                            color?.let { nd.color = it }
                            nd.color = nd.color or ((nextDatum.vec.alpha * 255.0).toInt() shl 24)
                            dD?.let { nd.displayData = dD }
                            nd.scale = nextDatum.vec.scale
                            nd.rotation = nextDatum.vec.rot
                            nd.translation = nextDatum.vec.translation
                            nd.emitter = this

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
                            flags.translation = (nextDatum.vec.translation != cp.vec.translation)
                            flags.transformationInterpolation = prevDatum?.let { (prevDt != cp.time - it.time) } ?: true
                            flags.transparency = (nextDatum.vec.alpha != cp.vec.alpha)
                            if (DEBUG_DISPLAY_DATA >= 1) println("  | TRANSPARENCY: ${nextDatum.vec.alpha * 255.0}")

                            particle.updatePackets(EntityDataBuilder, true, nd, flags)
                                ?.let {
                                    dataPackets += it
                                }
                        }
                    }
            }

            particle.data.age++
        }

        val data = cachedEmitterPath.emitterData.firstOrNull { it.time == time }
        if (data != null) {
            if (data.dead) {
//                println("O.${unrealizedEmitter.id} DATA DEAD at $time")
                dead = true
            }

            spawnParticles(data)
        }

        val deadParticleIDs: MutableList<Pair<Player, MutableList<Int>>> = spawningProcessor.processDead(dataPackets)

        blocked.set(false)

        return EmitterTickResult(true, deadParticleIDs)
    }

    private fun killParticles(particlesToKill: List<Particle>) {
        spawningProcessor.killParticles(particlesToKill)
    }

    private fun spawnParticles(timestampedEmitterData: TimestampedEmitterData) {
//        if (timestampedEmitterData.spawns.isNotEmpty()) println("O.${unrealizedEmitter.id} SPAWNING t:$time")

        val bundle: MutableList<PacketWrapper<*>> = mutableListOf()
        for (spawn in timestampedEmitterData.spawns) {
            val particleData = ParticleData(spawn)
            particleData.emitter = this
            particleData.billboardConstraints = billboardConstraints
            val particle = Particle(particleData)

            fun end(d: ParticleData = particle.data) {
                val packets = particle.getAddPacket(d)
                bundle.addAll(packets)

                particles += particle
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
            pd.billboardConstraints = billboardConstraints

            val firstDisp = displayData.first()
            val secondDisp = displayData[1]

            pd.transformationInterpolationDuration = (secondDisp.time - firstDisp.time)// - 1

            pd.scale = firstDisp.vec.scale
            pd.rotation = firstDisp.vec.rot
            pd.translation = firstDisp.vec.translation

            val firstTex = texturedColor.first()

            pd.color = firstTex.color
            pd.color = pd.color or ((firstDisp.vec.alpha * 255.0).toInt() shl 24)
            pd.displayData = firstTex.displayData
            pd.emitter = this

            val packets = particle.getAddPacket(pd)
            bundle.addAll(packets)

            particles += particle
        }

        spawningProcessor.sendSpawns(bundle)
    }

    override val players: List<Player>
        get() = spawningProcessor.players

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
        killed.set(true)
        dead = true
        killParticles(particles)
        particles.clear()
        runtime?.kill()
    }

    override fun applyEmitterRotation(input: Quaternionf): Quaternionf {
        return if (billboardConstraints == BillboardConstraints.FIXED) Quaternionf(emitterRotation).mul(input) else input
    }

    companion object {
        val DEBUG_LOCS = 0.0
        val DEBUG_DISPLAY_DATA = 0.0
        val DEBUG_TEXTURE_DATA = 0.0
    }
}