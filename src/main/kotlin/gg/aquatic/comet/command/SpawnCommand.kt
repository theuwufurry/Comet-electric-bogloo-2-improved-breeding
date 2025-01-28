package gg.aquatic.comet.command

import com.google.gson.JsonParser
import gg.aquatic.comet.emitter.EnvironmentData
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.comet.parsing.asNumberOrNull
import gg.aquatic.waves.command.ICommand
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.joml.Vector3d

object SpawnCommand : ICommand {
    override fun run(sender: CommandSender, args: Array<out String>) {
        // comet spawn <id> <world> <x> <y> <z> <yaw>
        if (args.size < 6) {
            sender.sendMessage("Usage: /comet spawn <id> <world> <x> <y> <z> {data}")
            return
        }

        val senderPos = if (sender is Entity) {
            sender.location.toVector().toVector3d()
        } else Vector3d()

        val emitter = ParticleJsonParser.jsonUnrealizedEmitters[args[1]]
        if (emitter == null) {
            sender.sendMessage("Unknown emitter: ${args[1]}")
            return
        }

        val world = Bukkit.getWorld(args[2]) ?: (sender as? Entity)?.world
        if (world == null) {
            sender.sendMessage("Unknown world: ${args[2]}")
            return
        }

        val x = handlePos(args[3], senderPos.x)
        if (x == null) {
            sender.sendMessage("Invalid x coordinate: ${args[3]}")
            return
        }

        val y = handlePos(args[4], senderPos.y)
        if (y == null) {
            sender.sendMessage("Invalid y coordinate: ${args[4]}")
            return
        }

        val z = handlePos(args[5], senderPos.z)
        if (z == null) {
            sender.sendMessage("Invalid z coordinate: ${args[5]}")
            return
        }

        val data = (if (args.size > 6) args[6] else "{}").parseEnvironmentData()

        val location = Location(world, x, y, z, 0f, 0f)
        emitter.realize(null, location, data)
    }

    private fun handlePos(input: String, origin: Double): Double? {
        return input.toDoubleOrNull()
            ?: if (input.startsWith("~")) {
                if (input.length == 1) return origin
                input.substring(1).toDoubleOrNull()?.let { it + origin }
            } else null
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> ParticleJsonParser.jsonUnrealizedEmitters.keys.toList()
            2 -> Bukkit.getWorlds().map { it.name }.toMutableList().apply { add("~") }
            else -> emptyList()
        }
    }
}

fun String.parseEnvironmentData(): EnvironmentData {
    val root = JsonParser.parseString(this).asJsonObject
    val size = root["size"]?.asNumberOrNull()?.toDouble() ?: 1.0
    return EnvironmentData(size)
}