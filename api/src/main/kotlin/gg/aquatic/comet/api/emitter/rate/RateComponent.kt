package gg.aquatic.comet.api.emitter.rate

import gg.aquatic.comet.api.emitter.EmitterData


interface RateComponent {
    fun toEmit(otherEmitterData: EmitterData): Int
}