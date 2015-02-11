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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

//represents a player claim
//creating an instance doesn't make an effective claim
//only claims which have been added to the datastore have any effect
public class Claim {
	//id number.  unique to this claim, never changes.
	Integer id = null;
	
	// Coordinates
	World world;
	int lesserX, lesserZ, greaterX, greaterZ; // corners
	
	//ownerID.  for admin claims, this is NULL
	//use getOwnerName() to get a friendly name (will be "an administrator" for admin claims)
	public UUID ownerID;
	
	
	//modification date.  this comes from the file timestamp during load, and is updated with runtime changes
	public Date modifiedDate;
	
	//permissions for this claim
	private ConcurrentHashMap<UUID, Integer> permissionMapPlayers = new ConcurrentHashMap<UUID, Integer>();
	private ConcurrentHashMap<String, Integer> permissionMapBukkit = new ConcurrentHashMap<String, Integer>();
	
	//whether or not this claim is in the data store
	//if a claim instance isn't in the data store, it isn't "active" - players can't interract with it 
	//why keep this?  so that claims which have been removed from the data store can be correctly 
	//ignored even though they may have references floating around
	public boolean inDataStore = false;
	
	public boolean areExplosivesAllowed = false;
	
	//parent claim
	//only used for claim subdivisions.  top level claims have null here
	public Claim parent = null;
	
	//children (subdivisions)
	//note subdivisions themselves never have children
	public ArrayList<Claim> children = new ArrayList<Claim>();
	
	//information about a siege involving this claim.  null means no siege is impacting this claim
	public SiegeData siegeData = null;
	
	//following a siege, buttons/levers are unlocked temporarily.  this represents that state
	public boolean doorsOpen = false;
	
	//main constructor.  note that only creating a claim instance does nothing - a claim must be added to the data store to be effective
	Claim(World world, int lesserX, int lesserZ, int greaterX, int greaterZ, UUID ownerID, HashMap<UUID, Integer> permissionMapPlayers, HashMap<String, Integer> permissionMapBukkit, Integer id)
	{
		this.modifiedDate = new Date();

		this.id = id;

		this.world=world;
		this.lesserX=lesserX;
		this.lesserZ=lesserZ;
		this.greaterX=greaterX;
		this.greaterZ=greaterZ;

		//owner
		this.ownerID = ownerID;
		if (permissionMapPlayers!=null) {
			this.permissionMapPlayers.putAll(permissionMapPlayers);
		}
		if (permissionMapBukkit!=null) {
			this.permissionMapBukkit.putAll(permissionMapBukkit);
		}
	}
	
	/** use this constructor if you stored Location, otherwise use the other constructor  */
	Claim(Location lesserCorner, Location greaterCorner, UUID ownerID, HashMap<UUID, Integer> permissionMapPlayers, HashMap<String, Integer> permissionMapBukkit, Integer id)
	{
		this.modifiedDate = new Date();

		this.id = id;

		this.world=lesserCorner.getWorld();
		this.lesserX=lesserCorner.getBlockX();
		this.lesserZ=lesserCorner.getBlockZ();
		this.greaterX=greaterCorner.getBlockX();
		this.greaterZ=greaterCorner.getBlockZ();

		//owner
		this.ownerID = ownerID;
		if (permissionMapPlayers!=null) {
			this.permissionMapPlayers.putAll(permissionMapPlayers);
		}
		if (permissionMapBukkit!=null) {
			this.permissionMapBukkit.putAll(permissionMapBukkit);
		}
	}
	
	
	//whether or not this is an administrative claim
	//administrative claims are created and maintained by players with the griefprevention.adminclaims permission.
	public boolean isAdminClaim()
	{
		if(this.parent != null) return this.parent.isAdminClaim();
	    if(this.ownerID==null) return false;
		return (this.ownerID.equals(GriefPreventionPlus.UUID1));
	}
	
	//accessor for ID
	public Integer getID()
	{
		return this.id;
	}
	
