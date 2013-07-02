package me.ryanhamshire.GriefPrevention.CommandHandling;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class GriefPreventionCommand implements CommandExecutor {
	public abstract boolean onCommand(CommandSender sender, Command command,
			String label, String[] args);
	public abstract String[] getLabels();
	
	
	
	

}
