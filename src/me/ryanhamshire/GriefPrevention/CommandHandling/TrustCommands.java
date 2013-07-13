package me.ryanhamshire.GriefPrevention.CommandHandling;

import java.util.ArrayList;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TrustCommands extends GriefPreventionCommand{

	
	private void handleTrustCommand(Player player, ClaimPermission permissionLevel, String recipientName) 
	{
		GriefPrevention inst = GriefPrevention.instance;
		//determine which claim the player is standing in
		Claim claim = inst.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/);
		
		//validate player or group argument
		String permission = null;
		OfflinePlayer otherPlayer = null;
		boolean isforceddenial = false;
		//if it starts with "!", remove it and set the forced denial value.
		//we use this flag to indicate to add in a "!" again when we set the perm.
		//This will have the effect of causing the logic to explicitly deny permissions for players that do not match.
		if(recipientName.startsWith("!")){
			isforceddenial=true;
			recipientName = recipientName.substring(1); //remove the exclamation for the rest of the parsing.
		}
		
		if(recipientName.startsWith("[") && recipientName.endsWith("]"))
		{
			permission = recipientName.substring(1, recipientName.length() - 1);
			if(permission == null || permission.isEmpty())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.InvalidPermissionID);
				return;
			}
		}
		
		else if(recipientName.contains("."))
		{
			permission = recipientName;
		}
		
		else
		{		
			otherPlayer = GriefPrevention.instance.resolvePlayer(recipientName);
			//addition: if it starts with G:, it indicates a group name, rather than a player name.
			
			if(otherPlayer == null && !recipientName.equals("public") && !recipientName.equals("all") &&
					!recipientName.toUpperCase().startsWith("G:"))
				
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
				return;
			}
			else if(recipientName.toUpperCase().startsWith("G:")){
				//keep it as is.
				//we will give trust to that group, that is...
				
			}
			
			else if(otherPlayer != null)
			{
				recipientName = otherPlayer.getName();
			}
			
			else
			{
				recipientName = "public";
			}
		}
		
		//determine which claims should be modified
		ArrayList<Claim> targetClaims = new ArrayList<Claim>();
		if(claim == null)
		{
			PlayerData playerData = inst.dataStore.getPlayerData(player.getName());
			for(int i = 0; i < playerData.claims.size(); i++)
			{
				targetClaims.add(playerData.claims.get(i));
			}
		}
		else
		{
			//check permission here
			if(claim.allowGrantPermission(player) != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
				return;
			}
			
			//see if the player has the level of permission he's trying to grant
			String errorMessage = null;
			
			//permission level null indicates granting permission trust
			if(permissionLevel == null)
			{
				errorMessage = claim.allowEdit(player);
				if(errorMessage != null)
				{
					errorMessage = "Only " + claim.getOwnerName() + " can grant /PermissionTrust here."; 
				}
			}
			
			//otherwise just use the ClaimPermission enum values
			else
			{
				switch(permissionLevel)
				{
					case Access:
						errorMessage = claim.allowAccess(player);
						break;
					case Inventory:
						errorMessage = claim.allowContainers(player);
						break;
					default:
						errorMessage = claim.allowBuild(player);					
				}
			}
			
			//error message for trying to grant a permission the player doesn't have
			if(errorMessage != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantGrantThatPermission);
				return;
			}
			
			targetClaims.add(claim);
		}
		
		//if we didn't determine which claims to modify, tell the player to be specific
		if(targetClaims.size() ==  0)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.GrantPermissionNoClaim);
			return;
		}
		//if forcedenial is true, we will add the exclamation back to the name for addition.
		if(isforceddenial) recipientName = "!" + recipientName;
		//apply changes
		for(int i = 0; i < targetClaims.size(); i++)
		{
			Claim currentClaim = targetClaims.get(i);
			if(permissionLevel == null)
			{
				if(!currentClaim.isManager(recipientName))
				{
					currentClaim.addManager(recipientName);
				}
			}
			else
			{				
				currentClaim.setPermission(recipientName, permissionLevel);
			}
			inst.dataStore.saveClaim(currentClaim);
		}
		
		//notify player
		if(recipientName.equals("public")) recipientName = inst.dataStore.getMessage(Messages.CollectivePublic);
		String permissionDescription;
		if(permissionLevel == null)
		{
			permissionDescription = inst.dataStore.getMessage(Messages.PermissionsPermission);
		}
		else if(permissionLevel == ClaimPermission.Build)
		{
			permissionDescription = inst.dataStore.getMessage(Messages.BuildPermission);
		}		
		else if(permissionLevel == ClaimPermission.Access)
		{
			permissionDescription = inst.dataStore.getMessage(Messages.AccessPermission);
		}
		else //ClaimPermission.Inventory
		{
			permissionDescription = inst.dataStore.getMessage(Messages.ContainersPermission);
		}
		
		String location;
		if(claim == null)
		{
			
			location = inst.dataStore.getMessage(Messages.LocationAllClaims);
		}
		else
		{
			location = inst.dataStore.getMessage(Messages.LocationCurrentClaim);
		}
		String userecipientName = recipientName;
		if(userecipientName.toUpperCase().startsWith("G:")){
			userecipientName="Group " + userecipientName.substring(2);
		}
		GriefPrevention.sendMessage(player, TextMode.Success, Messages.GrantPermissionConfirmation, recipientName, permissionDescription, location);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		Player player = (sender instanceof Player)?(Player)sender:null;
		if(command.getName().equalsIgnoreCase("trust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			//most trust commands use this helper method, it keeps them consistent
			this.handleTrustCommand(player, ClaimPermission.Build, args[0]);
			
			return true;
		}
		else if(command.getName().equalsIgnoreCase("accesstrust") && player!=null){
			
			if(args.length!=1) return false;
			this.handleTrustCommand(player,ClaimPermission.Access,args[0]);
			return true;
			
		}
		else if(command.getName().equalsIgnoreCase("containertrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			this.handleTrustCommand(player, ClaimPermission.Inventory, args[0]);
			
			return true;
		}
		
		//permissiontrust <player>
		else if(command.getName().equalsIgnoreCase("permissiontrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			this.handleTrustCommand(player, null, args[0]);  //null indicates permissiontrust to the helper method
			
			return true;
		}
		
		
		
		return false;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"trust","containertrust","accesstrust","permissiontrust"};
	}
	

}
