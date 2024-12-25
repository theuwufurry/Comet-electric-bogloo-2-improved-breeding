package gg.aquatic.comet.emitter

import org.bukkit.World
import org.joml.Quaterniond

data class EmitterData(
    var age: Double = 0.0,
    var world: World? = null,
    var rotation: Quaterniond? = Quaterniond(),
    var random: Double = Math.random()
) {
    fun copyFrom(other: EmitterData) {
        age = other.age
        world = other.world
        rotation = other.rotation
        random = other.random
    }
}