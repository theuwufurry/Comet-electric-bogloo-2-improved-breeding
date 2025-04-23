package gg.aquatic.comet.command

import gg.aquatic.comet.emitter.GlobalTicker
import gg.aquatic.waves.command.ICommand
import org.bukkit.command.CommandSender

object ClearParticlesCommand : ICommand {

    override fun run(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("comet.admin")) return
        GlobalTicker.killInstances()

        sender.sendMessage("Emitters have been cleared!")
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}