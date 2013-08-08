package me.ryanhamshire.GriefPrevention.CommandHandling;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public abstract class GriefPreventionCommand implements CommandExecutor {
	public abstract String[] getLabels();

	public abstract boolean onCommand(CommandSender sender, Command command, String label, String[] args);

}
