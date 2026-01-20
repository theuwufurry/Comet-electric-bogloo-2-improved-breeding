package gg.aquatic.comet.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class BaseCommand(
    name: String,
    description: String,
    aliases: MutableList<String> = mutableListOf(),
    val subCommands: MutableMap<String, ICommand> = mutableMapOf()
) : Command(name) {

    init {
        this.description = description
        this.aliases = aliases
    }

    /**
     * Executes the command or delegates to a subcommand if one exists.
     */
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) return true

        val cmd = subCommands[args[0]]
        if (cmd != null) {
            cmd.run(sender, args.drop(1).toTypedArray())
        }

        return true
    }

    /**
     * Tab completion for subcommands.
     */
    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            return subCommands.keys.filter { it.startsWith(args[0]) }
        }

        val cmd = subCommands[args[0]]
        return cmd?.tabComplete(sender, args.drop(1).toTypedArray()) ?: emptyList()
    }

    /**
     * Registers this command with the server under a given namespace.
     */
    fun registerCmd(namespace: String) {
        Bukkit.getCommandMap().register(namespace, this)
    }
}
