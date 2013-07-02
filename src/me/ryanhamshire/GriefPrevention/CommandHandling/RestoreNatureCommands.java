package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.ShovelMode;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RestoreNatureCommands extends GriefPreventionCommand {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		GriefPrevention inst = GriefPrevention.instance;
		Player player = (sender instanceof Player)?(Player)sender:null;
		if(player==null) return false;
		if(command.getName().equalsIgnoreCase("restorenature") && player != null)
		{
			//change shovel mode
			PlayerData playerData = inst.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.RestoreNature;
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.RestoreNatureActivate);
			return true;
		}
		
		//restore nature aggressive mode
		else if(command.getName().equalsIgnoreCase("restorenatureaggressive") && player != null)
		{
			//change shovel mode
			PlayerData playerData = inst.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.RestoreNatureAggressive;
			GriefPrevention.sendMessage(player, TextMode.Warn, Messages.RestoreNatureAggressiveActivate);
			return true;
		}
		
		//restore nature fill mode
		else if(command.getName().equalsIgnoreCase("restorenaturefill") && player != null)
		{
			//change shovel mode
			PlayerData playerData = inst.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.RestoreNatureFill;
			
			//set radius based on arguments
			playerData.fillRadius = 2;
			if(args.length > 0)
			{
				try
				{
					playerData.fillRadius = Integer.parseInt(args[0]);
				}
				catch(Exception exception){ }
			}
			
			if(playerData.fillRadius < 0) playerData.fillRadius = 2;
			
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.FillModeActive, String.valueOf(playerData.fillRadius));
			return true;
		}
		return false;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"restorenature","restorenatureaggressive","restorenaturefill"};
	}

}