	//basic constructor, just notes the creation time
	//see above declarations for other defaults
	/*Claim()
	{
		this.modifiedDate = Calendar.getInstance().getTime();
	}*/
	
	//players may only siege someone when he's not in an admin claim 
	//and when he has some level of permission in the claim
	public boolean canSiege(Player defender)
	{
		if(this.isAdminClaim()) return false;
		
		if(this.allowAccess(defender) != null) return false;
		
		return true;
	}
	
	//removes any lava above sea level in a claim
	//exclusionClaim is another claim indicating an sub-area to be excluded from this operation
	//it may be null
	public void removeSurfaceFluids(Claim exclusionClaim)
	{
		//don't do this for administrative claims
		if(this.isAdminClaim()) return;
		
		//don't do it for very large claims
		if(this.getArea() > 10000) return;
		
		//only in creative mode worlds
		if(!GriefPreventionPlus.instance.creativeRulesApply(this.world)) return;
		
		Location lesser = this.getLesserBoundaryCorner();
		Location greater = this.getGreaterBoundaryCorner();

		if(lesser.getWorld().getEnvironment() == Environment.NETHER) return;  //don't clean up lava in the nether
		
		int seaLevel = 0;  //clean up all fluids in the end
		
		//respect sea level in normal worlds
		if(lesser.getWorld().getEnvironment() == Environment.NORMAL) seaLevel = GriefPreventionPlus.instance.getSeaLevel(lesser.getWorld());
		
		for(int x = lesser.getBlockX(); x <= greater.getBlockX(); x++)
		{
			for(int z = lesser.getBlockZ(); z <= greater.getBlockZ(); z++)
			{
				for(int y = seaLevel - 1; y <= lesser.getWorld().getMaxHeight(); y++)
				{
					//dodge the exclusion claim
					Block block = lesser.getWorld().getBlockAt(x, y, z);
					if(exclusionClaim != null && exclusionClaim.contains(block.getLocation(), true, false)) continue;
					
					if(block.getType() == Material.STATIONARY_LAVA || block.getType() == Material.LAVA)
					{
						block.setType(Material.AIR);
					}
				}
			}
		}		
	}
	
