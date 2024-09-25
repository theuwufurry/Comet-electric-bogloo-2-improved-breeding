package com.ixume.particleemitter.emitter.rate

import com.ixume.particleemitter.emitter.EmitterData
import javax.script.Bindings

interface RateComponent {
    fun toEmit(emitterData: EmitterData, bindings: Bindings): Int
}