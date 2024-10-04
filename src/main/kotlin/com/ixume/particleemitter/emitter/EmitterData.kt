package com.ixume.particleemitter.emitter

import org.bukkit.World

data class EmitterData(var age: Double = 0.0, var world: World? = null) {
    fun copyFrom(other: EmitterData) {
        age = other.age
        world = other.world
    }
}