package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeathBlowCommand extends GriefPreventionCommand{

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		//deathblow <player> [recipientPlayer]
		
			//requires at least one parameter, the target player's name
			if(args.length < 1) return false;
			Player player = (sender instanceof Player)?(Player)sender:null;
			GriefPrevention inst = GriefPrevention.instance;
			//try to find that player
			Player targetPlayer = inst.getServer().getPlayer(args[0]);
			if(targetPlayer == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
				return true;
			}
			
			//try to find the recipient player, if specified
			Player recipientPlayer = null;
			if(args.length > 1)
			{
				recipientPlayer = inst.getServer().getPlayer(args[1]);
				if(recipientPlayer == null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
					return true;
				}
			}
			
			//if giving inventory to another player, teleport the target player to that receiving player
			if(recipientPlayer != null)
			{
				targetPlayer.teleport(recipientPlayer);
			}
			
			//otherwise, plan to "pop" the player in place
			else
			{
				//if in a normal world, shoot him up to the sky first, so his items will fall on the surface.
				if(targetPlayer.getWorld().getEnvironment() == Environment.NORMAL)
				{
					Location location = targetPlayer.getLocation();
					location.setY(location.getWorld().getMaxHeight());
					targetPlayer.teleport(location);
				}
			}
			 
			//kill target player
			targetPlayer.setHealth(0);
			
			//log entry
			if(player != null)
			{
				GriefPrevention.AddLogEntry(player.getName() + " used /DeathBlow to kill " + targetPlayer.getName() + ".");
			}
			else
			{
				GriefPrevention.AddLogEntry("Killed " + targetPlayer.getName() + ".");
			}
			
			return true;
	
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"deathblow"};
	}
	

}
