package gg.aquatic.comet.v2.runtime.virtual

import gg.aquatic.comet.v2.runtime.context.WorldContext
import gg.aquatic.comet.v2.runtime.emitter.Effect
import gg.aquatic.comet.v2.runtime.executable.BoundExecutable

class BoundExecutableWrapper(replacementEffect: Effect, val executable: BoundExecutable) : BoundExecutable {
    override val effect: Effect = replacementEffect

    override fun execute(context: WorldContext) {
        executable.execute(context)
    }
}