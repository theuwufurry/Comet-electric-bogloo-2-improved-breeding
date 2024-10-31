package com.ixume.particleemitter.emitter.rate

import com.ixume.particleemitter.emitter.EmitterData

interface RateComponent {
    fun toEmit(otherEmitterData: EmitterData): Int
}