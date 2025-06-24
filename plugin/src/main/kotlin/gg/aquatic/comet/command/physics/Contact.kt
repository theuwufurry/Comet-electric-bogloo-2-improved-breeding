package gg.aquatic.comet.command.physics

data class Contact(
    val first: Body,
    val second: Body,
    val result: CollisionResult,
    var jSum: Double = 0.0
)
