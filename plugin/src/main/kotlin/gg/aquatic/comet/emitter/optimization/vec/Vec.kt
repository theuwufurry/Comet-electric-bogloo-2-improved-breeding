package gg.aquatic.comet.emitter.optimization.vec

interface Vec {
    fun distanceSquared(other: Vec): Double
    fun clone(): Vec
    fun sub(other: Vec): Vec
    fun lengthSquared(): Double
    fun dot(other: Vec): Double
}