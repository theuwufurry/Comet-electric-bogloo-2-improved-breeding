package gg.aquatic.comet.command

import gg.aquatic.comet.api.emitter.environment.parseEnvironmentData
import gg.aquatic.comet.api.EntityMount
import gg.aquatic.comet.api.emitter.parent.pose
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.waves.command.ICommand
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender

object MountedSpawnCommand : ICommand {
    override fun run(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("comet.admin")) {
            sender.sendMessage("You don't have permission to do that!")
            return
        }
        //comet at <entity> <id>
        if (args.size < 3) {
            sender.sendMessage("Usage: /comet mount <entity> <id> {data}")
            return
        }

        val emitter = ParticleJsonParser.jsonUnrealizedEmitters[args[2]]
        if (emitter == null) {
            sender.sendMessage("Unknown emitter: ${args[2]}")
            return
        }

        val data = try {
            (if (args.size > 3) args[3] else "{}").parseEnvironmentData()
        } catch (ignored: Exception) {
            sender.sendMessage("Invalid data!")
            return
        }

        val entities = Bukkit.selectEntities(sender, args[1])
        for (entity in entities) {
            val l = Location(
                entity.location.world,
                entity.location.x,
                entity.location.y,
                entity.location.z,
            )

            emitter.realize(
                pose = l.pose(),
                mount = EntityMount(entity),
                environmentData = data,
            )
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            2 -> ParticleJsonParser.jsonUnrealizedEmitters.filter { it.value.isListed && it.key.startsWith(args[1]) }.keys.toList()
            else -> emptyList()
        }
    }
}
