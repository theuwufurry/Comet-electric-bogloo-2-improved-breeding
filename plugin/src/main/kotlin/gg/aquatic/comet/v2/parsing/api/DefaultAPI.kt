package gg.aquatic.comet.v2.parsing.api

import com.ixume.udar.body.active.ActiveBody
import gg.aquatic.comet.api.particle.display.sprite.SpriteData
import gg.aquatic.comet.udar.UdarBodyParent
import gg.aquatic.comet.v2.parsing.api.udar.PhysicsBodyWrapper

object DefaultAPI {
    fun createSpriteData(id: String): SpriteData {
        return SpriteData(id)
    }

    fun parentOf(body: ActiveBody): UdarBodyParent {
        return UdarBodyParent(body)
    }

    fun parentOf(body: PhysicsBodyWrapper): UdarBodyParent {
        return UdarBodyParent(body.actual)
    }
}