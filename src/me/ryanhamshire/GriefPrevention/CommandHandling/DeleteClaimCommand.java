package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeleteClaimCommand extends GriefPreventionCommand {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// determine which claim the player is standing in
		
		GriefPrevention inst = GriefPrevention.instance;
		Player player = (sender instanceof Player) ? (Player) sender : null;
		if (player == null)
			return false;
		
		WorldConfig wc = inst.getWorldCfg(player.getWorld());
		
		if (command.getName().equalsIgnoreCase("deleteclaim")) {
			Claim claim = inst.dataStore.getClaimAt(player.getLocation(),
					true /* ignore height */);

			if (claim == null) {
				GriefPrevention.sendMessage(player, TextMode.Err,
						Messages.DeleteClaimMissing);
			}

			else {
				// deleting an admin claim additionally requires the adminclaims
				// permission
				if (!claim.isAdminClaim()
						|| player.hasPermission("griefprevention.adminclaims")) {
					PlayerData playerData = inst.dataStore.getPlayerData(player
							.getName());
					if (claim.children.size() > 0
							&& !playerData.warnedAboutMajorDeletion) {
						GriefPrevention.sendMessage(player, TextMode.Warn,
								Messages.DeletionSubdivisionWarning);
						playerData.warnedAboutMajorDeletion = true;
					} else if (claim.neverdelete
							&& !playerData.warnedAboutMajorDeletion) {
						GriefPrevention.sendMessage(player, TextMode.Warn,
								Messages.DeleteLockedClaimWarning);
						playerData.warnedAboutMajorDeletion = true;
					} else {
						claim.removeSurfaceFluids(null);
						inst.dataStore.deleteClaim(claim);

						// if in a creative mode world, /restorenature the claim
						if (wc.getAutoRestoreUnclaimed()
								&& GriefPrevention.instance
										.creativeRulesApply(claim
												.getLesserBoundaryCorner())) {
							GriefPrevention.instance.restoreClaim(claim, 0);
						}

						GriefPrevention.sendMessage(player, TextMode.Success,
								Messages.DeleteSuccess);
						GriefPrevention.AddLogEntry(player.getName()
								+ " deleted "
								+ claim.getOwnerName()
								+ "'s claim at "
								+ GriefPrevention
										.getfriendlyLocationString(claim
												.getLesserBoundaryCorner()));

						// revert any current visualization
						Visualization.Revert(player);

						playerData.warnedAboutMajorDeletion = false;
					}
				} else {
					GriefPrevention.sendMessage(player, TextMode.Err,
							Messages.CantDeleteAdminClaim);
				}
			}
		}
		// deleteallclaims <player>
		else if (command.getName().equalsIgnoreCase("deleteallclaims")) {
			// requires one or two parameters, the other player's name and
			// whether to delete locked claims.
			if (args.length < 1 && args.length > 2)
				return false;

			// try to find that player
			OfflinePlayer otherPlayer = inst.resolvePlayer(args[0]);
			if (otherPlayer == null) {
				GriefPrevention.sendMessage(player, TextMode.Err,
						Messages.PlayerNotFound);
				return true;
			}

			boolean deletelocked = false;
			if (args.length == 2) {
				deletelocked = Boolean.parseBoolean(args[1]);
			}

			// delete all that player's claims
			inst.dataStore.deleteClaimsForPlayer(otherPlayer.getName(), true,
					deletelocked);

			if (deletelocked) {
				GriefPrevention.sendMessage(player, TextMode.Success,
						Messages.DeleteAllSuccessIncludingLocked,
						otherPlayer.getName());
			} else {
				GriefPrevention.sendMessage(player, TextMode.Success,
						Messages.DeleteAllSuccessExcludingLocked,
						otherPlayer.getName());
			}
			if (player != null) {
				GriefPrevention.AddLogEntry(player.getName()
						+ " deleted all claims belonging to "
						+ otherPlayer.getName() + ".");

				// revert any current visualization
				Visualization.Revert(player);
			}

			return true;
		}
		if(command.getName().equalsIgnoreCase("deletealladminclaims"))
		{
			if(!player.hasPermission("griefprevention.deleteclaims"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoDeletePermission);
				return true;
			}
			
			//delete all admin claims
			inst.dataStore.deleteClaimsForPlayer("", true, true);  //empty string for owner name indicates an administrative claim
			
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.AllAdminDeleted);
			if(player != null)
			{
				GriefPrevention.AddLogEntry(player.getName() + " deleted all administrative claims.");
			
				//revert any current visualization
				Visualization.Revert(player);
			}
			
			return true;
		}

		return false;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[] { "deleteclaim", "deleteallclaims","deletealladminclaims" };
	}

}
