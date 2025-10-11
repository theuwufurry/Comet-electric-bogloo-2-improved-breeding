package gg.aquatic.comet.v2.runtime.context

import org.bukkit.World
import org.graalvm.polyglot.proxy.ProxyExecutable

class Method(val name: String, val executable: ProxyExecutable) : WorldContextTransformer {
    override fun transform(world: World): Map<String, Any> {
        return mapOf(name to executable)
    }
}