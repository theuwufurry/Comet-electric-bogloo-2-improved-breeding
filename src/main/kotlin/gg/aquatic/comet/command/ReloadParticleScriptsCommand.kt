package gg.aquatic.comet.command

import gg.aquatic.comet.parsing.ParticleJsonParser
import gg.aquatic.waves.command.ICommand
import org.bukkit.command.CommandSender

object ReloadParticleScriptsCommand : ICommand {

    override fun run(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("comet.admin")) return
        ParticleJsonParser.parseJsons()
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}