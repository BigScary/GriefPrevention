package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AbandonAllClaimsCommand extends GriefPreventionCommand{

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		GriefPrevention inst = GriefPrevention.instance;
		if(args.length > 1) return false;
		Player player = (sender instanceof Player)?(Player)sender:null;
		if(player==null) return false;
		WorldConfig wc = inst.getWorldCfg(player.getLocation().getWorld());
		boolean deletelocked = false;
		if(args.length > 0) {
			deletelocked = Boolean.parseBoolean(args[0]);
		}
		
		if(!wc.getAllowUnclaim())
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoCreativeUnClaim);
			return true;
		}
		
		//count claims
		PlayerData playerData = inst.dataStore.getPlayerData(player.getName());
		int originalClaimCount = playerData.claims.size();
		
		//check count
		if(originalClaimCount == 0)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.YouHaveNoClaims);
			return true;
		}
		
		//delete them
		inst.dataStore.deleteClaimsForPlayer(player.getName(), false, deletelocked);
		
		//inform the player
		int remainingBlocks = playerData.getRemainingClaimBlocks();
		if(deletelocked) {
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandonIncludingLocked, String.valueOf(remainingBlocks));
		}else {
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandonExcludingLocked, String.valueOf(remainingBlocks));
		}
		
		//revert any current visualization
		Visualization.Revert(player);
		
		return true;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"abandonallclaims"};
	}

	

}
