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

import java.util.*;

import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

//represents a player claim
//creating an instance doesn't make an effective claim
//only claims which have been added to the datastore have any effect
public class Claim
{
	//two locations, which together define the boundaries of the claim
	//note that the upper Y value is always ignored, because claims ALWAYS extend up to the sky
	private Location lesserBoundaryCorner;
	private Location greaterBoundaryCorner;
	
	//modification date.  this comes from the file timestamp during load, and is updated with runtime changes
	//TODO: RoboMWM - is this even needed
	private Date modifiedDate;
	
	//id number.  unique to this claim, never changes.
	Long id = null;
	
	//ownerID.  for admin claims, this is NULL
	//use getOwnerName() to get a friendly name (will be "an administrator" for admin claims)
	private UUID ownerID;
	
	//list of players who (beyond the claim owner) have permission to grant permissions in this claim
	private ArrayList<String> managers = new ArrayList<String>();
	
	//permissions for this claim, see ClaimPermission class
	private HashMap<String, ClaimPermission> playerIDToClaimPermissionMap = new HashMap<String, ClaimPermission>();
	
	//whether or not this claim is in the data store
	//if a claim instance isn't in the data store, it isn't "active" - players can't interract with it 
	//why keep this?  so that claims which have been removed from the data store can be correctly 
	//ignored even though they may have references floating around
	//TODO: RoboMWM - probably should aim to remove this
	private boolean inDataStore = false;

	private boolean explosivesTemporarilyAllowed = false;

	public boolean areExplosivesAllowed()
	{
		return explosivesTemporarilyAllowed;
	}
	
