package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.ShovelMode;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaimModeCommands extends GriefPreventionCommand {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		//adminclaims
		
		Player player = (sender instanceof Player)?(Player)sender:null;
		if(player==null) return false;
		GriefPrevention inst = GriefPrevention.instance;
		
		if(command.getName().equalsIgnoreCase("adminclaims") && player != null)
			{
				PlayerData playerData = inst.dataStore.getPlayerData(player.getName());
				playerData.shovelMode = ShovelMode.Admin;
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdminClaimsMode);
				
				return true;
			}
			
		//basicclaims
		else if(command.getName().equalsIgnoreCase("basicclaims") && player != null)
		{
			PlayerData playerData = inst.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.Basic;
			playerData.claimSubdividing = null;
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.BasicClaimsMode);
			
			return true;
		}
		
		//subdivideclaims
		else if(command.getName().equalsIgnoreCase("subdivideclaims") && player != null)
		{
			PlayerData playerData = inst.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.Subdivide;
			playerData.claimSubdividing = null;
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionMode);
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionDemo);
			
			return true;
		}
		return false;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"adminclaims","basicclaims","subdivideclaims"};
	}

}
