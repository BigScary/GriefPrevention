package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UntrustCommand extends GriefPreventionCommand{

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		//requires exactly one parameter, the other player's name
		if(args.length != 1) return false;
		GriefPrevention inst = GriefPrevention.instance;
		Player player = (sender instanceof Player)?(Player)sender:null;
		if(player==null) return false;
		//determine which claim the player is standing in
		Claim claim = inst.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
		
		//bracket any permissions
		if(args[0].contains("."))
		{
			args[0] = "[" + args[0] + "]";
		}
		
		//determine whether a single player or clearing permissions entirely
		boolean clearPermissions = false;
		OfflinePlayer otherPlayer = null;
		//System.out.println("clearing perms for name:" + args[0]);
		if(args[0].equals("all"))				
		{
			if(claim == null || claim.allowEdit(player) == null)
			{
				clearPermissions = true;
			}
			else
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearPermsOwnerOnly);
				return true;
			}
		}
		
		else if((!args[0].startsWith("[") || !args[0].endsWith("]"))
			&& !args[0].toUpperCase().startsWith("G:") && ! args[0].startsWith("!"))
			{
				otherPlayer = inst.resolvePlayer(args[0]);
				if(!clearPermissions && otherPlayer == null && !args[0].equals("public"))
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
					return true;
				}
				
				//correct to proper casing
				if(otherPlayer != null)
					args[0] = otherPlayer.getName();
			}
		else if(args[0].startsWith("G:")){
			//make sure the group exists, otherwise show the message.
			String groupname = args[0].substring(2);
			if(!inst.config_player_groups.GroupExists(groupname)){
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.GroupNotFound);
				return true;
			}
		}
				
				
		
		//if no claim here, apply changes to all his claims
		if(claim == null)
		{
			PlayerData playerData = inst.dataStore.getPlayerData(player.getName());
			for(int i = 0; i < playerData.claims.size(); i++)
			{
				claim = playerData.claims.get(i);
				
				//if untrusting "all" drop all permissions
				if(clearPermissions)
				{	
					claim.clearPermissions();
				}
				
				//otherwise drop individual permissions
				else
				{
					claim.dropPermission(args[0]);
					claim.removeManager(args[0]);
					//claim.managers.remove(args[0]);
				}
				
				//save changes
				inst.dataStore.saveClaim(claim);
			}
			
			//beautify for output
			if(args[0].equals("public"))
			{
				args[0] = "the public";
			}
			
			//confirmation message
			if(!clearPermissions)
			{
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualAllClaims, args[0]);
			}
			else
			{
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustEveryoneAllClaims);
			}
		}			
		
		//otherwise, apply changes to only this claim
		else if(claim.allowGrantPermission(player) != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
		}
		else
		{
			//if clearing all
			if(clearPermissions)
			{
				claim.clearPermissions();
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClearPermissionsOneClaim);
			}
			
			//otherwise individual permission drop
			else
			{
				claim.dropPermission(args[0]);
				if(claim.allowEdit(player) == null)
				{
					claim.removeManager(args[0]);
											
					//beautify for output
					if(args[0].equals("public"))
					{
						args[0] = "the public";
					}
					
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualSingleClaim, args[0]);
				}
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustOwnerOnly, claim.getOwnerName());
				}
			}
			
			//save changes
			inst.dataStore.saveClaim(claim);										
		}
		
		return true;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"untrust"};
	}
	

}
