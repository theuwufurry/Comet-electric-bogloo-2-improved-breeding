package gg.aquatic.particleemitter.emitter.rate

import gg.aquatic.particleemitter.emitter.EmitterData


interface RateComponent {
    fun toEmit(otherEmitterData: EmitterData): Int
}