package gg.aquatic.comet.v2.runtime.context

import org.bukkit.World

@FunctionalInterface
interface WorldContextTransformer {
    fun transform(world: World): Map<String, Any>
}