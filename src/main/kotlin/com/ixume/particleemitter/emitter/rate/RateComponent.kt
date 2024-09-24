package com.ixume.particlesTesting.emitter.rate

import com.ixume.particlesTesting.emitter.EmitterMochaData

interface RateComponent {
    fun toEmit(emitterData: EmitterMochaData): Int
}