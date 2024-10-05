package com.ixume.particleemitter.emitter.lifetime

interface EmitterLifetimeComponent {
    fun keepAlive(): Boolean
}