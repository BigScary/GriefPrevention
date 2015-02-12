/*
    GriefPreventionPlus Server Plugin for Minecraft
    Copyright (C) 2015 Antonino Kai Pocorobba
    (forked from GriefPrevention by Ryan Hamshire)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.kaikk.mc.gpp;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandExec implements CommandExecutor {
	//handles slash commands
	GriefPreventionPlus gpp = GriefPreventionPlus.instance;
	DataStore dataStore = gpp.dataStore; 
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		
		Player player = null;
		if (sender instanceof Player)  {
			player = (Player) sender;
		}

		//abandonclaim
		if(cmd.getName().equalsIgnoreCase("abandonclaim") && player != null)
		{
			return this.abandonClaimHandler(player, false);
		}		
		
		//abandontoplevelclaim
		if(cmd.getName().equalsIgnoreCase("abandontoplevelclaim") && player != null)
		{
			return this.abandonClaimHandler(player, true);
		}
		
		//ignoreclaims
		if(cmd.getName().equalsIgnoreCase("ignoreclaims") && player != null)
		{
			PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
			
			playerData.ignoreClaims = !playerData.ignoreClaims;
			
			//toggle ignore claims mode on or off
			if(!playerData.ignoreClaims)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.RespectingClaims);
			}
			else
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.IgnoringClaims);
			}
			
			return true;
		}
		
		//abandonallclaims
		else if(cmd.getName().equalsIgnoreCase("abandonallclaims") && player != null)
		{
			if(args.length != 0) return false;
			
			//count claims
			PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
			int originalClaimCount = playerData.getClaims().size();
			
			//check count
			if(originalClaimCount == 0)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.YouHaveNoClaims);
				return true;
			}
			
			//adjust claim blocks
			for(Claim claim : playerData.getClaims())
			{
			    playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - (int)Math.ceil((claim.getArea() * (1 - gpp.config_claims_abandonReturnRatio))));
			}
			
			//delete them
			gpp.dataStore.deleteClaimsForPlayer(player.getUniqueId(), false);
			
			//inform the player
			int remainingBlocks = playerData.getRemainingClaimBlocks();
			GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandon, String.valueOf(remainingBlocks));
			
			//revert any current visualization
			Visualization.Revert(player);
			
			return true;
		}
		
		//restore nature
		else if(cmd.getName().equalsIgnoreCase("restorenature") && player != null)
		{
			//change shovel mode
			PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
			playerData.shovelMode = ShovelMode.RestoreNature;
			GriefPreventionPlus.sendMessage(player, TextMode.Instr, Messages.RestoreNatureActivate);
			return true;
		}
		
		//restore nature aggressive mode
		else if(cmd.getName().equalsIgnoreCase("restorenatureaggressive") && player != null)
		{
			//change shovel mode
			PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
			playerData.shovelMode = ShovelMode.RestoreNatureAggressive;
			GriefPreventionPlus.sendMessage(player, TextMode.Warn, Messages.RestoreNatureAggressiveActivate);
			return true;
		}
		
		//restore nature fill mode
		else if(cmd.getName().equalsIgnoreCase("restorenaturefill") && player != null)
		{
			//change shovel mode
			PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
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
			
			GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.FillModeActive, String.valueOf(playerData.fillRadius));
			return true;
		}
		
		//trust <player>
		else if(cmd.getName().equalsIgnoreCase("trust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			//most trust commands use this helper method, it keeps them consistent
			this.handleTrustCommand(player, ClaimPermission.BUILD, args[0]);
			
			return true;
		}
		
		//transferclaim <player>
		else if(cmd.getName().equalsIgnoreCase("transferclaim") && player != null)
		{
			//which claim is the user in?
			Claim claim = gpp.dataStore.getClaimAt(player.getLocation(), true, null);
			if(claim == null)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Instr, Messages.TransferClaimMissing);
				return true;
			}
			
			//check additional permission for admin claims
			if(claim.isAdminClaim() && !player.hasPermission("griefprevention.adminclaims"))
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.TransferClaimPermission);
				return true;
			}

			UUID newOwnerID = GriefPreventionPlus.UUID1;
			String ownerName = "admin";

			if(args.length > 0)
			{
				OfflinePlayer targetPlayer = GriefPreventionPlus.instance.resolvePlayerByName(args[0]);
				if(targetPlayer == null)
				{
					GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
					return true;
				}
				newOwnerID = targetPlayer.getUniqueId();
				ownerName = targetPlayer.getName();
			}
			
			//change ownerhsip
			try
			{
				gpp.dataStore.changeClaimOwner(claim, newOwnerID);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				GriefPreventionPlus.sendMessage(player, TextMode.Instr, Messages.TransferTopLevel);
				return true;
			}
			
			//confirm
			GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.TransferSuccess);
			GriefPreventionPlus.AddLogEntry(player.getName() + " transferred a claim at " + GriefPreventionPlus.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " to " + ownerName + ".");
			
			return true;
		}
		
		//trustlist
		else if(cmd.getName().equalsIgnoreCase("trustlist") && player != null)
		{
			Claim claim = gpp.dataStore.getClaimAt(player.getLocation(), true, null);
			
			//if no claim here, error message
			if(claim == null)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.TrustListNoClaim);
				return true;
			}
			
			//if no permission to manage permissions, error message
			String errorMessage = claim.allowGrantPermission(player);
			if(errorMessage != null) {
				GriefPreventionPlus.sendMessage(player, TextMode.Err, errorMessage);
				return true;
			}
			
			//otherwise build a list of explicit permissions by permission level
			//and send that to the player
			ArrayList<String> builders = new ArrayList<String>();
			ArrayList<String> containers = new ArrayList<String>();
			ArrayList<String> accessors = new ArrayList<String>();
			ArrayList<String> managers = new ArrayList<String>();
			claim.getPermissions(builders, containers, accessors, managers);
			
			player.sendMessage("Explicit permissions here:");
			
			StringBuilder permissions = new StringBuilder();
			permissions.append(ChatColor.GOLD + "M: ");
			
			if(managers.size() > 0) {
				for(int i = 0; i < managers.size(); i++)
					permissions.append(managers.get(i) + " ");
			}
			
			player.sendMessage(permissions.toString());
			permissions = new StringBuilder();
			permissions.append(ChatColor.YELLOW + "B: ");
			
			if(builders.size() > 0) {				
				for(int i = 0; i < builders.size(); i++)
					permissions.append(builders.get(i) + " ");		
			}
			
			player.sendMessage(permissions.toString());
			permissions = new StringBuilder();
			permissions.append(ChatColor.GREEN + "C: ");				
			
			if(containers.size() > 0) {
				for(int i = 0; i < containers.size(); i++)
					permissions.append(containers.get(i) + " ");		
			}
			
			player.sendMessage(permissions.toString());
			permissions = new StringBuilder();
			permissions.append(ChatColor.BLUE + "A: ");
				
			if(accessors.size() > 0) {
				for(int i = 0; i < accessors.size(); i++)
					permissions.append(accessors.get(i) + " ");			
			}
			
			player.sendMessage(permissions.toString());
			
			player.sendMessage("(M-anager, B-uilder, C-ontainers, A-ccess)");
			
			return true;
		}
		
		//untrust <player> or untrust [<group>]
		else if(cmd.getName().equalsIgnoreCase("untrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;

			String permBukkit = null;
			// if a permissionBukkit
			if(args[0].startsWith("[") && args[0].endsWith("]")) {
				permBukkit=args[0].substring(1, args[0].length()-1);
			}
			
			//determine which claim the player is standing in
			Claim claim = gpp.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);

			if (claim==null) { // all player's claims
				if (gpp.dataStore.getPlayerData(player.getUniqueId()).getClaims().size()>0) {
					GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.YouHaveNoClaims);
					return false;
				}
				
				if(args[0].equals("all")) { // clear all permissions from player's claims
					gpp.dataStore.clearPermissionsOnPlayerClaims(player.getUniqueId());
					
					GriefPreventionPlus.AddLogEntry(player.getName()+" removed all permissions from his claims");
					
					GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.UntrustEveryoneAllClaims);
				} else {// remove specific permission from player's claims
					if(permBukkit!=null) { // permissionbukkit
						if(args[0].length()<3) {
							GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.InvalidPermissionID);
							return false;
						}
						gpp.dataStore.dropPermissionOnPlayerClaims(player.getUniqueId(), permBukkit);
						GriefPreventionPlus.AddLogEntry(player.getName()+" removed "+args[0]+" permission from his claims");
						
					} else if(args[0].equals("public")) { // public
						gpp.dataStore.dropPermissionOnPlayerClaims(player.getUniqueId(), GriefPreventionPlus.UUID0);
						GriefPreventionPlus.AddLogEntry(player.getName()+" removed public permission from his claims");
					} else { // player?
						OfflinePlayer otherPlayer = gpp.resolvePlayerByName(args[0]);
						if (otherPlayer==null) {// player not found
							GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
							return true;
						}
						gpp.dataStore.dropPermissionOnPlayerClaims(player.getUniqueId(), otherPlayer.getUniqueId());
						GriefPreventionPlus.AddLogEntry(player.getName()+" removed "+otherPlayer.getName()+" permission from his claims");
					}
					GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.UntrustIndividualAllClaims, args[0]);
				}
			} else { // player's standing claim setting
				if(args[0].equals("all")) { // clear claim's perms
					if (claim.allowEdit(player) != null) { // no permissions
						GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.ClearPermsOwnerOnly);
						return true;
					}
					gpp.dataStore.dbUnsetPerm(claim.id);
					claim.clearMemoryPermissions();
					
					GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.UntrustOwnerOnly, claim.getOwnerName());
					GriefPreventionPlus.AddLogEntry(player.getName()+" removed all permissions from claim id "+claim.id);
				} else {
					if(claim.allowGrantPermission(player) != null) {
						GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
						return true;
					}

					if(permBukkit != null) { // permissionbukkit
						if(args[0].length()<3) {
							GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.InvalidPermissionID);
							return false;
						}
						
						// a manager needs the same permission or higher
						if (claim.checkPermission(player, claim.getPermission(permBukkit)) != null) {
							GriefPreventionPlus.sendMessage(player, TextMode.Err, "You need an higher permission to remove this permission.");
							return false;
						}
						
						gpp.dataStore.dbUnsetPerm(claim.id, permBukkit);
						claim.unsetPermission(permBukkit);
						GriefPreventionPlus.AddLogEntry(player.getName()+" removed "+args[0]+" permission from claim id "+claim.id);
					} else if(args[0].equals("public")) { // public
						gpp.dataStore.dbUnsetPerm(claim.id, GriefPreventionPlus.UUID0);
						claim.unsetPermission(GriefPreventionPlus.UUID0);
						GriefPreventionPlus.AddLogEntry(player.getName()+" removed public permission from claim id "+claim.id);
					} else { // player?
						OfflinePlayer otherPlayer = gpp.resolvePlayerByName(args[0]);
						if (otherPlayer==null) {// player not found
							GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
							return true;
						}
						
						// a manager needs the same permission or higher
						if (claim.checkPermission(player, claim.getPermission(otherPlayer.getUniqueId())) != null) {
							GriefPreventionPlus.sendMessage(player, TextMode.Err, "You need an higher permission to remove this permission.");
							return false;
						}
						
						gpp.dataStore.dbUnsetPerm(claim.id, otherPlayer.getUniqueId());
						claim.unsetPermission(otherPlayer.getUniqueId());
						GriefPreventionPlus.AddLogEntry(player.getName()+" removed "+otherPlayer.getName()+" permission from claim id "+claim.id);
					}
					GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.UntrustIndividualSingleClaim, args[0]);
				}
			}

			return true;
		}
		
		//accesstrust <player>
		else if(cmd.getName().equalsIgnoreCase("accesstrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			this.handleTrustCommand(player, ClaimPermission.ACCESS, args[0]);
			
			return true;
		}
		
		//containertrust <player>
		else if(cmd.getName().equalsIgnoreCase("containertrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			this.handleTrustCommand(player, ClaimPermission.CONTAINER, args[0]);
			
			return true;
		}
		
		//permissiontrust <player>
		else if(cmd.getName().equalsIgnoreCase("permissiontrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			this.handleTrustCommand(player, ClaimPermission.MANAGE, args[0]);
			
			return true;
		}
		
		//buyclaimblocks
		else if(cmd.getName().equalsIgnoreCase("buyclaimblocks") && player != null)
		{
			//if economy is disabled, don't do anything
			if(GriefPreventionPlus.economy == null)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
				return true;
			}
			
			if(!player.hasPermission("griefprevention.buysellclaimblocks"))
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.NoPermissionForCommand);
				return true;
			}
			
			//if purchase disabled, send error message
			if(GriefPreventionPlus.instance.config_economy_claimBlocksPurchaseCost == 0)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.OnlySellBlocks);
				return true;
			}
			
			//if no parameter, just tell player cost per block and balance
			if(args.length != 1)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Info, Messages.BlockPurchaseCost, String.valueOf(GriefPreventionPlus.instance.config_economy_claimBlocksPurchaseCost), String.valueOf(GriefPreventionPlus.economy.getBalance(player)));
				return false;
			}
			
			else
			{
				//determine max purchasable blocks
				PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
				int maxPurchasable = GriefPreventionPlus.instance.config_claims_maxAccruedBlocks - playerData.getAccruedClaimBlocks();
				
				//if the player is at his max, tell him so
				if(maxPurchasable <= 0)
				{
					GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.ClaimBlockLimit);
					return true;
				}
				
				//try to parse number of blocks
				int blockCount;
				try
				{
					blockCount = Integer.parseInt(args[0]);
				}
				catch(NumberFormatException numberFormatException)
				{
					return false;  //causes usage to be displayed
				}
				
				if(blockCount <= 0)
				{
					return false;
				}
				
				//correct block count to max allowed
				if(blockCount > maxPurchasable)
				{
					blockCount = maxPurchasable;
				}
				
				//if the player can't afford his purchase, send error message
				double balance = GriefPreventionPlus.economy.getBalance(player);				
				double totalCost = blockCount * GriefPreventionPlus.instance.config_economy_claimBlocksPurchaseCost;				
				if(totalCost > balance)
				{
					GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.InsufficientFunds, String.valueOf(totalCost),  String.valueOf(balance));
				}
				
				//otherwise carry out transaction
				else
				{
					//withdraw cost
					GriefPreventionPlus.economy.withdrawPlayer(player, totalCost);
					
					//add blocks
					playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() + blockCount);
					gpp.dataStore.savePlayerData(player.getUniqueId(), playerData);
					
					//inform player
					GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.PurchaseConfirmation, String.valueOf(totalCost), String.valueOf(playerData.getRemainingClaimBlocks()));
				}
				
				return true;
			}
		}
		
		//sellclaimblocks <amount> 
		else if(cmd.getName().equalsIgnoreCase("sellclaimblocks") && player != null)
		{
			//if economy is disabled, don't do anything
			if(GriefPreventionPlus.economy == null)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
				return true;
			}
			
			if(!player.hasPermission("griefprevention.buysellclaimblocks"))
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.NoPermissionForCommand);
				return true;
			}
			
			//if disabled, error message
			if(GriefPreventionPlus.instance.config_economy_claimBlocksSellValue == 0)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.OnlyPurchaseBlocks);
				return true;
			}
			
			//load player data
			PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
			int availableBlocks = playerData.getRemainingClaimBlocks();
			
			//if no amount provided, just tell player value per block sold, and how many he can sell
			if(args.length != 1)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Info, Messages.BlockSaleValue, String.valueOf(GriefPreventionPlus.instance.config_economy_claimBlocksSellValue), String.valueOf(availableBlocks));
				return false;
			}
						
			//parse number of blocks
			int blockCount;
			try
			{
				blockCount = Integer.parseInt(args[0]);
			}
			catch(NumberFormatException numberFormatException)
			{
				return false;  //causes usage to be displayed
			}
			
			if(blockCount <= 0)
			{
				return false;
			}
			
			//if he doesn't have enough blocks, tell him so
			if(blockCount > availableBlocks)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.NotEnoughBlocksForSale);
			}
			
			//otherwise carry out the transaction
			else
			{					
				//compute value and deposit it
				double totalValue = blockCount * GriefPreventionPlus.instance.config_economy_claimBlocksSellValue;					
				GriefPreventionPlus.economy.depositPlayer(player, totalValue);
				
				//subtract blocks
				playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - blockCount);
				gpp.dataStore.savePlayerData(player.getUniqueId(), playerData);
				
				//inform player
				GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.BlockSaleConfirmation, String.valueOf(totalValue), String.valueOf(playerData.getRemainingClaimBlocks()));
			}
			
			return true;
		}		
		
		//adminclaims
		else if(cmd.getName().equalsIgnoreCase("adminclaims") && player != null)
		{
			PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
			playerData.shovelMode = ShovelMode.Admin;
			GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.AdminClaimsMode);
			
			return true;
		}
		
		//basicclaims
		else if(cmd.getName().equalsIgnoreCase("basicclaims") && player != null)
		{
			PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
			playerData.shovelMode = ShovelMode.Basic;
			playerData.claimSubdividing = null;
			GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.BasicClaimsMode);
			
			return true;
		}
		
		//subdivideclaims
		else if(cmd.getName().equalsIgnoreCase("subdivideclaims") && player != null)
		{
			PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
			playerData.shovelMode = ShovelMode.Subdivide;
			playerData.claimSubdividing = null;
			GriefPreventionPlus.sendMessage(player, TextMode.Instr, Messages.SubdivisionMode);
			GriefPreventionPlus.sendMessage(player, TextMode.Instr, Messages.SubdivisionVideo2, DataStore.SUBDIVISION_VIDEO_URL);
			
			return true;
		}
		
		//deleteclaim
		else if(cmd.getName().equalsIgnoreCase("deleteclaim") && player != null)
		{
			//determine which claim the player is standing in
			Claim claim = gpp.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
			
			if(claim == null)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
			}
			
			else 
			{
				//deleting an admin claim additionally requires the adminclaims permission
				if(!claim.isAdminClaim() || player.hasPermission("griefprevention.adminclaims"))
				{
					PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
					if(claim.children.size() > 0 && !playerData.warnedAboutMajorDeletion)
					{
						GriefPreventionPlus.sendMessage(player, TextMode.Warn, Messages.DeletionSubdivisionWarning);
						playerData.warnedAboutMajorDeletion = true;
					}
					else
					{
						claim.removeSurfaceFluids(null);
						gpp.dataStore.deleteClaim(claim, true);
						
						//if in a creative mode world, /restorenature the claim
						if(GriefPreventionPlus.instance.creativeRulesApply(claim.world))
						{
							GriefPreventionPlus.instance.restoreClaim(claim, 0);
						}
						
						GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.DeleteSuccess);
						GriefPreventionPlus.AddLogEntry(player.getName() + " deleted " + claim.getOwnerName() + "'s claim at " + GriefPreventionPlus.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
						
						//revert any current visualization
						Visualization.Revert(player);
						
						playerData.warnedAboutMajorDeletion = false;
					}
				}
				else
				{
					GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.CantDeleteAdminClaim);
				}
			}

			return true;
		}
		
		else if(cmd.getName().equalsIgnoreCase("claimexplosions") && player != null)
		{
			//determine which claim the player is standing in
			Claim claim = gpp.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
			
			if(claim == null)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
			}
			
			else
			{
				String noBuildReason = claim.allowBuild(player, Material.TNT);
				if(noBuildReason != null)
				{
					GriefPreventionPlus.sendMessage(player, TextMode.Err, noBuildReason);
					return true;
				}
				
				if(claim.areExplosivesAllowed)
				{
					claim.areExplosivesAllowed = false;
					GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.ExplosivesDisabled);
				}
				else
				{
					claim.areExplosivesAllowed = true;
					GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.ExplosivesEnabled);
				}
			}

			return true;
		}
		
		//deleteallclaims <player>
		else if(cmd.getName().equalsIgnoreCase("deleteallclaims"))
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			//try to find that player
			OfflinePlayer otherPlayer = gpp.resolvePlayerByName(args[0]);
			if(otherPlayer == null)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
				return true;
			}
			
			//delete all that player's claims
			gpp.dataStore.deleteClaimsForPlayer(otherPlayer.getUniqueId(), true);
			
			GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.DeleteAllSuccess, otherPlayer.getName());
			if(player != null)
			{
				GriefPreventionPlus.AddLogEntry(player.getName() + " deleted all claims belonging to " + otherPlayer.getName() + ".");
			
				//revert any current visualization
				Visualization.Revert(player);
			}
			
			return true;
		}
		
		//claimslist or claimslist <player>
		else if(cmd.getName().equalsIgnoreCase("claimslist"))
		{
			//at most one parameter
			if(args.length > 1) return false;
			
			//player whose claims will be listed
			OfflinePlayer otherPlayer;
			
			//if another player isn't specified, assume current player
			if(args.length < 1)
			{
				if(player != null)
					otherPlayer = player;
				else
					return false;
			}
			
			//otherwise if no permission to delve into another player's claims data
			else if(player != null && !player.hasPermission("griefprevention.claimslistother"))
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.ClaimsListNoPermission);
				return true;
			}
						
			//otherwise try to find the specified player
			else
			{
				otherPlayer = gpp.resolvePlayerByName(args[0]);
				if(otherPlayer == null)
				{
					GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
					return true;
				}
			}
			
			//load the target player's data
			PlayerData playerData = gpp.dataStore.getPlayerData(otherPlayer.getUniqueId());
			GriefPreventionPlus.sendMessage(player, TextMode.Instr, " " + playerData.getAccruedClaimBlocks() + " blocks from play +" + (playerData.getBonusClaimBlocks() + gpp.dataStore.getGroupBonusBlocks(otherPlayer.getUniqueId())) + " bonus = " + (playerData.getAccruedClaimBlocks() + playerData.getBonusClaimBlocks() + gpp.dataStore.getGroupBonusBlocks(otherPlayer.getUniqueId())) + " total.");
			GriefPreventionPlus.sendMessage(player, TextMode.Instr, "Your Claims:");
			for(int i = 0; i < playerData.getClaims().size(); i++)
			{
				Claim claim = playerData.getClaims().get(i);
				GriefPreventionPlus.sendMessage(player, TextMode.Instr, GriefPreventionPlus.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " (-" + claim.getArea() + " blocks)");
			}
			
			if(playerData.getClaims().size() > 0)
				GriefPreventionPlus.sendMessage(player, TextMode.Instr, " = " + playerData.getRemainingClaimBlocks() + " blocks left to spend");
			
			//drop the data we just loaded, if the player isn't online
			if(!otherPlayer.isOnline())
				gpp.dataStore.clearCachedPlayerData(otherPlayer.getUniqueId());
			
			return true;
		}
		
		//unlockItems
		else if(cmd.getName().equalsIgnoreCase("unlockdrops") && player != null)
		{
			PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
		    playerData.dropsAreUnlocked = true;
		    GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.DropUnlockConfirmation);
			
			return true;
		}
		
		//deletealladminclaims
		else if(player != null && cmd.getName().equalsIgnoreCase("deletealladminclaims"))
		{
			if(!player.hasPermission("griefprevention.deleteclaims"))
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.NoDeletePermission);
				return true;
			}
			
			//delete all admin claims
			gpp.dataStore.deleteClaimsForPlayer(null, true);  //null for owner id indicates an administrative claim
			
			GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.AllAdminDeleted);
			if(player != null)
			{
				GriefPreventionPlus.AddLogEntry(player.getName() + " deleted all administrative claims.");
			
				//revert any current visualization
				Visualization.Revert(player);
			}
			
			return true;
		}
		
		//adjustbonusclaimblocks <player> <amount> or [<permission>] amount
		else if(cmd.getName().equalsIgnoreCase("adjustbonusclaimblocks"))
		{
			//requires exactly two parameters, the other player or group's name and the adjustment
			if(args.length != 2) return false;
			
			//parse the adjustment amount
			int adjustment;			
			try
			{
				adjustment = Integer.parseInt(args[1]);
			}
			catch(NumberFormatException numberFormatException)
			{
				return false;  //causes usage to be displayed
			}
			
			//if granting blocks to all players with a specific permission
			if(args[0].startsWith("[") && args[0].endsWith("]"))
			{
				String permissionIdentifier = args[0].substring(1, args[0].length() - 1);
				int newTotal = gpp.dataStore.adjustGroupBonusBlocks(permissionIdentifier, adjustment);
				
				GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.AdjustGroupBlocksSuccess, permissionIdentifier, String.valueOf(adjustment), String.valueOf(newTotal));
				if(player != null) GriefPreventionPlus.AddLogEntry(player.getName() + " adjusted " + permissionIdentifier + "'s bonus claim blocks by " + adjustment + ".");
				
				return true;
			}
			
			//otherwise, find the specified player
			OfflinePlayer targetPlayer = gpp.resolvePlayerByName(args[0]);
			if(targetPlayer == null)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
				return true;
			}
			
			//give blocks to player
			PlayerData playerData = gpp.dataStore.getPlayerData(targetPlayer.getUniqueId());
			playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
			gpp.dataStore.savePlayerData(targetPlayer.getUniqueId(), playerData);
			
			GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.AdjustBlocksSuccess, targetPlayer.getName(), String.valueOf(adjustment), String.valueOf(playerData.getBonusClaimBlocks()));
			if(player != null) GriefPreventionPlus.AddLogEntry(player.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".");
			
			return true;			
		}
		
		//trapped
		else if(cmd.getName().equalsIgnoreCase("trapped") && player != null)
		{
			//FEATURE: empower players who get "stuck" in an area where they don't have permission to build to save themselves
			
			PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
			Claim claim = gpp.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
			
			//if another /trapped is pending, ignore this slash command
			if(playerData.pendingTrapped)
			{
				return true;
			}
			
			//if the player isn't in a claim or has permission to build, tell him to man up
			if(claim == null || claim.allowBuild(player, Material.AIR) == null)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.NotTrappedHere);				
				return true;
			}
			
			//if the player is in the nether or end, he's screwed (there's no way to programmatically find a safe place for him)
			if(player.getWorld().getEnvironment() != Environment.NORMAL)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);				
				return true;
			}
			
			//if the player is in an administrative claim, he should contact an admin
			if(claim.isAdminClaim())
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);
				return true;
			}
			
			//send instructions
			GriefPreventionPlus.sendMessage(player, TextMode.Instr, Messages.RescuePending);
			
			//create a task to rescue this player in a little while
			PlayerRescueTask task = new PlayerRescueTask(player, player.getLocation());
			gpp.getServer().getScheduler().scheduleSyncDelayedTask(gpp, task, 200L);  //20L ~ 1 second
			
			return true;
		}
		
		//siege
		else if(cmd.getName().equalsIgnoreCase("siege") && player != null)
		{
			//error message for when siege mode is disabled
			if(!gpp.siegeEnabledForWorld(player.getWorld()))
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.NonSiegeWorld);
				return true;
			}
			
			//requires one argument
			if(args.length > 1)
			{
				return false;
			}
			
			//can't start a siege when you're already involved in one
			Player attacker = player;
			PlayerData attackerData = gpp.dataStore.getPlayerData(attacker.getUniqueId());
			if(attackerData.siegeData != null)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.AlreadySieging);
				return true;
			}
			
			//can't start a siege when you're protected from pvp combat
			if(attackerData.pvpImmune)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.CantFightWhileImmune);
				return true;
			}
			
			//if a player name was specified, use that
			Player defender = null;
			if(args.length >= 1)
			{
				defender = gpp.getServer().getPlayer(args[0]);
				if(defender == null)
				{
					GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
					return true;
				}
			}
			
			//otherwise use the last player this player was in pvp combat with 
			else if(attackerData.lastPvpPlayer.length() > 0)
			{
				defender = gpp.getServer().getPlayer(attackerData.lastPvpPlayer);
				if(defender == null)
				{
					return false;
				}
			}
			
			else
			{
				return false;
			}
			
			//victim must not be under siege already
			PlayerData defenderData = gpp.dataStore.getPlayerData(defender.getUniqueId());
			if(defenderData.siegeData != null)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.AlreadyUnderSiegePlayer);
				return true;
			}
			
			//victim must not be pvp immune
			if(defenderData.pvpImmune)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.NoSiegeDefenseless);
				return true;
			}
			
			Claim defenderClaim = gpp.dataStore.getClaimAt(defender.getLocation(), false, null);
			
			//defender must have some level of permission there to be protected
			if(defenderClaim == null || defenderClaim.allowAccess(defender) != null)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.NotSiegableThere);
				return true;
			}									
			
			//attacker must be close to the claim he wants to siege
			if(!defenderClaim.isNear(attacker.getLocation(), 25))
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.SiegeTooFarAway);
				return true;
			}
			
			//claim can't be under siege already
			if(defenderClaim.siegeData != null)
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.AlreadyUnderSiegeArea);
				return true;
			}
			
			//can't siege admin claims
			if(defenderClaim.isAdminClaim())
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.NoSiegeAdminClaim);
				return true;
			}
			
			//can't be on cooldown
			if(dataStore.onCooldown(attacker, defender, defenderClaim))
			{
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.SiegeOnCooldown);
				return true;
			}
			
			//start the siege
			dataStore.startSiege(attacker, defender, defenderClaim);			

			//confirmation message for attacker, warning message for defender
			GriefPreventionPlus.sendMessage(defender, TextMode.Warn, Messages.SiegeAlert, attacker.getName());
			GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.SiegeConfirmed, defender.getName());			
		}
		else if(cmd.getName().equalsIgnoreCase("softmute"))
		{
		    //requires one parameter
		    if(args.length != 1) return false;
		    
		    //find the specified player
            OfflinePlayer targetPlayer = gpp.resolvePlayerByName(args[0]);
            if(targetPlayer == null)
            {
                GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                return true;
            }
            
            //toggle mute for player
            boolean isMuted = gpp.dataStore.toggleSoftMute(targetPlayer.getUniqueId());
            if(isMuted)
            {
                GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.SoftMuted, targetPlayer.getName());
            }
            else
            {
                GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.UnSoftMuted, targetPlayer.getName());
            }
            
            return true;
		}
		
		else if(cmd.getName().equalsIgnoreCase("gpreload"))
		{
		    gpp.loadConfig();
		    if(player != null)
		    {
		        GriefPreventionPlus.sendMessage(player, TextMode.Success, "Configuration updated.  If you have updated your Grief Prevention JAR, you still need to /reload or reboot your server.");
		    }
		    else
		    {
		        GriefPreventionPlus.AddLogEntry("Configuration updated.  If you have updated your Grief Prevention JAR, you still need to /reload or reboot your server.");
		    }
		    
		    return true;
		}
		
		//givepet
		else if(cmd.getName().equalsIgnoreCase("givepet") && player != null)
		{
		    //requires one parameter
            if(args.length < 1) return false;
            
            PlayerData playerData = gpp.dataStore.getPlayerData(player.getUniqueId());
            
            //special case: cancellation
            if(args[0].equalsIgnoreCase("cancel"))
            {
                playerData.petGiveawayRecipient = null;
                GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.PetTransferCancellation);
                return true;
            }
            
            //find the specified player
            OfflinePlayer targetPlayer = gpp.resolvePlayerByName(args[0]);
            if(targetPlayer == null)
            {
                GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
                return true;
            }
            
            //remember the player's ID for later pet transfer
            playerData.petGiveawayRecipient = targetPlayer;
            
            //send instructions
            GriefPreventionPlus.sendMessage(player, TextMode.Instr, Messages.ReadyToTransferPet);
            
            return true;
		}
		
		//gpblockinfo
		else if(cmd.getName().equalsIgnoreCase("gpblockinfo") && player != null)
		{
		    ItemStack inHand = player.getItemInHand();
		    player.sendMessage("In Hand: " + String.format("%s(%d:%d)", inHand.getType().name(), inHand.getTypeId(), inHand.getData().getData()));
		    
		    Block inWorld = GriefPreventionPlus.getTargetNonAirBlock(player, 300);
		    player.sendMessage("In World: " + String.format("%s(%d:%d)", inWorld.getType().name(), inWorld.getTypeId(), inWorld.getData()));
		    
		    return true;
		}
		
		return false; 
	}
	
	boolean abandonClaimHandler(Player player, boolean deleteTopLevelClaim) 
	{
		PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
		
		//which claim is being abandoned?
		Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
		if(claim == null)
		{
			GriefPreventionPlus.sendMessage(player, TextMode.Instr, Messages.AbandonClaimMissing);
		}
		
		//verify ownership
		else if(claim.allowEdit(player) != null)
		{
			GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
		}
		
		//warn if has children and we're not explicitly deleting a top level claim
		else if(claim.children.size() > 0 && !deleteTopLevelClaim)
		{
			GriefPreventionPlus.sendMessage(player, TextMode.Instr, Messages.DeleteTopLevelClaim);
			return true;
		}
		
		else
		{
			//delete it
			claim.removeSurfaceFluids(null);
			GriefPreventionPlus.AddLogEntry(player.getName()+" deleted claim id "+claim.id+" at "+claim.locationToString());
			this.dataStore.deleteClaim(claim, true);
			
			//if in a creative mode world, restore the claim area
			if(GriefPreventionPlus.instance.creativeRulesApply(claim.world))
			{
				//GriefPreventionPlus.AddLogEntry(player.getName() + " abandoned a claim @ " + GriefPreventionPlus.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
				GriefPreventionPlus.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
				GriefPreventionPlus.instance.restoreClaim(claim, 20L * 60 * 2);
			}
			
			//adjust claim blocks
			playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - (int)Math.ceil((claim.getArea() * (1 - gpp.config_claims_abandonReturnRatio))));
			
			//tell the player how many claim blocks he has left
			int remainingBlocks = playerData.getRemainingClaimBlocks();
			GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.AbandonSuccess, String.valueOf(remainingBlocks));
			
			//revert any current visualization
			Visualization.Revert(player);
			
			playerData.warnedAboutMajorDeletion = false;
		}
		
		return true;
		
	}

	//helper method keeps the trust commands consistent and eliminates duplicate code
	void handleTrustCommand(Player player, ClaimPermission permissionLevel, String recipientName) 
	{
		//determine which claim the player is standing in
		Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
		
		if (claim==null) { // all player's claims
			PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
			if (playerData.getClaims().size()==0) {
				// no claims
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.GrantPermissionNoClaim);
				return;
			}
			if(recipientName.startsWith("[") && recipientName.endsWith("]")) { // permissionbukkit
				if(recipientName.length()<3) {
					GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.InvalidPermissionID);
					return;
				}
				
				for (Claim c : playerData.getClaims()) {
					c.setPermission(recipientName.substring(1, recipientName.length()-1), permissionLevel);
				}
				GriefPreventionPlus.AddLogEntry(player.getName()+" added "+recipientName+" permission to all his claims");
			} else if(recipientName.equals("public")) { // public
				for (Claim c : playerData.getClaims()) {
					c.setPermission(GriefPreventionPlus.UUID0, permissionLevel);
				}
				GriefPreventionPlus.AddLogEntry(player.getName()+" added public permission to all his claims");
			} else { //player?
				OfflinePlayer otherPlayer = gpp.resolvePlayerByName(recipientName);
				if (otherPlayer==null) {// player not found
					GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
					return;
				}
				
				for (Claim c : playerData.getClaims()) {
					c.setPermission(otherPlayer.getUniqueId(), permissionLevel);
				}
				GriefPreventionPlus.AddLogEntry(player.getName()+" added "+otherPlayer.getName()+" permission to all his claims");
			}
		} else { // claim the player is standing in
			if(claim.allowGrantPermission(player) != null) {
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
				return;
			}
			
			String errorMessage;
			switch(permissionLevel) {
				case MANAGE:
					errorMessage = claim.allowEdit(player);
					if(errorMessage != null) {
						errorMessage = "Only " + claim.getOwnerName() + " can grant /PermissionTrust here."; 
					}
					break;
				case ACCESS:
					errorMessage = claim.allowAccess(player);
					break;
				case CONTAINER:
					errorMessage = claim.allowContainers(player);
					break;
				default:
					errorMessage = claim.allowBuild(player, Material.AIR);				
					break;
			}
			
			if (errorMessage!=null) {
				GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.CantGrantThatPermission);
				return;
			}
			
			if(recipientName.startsWith("[") && recipientName.endsWith("]")) { // permissionbukkit
				if(recipientName.length()<3) {
					GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.InvalidPermissionID);
					return;
				}
				claim.setPermission(recipientName.substring(1, recipientName.length()-1), permissionLevel);
				GriefPreventionPlus.AddLogEntry(player.getName()+" added "+recipientName+" permission to claim id "+claim.id);
			} else if(recipientName.equals("public")) { // public
				claim.setPermission(GriefPreventionPlus.UUID0, permissionLevel);
				GriefPreventionPlus.AddLogEntry(player.getName()+" added public permission to claim id "+claim.id);
			} else { //player?
				OfflinePlayer otherPlayer = gpp.resolvePlayerByName(recipientName);
				if (otherPlayer==null) {// player not found
					GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.PlayerNotFound2);
					return;
				}
				GriefPreventionPlus.AddLogEntry(player.getName()+" added "+otherPlayer.getName()+" permission to claim id "+claim.id);
				claim.setPermission(otherPlayer.getUniqueId(), permissionLevel);
			}
		}
		

		//notify player
		if(recipientName.equals("public")) recipientName = this.dataStore.getMessage(Messages.CollectivePublic);
		String permissionDescription;
		
		switch(permissionLevel) {
			case MANAGE:
				permissionDescription = this.dataStore.getMessage(Messages.PermissionsPermission);
				break;
			case ACCESS:
				permissionDescription = this.dataStore.getMessage(Messages.AccessPermission);
				break;
			case CONTAINER:
				permissionDescription = this.dataStore.getMessage(Messages.ContainersPermission);
				break;
			default:
				permissionDescription = this.dataStore.getMessage(Messages.BuildPermission);	
				break;
		}
		
		String location;
		if(claim == null) {
			location = this.dataStore.getMessage(Messages.LocationAllClaims);
		} else {
			location = this.dataStore.getMessage(Messages.LocationCurrentClaim);
		}
		
		GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.GrantPermissionConfirmation, recipientName, permissionDescription, location);
	}

}
