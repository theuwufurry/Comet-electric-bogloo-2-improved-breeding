package gg.aquatic.comet.command

import gg.aquatic.comet.command.physics.Body
import gg.aquatic.comet.command.physics.Contact
import gg.aquatic.comet.command.physics.Mesh
import org.bukkit.World

class PhysicsWorld(
    val world: World
) {
    val bodies: MutableList<Body> = mutableListOf()
    var contacts: MutableList<Contact> = mutableListOf()
    var meshes: MutableList<Mesh> = mutableListOf()
}