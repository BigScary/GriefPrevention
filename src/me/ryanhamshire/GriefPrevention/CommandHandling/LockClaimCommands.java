package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LockClaimCommands extends GriefPreventionCommand {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		DataStore dataStore = GriefPrevention.instance.dataStore;
		Player player = (sender instanceof Player)?(Player)sender:null;
		if(command.getName().equalsIgnoreCase("lockclaim") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 0) return false;
			
			Claim claim = dataStore.getClaimAt(player.getLocation(), true /*ignore height*/);
			if((player.hasPermission("griefprevention.lock") && claim.ownerName.equalsIgnoreCase(player.getName())) || player.hasPermission("griefprevention.adminlock")) {
				claim.neverdelete = true;
				dataStore.saveClaim(claim);
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClaimLocked);
			}
			
			return true;
		}
		
		//unlockclaim
		else if(command.getName().equalsIgnoreCase("unlockclaim") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 0) return false;
			
			Claim claim = dataStore.getClaimAt(player.getLocation(), true /*ignore height*/);
			if((player.hasPermission("griefprevention.lock") && claim.ownerName.equalsIgnoreCase(player.getName())) || player.hasPermission("griefprevention.adminlock")) {
				claim.neverdelete = false;
				dataStore.saveClaim(claim);
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClaimUnlocked);
			}
			
			return true;
		}
		return false;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"lockclaim","unlockclaim"};
	}

}
