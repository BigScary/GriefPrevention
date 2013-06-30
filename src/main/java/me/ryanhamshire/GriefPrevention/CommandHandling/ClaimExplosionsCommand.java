package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaimExplosionsCommand extends GriefPreventionCommand{

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		//determine which claim the player is standing in
		Player player = (sender instanceof Player)?(Player)sender:null;
		GriefPrevention inst = GriefPrevention.instance;
		
		Claim claim = inst.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
		
		if(claim == null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
			
		}
		
		else
		{
			String noBuildReason = claim.allowBuild(player);
			if(noBuildReason != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
				return true;
			}
			
			if(claim.areExplosivesAllowed)
			{
				claim.areExplosivesAllowed = false;
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.ExplosivesDisabled);
			}
			else
			{
				claim.areExplosivesAllowed = true;
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.ExplosivesEnabled);
			}
		}

		
		return true;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"claimexplosions"};
	}
	

}
