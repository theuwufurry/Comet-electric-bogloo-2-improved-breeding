package gg.aquatic.comet.emitter

import gg.aquatic.comet.Component

interface EmitterComponent : Component {
    fun init(otherEmitterData: EmitterData)
    fun execute(otherEmitterData: EmitterData)
    fun die(otherEmitterData: EmitterData)
}