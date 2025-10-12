package gg.aquatic.comet.v2.runtime

import gg.aquatic.comet.api.AbstractParticleEmitter
import gg.aquatic.comet.v2.parsing.V2Parser
import gg.aquatic.comet.v2.parsing.api.DefaultAPI
import gg.aquatic.comet.v2.parsing.api.EffectRuntimeProxy
import gg.aquatic.comet.v2.parsing.api.JSEffectAPI
import gg.aquatic.comet.v2.runtime.context.WorldContext
import gg.aquatic.comet.v2.runtime.emitter.Effect
import gg.aquatic.comet.v2.runtime.emitter.TemporalEffect
import gg.aquatic.comet.v2.runtime.executable.BoundExecutable
import gg.aquatic.comet.v2.runtime.virtual.OptimizedEffect
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.graalvm.polyglot.Context
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureNanoTime

class WorldRuntime(
    val world: World,
) : EffectRuntime {
    private val initialized = AtomicBoolean(false)
    override val players: Collection<Player>
        get() = world.players

    private val apis = mutableMapOf<String, JSEffectAPI>()

    private val toExecute = ConcurrentLinkedQueue<BoundExecutable>()
    private val initializationRequests = ConcurrentLinkedQueue<EffectInitializationRequest>()
    private val effectsToRemove = ConcurrentLinkedQueue<Effect>()
    private val uuids = CopyOnWriteArrayList<UUID>()
    private val effects = CopyOnWriteArrayList<Effect>()

    private var task: BukkitTask? = null
    private val loadedAPIs = AtomicBoolean(false)
    private val blocked = AtomicBoolean(false)

    private val apiRequests = ConcurrentLinkedQueue<Pair<String, (JSEffectAPI?) -> Unit>>()

    override val proxy = EffectRuntimeProxy(this)
    private val worldContext = WorldContext.construct(world)

    fun init() {
        if (!initialized.compareAndSet(false, true)) return

        task = Bukkit.getScheduler().runTaskTimerAsynchronously(AbstractParticleEmitter.INSTANCE, Runnable {
            if (!blocked.compareAndSet(false, true)) return@Runnable

            try {
                val t = measureNanoTime {
                    if (loadedAPIs.compareAndSet(false, true)) {
                        loadAPIs()
                        var request: Pair<String, (JSEffectAPI?) -> Unit>? = null
                        while (apiRequests.poll()?.let { request = it } != null) {
                            request!!
                            request.second(apis[request.first])
                        }
                    }

                    tick()
                }

//                if (world.name == "world") {
//                    println("Tick took ${t.toDuration(DurationUnit.NANOSECONDS)}")
//                }
            } finally {
                blocked.set(false)
            }
        }, 1, 1)
    }

    fun reload() {
        loadedAPIs.set(false)
    }

    private fun loadAPIs() {
        apis.values.forEach { it.close() }
        apis.clear()
        clear()

        for ((id, source) in V2Parser.effects) {
            val context = Context.newBuilder("js")
                .engine(V2Parser.engine)
                .allowHostAccess(V2Parser.hostAccess)
                .allowAllAccess(true)
                .build()

            val api = JSEffectAPI(context)
            context.getBindings("js").putMember("effect", api)
            context.getBindings("js").putMember("comet", DefaultAPI)
            context.eval(source)

            apis[id] = api
        }
    }

    fun getAPI(id: String, after: (JSEffectAPI?) -> Unit) {
        if (loadedAPIs.get()) {
            after(apis[id])
        } else {
            apiRequests += id to after
        }
    }

    private fun tick() {
        processInitializations()
        processRemovals()
        processExecutables()

        for (emitter in effects) {
            emitter.tick()
        }
    }

    private fun processExecutables() {
        var exec: BoundExecutable? = null
        while (toExecute.poll()?.let { exec = it } != null) {
            exec!!

            if (exec.effect.uuid in uuids) {
                exec.execute(worldContext)
            } else {
                exec.invalidate()
            }
        }
    }

    private fun processInitializations() {
        val added = mutableListOf<Effect>()
        var req: EffectInitializationRequest? = null
        while (initializationRequests.poll()?.let { req = it } != null) {
            req!!
            val effect = if (req.unrealized.optimization.enabled) OptimizedEffect(
                relPose = req.pose,
                runtime = this,
                api = req.unrealized,
                parent = req.parent,
            ) else TemporalEffect(
                relPose = req.pose,
                runtime = this,
                api = req.unrealized,
                parent = req.parent,
            )

            req.after.accept(effect)
            added += effect
            uuids += effect.uuid
        }

        effects += added
    }

    private fun processRemovals() {
        var effect: Effect? = null
        while (effectsToRemove.poll()?.let { effect = it } != null) {
            effect!!

            effects -= effect
            uuids -= effect.uuid

            effect.onKill()
        }
    }

    fun registerRequest(request: EffectInitializationRequest) {
        initializationRequests += request
    }

    override fun remove(effect: Effect) {
        effectsToRemove += effect
    }

    override fun submitExecutable(boundExecutable: BoundExecutable) {
        toExecute += boundExecutable
    }

    fun clear() {
        toExecute.clear()
        initializationRequests.clear()
        effectsToRemove += effects
    }

    fun kill() {
        toExecute.clear()
        initializationRequests.clear()
        task?.cancel()
        task = null

        apis.values.forEach { it.close() }
        apis.clear()

        runtimes -= world
    }

    companion object {
        val runtimes = ConcurrentHashMap<World, WorldRuntime>()

        val World.cometRuntime: WorldRuntime
            get() = runtimes.getOrPut(this) { WorldRuntime(this) }.also { it.init() }
    }
}