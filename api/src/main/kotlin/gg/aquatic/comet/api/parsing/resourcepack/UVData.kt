package gg.aquatic.comet.api.parsing.resourcepack

import org.joml.Vector2i

data class UVData(
    val coords: Vector2i,
    val size: Vector2i,
    val texture: String
)