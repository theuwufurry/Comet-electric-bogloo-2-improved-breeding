package gg.aquatic.comet.api.emitter

import org.bukkit.entity.Player
import java.util.UUID

object GlobalAudience : Audience {
    override val uuids: Collection<UUID> = emptyList()
    override fun canBeApplied(player: Player) = true
}