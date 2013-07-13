package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TransferClaimCommand extends GriefPreventionCommand {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// if(args.length>2) return false;
		GriefPrevention inst = GriefPrevention.instance;

		if (sender instanceof Player) {

			Player p = (Player) sender;
			PlayerData pdata = inst.dataStore.getPlayerData(p.getName());
			Claim claim = inst.dataStore.getClaimAt(p.getLocation(), true);
			if (claim == null) {
				GriefPrevention.sendMessage(p, TextMode.Err,
						"There is no claim here.");
			}
			if (!p.hasPermission("GriefPrevention.TransferClaim")) {
				GriefPrevention.sendMessage(p, TextMode.Err,
						Messages.NoPermissionForCommand);
				return true;
			}
			if (!pdata.warnedAboutMajorDeletion) {
				GriefPrevention
						.sendMessage(p, TextMode.Warn,
								"Use /transferclaim again to make this claim an admin claim");
				pdata.warnedAboutMajorDeletion = true;
				return true;

			}

			try {
				inst.dataStore.changeClaimOwner(claim, "");
				pdata.warnedAboutMajorDeletion = false;
			} catch (Exception e) {
				GriefPrevention.sendMessage(p, TextMode.Instr,
						Messages.TransferTopLevel);
				return true;
			}

		}
		return true;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[] { "transferclaim" };
	}

}
