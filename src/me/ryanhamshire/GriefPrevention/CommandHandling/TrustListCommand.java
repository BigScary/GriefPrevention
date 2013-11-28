package me.ryanhamshire.GriefPrevention.CommandHandling;

import java.util.ArrayList;

import me.ryanhamshire.GriefPrevention.*;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TrustListCommand extends GriefPreventionCommand {

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[] { "trustlist" };
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// TODO Auto-generated method stub
		Player player = (sender instanceof Player) ? (Player) sender : null;
        PlayerData pdata = GriefPrevention.instance.dataStore.getPlayerData(player.getName());
		if (player == null)
			return false;
		if(!EnsurePermission(player, label)) return true;
		GriefPrevention inst = GriefPrevention.instance;

		Claim claim = inst.dataStore.getClaimAt(player.getLocation(), true);

		// if no claim here, error message
		if (claim == null) {
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrustListNoClaim);
			return true;
		}

		// if no permission to manage permissions, error message
        // Change: also allow those with admin permission.
		String errorMessage = claim.allowGrantPermission(player);
		if (errorMessage != null && !pdata.ignoreClaims) {
			GriefPrevention.sendMessage(player, TextMode.Err, errorMessage);
			return true;
		}

		// otherwise build a list of explicit permissions by permission level
		// and send that to the player
		ArrayList<String> builders = new ArrayList<String>();
		ArrayList<String> containers = new ArrayList<String>();
		ArrayList<String> accessors = new ArrayList<String>();
		ArrayList<String> managers = new ArrayList<String>();
		claim.getPermissions(builders, containers, accessors, managers);

		player.sendMessage("Explicit permissions here:");

		StringBuilder permissions = new StringBuilder();
		permissions.append(ChatColor.GOLD + "M: ");

		if (managers.size() > 0) {
			for (String manager : managers)
				permissions.append(manager + " ");
		}

		player.sendMessage(permissions.toString());
		permissions = new StringBuilder();
		permissions.append(ChatColor.YELLOW + "B: ");

		if (builders.size() > 0) {
			for (String builder : builders)
				permissions.append(builder + " ");
		}

		player.sendMessage(permissions.toString());
		permissions = new StringBuilder();
		permissions.append(ChatColor.GREEN + "C: ");

		if (containers.size() > 0) {
			for (String container : containers)
				permissions.append(container + " ");
		}

		player.sendMessage(permissions.toString());
		permissions = new StringBuilder();
		permissions.append(ChatColor.BLUE + "A :");

		if (accessors.size() > 0) {
			for (String accessor : accessors)
				permissions.append(accessor + " ");
		}

		player.sendMessage(permissions.toString());

		player.sendMessage("(M-anager, B-builder, C-ontainers, A-ccess)");

		return true;
	}

}
