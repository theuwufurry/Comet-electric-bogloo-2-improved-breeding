package gg.aquatic.comet.v2.runtime.audience

import org.bukkit.entity.Player

@FunctionalInterface
interface Audience {
    fun includes(player: Player): Boolean
}