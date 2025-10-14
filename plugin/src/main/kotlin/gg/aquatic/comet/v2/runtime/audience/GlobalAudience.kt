package gg.aquatic.comet.v2.runtime.audience

import org.bukkit.entity.Player

class GlobalAudience : Audience {
    override fun includes(player: Player): Boolean {
        return true
    }
}