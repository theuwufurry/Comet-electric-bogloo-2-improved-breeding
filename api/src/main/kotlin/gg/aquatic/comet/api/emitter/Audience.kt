package gg.aquatic.comet.api.emitter

import org.bukkit.entity.Player
import java.util.UUID

interface Audience {
    val uuids: Collection<UUID>
    fun canBeApplied(player: Player): Boolean
}