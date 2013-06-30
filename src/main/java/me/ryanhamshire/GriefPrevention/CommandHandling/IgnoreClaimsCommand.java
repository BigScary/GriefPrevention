package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class IgnoreClaimsCommand extends GriefPreventionCommand {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		if(!(sender instanceof Player)) return false;
		Player player = (Player)sender;
		PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getName());
		
		playerData.ignoreClaims = !playerData.ignoreClaims;
		
		//toggle ignore claims mode on or off
		if(!playerData.ignoreClaims)
		{
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.RespectingClaims);
		}
		else
		{
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.IgnoringClaims);
		}
		
		return true;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"ignoreclaims"};
	}

	
}
