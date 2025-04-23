package gg.aquatic.comet.api.parsing

import gg.aquatic.comet.api.emitter.AbstractUnrealizedEmitter

interface PostInit {
    fun realize(unrealizedEmitter: AbstractUnrealizedEmitter)
}