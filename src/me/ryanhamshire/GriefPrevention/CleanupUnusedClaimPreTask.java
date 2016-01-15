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

import org.bukkit.Bukkit;

//asynchronously loads player data without caching it in the datastore, then
//passes those data to a claim cleanup task which might decide to delete a claim for inactivity

class CleanupUnusedClaimPreTask implements Runnable 
{	
	private Claim claim = null;
	
	CleanupUnusedClaimPreTask(Claim claim)
	{
		this.claim = claim;
	}
	
	@Override
	public void run()
	{
		//get the data
	    PlayerData ownerData = GriefPrevention.instance.dataStore.getPlayerDataFromStorage(claim.ownerID);
	    
	    //skip claims belonging to exempted players based on block totals in config
	    int bonusBlocks = ownerData.getBonusClaimBlocks();
	    if(bonusBlocks >= GriefPrevention.instance.config_claims_expirationExemptionBonusBlocks) return;
	    if(bonusBlocks + ownerData.getAccruedClaimBlocks() >= GriefPrevention.instance.config_claims_expirationExemptionTotalBlocks) return;
		
	    //pass it back to the main server thread, where it's safe to delete a claim if needed
	    Bukkit.getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, new CleanupUnusedClaimTask(claim, ownerData), 1L);
	}
}