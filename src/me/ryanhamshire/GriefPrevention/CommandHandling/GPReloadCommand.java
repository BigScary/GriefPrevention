package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PermNodes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GPReloadCommand extends GriefPreventionCommand {

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[] { "gpreload" };
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// TODO Auto-generated method stub
		Player player = (sender instanceof Player) ? (Player) sender : null;
		if(player!=null && !EnsurePermission(player,command.getName())) return true;
		if (player == null || player.hasPermission(PermNodes.ReloadPermission)) {
			GriefPrevention useinstance = GriefPrevention.instance; // we need a
																	// reference
																	// to
																	// re-enable
																	// it,
			useinstance.onDisable();
			useinstance.onEnable();
			// GriefPrevention.instance.reloadConfiguration();
			// FileConfiguration config =
			// YamlConfiguration.loadConfiguration(new
			// File(DataStore.configFilePath));
			// FileConfiguration outConfig = new YamlConfiguration();

			return true;
		}
		return false;
	}

}
