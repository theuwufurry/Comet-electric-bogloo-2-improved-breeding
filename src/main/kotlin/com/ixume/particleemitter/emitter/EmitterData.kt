package com.ixume.particleemitter.emitter

import org.bukkit.World
import org.joml.Quaterniond

data class EmitterData(var age: Double = 0.0, var world: World? = null, var rotation: Quaterniond? = Quaterniond()) {
    fun copyFrom(other: EmitterData) {
        age = other.age
        world = other.world
        rotation = other.rotation
    }
}