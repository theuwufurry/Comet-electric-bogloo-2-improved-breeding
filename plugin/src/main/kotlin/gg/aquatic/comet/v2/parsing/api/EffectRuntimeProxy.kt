package gg.aquatic.comet.v2.parsing.api

import gg.aquatic.comet.v2.runtime.EffectRuntime
import gg.aquatic.comet.v2.runtime.context.WorldContext
import gg.aquatic.comet.v2.runtime.emitter.Effect
import gg.aquatic.comet.v2.runtime.executable.BoundExecutable
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject

class EffectRuntimeProxy(val runtime: EffectRuntime) : ProxyObject {
    private val run = ProxyExecutable { args ->
        val effect = args[0]!!.asProxyObject<V2EffectProxy>()
        val lambda = args[1]!!
        check(lambda.canExecute())
        runtime.submitExecutable(object : BoundExecutable {
            override val effect: Effect = effect.effect

            override fun execute(context: WorldContext) {
                lambda.executeVoid(context)
            }
        })
    }

    private val fields = arrayOf(
        "run"
    )

    override fun getMember(key: String?): Any? {
        return when (key) {
            "run" -> run
            else -> null
        }
    }

    override fun getMemberKeys(): Any? {
        return fields
    }

    override fun hasMember(key: String?): Boolean {
        return key in fields
    }

    override fun putMember(key: String?, value: Value?) {
        throw UnsupportedOperationException()
    }
}