	//whether or not this is an administrative claim
	//administrative claims are created and maintained by players with the griefprevention.adminclaims permission.
	public boolean isAdminClaim()
	{
		return (this.ownerID == null);
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
		this.modifiedDate = Calendar.getInstance().getTime();
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
		if(!GriefPrevention.instance.creativeRulesApply(this.lesserBoundaryCorner)) return;
		
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
					if(exclusionClaim != null && exclusionClaim.contains(block.getLocation(), false)) continue;
					
					if(block.getType() == Material.STATIONARY_LAVA || block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER || block.getType() == Material.LAVA)
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
		if(lesser.getWorld().getEnvironment() == Environment.NORMAL) seaLevel = GriefPrevention.instance.getSeaLevel(lesser.getWorld());
		
		for(int x = lesser.getBlockX(); x <= greater.getBlockX(); x++)
		{
			for(int z = lesser.getBlockZ(); z <= greater.getBlockZ(); z++)
			{
				for(int y = seaLevel - 1; y <= lesser.getWorld().getMaxHeight(); y++)
				{
					//dodge the exclusion claim
					Block block = lesser.getWorld().getBlockAt(x, y, z);
					
					if(block.getType() == Material.STATIONARY_LAVA || block.getType() == Material.WATER || block.getType() == Material.STATIONARY_WATER || block.getType() == Material.LAVA)
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	//main constructor.  note that only creating a claim instance does nothing - a claim must be added to the data store to be effective
	Claim(Location lesserBoundaryCorner, Location greaterBoundaryCorner, UUID ownerID, List<String> builderIDs, List<String> containerIDs, List<String> accessorIDs, List<String> managerIDs, Long id)
	{
		//modification date
		this.modifiedDate = Calendar.getInstance().getTime();
		
		//id
		this.id = id;
		
		//store corners
		this.lesserBoundaryCorner = lesserBoundaryCorner;
		this.greaterBoundaryCorner = greaterBoundaryCorner;
		
		//owner
		this.ownerID = ownerID;
		
		//other permissions
		for(String builderID : builderIDs)
		{
			if(builderID != null && !builderID.isEmpty())
			{
				this.playerIDToClaimPermissionMap.put(builderID, ClaimPermission.Build);
			}
		}
		
		for(String containerID : containerIDs)
		{
			if(containerID != null && !containerID.isEmpty())
			{
				this.playerIDToClaimPermissionMap.put(containerID, ClaimPermission.Inventory);
			}
		}
		
		for(String accessorID : accessorIDs)
		{
			if(accessorID != null && !accessorID.isEmpty())
			{
				this.playerIDToClaimPermissionMap.put(accessorID, ClaimPermission.Access);
			}
		}
		
		for(String managerID : managerIDs)
		{
			if(managerID != null && !managerID.isEmpty())
			{
				this.managers.add(managerID);
			}
		}
	}
	
	//measurements.  all measurements are in blocks
	public int getArea()
	{
		int claimWidth = this.greaterBoundaryCorner.getBlockX() - this.lesserBoundaryCorner.getBlockX() + 1;
		int claimHeight = this.greaterBoundaryCorner.getBlockZ() - this.lesserBoundaryCorner.getBlockZ() + 1;
		
		return claimWidth * claimHeight;		
	}
	
	public int getWidth()
	{
		return this.greaterBoundaryCorner.getBlockX() - this.lesserBoundaryCorner.getBlockX() + 1;		
	}
	
	public int getHeight()
	{
		return this.greaterBoundaryCorner.getBlockZ() - this.lesserBoundaryCorner.getBlockZ() + 1;		
	}
	
	//distance check for claims, distance in this case is a band around the outside of the claim rather then euclidean distance
	public boolean isNear(Location location, int howNear)
	{
		Claim claim = new Claim
			(new Location(this.lesserBoundaryCorner.getWorld(), this.lesserBoundaryCorner.getBlockX() - howNear, this.lesserBoundaryCorner.getBlockY(), this.lesserBoundaryCorner.getBlockZ() - howNear),
			 new Location(this.greaterBoundaryCorner.getWorld(), this.greaterBoundaryCorner.getBlockX() + howNear, this.greaterBoundaryCorner.getBlockY(), this.greaterBoundaryCorner.getBlockZ() + howNear),
			 null, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), null);
		
		return claim.contains(location, true);
	}

	//grants a permission for a player or the public
	public void setPermission(String playerID, ClaimPermission permissionLevel)
	{
		this.playerIDToClaimPermissionMap.put(playerID.toLowerCase(),  permissionLevel);
	}

	//revokes a permission for a player or the public
	public void dropPermission(String playerID)
	{
		this.playerIDToClaimPermissionMap.remove(playerID.toLowerCase());
	}

	//clears all permissions (except owner of course)
	public void clearPermissions()
	{
		this.playerIDToClaimPermissionMap.clear();
		this.managers.clear();
	}

	//gets ALL permissions
	//useful for  making copies of permissions during a claim resize and listing all permissions in a claim
	public void getPermissions(ArrayList<String> builders, ArrayList<String> containers, ArrayList<String> accessors, ArrayList<String> managers)
	{
		//loop through all the entries in the hash map
		Iterator<Map.Entry<String, ClaimPermission>> mappingsIterator = this.playerIDToClaimPermissionMap.entrySet().iterator();
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
		for(int i = 0; i < this.managers.size(); i++)
		{
			managers.add(this.managers.get(i));
		}
	}
	
	//returns a copy of the location representing lower x, y, z limits
	public Location getLesserBoundaryCorner()
	{
		return this.lesserBoundaryCorner.clone();
	}
	
	//returns a copy of the location representing upper x, y, z limits
	//NOTE: remember upper Y will always be ignored, all claims always extend to the sky
	public Location getGreaterBoundaryCorner()
	{
		return this.greaterBoundaryCorner.clone();
	}
	
	//returns a friendly owner name (for admin claims, returns "an administrator" as the owner)
	public String getOwnerName()
	{
		if (this.ownerID == null)
			return GriefPrevention.instance.dataStore.getMessage(Messages.OwnerNameForAdminClaims);
		
		return GriefPrevention.lookupPlayerName(this.ownerID);
	}	
	
	//whether or not a location is in a claim
	//ignoreHeight = true means location UNDER the claim will return TRUE
	//excludeSubdivisions = true means that locations inside subdivisions of the claim will return FALSE
	public boolean contains(Location location, boolean ignoreHeight)
	{
	    //not in the same world implies false
		if(!location.getWorld().equals(this.lesserBoundaryCorner.getWorld())) return false;
		
		double x = location.getX();
		double y = location.getY();
		double z = location.getZ();
		
		//main check
		boolean inClaim = (ignoreHeight || y >= this.lesserBoundaryCorner.getY()) &&
				x >= this.lesserBoundaryCorner.getX() &&
				x < this.greaterBoundaryCorner.getX() + 1 &&
				z >= this.lesserBoundaryCorner.getZ() &&
				z < this.greaterBoundaryCorner.getZ() + 1;
				
		if(!inClaim) return false;
		
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
		if(otherClaim.contains(this.lesserBoundaryCorner, false)) return true;
		if(otherClaim.contains(this.greaterBoundaryCorner, false)) return true;
		if(otherClaim.contains(new Location(this.lesserBoundaryCorner.getWorld(), this.lesserBoundaryCorner.getBlockX(), 0, this.greaterBoundaryCorner.getBlockZ()), false)) return true;
		if(otherClaim.contains(new Location(this.lesserBoundaryCorner.getWorld(), this.greaterBoundaryCorner.getBlockX(), 0, this.lesserBoundaryCorner.getBlockZ()), false)) return true;
		
		//verify that no claim's lesser boundary point is inside this new claim, to cover the "existing claim is entirely inside new claim" case
		if(this.contains(otherClaim.getLesserBoundaryCorner(), false)) return true;
		
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
	
	//whether more entities may be added to a claim
	public String allowMoreEntities(boolean remove)
	{
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
		int totalEntities = 0;
		ArrayList<Chunk> chunks = this.getChunks();
		for(Chunk chunk : chunks)
		{
			Entity [] entities = chunk.getEntities();
			for(int i = 0; i < entities.length; i++)
			{
				Entity entity = entities[i];
				if(!(entity instanceof Player) && this.contains(entity.getLocation(), false))
				{
					totalEntities++;
					if(remove && totalEntities > maxEntities) entity.remove();
				}
			}
		}

		if(totalEntities >= maxEntities) return GriefPrevention.instance.dataStore.getMessage(Messages.TooManyEntitiesInClaim);
		
		return null;
	}
	
	public String allowMoreActiveBlocks()
    {
	    //determine maximum allowable entity count, based on claim size
        int maxActives = this.getArea() / 100;      
        if(maxActives == 0) return GriefPrevention.instance.dataStore.getMessage(Messages.ClaimTooSmallForActiveBlocks);
        
        //count current actives
        int totalActives = 0;
        ArrayList<Chunk> chunks = this.getChunks();
        for(Chunk chunk : chunks)
        {
            BlockState [] actives = chunk.getTileEntities();
            for(int i = 0; i < actives.length; i++)
            {
                BlockState active = actives[i];
                if(BlockEventHandler.isActiveBlock(active))
                {
                    if(this.contains(active.getLocation(), false))
                    {
                        totalActives++;
                    }
                }
            }
        }

        if(totalActives >= maxActives) return GriefPrevention.instance.dataStore.getMessage(Messages.TooManyActiveBlocksInClaim);
        
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
	
	@SuppressWarnings("deprecation")
    long getPlayerInvestmentScore()
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

    public ArrayList<Chunk> getChunks()
    {
        ArrayList<Chunk> chunks = new ArrayList<Chunk>();
        
        World world = this.getLesserBoundaryCorner().getWorld();
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

    ArrayList<Long> getChunkHashes()
    {
        ArrayList<Long> hashes = new ArrayList<Long>();
        int smallX = this.getLesserBoundaryCorner().getBlockX() >> 4;
        int smallZ = this.getLesserBoundaryCorner().getBlockZ() >> 4;
		int largeX = this.getGreaterBoundaryCorner().getBlockX() >> 4;
		int largeZ = this.getGreaterBoundaryCorner().getBlockZ() >> 4;
		
		for(int x = smallX; x <= largeX; x++)
		{
		    for(int z = smallZ; z <= largeZ; z++)
		    {
		        hashes.add(DataStore.getChunkHash(x, z));
		    }
		}
		
		return hashes;
    }
}
