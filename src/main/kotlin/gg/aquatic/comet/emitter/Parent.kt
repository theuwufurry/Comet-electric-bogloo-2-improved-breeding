package gg.aquatic.comet.emitter

import org.joml.Vector3d

enum class EmitterSpace {
    WORLD,
    PARENT_EMITTER,
    PARENT_PARTICLE
}

interface Parent {
    fun location(): Vector3d
}