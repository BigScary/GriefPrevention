package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaimsListCommand extends GriefPreventionCommand {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		//at most one parameter
		if(args.length > 1) return false;
		Player player = (sender instanceof Player)?(Player)sender:null;
		GriefPrevention inst = GriefPrevention.instance;
		//player whose claims will be listed
		OfflinePlayer otherPlayer;
		
		//if another player isn't specified, assume current player
		if(args.length < 1)
		{
			if(player != null)
				otherPlayer = player;
			else
				return false;
		}
		
		//otherwise if no permission to delve into another player's claims data
		else if(player != null && !player.hasPermission("griefprevention.deleteclaims"))
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimsListNoPermission);
			return true;
		}
					
		//otherwise try to find the specified player
		else
		{
			otherPlayer = inst.resolvePlayer(args[0]);
			if(otherPlayer == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
				return true;
			}
		}
		
		//load the target player's data
		PlayerData playerData = inst.dataStore.getPlayerData(otherPlayer.getName());
		GriefPrevention.sendMessage(player, TextMode.Instr, " " + playerData.accruedClaimBlocks + "(+" + (playerData.bonusClaimBlocks + inst.dataStore.getGroupBonusBlocks(otherPlayer.getName())) + ")=" + (playerData.accruedClaimBlocks + playerData.bonusClaimBlocks + inst.dataStore.getGroupBonusBlocks(otherPlayer.getName())));
		for(int i = 0; i < playerData.claims.size(); i++)
		{
			Claim claim = playerData.claims.get(i);
			GriefPrevention.sendMessage(player, TextMode.Instr, "  (Area:" + claim.getArea() + ") " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
		}
		
		if(playerData.claims.size() > 0)
			GriefPrevention.sendMessage(player, TextMode.Instr, "  Remaining Blocks: =" + playerData.getRemainingClaimBlocks());
		
		//drop the data we just loaded, if the player isn't online
		if(!otherPlayer.isOnline())
			inst.dataStore.clearCachedPlayerData(otherPlayer.getName());
		
		return true;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"claimslist"};
	}
	

}
