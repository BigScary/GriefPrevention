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

package me.ryanhamshire.GriefPrevention.claim;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

	public UUID getOwnerID()
	{
		return ownerID;
	}
	
	//permissions for this claim, see ClaimPermission class
	private HashMap<UUID, ClaimPermission> playerIDToClaimPermissionMap = new HashMap<UUID, ClaimPermission>();
	
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

	//copy constructor
	public Claim(Claim claim, UUID ownerID)
	{
		this.modifiedDate = Calendar.getInstance().getTime();
		this.lesserBoundaryCorner = claim.lesserBoundaryCorner;
		this.greaterBoundaryCorner = claim.greaterBoundaryCorner;
		this.playerIDToClaimPermissionMap = claim.playerIDToClaimPermissionMap;
		this.id = claim.id;

		this.ownerID = ownerID;
	}

	//basic constructor, just notes the creation time
	//see above declarations for other defaults
	Claim()
	{
		this.modifiedDate = Calendar.getInstance().getTime();
	}
	
	//main constructor.  note that only creating a claim instance does nothing - a claim must be added to the data store to be effective
	Claim(Location lesserBoundaryCorner, Location greaterBoundaryCorner, UUID ownerID, List<UUID> builderIDs, List<UUID> containerIDs, List<UUID> accessorIDs, List<UUID> managerIDs, Long id)
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
		for(UUID builderID : builderIDs)
		{
			this.playerIDToClaimPermissionMap.put(builderID, ClaimPermission.Build);
		}
		
		for(UUID containerID : containerIDs)
		{
			this.playerIDToClaimPermissionMap.put(containerID, ClaimPermission.Inventory);
		}
		
		for(UUID accessorID : accessorIDs)
		{
			this.playerIDToClaimPermissionMap.put(accessorID, ClaimPermission.Access);
		}
		
		for(UUID managerID : managerIDs)
		{
			this.playerIDToClaimPermissionMap.put(managerID, ClaimPermission.Manage);
		}
	}
	
	//measurements.  all measurements are in blocks
	public int getArea()
	{
		return this.getWidth() * this.getHeight();
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
			 null, new ArrayList<UUID>(), new ArrayList<UUID>(), new ArrayList<UUID>(), new ArrayList<UUID>(), null);
		
		return claim.contains(location, true);
	}

	//grants a permission for a player or the public
	public void setPermission(String playerID, ClaimPermission permissionLevel)
	{
		this.playerIDToClaimPermissionMap.put(UUID.fromString(playerID),  permissionLevel);
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
	}

	//gets ALL permissions
	//useful for  making copies of permissions during a claim resize and listing all permissions in a claim
	public void getPermissions(ArrayList<String> builders, ArrayList<String> containers, ArrayList<String> accessors, ArrayList<String> managers)
	{
		//loop through all the entries in the hash map
		Iterator<Map.Entry<UUID, ClaimPermission>> mappingsIterator = this.playerIDToClaimPermissionMap.entrySet().iterator();
		while(mappingsIterator.hasNext())
		{
			Map.Entry<UUID, ClaimPermission> entry = mappingsIterator.next();

			//build up a list for each permission level
			if(entry.getValue() == ClaimPermission.Build)
			{
				builders.add(entry.getKey().toString());
			}
			else if(entry.getValue() == ClaimPermission.Inventory)
			{
				containers.add(entry.getKey().toString());
			}
			else if (entry.getValue() == ClaimPermission.Access)
			{
				accessors.add(entry.getKey().toString());
			}
			else if (entry.getValue() == ClaimPermission.Manage)
			{
				managers.add(entry.getKey().toString());
			}
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
	public boolean overlaps(Claim otherClaim)
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
}
