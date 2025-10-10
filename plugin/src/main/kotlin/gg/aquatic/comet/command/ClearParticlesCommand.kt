package gg.aquatic.comet.command

import gg.aquatic.comet.emitter.GlobalTicker
import gg.aquatic.comet.v2.runtime.WorldRuntime.Companion.cometRuntime
import gg.aquatic.waves.command.ICommand
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

object ClearParticlesCommand : ICommand {

    override fun run(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("comet.admin")) {
            sender.sendMessage("You don't have permission to do that!")
            return
        }

        GlobalTicker.killInstances()
        
        for (world in Bukkit.getWorlds()) {
            world.cometRuntime
        }

        sender.sendMessage("Emitters have been cleared!")
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}