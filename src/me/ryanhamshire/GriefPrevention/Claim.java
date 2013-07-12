/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

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

package me.ryanhamshire.GriefPrevention;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.events.ClaimModifiedEvent;
import me.ryanhamshire.GriefPrevention.tasks.RestoreNatureProcessingTask;

import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

//represents a player claim
//creating an instance doesn't make an effective claim
//only claims which have been added to the datastore have any effect
/**
 * Represents a claim. Creating an instance of this class does not create a claim; instances must be 
 * added to the active dataStore in order to take effect.
 */
public class Claim
{
	
	//String representations of the lesser and Greater Boundary corners.
	//these are ONLY used when passed in the constructor.
	//an Attempt is made to parse them and if the world isn't loaded the claim will be
	//"dormant" which means it will return false for most queries.
	private String LesserCorner;
	private String GreaterCorner;
	private boolean WasDeferred=false;
	private String ClaimWorldName;
	/**
	 * Retrieves the world name in which this claim resides.
	 * @return World Name for World in which this Claim resides.
	 */
	public String getClaimWorldName(){ return ClaimWorldName;}
	//two locations, which together define the boundaries of the claim
	//note that the upper Y value is always ignored, because claims ALWAYS extend up to the sky
	/**
	 * Lower X/Z Corner of this Claim.
	 */
	Location lesserBoundaryCorner;
	/**
	 * Upper X/Z Corner of this Claim.
	 */
	Location greaterBoundaryCorner;
	//test
	/**
	 * modification date.  this comes from the file timestamp during load, and is updated with runtime changes
	 */
	public Date modifiedDate;
	
	/**id number.  unique to this claim, never changes.
	 * 
	 */
	Long id = null;
	
	/**
	 * Subclaim ID. null for top-level claims, unique among subclaims otherwise.
	 */
	Long subClaimid=null;
	
	/**
	 * ownername.  for admin claims, this is the empty string
	 * use getOwnerName() to get a friendly name (will be "an administrator" for admin claims)
	 */
	public String ownerName;
	
	/**
	 * list of players who (beyond the claim owner) have permission to grant permissions in this claim
	 */
	private ArrayList<String> managers = new ArrayList<String>();
	
	//permissions for this claim, see ClaimPermission class
	private HashMap<String, ClaimPermission> playerNameToClaimPermissionMap = new HashMap<String, ClaimPermission>();
	
	/**
	 * whether or not this claim is in the data store
	 * if a claim instance isn't in the data store, it isn't "active" - players can't interract with it 
	 * why keep this?  so that claims which have been removed from the data store can be correctly 
	 * ignored even though they may have references floating around
	 */
	
	public boolean inDataStore = false;
	/**
	 * seld-explanatory: whether Explosives can affect this claim. This is an additional requirement in addition to
	 * the world configuration allowing for Explosions within claims either above or below sea-level.
	 */
	public boolean areExplosivesAllowed = false;
	

	/** parent claim
	 * only used for claim subdivisions.  top level claims have null here
	 */
	public Claim parent = null;
	
	/**
	 * children (subdivisions)
	 * note subdivisions themselves never have children
	 */
	
	public ArrayList<Claim> children = new ArrayList<Claim>();
	
	/**
	 * information about a siege involving this claim.  null means no siege is currently impacting this claim
	 */
	public SiegeData siegeData = null;
	
	/**
	 * following a siege, buttons/levers are unlocked temporarily.  this represents that state
	 */
	public boolean doorsOpen = false;
	/**
	 * following a siege, anybody can open containers in that claim, up to a set limit.
	 */
	public int LootedChests = 0;
	/**
	 * This variable sets whether a claim gets deleted with the automatic cleanup.
	 */
	public boolean neverdelete = false;
	
	//whether or not this is an administrative claim
	//administrative claims are created and maintained by players with the griefprevention.adminclaims permission.
	
	/**
	 * retrieves the index/ID of a given subclaim.
	 * @param childclaim Claim to get the index of.
	 * @return -1 if the given claim is not a subdivided claim of this claim. otherwise, the index of the claim
	 */
	
	/**
	 * looks for a child claim/subdivision at the given location.
	 * @param testlocation Location to check.
	 * @return Claim corresponding to a Subdivision/child claim that contains the given Location, or null otherwise.
	 */
	public Claim getChildAt(Location testlocation){
		
		for(Claim iterate:children){
			if(iterate.contains(testlocation, false, false))
				return iterate;
		}
		return null;
		
	}
	
	/**
	 * Whether or not this is an administrative claim.<br />
	 * Administrative claims are created and maintained by players with the griefprevention.adminclaims permission.
	 * @return
	 */
	public boolean isAdminClaim()
	{
		return (this.ownerName == null || this.ownerName.isEmpty());
	}
	/**
	 * retrieves a Subclaim by the Subclaim's unique index.
	 * @param pID
	 * @return
	 */
	public Claim getSubClaim(long pID){
		for(Claim subclaim:children){
			if(subclaim.getSubClaimID()==pID) return subclaim;
		}
		return null;
	}
	/**
	 * Retrieves the subclaimID associated with this claim, if any.
	 * @return
	 */
	public Long getSubClaimID(){
		return this.subClaimid;
	}
	//accessor for ID
	public Long getID()
	{
		return this.id;
	}
	
	//basic constructor, just notes the creation time
	//see above declarations for other defaults
	Claim()
	{
		this.modifiedDate = new Date();
	}
	
