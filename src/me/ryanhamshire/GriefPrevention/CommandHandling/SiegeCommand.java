package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SiegeCommand extends GriefPreventionCommand {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		//error message for when siege mode is disabled
		Player player = (sender instanceof Player)?(Player)sender:null;
		GriefPrevention inst = GriefPrevention.instance;
		DataStore dataStore = inst.dataStore;
		
		if(!inst.siegeEnabledForWorld(player.getWorld()))
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NonSiegeWorld);
			return true;
		}
		
		//requires one argument
		if(args.length > 1)
		{
			return false;
		}
		
		//can't start a siege when you're already involved in one
		Player attacker = player;
		PlayerData attackerData = inst.dataStore.getPlayerData(attacker.getName());
		if(attackerData.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.AlreadySieging);
			return true;
		}
		
		//can't start a siege when you're protected from pvp combat
		if(attackerData.pvpImmune)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantFightWhileImmune);
			return true;
		}
		
		//if a player name was specified, use that
		Player defender = null;
		if(args.length >= 1)
		{
			defender = inst.getServer().getPlayer(args[0]);
			if(defender == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
				return true;
			}
		}
		
		//otherwise use the last player this player was in pvp combat with 
		else if(attackerData.lastPvpPlayer.length() > 0)
		{
			defender = inst.getServer().getPlayer(attackerData.lastPvpPlayer);
			if(defender == null)
			{
				return false;
			}
		}
		
		else
		{
			return false;
		}
		//defender cannot be attacker.
		if(defender.getName().equalsIgnoreCase(attacker.getName())){
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantSiegeYourself);
			return true;
		}
		
		
		//victim must not be under siege already
		PlayerData defenderData = inst.dataStore.getPlayerData(defender.getName());
		if(defenderData.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.AlreadyUnderSiegePlayer);
			return true;
		}
		
		//victim must not be pvp immune
		if(defenderData.pvpImmune)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoSiegeDefenseless);
			return true;
		}
		
		Claim defenderClaim = inst.dataStore.getClaimAt(defender.getLocation(), false, null);
		
		//defender must have some level of permission there to be protected
		if(defenderClaim == null || defenderClaim.allowAccess(defender) != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotSiegableThere);
			return true;
		}									
		
		//attacker must be close to the claim he wants to siege
		if(!defenderClaim.isNear(attacker.getLocation(), 25))
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeTooFarAway);
			return true;
		}
		
		//claim can't be under siege already
		if(defenderClaim.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.AlreadyUnderSiegeArea);
			return true;
		}
		
		//can't siege admin claims
		if(defenderClaim.isAdminClaim())
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoSiegeAdminClaim);
			return true;
		}
		
		//can't be on cooldown
		if(dataStore.onCooldown(attacker, defender, defenderClaim))
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeOnCooldown);
			return true;
		}
		
		//start the siege
		if(dataStore.startSiege(attacker, defender, defenderClaim)){			

		   //confirmation message for attacker, warning message for defender
		   GriefPrevention.sendMessage(defender, TextMode.Warn, Messages.SiegeAlert, attacker.getName());
		   GriefPrevention.sendMessage(player, TextMode.Success, Messages.SiegeConfirmed, defender.getName());
		   return true;
		}
		return false;
	}
	
	

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"siege"};
	}
	

}
