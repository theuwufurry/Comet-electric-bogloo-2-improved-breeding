package gg.aquatic.comet.command.physics

data class Mesh(
    val boundedFaces: List<BoundedAAFace>,
    val edges: Set<Edge>
)
