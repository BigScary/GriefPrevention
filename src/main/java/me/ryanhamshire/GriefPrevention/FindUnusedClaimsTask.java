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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;

//FEATURE: automatically remove claims owned by inactive players which:
//...aren't protecting much OR
//...are a free new player claim (and the player has no other claims) OR
//...because the player has been gone a REALLY long time, and that expiration has been configured in config.yml

//runs every 1 minute in the main thread
class FindUnusedClaimsTask implements Runnable 
{
	private Set<UUID> claimOwnerUUIDs = new HashSet<>();
	private Iterator<UUID> claimOwnerIterator;
	
	FindUnusedClaimsTask()
	{
		refreshUUIDs();
	}
	
	@Override
	public void run()
	{
		//don't do anything when there are no claims
		if(claimOwnerUUIDs.isEmpty()) return;

		//wrap search around to beginning
		if(!claimOwnerIterator.hasNext())
		{
			refreshUUIDs();
			return;
		}
		
		Bukkit.getScheduler().runTaskAsynchronously(GriefPrevention.instance, new CleanupUnusedClaimPreTask(claimOwnerIterator.next()));
	}

	public void refreshUUIDs()
	{
		claimOwnerUUIDs.clear();
		for (Claim claim : GriefPrevention.instance.dataStore.claims)
			claimOwnerUUIDs.add(claim.ownerID);
		claimOwnerUUIDs.remove(null);
		claimOwnerIterator = claimOwnerUUIDs.iterator();
	}
}