	/**
	 * Players may only siege someone when he's not in an admin claim and when he has some level of permission in the claim.
	 * @param defender
	 * @return
	 */
	public boolean canSiege(Player defender)
	{
		if(this.isAdminClaim()) return false;
		
		if(this.allowAccess(defender) != null) return false;
		
		return true;
	}
	
	/**
	 * Removes any fluids above sea level in a claim.
	 * @param exclusionClaim another claim indicating a sub-area to be excluded from this operation. Can be null.
	 */
	public void removeSurfaceFluids(Claim exclusionClaim)
	{
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(getLesserBoundaryCorner().getWorld());
		
		//don't do this for administrative claims
		if(this.isAdminClaim()) return;
		
		//don't do it for very large claims
		if(this.getArea() > 10000) return;
		
		//don't do it when surface fluids aren't allowed to be dumped
		if(wc.getWaterBucketEmptyBehaviour().Allowed(getLesserBoundaryCorner(),null).Denied())
			return;
		
		Location lesser = this.getLesserBoundaryCorner();
		Location greater = this.getGreaterBoundaryCorner();

		if(lesser.getWorld().getEnvironment() == Environment.NETHER) return;  //don't clean up lava in the nether
		
		int seaLevel = 0;  //clean up all fluids in the end
		
		//respect sea level in normal worlds
		if(lesser.getWorld().getEnvironment() == Environment.NORMAL) seaLevel = GriefPrevention.instance.getSeaLevel(lesser.getWorld());
		
		for(int x = lesser.getBlockX(); x <= greater.getBlockX(); x++)
		{
			for(int z = lesser.getBlockZ(); z <= greater.getBlockZ(); z++)
			{
				for(int y = seaLevel - 1; y <= lesser.getWorld().getMaxHeight(); y++)
				{
					//dodge the exclusion claim
					Block block = lesser.getWorld().getBlockAt(x, y, z);
					if(exclusionClaim != null && exclusionClaim.contains(block.getLocation(), true, false)) continue;
					
					if(block.getType() == Material.STATIONARY_WATER || block.getType() == Material.STATIONARY_LAVA || block.getType() == Material.LAVA || block.getType() == Material.WATER)
					{
						block.setType(Material.AIR);
					}
				}
			}
		}		
	}
	
