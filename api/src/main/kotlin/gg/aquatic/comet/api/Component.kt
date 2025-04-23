package gg.aquatic.comet.api

import gg.aquatic.comet.api.emitter.EmitterData
import gg.aquatic.comet.api.emitter.environment.EnvironmentData

interface Component {
    val priority: Int
}

interface PreInitComponent {
    fun init(otherEmitterData: EmitterData, environmentData: EnvironmentData)
}