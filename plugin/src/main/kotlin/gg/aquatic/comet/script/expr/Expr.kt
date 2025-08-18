package gg.aquatic.comet.script.expr

import gg.aquatic.comet.api.AbstractParticleEmitter

interface Expr<T> {
    fun eval(): Result<T>
}

fun <T> Result<T>.getOrPrint(emitterID: String): T? {
    return fold(
        { it },
        {
            AbstractParticleEmitter.INSTANCE.logger.severe("Error in $emitterID: ${it.message}")
            null
        }
    )
}