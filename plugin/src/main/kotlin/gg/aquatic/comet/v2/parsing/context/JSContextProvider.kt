package gg.aquatic.comet.v2.parsing.context

import gg.aquatic.comet.v2.parsing.api.udar.PhysicsBodyWrapper
import gg.aquatic.comet.v2.parsing.getMemberOrNull
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

/**
 * Since JS is run from files in the system and from commands as well, the context and host access must be sandboxed.
 * This however should also be modifiable by other plugins in case they want to add their own APIs. For this reason
 * a public list of processors will be used.
 */
object JSContextProvider {
    private fun createEngine(): Engine = Engine.newBuilder("js").build()
    private val engine: AtomicReference<Engine> = AtomicReference(createEngine())

    val hostAccessProcessors = CopyOnWriteArrayList<Consumer<HostAccess.Builder>>().apply {
        this += Consumer { hostAccess ->
            hostAccess
                .targetTypeMapping(
                    Value::class.java,
                    Vector3d::class.java,
                    { value -> value.hasMembers() || value.hasArrayElements() },
                    { value ->
                        if (value.hasArrayElements()) {
                            Vector3d(
                                value.getArrayElement(0L).asDouble(),
                                value.getArrayElement(1L).asDouble(),
                                value.getArrayElement(2L).asDouble(),
                            )
                        } else {
                            Vector3d(
                                value.getMemberOrNull("x")?.asDouble() ?: 0.0,
                                value.getMemberOrNull("y")?.asDouble() ?: 0.0,
                                value.getMemberOrNull("z")?.asDouble() ?: 0.0,
                            )
                        }
                    }
                )
                .targetTypeMapping(
                    Value::class.java,
                    Vector3f::class.java,
                    { value -> value.hasMembers() || value.hasArrayElements() },
                    { value ->
                        if (value.hasArrayElements()) {
                            Vector3f(
                                value.getArrayElement(0L).asDouble().toFloat(),
                                value.getArrayElement(1L).asDouble().toFloat(),
                                value.getArrayElement(2L).asDouble().toFloat(),
                            )
                        } else {
                            Vector3f(
                                value.getMemberOrNull("x")?.asDouble()?.toFloat() ?: 0f,
                                value.getMemberOrNull("y")?.asDouble()?.toFloat() ?: 0f,
                                value.getMemberOrNull("z")?.asDouble()?.toFloat() ?: 0f,
                            )
                        }
                    }
                )
                .targetTypeMapping(
                    Value::class.java,
                    PhysicsBodyWrapper::class.java,
                    { value -> value.isProxyObject },
                    { value -> value.asProxyObject() }
                )
        }
        this += Consumer { hostAccess ->
            hostAccess.allowPublicAccess(true)
        }
    }

    private fun createHostAccess(): HostAccess {
        return HostAccess.newBuilder()
            .also { builder -> hostAccessProcessors.forEach { it.accept(builder) } }
            .build()
    }

    @Volatile
    private var hostAccess: HostAccess = createHostAccess()

    val contextProcessors = CopyOnWriteArrayList<Consumer<Context.Builder>>().apply {
        this += Consumer { context ->
            context.engine(engine.get())
        }

        this += Consumer { context ->
            context.allowAllAccess(true)
        }
    }

    @Synchronized
    fun createContext(): Context {
        val context = Context.newBuilder("js")
            .also { builder -> contextProcessors.forEach { it.accept(builder) } }
            .allowHostAccess(hostAccess)
            .build()

        return context
    }

    fun reload() {
        engine.getAndSet(createEngine()).close()
        hostAccess = createHostAccess()
    }

    fun close() {
        engine.getAndSet(null).close()
    }
}