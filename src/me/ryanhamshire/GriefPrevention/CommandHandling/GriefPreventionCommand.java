package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PermNodes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class GriefPreventionCommand implements CommandExecutor {
	public abstract String[] getLabels();

	public abstract boolean onCommand(CommandSender sender, Command command, String label, String[] args);

	public boolean EnsurePermission(Player p,String commandLabel){
		if(p==null) return false;
		
		String checknode = PermNodes.getCommandPermission(commandLabel);
		
		if(!p.hasPermission(checknode)){
			//print out appropriate message.
			GriefPrevention.sendMessage(p, TextMode.Err, Messages.NoPermissionForCommand);
			
			return false;
		}
		
		return true;
		
	}
}
