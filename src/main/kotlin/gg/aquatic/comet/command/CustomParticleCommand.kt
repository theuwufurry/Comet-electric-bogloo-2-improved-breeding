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
        if (args.size < 6) {
            sender.sendMessage("Usage: /comet spawn <id> <world> <x> <y> <z> [yaw]")
            return
        }
        val emitter = ParticleJsonParser.jsonUnrealizedEmitters[args[1]]
        if (emitter == null) {
            sender.sendMessage("Unknown emitter: ${args[1]}")
            return
        }
        val world = Bukkit.getWorld(args[2])
        if (world == null) {
            sender.sendMessage("Unknown world: ${args[2]}")
            return
        }
        val x = args[3].toDoubleOrNull()
        if (x == null) {
            sender.sendMessage("Invalid x coordinate: ${args[3]}")
            return
        }
        val y = args[4].toDoubleOrNull()
        if (y == null) {
            sender.sendMessage("Invalid y coordinate: ${args[4]}")
            return
        }
        val z = args[5].toDoubleOrNull()
        if (z == null) {
            sender.sendMessage("Invalid z coordinate: ${args[5]}")
            return
        }
        val yaw = if (args.size > 6) args[6].toFloatOrNull() else 0f
        if (yaw == null) {
            sender.sendMessage("Invalid yaw: ${args[6]}")
            return
        }
        val location = Location(world, x, y, z,yaw,0f)
        emitter.realize(location)
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when(args.size) {
            1 -> ParticleJsonParser.jsonUnrealizedEmitters.keys.toList()
            2 -> Bukkit.getWorlds().map { it.name }
            else -> emptyList()
        }
    }
}