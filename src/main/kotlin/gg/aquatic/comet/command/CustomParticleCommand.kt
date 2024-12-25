package gg.aquatic.comet.command

import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.waves.command.ICommand
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender

object CustomParticleCommand : ICommand {

    override fun run(sender: CommandSender, args: Array<out String>) {

        if (!sender.hasPermission("comet.admin")) return

        // comet spawn <world> <x> <y> <z> <yaw>
        if (args.size < 5) {
            sender.sendMessage("Usage: /comet spawn <id> <world> <x> <y> <z> [yaw]")
            return
        }
        val emitter = ParticleJsonParser.jsonUnrealizedEmitters[args[0]]
        if (emitter == null) {
            sender.sendMessage("Unknown emitter: ${args[0]}")
            return
        }
        val world = Bukkit.getWorld(args[1])
        if (world == null) {
            sender.sendMessage("Unknown world: ${args[1]}")
            return
        }
        val x = args[2].toDoubleOrNull()
        if (x == null) {
            sender.sendMessage("Invalid x coordinate: ${args[2]}")
            return
        }
        val y = args[3].toDoubleOrNull()
        if (y == null) {
            sender.sendMessage("Invalid y coordinate: ${args[3]}")
            return
        }
        val z = args[4].toDoubleOrNull()
        if (z == null) {
            sender.sendMessage("Invalid z coordinate: ${args[4]}")
            return
        }
        val yaw = if (args.size > 5) args[5].toFloatOrNull() else 0f
        if (yaw == null) {
            sender.sendMessage("Invalid yaw: ${args[5]}")
            return
        }
        val location = Location(world, x, y, z,yaw,0f)
        emitter.realize(location)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}