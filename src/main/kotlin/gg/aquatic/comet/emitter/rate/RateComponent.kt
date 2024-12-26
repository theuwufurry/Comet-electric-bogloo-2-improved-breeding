package gg.aquatic.comet.emitter.rate

import gg.aquatic.comet.emitter.EmitterData

interface RateComponent {
    fun toEmit(otherEmitterData: EmitterData): Int
}