package gg.aquatic.comet.command

import gg.aquatic.comet.api.parsing.resourcepack.ResourcepackCreator
import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.waves.command.ICommand
import org.bukkit.command.CommandSender

object ReloadParticleScriptsCommand : ICommand {

    override fun run(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("comet.admin")) {
            sender.sendMessage("You don't have permission to do that!")
            return
        }

        ParticleJsonParser.parseJsons()

        ResourcepackCreator.genPack()

        sender.sendMessage("Comet has been reloaded!")
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}