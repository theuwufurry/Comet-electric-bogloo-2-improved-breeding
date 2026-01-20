package gg.aquatic.comet.command

public interface ICommand {
    public abstract fun run(sender: org.bukkit.command.CommandSender, args: kotlin.Array<out kotlin.String>): kotlin.Unit

    public abstract fun tabComplete(sender: org.bukkit.command.CommandSender, args: kotlin.Array<out kotlin.String>): kotlin.collections.List<kotlin.String>
}
