package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdjustBonusClaimBlocksCommand extends GriefPreventionCommand{

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		//adjustbonusclaimblocks <player> <amount> or [<permission>] amount
		//requires exactly two parameters, the other player or group's name and the adjustment
		if(args.length != 2) return false;
		Player player = (sender instanceof Player)?(Player)sender:null;
		GriefPrevention inst = GriefPrevention.instance;
		//parse the adjustment amount
		int adjustment;			
		try
		{
			adjustment = Integer.parseInt(args[1]);
		}
		catch(NumberFormatException numberFormatException)
		{
			return false;  //causes usage to be displayed
		}
		
		//if granting blocks to all players with a specific permission
		if(args[0].startsWith("[") && args[0].endsWith("]"))
		{
			String permissionIdentifier = args[0].substring(1, args[0].length() - 1);
			int newTotal = inst.dataStore.adjustGroupBonusBlocks(permissionIdentifier, adjustment);
			
			if(player!=null) GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustGroupBlocksSuccess, permissionIdentifier, String.valueOf(adjustment), String.valueOf(newTotal));
			if(player != null) GriefPrevention.AddLogEntry(player.getName() + " adjusted " + permissionIdentifier + "'s bonus claim blocks by " + adjustment + ".");
			
			return true;
		}
		
		//otherwise, find the specified player
		OfflinePlayer targetPlayer = inst.resolvePlayer(args[0]);
		if(targetPlayer == null)
		{
			if(player!=null) GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
			return true;
		}
		
		//give blocks to player
		PlayerData playerData = inst.dataStore.getPlayerData(targetPlayer.getName());
		playerData.bonusClaimBlocks += adjustment;
		inst.dataStore.savePlayerData(targetPlayer.getName(), playerData);
		
		GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustBlocksSuccess, targetPlayer.getName(), String.valueOf(adjustment), String.valueOf(playerData.bonusClaimBlocks));
		if(player != null) GriefPrevention.AddLogEntry(player.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".");
		
		return true;			
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"adjustbonusclaimblocks"};
	}
	

}
