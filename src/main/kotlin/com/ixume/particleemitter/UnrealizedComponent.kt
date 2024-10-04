package com.ixume.particleemitter

import com.ixume.particleemitter.emitter.EmitterData

interface UnrealizedComponent<T> {
    fun realizeComponent(emitterData: EmitterData): T
}