package gg.aquatic.comet.command

import gg.aquatic.waves.command.ICommand
import org.bukkit.command.CommandSender

object HelpCommand : ICommand {
    override fun run(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("comet.admin")) {
            sender.sendMessage("You don't have permission to do that!")
            return
        }

        sender.sendMessage(
            """
            /comet spawn <id> <world> <x> <y> <z> <yaw>
                Spawn an effect. This supports relative positions (using ~)
            /comet at <entity> <id>
                 Binds an emitter to an entity
            /comet help
            /comet reload
                Reload configurations
            /comet clear
                Clear particle effects
        """.trimIndent()
        )
    }

    override fun tabComplete(sender: CommandSender, args: Array<out String>): List<String> {
        return emptyList()
    }
}