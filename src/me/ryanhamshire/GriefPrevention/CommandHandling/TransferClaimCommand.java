package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PermNodes;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.events.ClaimTransferEvent;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * TransferClaim transfers a claim from one player to another. This is sort of
 * like /giveclaim but /giveclaim can only be used by the claim owner.
 * 
 */
public class TransferClaimCommand extends GriefPreventionCommand {

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[] { "transferclaim" };
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// if(args.length>2) return false;
		GriefPrevention inst = GriefPrevention.instance;
		String OriginalOwner;
		String TargetOwner;
		if (!(sender instanceof Player))
			return false;
		Player player = (Player) sender;
		if(!EnsurePermission(player, label)) return true;
		PlayerData pd = inst.dataStore.getPlayerData(player.getName());
		Claim inclaim = inst.dataStore.getClaimAt(player.getLocation(), true);
		boolean toAdmin = false;
		if (inclaim == null) {
			// not inside a claim, so not valid.
			GriefPrevention.sendMessage(player, TextMode.Err, "There is no claim here.");

		} else {
			toAdmin = !inclaim.isAdminClaim();
		}
		// /TransferClaim
		// when given no arguments, /TransferClaim will make an owned claim into
		// an admin claim.
		// requires transferclaim and adminclaims permissions.
		if (args.length == 0) {
			// with no arguments, the player must be inside a claim AND that
			// claim must
			// not be an admin claim.
			// check permissions.
			if (!(player.hasPermission(PermNodes.AdminClaimsPermission) && player.hasPermission(PermNodes.TransferClaimsPermission))) {
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionForCommand);
				return true;
			}
			// otherwise, the appropriate perms are present.
			// make sure that they have been warned.
			if (!pd.getWarned("TransferClaim")) {
				// they have not been warned, tell them what will happen and how
				// to proceed.
				pd.setWarned("TransferClaim");
				GriefPrevention.sendMessage(player, TextMode.Info, "use /TransferClaim again to make this claim an admin claim");
				return true;

			} else {
				// warned... make admin claim.
				ClaimTransferEvent te = new ClaimTransferEvent(inclaim, "");
				Bukkit.getPluginManager().callEvent(te);
				if (!te.isCancelled()) {
					String previousOwner = inclaim.getOwnerName();
					try {
						inst.dataStore.changeClaimOwner(inclaim, "");
						GriefPrevention.sendMessage(player, TextMode.Success, "This claim is now an administrator claim, and no longer belongs to " + previousOwner + ".");
					} catch (Exception exx) {
						GriefPrevention.sendMessage(player, TextMode.Err, "TransferClaim Exception " + exx.getMessage());
					}
					return true;
				}
			}

		} else if (args.length == 1) {
			// one argument: transfer FROM adminclaim or another player, to the
			// given player.
			// this requires higher perms than giveclaim, FWIW.
			String targetplayer = args[0];
			// require perms.
			if (!(player.hasPermission(PermNodes.AdminClaimsPermission) && player.hasPermission(PermNodes.TransferClaimsPermission))) {
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionForCommand);
				return true;
			} else {
				String previousOwner = inclaim.getOwnerName();

				ClaimTransferEvent te = new ClaimTransferEvent(inclaim, targetplayer);
				Bukkit.getPluginManager().callEvent(te);
				if (!te.isCancelled()) {
					try {
						inst.dataStore.changeClaimOwner(inclaim, targetplayer);
						GriefPrevention.sendMessage(player, TextMode.Success, "Claim ownership transferred from " + previousOwner + " to " + targetplayer + ".");
						pd.setWarned("TransferClaim",false);
					} catch (Exception exx) {
						GriefPrevention.sendMessage(player, TextMode.Err, "TransferClaim Exception " + exx.getMessage());
					}
				}

				return true;
			}

		}

		return false;
	}

}
