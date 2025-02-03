package gg.aquatic.comet.api.emitter

import gg.aquatic.comet.api.Component

interface EmitterComponent : Component {
    fun init(otherEmitterData: EmitterData)
    fun execute(otherEmitterData: EmitterData)
    fun die(otherEmitterData: EmitterData)
}