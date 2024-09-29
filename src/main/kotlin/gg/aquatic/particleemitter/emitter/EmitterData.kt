package gg.aquatic.particleemitter.emitter

data class EmitterData(var age: Double) {
    constructor() : this(0.0)

    fun copyFrom(other: EmitterData) {
        this.age = other.age
    }
}