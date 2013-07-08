package me.ryanhamshire.GriefPrevention.CommandHandling;

import java.io.File;

import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class GPReloadCommand extends GriefPreventionCommand{

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		Player player = (sender instanceof Player)?(Player)sender:null;
		if(player==null || player.hasPermission("griefprevention.reload")){
			GriefPrevention useinstance = GriefPrevention.instance; //we need a reference to re-enable it,
			useinstance.onDisable();
			useinstance.onEnable();
			//GriefPrevention.instance.reloadConfiguration();
			//FileConfiguration config = YamlConfiguration.loadConfiguration(new File(DataStore.configFilePath));
			//FileConfiguration outConfig = new YamlConfiguration();
			
			return true;
			}
		return false;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"gpreload"};
	}
	

}
