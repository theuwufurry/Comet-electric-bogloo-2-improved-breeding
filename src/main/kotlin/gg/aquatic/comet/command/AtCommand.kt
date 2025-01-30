package gg.aquatic.comet.command

import gg.aquatic.comet.emitter.parent.EntityParent
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.waves.command.ICommand
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

object AtCommand : ICommand {
    override fun run(sender: CommandSender, args: Array<out String>) {
        //comet at <entity> <id>
        if (args.size < 3) {
            sender.sendMessage("Usage: /comet at <entity> <id> {data}")
            return
        }

        val emitter = ParticleJsonParser.jsonUnrealizedEmitters[args[2]]
        if (emitter == null) {
            sender.sendMessage("Unknown emitter: ${args[2]}")
            return
        }

        val entities = Bukkit.selectEntities(sender, args[1])
        for (entity in entities) {
            val asParent = EntityParent(entity)
            emitter.realize(asParent, entity.location)
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return when (args.size) {
            2 -> ParticleJsonParser.jsonUnrealizedEmitters.keys.toList()
            else -> emptyList()
        }
    }
}