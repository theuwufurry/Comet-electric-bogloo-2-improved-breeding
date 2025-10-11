package gg.aquatic.comet.v2.runtime.executable

import gg.aquatic.comet.v2.runtime.context.WorldContext
import gg.aquatic.comet.v2.runtime.emitter.Effect
import org.bukkit.World
import org.graalvm.polyglot.Context

interface BoundExecutable {
    val effect: Effect
    fun execute(context: WorldContext)
    fun invalidate() {}
}