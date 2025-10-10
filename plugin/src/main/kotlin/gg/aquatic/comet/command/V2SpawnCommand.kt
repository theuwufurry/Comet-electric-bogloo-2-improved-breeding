package gg.aquatic.comet.command

import gg.aquatic.comet.api.emitter.environment.parseEnvironmentData
import gg.aquatic.comet.api.emitter.parent.pose
import gg.aquatic.comet.v2.parsing.V2Parser
import gg.aquatic.waves.command.ICommand
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.joml.Vector3d

object V2SpawnCommand : ICommand {
    override fun run(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("comet.admin")) {
            sender.sendMessage("You don't have permission to do that!")
            return
        }
        // comet spawn <id> <world> <x> <y> <z> <yaw>
        if (args.size < 6) {
            sender.sendMessage("Usage: /comet spawn2 <id> <world> <x> <y> <z> <yaw> <pitch> {data}")
            return
        }

        val senderPos = if (sender is Entity) {
            sender.location.toVector().toVector3d()
        } else Vector3d()

        val api = V2Parser.effects[args[1]]
        if (api == null) {
            sender.sendMessage("Unknown effect: ${args[1]}")
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

        val (yaw, pitch, data) = try {
            when (args.size) {
                7 -> {
                    Triple(0f, 0f, args[6].parseEnvironmentData())
                }

                in 8..9 -> {
                    val yaw = args[6].toFloatOrNull()
                    if (yaw == null) {
                        sender.sendMessage("Invalid yaw: ${args[6]}")
                        return
                    }

                    val pitch = args[7].toFloatOrNull()
                    if (pitch == null) {
                        sender.sendMessage("Invalid pitch: ${args[6]}")
                        return
                    }

                    val data = (if (args.size == 9) args[8] else "{}").parseEnvironmentData()
                    Triple(yaw, pitch, data)
                }

                else -> {
                    Triple(0f, 0f, "{}".parseEnvironmentData())
                }
            }
        } catch (ignored: Exception) {
            sender.sendMessage("Invalid data!")
            return
        }

        val location = Location(world, x, y, z, yaw, pitch)
        val pose = location.pose()
        api.realize(world, pose)
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
            1 -> V2Parser.effects.keys.filter { it.startsWith(args[0]) }.toList()
            2 -> Bukkit.getWorlds().map { it.name }.toMutableList().apply { add("~") }
            else -> emptyList()
        }
    }
}