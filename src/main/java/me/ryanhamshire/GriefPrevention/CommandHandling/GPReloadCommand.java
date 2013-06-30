package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GPReloadCommand extends GriefPreventionCommand{

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		Player player = (sender instanceof Player)?(Player)sender:null;
		if(player==null || player.hasPermission("griefprevention.reload")){
			GriefPrevention.instance.onDisable();
			GriefPrevention.instance.onEnable();
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
