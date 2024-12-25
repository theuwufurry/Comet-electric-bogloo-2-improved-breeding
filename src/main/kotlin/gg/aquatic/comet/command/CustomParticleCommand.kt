package gg.aquatic.comet.command

import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.waves.command.ICommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object CustomParticleCommand : ICommand {

    override fun run(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) return

        if (!sender.hasPermission("comet.admin")) return

        if (args.isNotEmpty()) {
            val spawnLoc = sender.eyeLocation
            ParticleJsonParser.jsonUnrealizedEmitters[args[0]]!!.realize(spawnLoc)
        } else {
            sender.sendMessage("You must specify an emitter to spawn!")
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}