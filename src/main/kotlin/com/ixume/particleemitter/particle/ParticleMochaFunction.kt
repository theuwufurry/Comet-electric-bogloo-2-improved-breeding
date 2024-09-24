package com.ixume.particlesTesting.particle

import team.unnamed.mocha.runtime.compiled.MochaCompiledFunction
import team.unnamed.mocha.runtime.compiled.Named

interface ParticleMochaFunction : MochaCompiledFunction {
    fun eval(@Named("emitter_age") emitterAge: Double, @Named("particle_age") particleAge: Double): Double
}