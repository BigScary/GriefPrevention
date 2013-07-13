package me.ryanhamshire.GriefPrevention.CommandHandling;

import java.util.Calendar;
import java.util.Date;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.tasks.PlayerRescueTask;

import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TrappedCommand extends GriefPreventionCommand{

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		//FEATURE: empower players who get "stuck" in an area where they don't have permission to build to save themselves
		GriefPrevention inst = GriefPrevention.instance;
		Player player = (sender instanceof Player)?(Player)sender:null;
		if(player==null) return false;
		WorldConfig wc = inst.getWorldCfg(player.getWorld());
		PlayerData playerData = inst.dataStore.getPlayerData(player.getName());
		Claim claim = inst.dataStore.getClaimAt(player.getLocation(), false);
		
		//if another /trapped is pending, ignore this slash command
		if(playerData.pendingTrapped)
		{
			return true;
		}
		
		//if the player isn't in a claim or has permission to build, tell him to man up
		if(claim == null || claim.allowBuild(player) == null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotTrappedHere);				
			return true;
		}
		
		//if the player is in the nether or end, he's screwed (there's no way to programmatically find a safe place for him)
		if(player.getWorld().getEnvironment() != Environment.NORMAL)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);				
			return true;
		}
		
		//if the player is in an administrative claim, he should contact an admin
		if(claim.isAdminClaim())
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);
			return true;
		}
		
		//check cooldown
		long lastTrappedUsage = playerData.lastTrappedUsage.getTime();
		long nextTrappedUsage = lastTrappedUsage + 1000 * 60 * wc.getClaimsTrappedCooldownMinutes(); 
		long now =  new Date().getTime();
		if(now < nextTrappedUsage)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedOnCooldown, String.valueOf(wc.getClaimsTrappedCooldownMinutes()), String.valueOf((nextTrappedUsage - now) / (1000 * 60) + 1));
			return true;
		}
		
		//send instructions
		GriefPrevention.sendMessage(player, TextMode.Instr, Messages.RescuePending);
		
		//create a task to rescue this player in a little while
		PlayerRescueTask task = new PlayerRescueTask(player, player.getLocation());
		inst.getServer().getScheduler().scheduleSyncDelayedTask(inst, task, 200L);  //20L ~ 1 second
		
		return true;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"trapped"};
	}

}
