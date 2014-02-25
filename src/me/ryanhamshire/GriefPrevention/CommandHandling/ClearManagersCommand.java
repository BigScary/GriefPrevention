package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.*;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearManagersCommand extends GriefPreventionCommand {

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[] { "clearmanagers" };
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// TODO Auto-generated method stub
		DataStore dataStore = GriefPrevention.instance.dataStore;
		if (!(sender instanceof Player))
			return false;
		Player player = (Player) sender;

		Claim claimatpos = dataStore.getClaimAt(player.getLocation(), true);
		PlayerData pdata = dataStore.getPlayerData(player.getName());
		if (claimatpos != null) {
			if (claimatpos.isAdminClaim() && !player.hasPermission(PermNodes.AdminClaimsPermission)) {
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearManagersNotAdmin);
				return true;
			}
			if (pdata.ignoreClaims || claimatpos.getOwnerName().equalsIgnoreCase(player.getName())) {
				for (String currmanager : claimatpos.getManagerList()) {
					claimatpos.removeManager(currmanager);
					return true;
				}
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearManagersSuccess);
				return true;
			} else {
				// nope
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearManagersNotOwned);
				return true;
			}

		} else {
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearManagersNotFound);
			return true;
		}

	}

}
