package gg.aquatic.comet.v2.parsing.api

import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import org.graalvm.polyglot.HostAccess

object DefaultAPI {
    @HostAccess.Export
    fun createSpriteData(id: String): SpriteData {
        return SpriteData(id)
    }
}