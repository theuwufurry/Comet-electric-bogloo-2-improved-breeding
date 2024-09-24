package com.ixume.particlesTesting.emitter

import team.unnamed.mocha.runtime.compiled.MochaCompiledFunction
import team.unnamed.mocha.runtime.compiled.Named

interface EmitterMochaFunction : MochaCompiledFunction {
    fun eval(@Named("emitter_age") emitterAge: Double): Double
}