	//determines whether or not a claim has surface lava
	//used to warn players when they abandon their claims about automatic fluid cleanup
	boolean hasSurfaceFluids()
	{
		Location lesser = this.getLesserBoundaryCorner();
		Location greater = this.getGreaterBoundaryCorner();

		//don't bother for very large claims, too expensive
		if(this.getArea() > 10000) return false;
		
		int seaLevel = 0;  //clean up all fluids in the end
		
		//respect sea level in normal worlds
		if(lesser.getWorld().getEnvironment() == Environment.NORMAL) seaLevel = GriefPreventionPlus.instance.getSeaLevel(lesser.getWorld());
		
		for(int x = lesser.getBlockX(); x <= greater.getBlockX(); x++)
		{
			for(int z = lesser.getBlockZ(); z <= greater.getBlockZ(); z++)
			{
				for(int y = seaLevel - 1; y <= lesser.getWorld().getMaxHeight(); y++)
				{
					//dodge the exclusion claim
					Block block = lesser.getWorld().getBlockAt(x, y, z);
					
					if(block.getType() == Material.STATIONARY_LAVA || block.getType() == Material.LAVA)
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	//measurements.  all measurements are in blocks
	public int getArea()
	{
		return getWidth()*getHeight();	
	}
	
	public int getWidth()
	{
		return this.greaterX - this.lesserX + 1;	
	}
	
	public int getHeight()
	{
		return this.greaterZ - this.lesserZ + 1;	
	}
	
	public void setLocation(World world, int lx, int lz, int gx, int gz) {
		this.world=world;
		this.lesserX=lx;
		this.lesserZ=lz;
		this.greaterX=gx;
		this.greaterZ=gz;
	}
	
	//distance check for claims, distance in this case is a band around the outside of the claim rather then euclidean distance
	public boolean isNear(Location location, int howNear)
	{
		Claim claim = new Claim
			(this.world, this.lesserX - howNear, this.lesserZ - howNear,
			  this.greaterX + howNear, this.greaterZ + howNear,
			 null, null, null, null);
		
		return claim.contains(location, false, true);
	}
	
	//permissions.  note administrative "public" claims have different rules than other claims
	//all of these return NULL when a player has permission, or a String error message when the player doesn't have permission
	public String allowEdit(Player player)
	{
		//if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
		if(player == null) return "";
		
		//special cases...
		
		//admin claims need adminclaims permission only.
		if(this.isAdminClaim()) {
			if(player.hasPermission("griefprevention.adminclaims")) return null;
		}
		
		//anyone with deleteclaims permission can modify non-admin claims at any time
		else {
			if(player.hasPermission("griefprevention.deleteclaims")) return null;
		}
		
		//no resizing, deleting, and so forth while under siege
		if(player.getUniqueId().equals(this.ownerID)) {
			if(this.siegeData != null) {
				return GriefPreventionPlus.instance.dataStore.getMessage(Messages.NoModifyDuringSiege);
			}
			
			//otherwise, owners can do whatever
			return null;
		}
		
		//permission inheritance for subdivisions
		if(this.parent != null)
			return this.parent.allowEdit(player);
		
		//error message if all else fails
		return GriefPreventionPlus.instance.dataStore.getMessage(Messages.OnlyOwnersModifyClaims, this.getOwnerName());
	}
	
	private List<Material> placeableFarmingBlocksList = Arrays.asList(
	        Material.PUMPKIN_STEM,
	        Material.CROPS,
	        Material.MELON_STEM,
	        Material.CARROT,
	        Material.POTATO,
	        Material.NETHER_WARTS);
	    
    private boolean placeableForFarming(Material material)
    {
        return this.placeableFarmingBlocksList.contains(material);
    }
	
	//build permission check
	public String allowBuild(Player player, Material material)
	{
		//if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
		if(player == null) return "";
		
		//when a player tries to build in a claim, if he's under siege, the siege may extend to include the new claim
		GriefPreventionPlus.instance.dataStore.tryExtendSiege(player, this);
		
		//admin claims can always be modified by admins, no exceptions
		if(this.isAdminClaim()) {
			if(player.hasPermission("griefprevention.adminclaims")) return null;
		}
		
		//no building while under siege
		if(this.siegeData != null) {
			return GriefPreventionPlus.instance.dataStore.getMessage(Messages.NoBuildUnderSiege, this.siegeData.attacker.getName());
		}
		
		//no building while in pvp combat
		PlayerData playerData = GriefPreventionPlus.instance.dataStore.getPlayerData(player.getUniqueId());
		if(playerData.inPvpCombat()) {
			return GriefPreventionPlus.instance.dataStore.getMessage(Messages.NoBuildPvP);			
		}
		
		//owners can make changes, or admins with ignore claims mode enabled
		if(player.getUniqueId().equals(this.ownerID) || GriefPreventionPlus.instance.dataStore.getPlayerData(player.getUniqueId()).ignoreClaims) return null;
		
		//anyone with explicit build permission can make changes
		if(this.hasExplicitPermission(player, ClaimPermission.BUILD)) return null;
		
		//check for public permission
		if (this.hasPublicPermission(ClaimPermission.BUILD)) return null;
		
		//subdivision permission inheritance
		if(this.parent != null)
			return this.parent.allowBuild(player, material);
		
		//failure message for all other cases
		String reason = GriefPreventionPlus.instance.dataStore.getMessage(Messages.NoBuildPermission, this.getOwnerName());
		if(player.hasPermission("griefprevention.ignoreclaims"))
				reason += "  " + GriefPreventionPlus.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
		
		//allow for farming with /containertrust permission
		if(reason != null && this.allowContainers(player) == null) {
            //do allow for farming, if player has /containertrust permission
            if(this.placeableForFarming(material))
            {
                return null;
            }
        }
        
        return reason;
	}
	
	// The player need that explicit permission
	public boolean hasExplicitPermission(Player player, ClaimPermission level) {
		if ((this.getPermission(player.getUniqueId()) & level.perm) != 0) {
			return true;
		}
		
		// check for public permission
		if ((this.getPermission(GriefPreventionPlus.UUID0) & level.perm) != 0) {
			return true;
		}
		
		// check if the player has the default permissionBukkit permission
		switch(level) {
		case ACCESS:
			if (player.hasPermission("gpp.c"+this.id+".a")) {
				return true;
			}
			break;
		case CONTAINER:
			if (player.hasPermission("gpp.c"+this.id+".c")) {
				return true;
			}
			break;
		case BUILD:
			if (player.hasPermission("gpp.c"+this.id+".b")) {
				return true;
			}
			break;
		case MANAGE:
			if (player.hasPermission("gpp.c"+this.id+".m")) {
				return true;
			}
			break;
		}
		
		// check if the player has an explicit permissionBukkit permission
		for (Entry<String, Integer> e : this.permissionMapBukkit.entrySet()) {
			if ((e.getValue() & level.perm)!=0 && player.hasPermission(e.getKey())) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean hasPublicPermission(ClaimPermission level) {
		if ((this.getPermission(GriefPreventionPlus.UUID0) & level.perm) != 0) {
			return true;
		}
		
		return false;
	}
	
	//break permission check
	public String allowBreak(Player player, Material material)
	{
		//if under siege, some blocks will be breakable
		if(this.siegeData != null)
		{
			boolean breakable = false;
			
			//search for block type in list of breakable blocks
			for(int i = 0; i < GriefPreventionPlus.instance.config_siege_blocks.size(); i++)
			{
				Material breakableMaterial = GriefPreventionPlus.instance.config_siege_blocks.get(i);
				if(breakableMaterial == material)
				{
					breakable = true;
					break;
				}
			}
			
			//custom error messages for siege mode
			if(!breakable) {
				return GriefPreventionPlus.instance.dataStore.getMessage(Messages.NonSiegeMaterial);
			} else if(player.getUniqueId().equals(this.ownerID)) {
				return GriefPreventionPlus.instance.dataStore.getMessage(Messages.NoOwnerBuildUnderSiege);
			} else {
				return null;
			}
		}
		
		//if not under siege, build rules apply
		return this.allowBuild(player, material);
	}
	
	//access permission check
	public String allowAccess(Player player) {
		//following a siege where the defender lost, the claim will allow everyone access for a time
		if(this.doorsOpen) return null;
		
		//admin claims need adminclaims permission only.
		if(this.isAdminClaim()) {
			if(player.hasPermission("griefprevention.adminclaims")) return null;
		}
		
		//claim owner and admins in ignoreclaims mode have access
		if(player.getUniqueId().equals(this.ownerID) || GriefPreventionPlus.instance.dataStore.getPlayerData(player.getUniqueId()).ignoreClaims) return null;
		
		//look for explicit (or public) individual access, inventory, or build permission
		if(this.hasExplicitPermission(player, ClaimPermission.ACCESS)) return null;
		if(this.hasExplicitPermission(player, ClaimPermission.CONTAINER)) return null;
		if(this.hasExplicitPermission(player, ClaimPermission.BUILD)) return null;
		
		//also check for public permission
		if(this.hasPublicPermission(ClaimPermission.ACCESS)) return null;
		if(this.hasPublicPermission(ClaimPermission.CONTAINER)) return null;
		if(this.hasPublicPermission(ClaimPermission.BUILD)) return null;
		
		//permission inheritance for subdivisions
		if(this.parent != null)
			return this.parent.allowAccess(player);
		
		//catch-all error message for all other cases
		String reason = GriefPreventionPlus.instance.dataStore.getMessage(Messages.NoAccessPermission, this.getOwnerName());
		if(player.hasPermission("griefprevention.ignoreclaims"))
			reason += "  " + GriefPreventionPlus.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
		return reason;
	}
	
	//inventory permission check
	public String allowContainers(Player player)
	{		
		//if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
		if(player == null) return "";
		
		//trying to access inventory in a claim may extend an existing siege to include this claim
		GriefPreventionPlus.instance.dataStore.tryExtendSiege(player, this);
		
		//if under siege, nobody accesses containers
		if(this.siegeData != null) {
			return GriefPreventionPlus.instance.dataStore.getMessage(Messages.NoContainersSiege, siegeData.attacker.getName());
		}
		
		//owner and administrators in ignoreclaims mode have access
		if(player.getUniqueId().equals(this.ownerID) || GriefPreventionPlus.instance.dataStore.getPlayerData(player.getUniqueId()).ignoreClaims) return null;
		
		//admin claims need adminclaims permission only.
		if(this.isAdminClaim()) {
			if(player.hasPermission("griefprevention.adminclaims")) return null;
		}
		
		//check for explicit individual container or build permission 
		if(this.hasExplicitPermission(player, ClaimPermission.CONTAINER)) return null;
		if(this.hasExplicitPermission(player, ClaimPermission.BUILD)) return null;
		
		//check for public container or build permission
		if(this.hasPublicPermission(ClaimPermission.CONTAINER)) return null;
		if(this.hasPublicPermission(ClaimPermission.BUILD)) return null;
		
		//permission inheritance for subdivisions
		if(this.parent != null)
			return this.parent.allowContainers(player);
		
		//error message for all other cases
		String reason = GriefPreventionPlus.instance.dataStore.getMessage(Messages.NoContainersPermission, this.getOwnerName());
		if(player.hasPermission("griefprevention.ignoreclaims"))
			reason += "  " + GriefPreventionPlus.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
		return reason;
	}
	
	//grant permission check, relatively simple
	public String allowGrantPermission(Player player) // add permission level
	{
		// TODO who has manager pemission should manage perms up to his level (managers excluded)
		//if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
		if(player == null) return "";
		
		//anyone who can modify the claim can do this
		if(this.allowEdit(player) == null) return null;
		
		if(this.hasExplicitPermission(player, ClaimPermission.MANAGE)) {
			return null;
		}

		//permission inheritance for subdivisions
		if(this.parent != null) {
			return this.parent.allowGrantPermission(player);
		}
		
		//generic error message
		String reason = GriefPreventionPlus.instance.dataStore.getMessage(Messages.NoPermissionTrust, this.getOwnerName());
		if(player.hasPermission("griefprevention.ignoreclaims")) {
			reason += "  " + GriefPreventionPlus.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
		}
		return reason;
	}
	
	//grants a permission for a player or the public
	public Integer getPermission(UUID playerID)
	{
		Integer perm = this.permissionMapPlayers.get(playerID);
		if (perm==null) {
			perm=0;
		}
		return perm;
	}
	
	//grants a permission for a bukkit permission
	public Integer getPermission(String permissionBukkit)
	{
		Integer perm = this.permissionMapBukkit.get(permissionBukkit);
		if (perm==null) {
			perm=0;
		}
		return perm;
	}

	
	public ConcurrentHashMap<UUID, Integer> getPermissionMapPlayers() {
		return permissionMapPlayers;
	}


	public ConcurrentHashMap<String, Integer> getPermissionMapBukkit() {
		return permissionMapBukkit;
	}


	//grants a permission for a player or the public
	public void setPermission(UUID playerID, ClaimPermission permissionLevel)
	{
		Integer currentPermission = this.getPermission(playerID);

		this.permissionMapPlayers.put(playerID,  currentPermission | permissionLevel.perm);
		
		GriefPreventionPlus.instance.dataStore.dbSetPerm(this.id, playerID, permissionLevel.perm);
	}
	
	//grants a permission for a bukkit permission
	public void setPermission(String permissionBukkit, ClaimPermission permissionLevel)
	{
		Integer currentPermission = this.getPermission(permissionBukkit);
		
		this.permissionMapBukkit.put(permissionBukkit,  currentPermission | permissionLevel.perm);
		
		GriefPreventionPlus.instance.dataStore.dbSetPerm(this.id, permissionBukkit, permissionLevel.perm);
	}
	
	//revokes a permission for a player or the public
	public void dropPermission(UUID playerID)
	{
		this.permissionMapPlayers.remove(playerID);
	}
	
	//revokes a permission for a bukkit permission
	public void dropPermission(String permissionBukkit)
	{
		this.permissionMapBukkit.remove(permissionBukkit);
	}
	
	//clears all permissions (except owner of course)
	public void clearPermissions()
	{
		this.permissionMapPlayers.clear();
		this.permissionMapBukkit.clear();
	}
	
	//gets ALL permissions
	//useful for  making copies of permissions during a claim resize and listing all permissions in a claim
	public void getPermissions(ArrayList<String> builders, ArrayList<String> containers, ArrayList<String> accessors, ArrayList<String> managers)
	{
		//loop through all the entries in the hash map
		for(Entry<UUID, Integer> entry : this.permissionMapPlayers.entrySet()) {
			if((entry.getValue() & ClaimPermission.MANAGE.perm)!=0) {
				managers.add(GriefPreventionPlus.instance.getServer().getOfflinePlayer(entry.getKey()).getName());
			}
			
			if((entry.getValue() & ClaimPermission.BUILD.perm)!=0) {
				builders.add(GriefPreventionPlus.instance.getServer().getOfflinePlayer(entry.getKey()).getName());
			} else if((entry.getValue() & ClaimPermission.CONTAINER.perm)!=0) {
				containers.add(GriefPreventionPlus.instance.getServer().getOfflinePlayer(entry.getKey()).getName());
			} else if((entry.getValue() & ClaimPermission.ACCESS.perm)!=0) {
				accessors.add(GriefPreventionPlus.instance.getServer().getOfflinePlayer(entry.getKey()).getName());
			}			
		}
		
		for(Entry<String, Integer> entry : this.permissionMapBukkit.entrySet()) {
			if((entry.getValue() & ClaimPermission.MANAGE.perm)!=0) {
				managers.add("["+entry.getKey()+"]");
			}
			
			if((entry.getValue() & ClaimPermission.BUILD.perm)!=0) {
				builders.add("["+entry.getKey()+"]");
			} else if((entry.getValue() & ClaimPermission.CONTAINER.perm)!=0) {
				containers.add("["+entry.getKey()+"]");
			} else if((entry.getValue() & ClaimPermission.ACCESS.perm)!=0) {
				accessors.add("["+entry.getKey()+"]");
			}
		}
	}
	
	//returns a copy of the location representing lower x, y, z limits
	public Location getLesserBoundaryCorner() {
		return new Location(this.world, this.lesserX, 0, this.lesserZ);
	}
	
	/**returns a copy of the location representing upper x, y, z limits
	 * NOTE: remember upper Y will always be ignored, all claims always extend to the sky*/
	public Location getGreaterBoundaryCorner() {
		return new Location(this.world, this.greaterX, 0, this.greaterZ);
	}
	
	/** returns a friendly owner name (for admin claims, returns "an administrator" as the owner) */
	public String getOwnerName() {
		if(this.parent != null)
			return this.parent.getOwnerName();
		
		if(this.ownerID == null || this.ownerID.equals(GriefPreventionPlus.UUID1))
			return GriefPreventionPlus.instance.dataStore.getMessage(Messages.OwnerNameForAdminClaims);
		
		return GriefPreventionPlus.lookupPlayerName(this.ownerID);
	}	
	
	/** whether or not a location is in a claim
	 *  ignoreHeight = true means location UNDER the claim will return TRUE
	 *  excludeSubdivisions = true means that locations inside subdivisions of the claim will return FALSE */
	public boolean contains(Location location, boolean ignoreHeight, boolean excludeSubdivisions)
	{
	    //not in the same world implies false
		if(!location.getWorld().equals(this.world)) return false;
		
		double x = location.getX();
		double y = location.getY();
		double z = location.getZ();
		
		//main check
		boolean inClaim = (ignoreHeight || y >= GriefPreventionPlus.instance.config_claims_maxDepth) &&
				x >= this.lesserX &&
				x < this.greaterX + 1 &&
				z >= this.lesserZ &&
				z < this.greaterZ + 1;
		
		if(!inClaim) return false;
				
	    //additional check for subdivisions
		//you're only in a subdivision when you're also in its parent claim
		//NOTE: if a player creates subdivions then resizes the parent claim, it's possible that
		//a subdivision can reach outside of its parent's boundaries.  so this check is important!
		if(this.parent != null) {
	    	return this.parent.contains(location, ignoreHeight, false);
	    }
		
		//code to exclude subdivisions in this check
		else if(excludeSubdivisions) {
			//search all subdivisions to see if the location is in any of them
			for(int i = 0; i < this.children.size(); i++) {
				//if we find such a subdivision, return false
				if(this.children.get(i).contains(location, ignoreHeight, true)) {
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
		
		if(!this.world.equals(otherClaim.world)) return false;
		
		//first, check the corners of this claim aren't inside any existing claims
		if(otherClaim.contains(this.getLesserBoundaryCorner(), true, false)) return true;
		if(otherClaim.contains(this.getGreaterBoundaryCorner(), true, false)) return true;
		if(otherClaim.contains(new Location(this.world, this.lesserX, 0, this.greaterZ), true, false)) return true;
		if(otherClaim.contains(new Location(this.world, this.greaterX, 0, this.lesserZ), true, false)) return true;
		
		
		//verify that no claim's lesser boundary point is inside this new claim, to cover the "existing claim is entirely inside new claim" case
		if(this.contains(otherClaim.getLesserBoundaryCorner(), true, false)) return true;
		
		//verify this claim doesn't band across an existing claim, either horizontally or vertically		
		if(	this.lesserZ <= otherClaim.greaterZ && 
			this.lesserZ >= otherClaim.lesserZ && 
			this.lesserX < otherClaim.lesserX &&
			this.greaterX > otherClaim.greaterX )
			return true;
		
		if(	this.greaterZ <= otherClaim.greaterZ && 
			this.greaterZ >= otherClaim.lesserZ && 
			this.lesserX < otherClaim.lesserX &&
			this.greaterX > otherClaim.greaterX )
			return true;
		
		if(	this.lesserX <= otherClaim.greaterX && 
			this.lesserX >= otherClaim.lesserX && 
			this.lesserZ < otherClaim.lesserZ &&
			this.greaterZ > otherClaim.greaterZ )
			return true;
			
		if(	this.greaterX <= otherClaim.greaterX && 
			this.greaterX >= otherClaim.lesserX && 
			this.lesserZ < otherClaim.lesserZ &&
			this.greaterZ > otherClaim.greaterZ )
			return true;
		
		return false;
	}
	
	//whether more entities may be added to a claim
	public String allowMoreEntities()
	{
		if(this.parent != null) return this.parent.allowMoreEntities();
		
		//this rule only applies to creative mode worlds
		if(!GriefPreventionPlus.instance.creativeRulesApply(this.world)) return null;
		
		//admin claims aren't restricted
		if(this.isAdminClaim()) return null;
		
		//don't apply this rule to very large claims
		if(this.getArea() > 10000) return null;
		
		//determine maximum allowable entity count, based on claim size
		int maxEntities = this.getArea() / 50;		
		if(maxEntities == 0) return GriefPreventionPlus.instance.dataStore.getMessage(Messages.ClaimTooSmallForEntities);
		
		//count current entities (ignoring players)
		int totalEntities = 0;
		ArrayList<Chunk> chunks = this.getChunks();
		for(Chunk chunk : chunks)
		{
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

		if(totalEntities > maxEntities) return GriefPreventionPlus.instance.dataStore.getMessage(Messages.TooManyEntitiesInClaim);
		
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
	
	long getPlayerInvestmentScore()
	{
		//decide which blocks will be considered player placed
		ArrayList<Integer> playerBlocks = RestoreNatureProcessingTask.getPlayerBlocks(this.world.getEnvironment(), this.getLesserBoundaryCorner().getBlock().getBiome());
		
		//scan the claim for player placed blocks
		double score = 0;
		
		boolean creativeMode = GriefPreventionPlus.instance.creativeRulesApply(this.world);
		
		for(int x = this.lesserX; x <= this.greaterX; x++)
		{
			for(int z = this.lesserZ; z <= this.greaterZ; z++)
			{
				int y = GriefPreventionPlus.instance.config_claims_maxDepth;
				for(; y < GriefPreventionPlus.instance.getSeaLevel(this.world) - 5; y++)
				{
					Block block = this.world.getBlockAt(x, y, z);
					if(playerBlocks.contains(block.getTypeId()))
					{
						if(block.getType() == Material.CHEST && !creativeMode) {
							score += 10;
						} else if (block.getType() != Material.DIRT && block.getType() != Material.STONE && block.getType() != Material.COBBLESTONE && block.getType() != Material.WOOD && block.getType() != Material.BEDROCK && block.getType() != Material.GRAVEL) {
							score += .5;
						}						
					}
				}
				
				for(; y < this.world.getMaxHeight(); y++)
				{
					Block block = this.world.getBlockAt(x, y, z);
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

    public ArrayList<Chunk> getChunks()
    {
        ArrayList<Chunk> chunks = new ArrayList<Chunk>();
        
        World world = this.world;
        Chunk lesserChunk = this.getLesserBoundaryCorner().getChunk();
        Chunk greaterChunk = this.getGreaterBoundaryCorner().getChunk();
        
        for(int x = lesserChunk.getX(); x <= greaterChunk.getX(); x++)
        {
            for(int z = lesserChunk.getZ(); z <= greaterChunk.getZ(); z++)
            {
                chunks.add(world.getChunkAt(x, z));
            }
        }
        
        return chunks;
    }

    public ArrayList<String> getChunkStrings() {
        ArrayList<String> chunkStrings = new ArrayList<String>();
        World world = this.world;
        int smallX = this.lesserX >> 4;
        int smallZ = this.lesserZ >> 4;
		int largeX = this.greaterX >> 4;
		int largeZ = this.greaterZ >> 4;
		
		for(int x = smallX; x <= largeX; x++)
		{
		    for(int z = smallZ; z <= largeZ; z++)
		    {
		        StringBuilder builder = new StringBuilder(String.valueOf(x)).append(world.getName()).append(z);
		        chunkStrings.add(builder.toString());
		    }
		}
		
		return chunkStrings;
    }
    
	public String locationToString() {
		return this.world.getName()+"["+this.lesserX+","+this.lesserZ+"~"+this.greaterX+","+this.greaterZ+"]";
	}
	
	/** check if this player has that permission on this claim 
	 * @return null string if the player has the permission */
	public String checkPermission(Player player, Integer perm) {
		if (perm==null) {
			return "null";
		}
		if (this.allowEdit(player) == null) { // owner and admins always pass this check
			return null;
		}
		if ((perm&ClaimPermission.BUILD.perm)!=0) {
			return this.allowBuild(player, Material.AIR);
		} 
		if ((perm&ClaimPermission.CONTAINER.perm)!=0) {
			return this.allowContainers(player);
		} 
		if ((perm&ClaimPermission.ACCESS.perm)!=0) {
			return this.allowAccess(player);
		}
		return "invalid";
	}
}