	//determines whether or not a claim has surface fluids (lots of water blocks, or any lava blocks)
	//used to warn players when they abandon their claims about automatic fluid cleanup
	public boolean hasSurfaceFluids()
	{
		Location lesser = this.getLesserBoundaryCorner();
		Location greater = this.getGreaterBoundaryCorner();

		//don't bother for very large claims, too expensive
		if(this.getArea() > 10000) return false;
		
		int seaLevel = 0;  //clean up all fluids in the end
		
		//respect sea level in normal worlds
		if(lesser.getWorld().getEnvironment() == Environment.NORMAL) seaLevel = GriefPrevention.instance.getSeaLevel(lesser.getWorld());
		
		int waterCount = 0;
		for(int x = lesser.getBlockX(); x <= greater.getBlockX(); x++)
		{
			for(int z = lesser.getBlockZ(); z <= greater.getBlockZ(); z++)
			{
				for(int y = seaLevel - 1; y <= lesser.getWorld().getMaxHeight(); y++)
				{
					//dodge the exclusion claim
					Block block = lesser.getWorld().getBlockAt(x, y, z);
					
					if(block.getType() == Material.STATIONARY_WATER || block.getType() == Material.WATER)
					{
						waterCount++;
						if(waterCount > 10) return true;
					}
					
					else if(block.getType() == Material.STATIONARY_LAVA || block.getType() == Material.LAVA)
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Main constructor.  Note that only creating a claim instance does nothing - a claim must be added to the data store to be effective.
	 * @param lesserBoundaryCorner
	 * @param greaterBoundaryCorner
	 * @param ownerName
	 * @param builderNames
	 * @param containerNames
	 * @param accessorNames
	 * @param managerNames
	 * @param id
	 * @param neverdelete
	 */
	public Claim(Location lesserBoundaryCorner, Location greaterBoundaryCorner, String ownerName, String [] builderNames, String [] containerNames, String [] accessorNames, String [] managerNames, Long id, boolean neverdelete)
	{
		
		//modification date
		this.modifiedDate = new Date();
		this.ClaimWorldName = lesserBoundaryCorner.getWorld().getName();
		//id
		this.id = id;
		
		//store corners
		this.lesserBoundaryCorner = lesserBoundaryCorner;
		this.greaterBoundaryCorner = greaterBoundaryCorner;
		
		//owner
		this.ownerName = ownerName;
		
		//other permissions
		for(int i = 0; i < builderNames.length; i++)
		{
			String name = builderNames[i];
			if(name != null && !name.isEmpty())
			{
				this.playerNameToClaimPermissionMap.put(name, ClaimPermission.Build);
			}
		}
		
		for(int i = 0; i < containerNames.length; i++)
		{
			String name = containerNames[i];
			if(name != null && !name.isEmpty())
			{
				this.playerNameToClaimPermissionMap.put(name, ClaimPermission.Inventory);
			}
		}
		
		for(int i = 0; i < accessorNames.length; i++)
		{
			String name = accessorNames[i];
			if(name != null && !name.isEmpty())
			{
				this.playerNameToClaimPermissionMap.put(name, ClaimPermission.Access);
			}
		}
		
		for(int i = 0; i < managerNames.length; i++)
		{
			String name = managerNames[i];
			if(name != null && !name.isEmpty())
			{
				this.managers.add(name);
			}
		}
		
		this.neverdelete = neverdelete;
	}
	
	/**
	 * Measurements.  All measurements are in blocks
	 * @return
	 */
	public int getArea()
	{
		int claimWidth = this.greaterBoundaryCorner.getBlockX() - this.lesserBoundaryCorner.getBlockX() + 1;
		int claimHeight = this.greaterBoundaryCorner.getBlockZ() - this.lesserBoundaryCorner.getBlockZ() + 1;
		
		return claimWidth * claimHeight;		
	}
	public void setLocation(Location FirstBorder,Location SecondBorder){
		if(FirstBorder.getWorld()!=SecondBorder.getWorld()) return;
		Location pA = FirstBorder;
		Location pB = SecondBorder;
		int MinX = Math.min(pA.getBlockX(), pB.getBlockX());
		int MinY = Math.min(pA.getBlockY(),pB.getBlockY());
		int MinZ = Math.min(pA.getBlockZ(), pB.getBlockZ());
		int MaxX = Math.max(pA.getBlockX(), pB.getBlockX());
		int MaxY = Math.max(pA.getBlockY(),pB.getBlockY());
		int MaxZ = Math.max(pA.getBlockZ(), pB.getBlockZ());
		Location FirstPos = new Location(FirstBorder.getWorld(),MinX,MinY,MinZ);
		Location SecondPos = new Location(FirstBorder.getWorld(),MaxX,MaxY,MaxZ);
		lesserBoundaryCorner = FirstPos;
		greaterBoundaryCorner = SecondPos;
		
		
		
		
	}
	/**
	 * Gets the width of the claim.
	 * @return
	 */
	public int getWidth()
	{
		return this.greaterBoundaryCorner.getBlockX() - this.lesserBoundaryCorner.getBlockX() + 1;		
	}
	
	/**
	 * Gets the height of the claim.
	 * @return
	 */
	public int getHeight()
	{
		return this.greaterBoundaryCorner.getBlockZ() - this.lesserBoundaryCorner.getBlockZ() + 1;		
	}
	
	/**
	 * Distance check for claims, distance in this case is a band around the outside of the claim rather then euclidean distance.
	 * @param location
	 * @param howNear
	 * @return
	 */
	public boolean isNear(Location location, int howNear)
	{
		Claim claim = new Claim
			(new Location(this.lesserBoundaryCorner.getWorld(), this.lesserBoundaryCorner.getBlockX() - howNear, this.lesserBoundaryCorner.getBlockY(), this.lesserBoundaryCorner.getBlockZ() - howNear),
			 new Location(this.greaterBoundaryCorner.getWorld(), this.greaterBoundaryCorner.getBlockX() + howNear, this.greaterBoundaryCorner.getBlockY(), this.greaterBoundaryCorner.getBlockZ() + howNear),
			 "", new String[] {}, new String[] {}, new String[] {}, new String[] {}, null, false);
		
		return claim.contains(location, false, true);
	}
	
	/**
	 * Permissions.  Note administrative "public" claims have different rules than other claims.<br />
	 * All of these return NULL when a player has permission, or a String error message when the player doesn't have permission.
	 * @param player
	 * @return
	 */
	public String allowEdit(Player player)
	{
		//if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
		if(player == null) return "";
		
		//special cases...
		
		//admin claims need adminclaims permission only.
		if(this.isAdminClaim())
		{
			if(player.hasPermission("griefprevention.adminclaims")) return null;
		}
		
		//anyone with deleteclaims permission can modify non-admin claims at any time
		else
		{
			if(player.hasPermission("griefprevention.deleteclaims")) return null;
		}
		
		//no resizing, deleting, and so forth while under siege
		if(this.ownerName.equals(player.getName()))
		{
			if(this.siegeData != null)
			{
				return GriefPrevention.instance.dataStore.getMessage(Messages.NoModifyDuringSiege);
			}
			
			//otherwise, owners can do whatever
			return null;
		}
		
		//permission inheritance for subdivisions
		if(this.parent != null)
			return this.parent.allowBuild(player);
		
		//error message if all else fails
		return GriefPrevention.instance.dataStore.getMessage(Messages.OnlyOwnersModifyClaims, this.getOwnerName());
	}
	
	/**
	 * Build permission check
	 * @param player
	 * @return
	 */
	public String allowBuild(Player player)
	{
		//if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
		if(player == null) return "";
		
		//when a player tries to build in a claim, if he's under siege, the siege may extend to include the new claim
		GriefPrevention.instance.dataStore.tryExtendSiege(player, this);
		
		//admin claims can always be modified by admins, no exceptions
		if(this.isAdminClaim())
		{
			if(player.hasPermission("griefprevention.adminclaims")) return null;
		}
		
		//no building while under siege
		if(this.siegeData != null)
		{
			return GriefPrevention.instance.dataStore.getMessage(Messages.NoBuildUnderSiege, this.siegeData.attacker.getName());
		}
		
		//no building while in pvp combat
		PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getName());
		if(playerData.inPvpCombat())
		{
			return GriefPrevention.instance.dataStore.getMessage(Messages.NoBuildPvP);			
		}
		
		//owners can make changes, or admins with ignore claims mode enabled
		if(this.ownerName.equals(player.getName()) || GriefPrevention.instance.dataStore.getPlayerData(player.getName()).ignoreClaims) return null;
		
		//anyone with explicit build permission can make changes
		if(this.hasExplicitPermission(player, ClaimPermission.Build)) return null;
		
		//also everyone is a member of the "public", so check for public permission
		ClaimPermission permissionLevel = this.playerNameToClaimPermissionMap.get("public");
		if(ClaimPermission.Build == permissionLevel) return null;
		
		//subdivision permission inheritance
		if(this.parent != null)
			return this.parent.allowBuild(player);
		
		//failure message for all other cases
		String reason = GriefPrevention.instance.dataStore.getMessage(Messages.NoBuildPermission, this.getOwnerName());
		if(player.hasPermission("griefprevention.ignoreclaims"))
				reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
		return reason;
	}
	/**
	 * returns whether the given player name fits the given identifier.
	 * this is added to allow for Group Permissions on a claim.
	 * @param identifier name of player, or a name of a group. group names must be prefixed with g:
	 * @param pName name of player to test.
	 * @return
	 */
	private boolean isApplicablePlayer(String identifier,String pName){
		//System.out.println("Checking player " + pName + " matches " + identifier);
		if(identifier.equalsIgnoreCase(pName)) return true;
		if(identifier.toUpperCase().startsWith("G:")){
			identifier = identifier.substring(2);
			//try to get the player (pName).
			
			//try to get this group from the GP instance PlayerGroups cfg.
			PlayerGroup FoundGroup = GriefPrevention.instance.config_player_groups.getGroupByName(identifier);
			if(FoundGroup==null) return false; //group not found. Well THIS is awkward.
			
			return FoundGroup.MatchPlayer(pName);
			
			
			
			
			
		}
		
		return false;
	}
	private boolean hasExplicitPermission(Player player, ClaimPermission level)
	{
		
		
		
		String playerName = player.getName();
		Set<String> keys = this.playerNameToClaimPermissionMap.keySet();
		Iterator<String> iterator = keys.iterator();
		while(iterator.hasNext())
		{
			String identifier = iterator.next();
			
			//if(playerName.equalsIgnoreCase(identifier) && this.playerNameToClaimPermissionMap.get(identifier) == level) return true;
			boolean forcedeny = false;
			//special logic: names starting with ! mean to explicitly deny that permission to any player that matches the name after !.
			//in order to allow group ability, we have this flag. if forcedeny is false, a match means
			//we need to forcibly return false if it matches.
			if(identifier.startsWith("!")){
				//if it starts with a exclamation, remove it and set forcedeny to true.
				identifier=identifier.substring(1);
				forcedeny=true;
			}
			
			
			if(isApplicablePlayer(identifier,playerName) && this.playerNameToClaimPermissionMap.get(identifier) == level) {
				//it matches. if we started with a !, however, it means that we are to explicitly
				//DENY that permission. Otherwise, we return true.
				return !forcedeny;
				
			}
			
			else if(identifier.startsWith("[") && identifier.endsWith("]"))
			{
				//drop the brackets
				String permissionIdentifier = identifier.substring(1, identifier.length() - 1);
				
				//defensive coding
				if(permissionIdentifier == null || permissionIdentifier.isEmpty()) continue;
				
				//check permission
				if(player.hasPermission(permissionIdentifier) && this.playerNameToClaimPermissionMap.get(identifier) == level) 
					return !forcedeny;
			}
		}
		
		return false;			
	}
	public String allowBreak(Player player, Location l){
		return allowBreak(player,l.getBlock());
	}
	/**
	 * Break permission check
	 * @param player
	 * @param BlocktoCheck
	 * @return
	 */
	public String allowBreak(Player player, Block BlocktoCheck)
	{
		Material material = BlocktoCheck.getType();
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		//if under siege, some blocks will be breakable
		if(this.siegeData != null)
		{
			boolean breakable = false;
			
			//search for block type in list of breakable blocks
			for(int i = 0; i < wc.getSiegeBlocks().size(); i++)
			{
				Material breakableMaterial = wc.getSiegeBlocks().get(i);
				if(breakableMaterial.getId() == material.getId())
				{
					breakable = true;
					break;
				}
			}
			breakable|=BrokenBlockInfo.canBreak(BlocktoCheck.getLocation());
			//custom error messages for siege mode
			if(!breakable)
			{
				return GriefPrevention.instance.dataStore.getMessage(Messages.NonSiegeMaterial);
			}
			else if(this.ownerName.equals(player.getName()))
			{
				return GriefPrevention.instance.dataStore.getMessage(Messages.NoOwnerBuildUnderSiege);
			}
			else
			{
				if(wc.getSiegeBlockRevert()){
				siegeData.SiegedBlocks.add(new BrokenBlockInfo(BlocktoCheck.getLocation()));
				}
				return null;
			}
		}
		
		//if not under siege, build rules apply
		return this.allowBuild(player);		
	}
	
	/**
	 * Access permission check
	 * @param player
	 * @return
	 */
	public String allowAccess(Player player)
	{
		
		
		
		//following a siege where the defender lost, the claim will allow everyone access for a time
		if(this.doorsOpen) return null;
		
		//admin claims need adminclaims permission only.
		if(this.isAdminClaim())
		{
			if(player.hasPermission("griefprevention.adminclaims")) return null;
		}
		
		//claim owner and admins in ignoreclaims mode have access
		if(this.ownerName.equals(player.getName()) || GriefPrevention.instance.dataStore.getPlayerData(player.getName()).ignoreClaims) return null;
		
		//look for explicit individual access, inventory, or build permission
		if(this.hasExplicitPermission(player, ClaimPermission.Access)) return null;
		if(this.hasExplicitPermission(player, ClaimPermission.Inventory)) return null;
		if(this.hasExplicitPermission(player, ClaimPermission.Build)) return null;
		
		//also check for public permission
		ClaimPermission permissionLevel = this.playerNameToClaimPermissionMap.get("public");
		if(ClaimPermission.Build == permissionLevel || ClaimPermission.Inventory == permissionLevel || ClaimPermission.Access == permissionLevel) return null;		
		
		//permission inheritance for subdivisions
		if(this.parent != null)
			return this.parent.allowAccess(player);
		
		//catch-all error message for all other cases
		String reason = GriefPrevention.instance.dataStore.getMessage(Messages.NoAccessPermission, this.getOwnerName());
		if(player.hasPermission("griefprevention.ignoreclaims"))
			reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
		return reason;
	}
	
	/**
	 * Inventory permission check
	 * @param player
	 * @return
	 */
	public String allowContainers(Player player)
	{		
		
		//if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
		if(player == null) return "";
		
		//trying to access inventory in a claim may extend an existing siege to include this claim
		GriefPrevention.instance.dataStore.tryExtendSiege(player, this);
		
		//if under siege, nobody accesses containers
		if(this.siegeData != null)
		{
			return GriefPrevention.instance.dataStore.getMessage(Messages.NoContainersSiege, siegeData.attacker.getName());
		}
		
		//owner and administrators in ignoreclaims mode have access
		if(this.ownerName.equals(player.getName()) || GriefPrevention.instance.dataStore.getPlayerData(player.getName()).ignoreClaims) return null;
		
		//admin claims need adminclaims permission only.
		if(this.isAdminClaim())
		{
			if(player.hasPermission("griefprevention.adminclaims")) return null;
		}
		
		//check for explicit individual container or build permission 
		if(this.hasExplicitPermission(player, ClaimPermission.Inventory)) return null;
		if(this.hasExplicitPermission(player, ClaimPermission.Build)) return null;
		
		//check for public container or build permission
		ClaimPermission permissionLevel = this.playerNameToClaimPermissionMap.get("public");
		if(ClaimPermission.Build == permissionLevel || ClaimPermission.Inventory == permissionLevel) return null;
		
		//permission inheritance for subdivisions
		if(this.parent != null)
			return this.parent.allowContainers(player);
		
		//error message for all other cases
		String reason = GriefPrevention.instance.dataStore.getMessage(Messages.NoContainersPermission, this.getOwnerName());
		if(player.hasPermission("griefprevention.ignoreclaims"))
			reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
		return reason;
	}
	
	/**
	 * Grant permission check, relatively simple
	 * @param player
	 * @return
	 */
	public String allowGrantPermission(Player player)
	{
		//if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
		if(player == null) return "";
		
		//anyone who can modify the claim can do this
		if(this.allowEdit(player) == null) return null;
		
		//anyone who's in the managers (/PermissionTrust) list can do this
		for(int i = 0; i < this.managers.size(); i++)
		{
			String managerID = this.managers.get(i);
			if (this.isApplicablePlayer(managerID, player.getName())) return null;
			
			else if(managerID.startsWith("[") && managerID.endsWith("]"))
			{
				managerID = managerID.substring(1, managerID.length() - 1);
				if(managerID == null || managerID.isEmpty()) continue;
				if(player.hasPermission(managerID)) return null;
			}
		}
		
		//permission inheritance for subdivisions
		if(this.parent != null)
			return this.parent.allowGrantPermission(player);
		
		//generic error message
		String reason = GriefPrevention.instance.dataStore.getMessage(Messages.NoPermissionTrust, this.getOwnerName());
		if(player.hasPermission("griefprevention.ignoreclaims"))
			reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
		return reason;
	}
	
	/**
	 * Grants a permission for a player or the public
	 * @param playerName
	 * @param permissionLevel
	 * @return
	 */
	public boolean setPermission(String playerName, ClaimPermission permissionLevel)
	{
		//we only want to send events if the claim is in the data store
		if(inDataStore) {
			ClaimModifiedEvent.Type permtype;
			switch(permissionLevel) {
			case Access:
				permtype = ClaimModifiedEvent.Type.AddedAccessTrust;
				break;
			case Build:
				permtype = ClaimModifiedEvent.Type.AddedBuildTrust;
				break;
			case Inventory:
				permtype = ClaimModifiedEvent.Type.AddedInventoryTrust;
				break;
			default:
				permtype = null;
			}
			
			
			
			ClaimModifiedEvent claimevent = new ClaimModifiedEvent(this, null, permtype);
			Bukkit.getServer().getPluginManager().callEvent(claimevent);
			if(claimevent.isCancelled()) {
				return false;
			}
		}
		this.playerNameToClaimPermissionMap.put(playerName.toLowerCase(),  permissionLevel);
		return true;
	}
	
	/**
	 * Revokes a permission for a player or the public
	 * @param playerName
	 */
	public boolean dropPermission(String playerName)
	{
		//we only want to send events if the claim is in the data store
		if(inDataStore) {
			ClaimPermission perm = this.playerNameToClaimPermissionMap.get(playerName.toLowerCase());
			//If they aren't in the map, let's just return
			if(perm == null) {
				return true;
			}
			ClaimModifiedEvent.Type permtype;
			switch(perm) {
			case Access:
				permtype = ClaimModifiedEvent.Type.RemovedAccessTrust;
				break;
			case Build:
				permtype = ClaimModifiedEvent.Type.RemovedBuildTrust;
				break;
			case Inventory:
				permtype = ClaimModifiedEvent.Type.RemovedInventoryTrust;
				break;
			default:
				permtype = null;
			}
			ClaimModifiedEvent claimevent = new ClaimModifiedEvent(this, null, permtype);
			Bukkit.getServer().getPluginManager().callEvent(claimevent);
			if(claimevent.isCancelled()) {
				return false;
			}
		}
		this.playerNameToClaimPermissionMap.remove(playerName.toLowerCase());
		return true;
	}
	
	/**
	 * Clears all permissions (except owner of course)
	 * @return true if permissions were cleared successfully, false if a plugin prevented it from clearing them.
	 */
	public boolean clearPermissions()
	{
		//we only want to send events if the claim is in the data store
		if(inDataStore) {
			ClaimModifiedEvent claimevent = new ClaimModifiedEvent(this, null, ClaimModifiedEvent.Type.PermissionsCleared);
			Bukkit.getServer().getPluginManager().callEvent(claimevent);
			if(claimevent.isCancelled()) {
				return false;
			}
		}
		this.playerNameToClaimPermissionMap.clear();
		return true;
	}

	/**
	 * Gets ALL permissions.<br />
	 * Useful for  making copies of permissions during a claim resize and listing all permissions in a claim.
	 * @param builders
	 * @param containers
	 * @param accessors
	 * @param managers
	 */
	public void getPermissions(List<String> builders, List<String> containers, List<String> accessors, List<String> managers)
	{
		//loop through all the entries in the hash map
		//if we have a parent, add the parent permissions first, then overwrite them.
		Iterator<Map.Entry<String, ClaimPermission>> mappingsIterator = this.playerNameToClaimPermissionMap.entrySet().iterator(); 
		while(mappingsIterator.hasNext())
		{
			Map.Entry<String, ClaimPermission> entry = mappingsIterator.next();
			
			//build up a list for each permission level
			if(entry.getValue() == ClaimPermission.Build)
			{
				builders.add(entry.getKey());
			}
			else if(entry.getValue() == ClaimPermission.Inventory)
			{
				containers.add(entry.getKey());
			}
			else
			{
				accessors.add(entry.getKey());
			}			
		}
		
		//managers are handled a little differently
		for(String manager:managers)
		{
			managers.add(manager);
		}


		
		
	}
	
	/**
	 * Returns a copy of the location representing lower x, y, z limits
	 * @return
	 */
	public Location getLesserBoundaryCorner()
	{
		return this.lesserBoundaryCorner.clone();
	}
	
	/**
	 * Returns a copy of the location representing upper x, y, z limits.<br />
	 * NOTE: remember upper Y will always be ignored, all claims always extend to the sky.
	 * @return
	 */
	public Location getGreaterBoundaryCorner()
	{
		return this.greaterBoundaryCorner.clone();
	}
	
	/**
	 * Returns a friendly owner name (for admin claims, returns "an administrator" as the owner)
	 * @return
	 */
	public String getOwnerName()
	{
		if(this.parent != null)
			return this.parent.getOwnerName();
		
		if(this.ownerName.length() == 0)
			return GriefPrevention.instance.dataStore.getMessage(Messages.OwnerNameForAdminClaims);
		
		return this.ownerName;
	}
	public boolean contains(Claim otherclaim,boolean ignoreHeight){
		return(contains(otherclaim.lesserBoundaryCorner,ignoreHeight,false) &&
				contains(otherclaim.greaterBoundaryCorner,ignoreHeight,false));
	}
	public static boolean Contains(Location pA,Location pB,Location Target,boolean ignoreHeight){
		
		int MinX = Math.min(pA.getBlockX(), pB.getBlockX());
		int MinY = Math.min(pA.getBlockY(),pB.getBlockY());
		int MinZ = Math.min(pA.getBlockZ(), pB.getBlockZ());
		int MaxX = Math.max(pA.getBlockX(), pB.getBlockX());
		int MaxY = Math.max(pA.getBlockY(),pB.getBlockY());
		int MaxZ = Math.max(pA.getBlockZ(), pB.getBlockZ());
		
		if(Target.getBlockX() < MinX || Target.getBlockX() > MaxX)
			return false;
		if(Target.getBlockZ() < MinZ || Target.getBlockZ() > MaxZ)
			return false;
		if(!ignoreHeight && (Target.getBlockY() < MaxY || Target.getBlockY() > MaxY))
			return false;
		
		
		return true;
			
		
		
		
	}
	private void ensureValid(){
		//makes sure our two Locations point to a valid world.
		
	}
	/**
	 * Whether or not a location is in the claim.
	 * @param location
	 * @param ignoreHeight true means location UNDER the claim will return TRUE
	 * @param excludeSubdivisions true means that locations inside subdivisions of the claim will return FALSE
	 * @return
	 */
	public boolean contains(Location location, boolean ignoreHeight, boolean excludeSubdivisions)
	{
		//not in the same world implies false
		if(!location.getWorld().equals(this.lesserBoundaryCorner.getWorld())) return false;
		
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		
		//main check
		boolean inClaim = (ignoreHeight || y >= this.lesserBoundaryCorner.getBlockY()) &&
				x >= this.lesserBoundaryCorner.getBlockX() &&
				x <= this.greaterBoundaryCorner.getBlockX() &&
				z >= this.lesserBoundaryCorner.getBlockZ() &&
				z <= this.greaterBoundaryCorner.getBlockZ();
				
		if(!inClaim) return false;
				
	    //additional check for subdivisions
		//you're only in a subdivision when you're also in its parent claim
		//NOTE: if a player creates subdivions then resizes the parent claim, it's possible that
		//a subdivision can reach outside of its parent's boundaries.  so this check is important!
		if(this.parent != null)
	    {
	    	return this.parent.contains(location, ignoreHeight, false);
	    }
		
		//code to exclude subdivisions in this check
		else if(excludeSubdivisions)
		{
			//search all subdivisions to see if the location is in any of them
			for(int i = 0; i < this.children.size(); i++)
			{
				//if we find such a subdivision, return false
				if(this.children.get(i).contains(location, ignoreHeight, true))
				{
					return false;
				}
			}
		}
		
		//otherwise yes
		return true;				
	}
	
	//whether or not two claims overlap
	//used internally to prevent overlaps when creating claims
	boolean overlaps(Claim otherClaim)
	{
		//NOTE:  if trying to understand this makes your head hurt, don't feel bad - it hurts mine too.  
		//try drawing pictures to visualize test cases.
		
		if(!this.lesserBoundaryCorner.getWorld().equals(otherClaim.getLesserBoundaryCorner().getWorld())) return false;
		
		//first, check the corners of this claim aren't inside any existing claims
		if(otherClaim.contains(this.lesserBoundaryCorner, true, false)) return true;
		if(otherClaim.contains(this.greaterBoundaryCorner, true, false)) return true;
		if(otherClaim.contains(new Location(this.lesserBoundaryCorner.getWorld(), this.lesserBoundaryCorner.getBlockX(), 0, this.greaterBoundaryCorner.getBlockZ()), true, false)) return true;
		if(otherClaim.contains(new Location(this.lesserBoundaryCorner.getWorld(), this.greaterBoundaryCorner.getBlockX(), 0, this.lesserBoundaryCorner.getBlockZ()), true, false)) return true;
		
		//verify that no claim's lesser boundary point is inside this new claim, to cover the "existing claim is entirely inside new claim" case
		if(this.contains(otherClaim.getLesserBoundaryCorner(), true, false)) return true;
		
		//verify this claim doesn't band across an existing claim, either horizontally or vertically		
		if(	this.getLesserBoundaryCorner().getBlockZ() <= otherClaim.getGreaterBoundaryCorner().getBlockZ() && 
			this.getLesserBoundaryCorner().getBlockZ() >= otherClaim.getLesserBoundaryCorner().getBlockZ() && 
			this.getLesserBoundaryCorner().getBlockX() < otherClaim.getLesserBoundaryCorner().getBlockX() &&
			this.getGreaterBoundaryCorner().getBlockX() > otherClaim.getGreaterBoundaryCorner().getBlockX() )
			return true;
		
		if(	this.getGreaterBoundaryCorner().getBlockZ() <= otherClaim.getGreaterBoundaryCorner().getBlockZ() && 
			this.getGreaterBoundaryCorner().getBlockZ() >= otherClaim.getLesserBoundaryCorner().getBlockZ() && 
			this.getLesserBoundaryCorner().getBlockX() < otherClaim.getLesserBoundaryCorner().getBlockX() &&
			this.getGreaterBoundaryCorner().getBlockX() > otherClaim.getGreaterBoundaryCorner().getBlockX() )
			return true;
		
		if(	this.getLesserBoundaryCorner().getBlockX() <= otherClaim.getGreaterBoundaryCorner().getBlockX() && 
			this.getLesserBoundaryCorner().getBlockX() >= otherClaim.getLesserBoundaryCorner().getBlockX() && 
			this.getLesserBoundaryCorner().getBlockZ() < otherClaim.getLesserBoundaryCorner().getBlockZ() &&
			this.getGreaterBoundaryCorner().getBlockZ() > otherClaim.getGreaterBoundaryCorner().getBlockZ() )
			return true;
			
		if(	this.getGreaterBoundaryCorner().getBlockX() <= otherClaim.getGreaterBoundaryCorner().getBlockX() && 
			this.getGreaterBoundaryCorner().getBlockX() >= otherClaim.getLesserBoundaryCorner().getBlockX() && 
			this.getLesserBoundaryCorner().getBlockZ() < otherClaim.getLesserBoundaryCorner().getBlockZ() &&
			this.getGreaterBoundaryCorner().getBlockZ() > otherClaim.getGreaterBoundaryCorner().getBlockZ() )
			return true;
		
		return false;
	}
	
	/**
	 * Whether more entities may be added to a claim
	 * @return
	 */
	public String allowMoreEntities()
	{
		if(this.parent != null) return this.parent.allowMoreEntities();
		
		//this rule only applies to creative mode worlds
		if(!GriefPrevention.instance.creativeRulesApply(this.getLesserBoundaryCorner())) return null;
		
		//admin claims aren't restricted
		if(this.isAdminClaim()) return null;
		
		//don't apply this rule to very large claims
		if(this.getArea() > 10000) return null;
		
		//determine maximum allowable entity count, based on claim size
		int maxEntities = this.getArea() / 50;		
		if(maxEntities == 0) return GriefPrevention.instance.dataStore.getMessage(Messages.ClaimTooSmallForEntities);
		
		//count current entities (ignoring players)
		Chunk lesserChunk = this.getLesserBoundaryCorner().getChunk();
		Chunk greaterChunk = this.getGreaterBoundaryCorner().getChunk();
		
		int totalEntities = 0;
		for(int x = lesserChunk.getX(); x <= greaterChunk.getX(); x++)
			for(int z = lesserChunk.getZ(); z <= greaterChunk.getZ(); z++)
			{
				Chunk chunk = lesserChunk.getWorld().getChunkAt(x, z);
				Entity [] entities = chunk.getEntities();
				for(int i = 0; i < entities.length; i++)
				{
					Entity entity = entities[i];
					if(!(entity instanceof Player) && this.contains(entity.getLocation(), false, false))
					{
						totalEntities++;
						if(totalEntities > maxEntities) entity.remove();
					}
				}
			}

		if(totalEntities > maxEntities) return GriefPrevention.instance.dataStore.getMessage(Messages.TooManyEntitiesInClaim);
		
		return null;
	}
	
	//implements a strict ordering of claims, used to keep the claims collection sorted for faster searching
	boolean greaterThan(Claim otherClaim)
	{
		Location thisCorner = this.getLesserBoundaryCorner();
		Location otherCorner = otherClaim.getLesserBoundaryCorner();
		
		if(thisCorner.getBlockX() > otherCorner.getBlockX()) return true;
		
		if(thisCorner.getBlockX() < otherCorner.getBlockX()) return false;
		
		if(thisCorner.getBlockZ() > otherCorner.getBlockZ()) return true;
		
		if(thisCorner.getBlockZ() < otherCorner.getBlockZ()) return false;
		
		return thisCorner.getWorld().getName().compareTo(otherCorner.getWorld().getName()) < 0;
	}
	
	public long getPlayerInvestmentScore()
	{
		//decide which blocks will be considered player placed
		Location lesserBoundaryCorner = this.getLesserBoundaryCorner();
		ArrayList<Integer> playerBlocks = RestoreNatureProcessingTask.getPlayerBlocks(lesserBoundaryCorner.getWorld().getEnvironment(), lesserBoundaryCorner.getBlock().getBiome());
		
		//scan the claim for player placed blocks
		double score = 0;
		
		boolean creativeMode = GriefPrevention.instance.creativeRulesApply(lesserBoundaryCorner);
		
		for(int x = this.lesserBoundaryCorner.getBlockX(); x <= this.greaterBoundaryCorner.getBlockX(); x++)
		{
			for(int z = this.lesserBoundaryCorner.getBlockZ(); z <= this.greaterBoundaryCorner.getBlockZ(); z++)
			{
				int y = this.lesserBoundaryCorner.getBlockY();
				for(; y < GriefPrevention.instance.getSeaLevel(this.lesserBoundaryCorner.getWorld()) - 5; y++)
				{
					Block block = this.lesserBoundaryCorner.getWorld().getBlockAt(x, y, z);
					if(playerBlocks.contains(block.getTypeId()))
					{
						if(block.getType() == Material.CHEST && !creativeMode)
						{
							score += 10;
						}
						else
						{
							score += .5;
						}						
					}
				}
				
				for(; y < this.lesserBoundaryCorner.getWorld().getMaxHeight(); y++)
				{
					Block block = this.lesserBoundaryCorner.getWorld().getBlockAt(x, y, z);
					if(playerBlocks.contains(block.getTypeId()))
					{
						if(block.getType() == Material.CHEST && !creativeMode)
						{
							score += 10;
						}
						else if(creativeMode && (block.getType() == Material.LAVA || block.getType() == Material.STATIONARY_LAVA))
						{
							score -= 10;
						}
						else 
						{
							score += 1;
						}						
					}
				}
			}
		}

		return (long)score;
	}
/**
 * special routine added for Group support. We need to be able to recognize
 * player names from a list where the name is actually within a group that is in that list.
 * @param testlist list of names (possibly including groups) 
 * @param testfor player name to test for.
 * @return
 */
	private boolean PlayerinList(List<String> testlist,String testfor){
		//if it starts with g: or G:, remove it.
		
		for(String iteratename:testlist){
			//test if applicable.
			if(isApplicablePlayer(iteratename,testfor))
				return true;

			
			
		
			
		}
		//we tried to find it in the list and all groups that were in that list, but we didn't find it.
		return false;
		
	}
	
	/**
	 * Checks to see if this player is a manager.
	 * @param player
	 * @return
	 */
	public boolean isManager(String player) {
		//if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
		if(player == null) return false;
		return PlayerinList(managers,player);
		//return managers.contains(player);
	}
	
	/**
	 * Adds a manager to the claim.
	 * @param player
	 * @return
	 */
	public boolean addManager(String player) {
		//we only want to send events if the claim is in the data store
		if(inDataStore) {
			ClaimModifiedEvent claimevent = new ClaimModifiedEvent(this, player, ClaimModifiedEvent.Type.AddedManager);
			Bukkit.getServer().getPluginManager().callEvent(claimevent);
			if(claimevent.isCancelled()) {
				return false;
			}
		}
		managers.add(player);
		return true;
	}

	/**
	 * Removes a manager from the claim.
	 * @param player
	 * @return
	 */
	public boolean removeManager(String player) {
		//we only want to send events if the claim is in the data store
		if(inDataStore) {
			ClaimModifiedEvent claimevent = new ClaimModifiedEvent(this, player, ClaimModifiedEvent.Type.RemovedManager);
			Bukkit.getServer().getPluginManager().callEvent(claimevent);
			if(claimevent.isCancelled()) {
				return false;
			}
		}
		managers.remove(player);
		return true;
	}
	public void clearManagers(){
		
		for(String iterateman:managers){
			removeManager(iterateman);
		}
		
		
	}
	/**
	 * 
	 * @param PluginKey
	 * @return
	 */
	public FileConfiguration getPluginMetadata(String PluginKey){
		return GriefPrevention.instance.getMetaHandler().getClaimMeta(PluginKey, this);
	}
	
	/**
	 * This returns a copy of the managers list. Additions and removals in this list do not affect the claim.
	 * @return The list of the managers.
	 */
	public ArrayList<String> getManagerList() {
		return (ArrayList<String>) managers.clone();
	}
}
