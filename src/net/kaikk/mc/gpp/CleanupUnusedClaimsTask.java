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

import java.util.Calendar;
import java.util.Random;
import java.util.Vector;

import org.bukkit.Chunk;
import org.bukkit.World;

//FEATURE: automatically remove claims owned by inactive players which:
//...aren't protecting much OR
//...are a free new player claim (and the player has no other claims) OR
//...because the player has been gone a REALLY long time, and that expiration has been configured in config.yml

//runs every 1 minute in the main thread
class CleanupUnusedClaimsTask implements Runnable 
{	
	int nextClaimIndex;
	
	CleanupUnusedClaimsTask()
	{
		//start scanning in a random spot
		if(GriefPreventionPlus.instance.dataStore.claims.size() == 0) {
			this.nextClaimIndex = 0;
		} else {
			Random randomNumberGenerator = new Random();
			this.nextClaimIndex = randomNumberGenerator.nextInt(GriefPreventionPlus.instance.dataStore.claims.size());
		}
	}
	
	@Override
	public void run()
	{
		//don't do anything when there are no claims
		if(GriefPreventionPlus.instance.dataStore.claims.size() == 0) return;
		
		//wrap search around to beginning
		if(this.nextClaimIndex >= GriefPreventionPlus.instance.dataStore.claims.size()) this.nextClaimIndex = 0;
		
		//decide which claim to check next
		long j=0;
		Claim claim=null;
		for (long k : GriefPreventionPlus.instance.dataStore.claims.keySet()) {
			if (j==this.nextClaimIndex) {
				claim = GriefPreventionPlus.instance.dataStore.claims.get(k);
				this.nextClaimIndex++;
				break;
			}
			j++;
		}
		
		if (claim==null) {
			return;
		}
		
		//skip administrative claims
		if(claim.isAdminClaim()) return;
		
		//track whether we do any important work which would require cleanup afterward
		boolean cleanupChunks = false;
		
		//get data for the player, especially last login timestamp
		PlayerData playerData = null;
		
		//determine area of the default chest claim
		int areaOfDefaultClaim = 0;
		if(GriefPreventionPlus.instance.config_claims_automaticClaimsForNewPlayersRadius >= 0) {
			areaOfDefaultClaim = (int)Math.pow(GriefPreventionPlus.instance.config_claims_automaticClaimsForNewPlayersRadius * 2 + 1, 2);  
		}
		
		//if this claim is a chest claim and those are set to expire
        if(claim.getArea() <= areaOfDefaultClaim && GriefPreventionPlus.instance.config_claims_chestClaimExpirationDays > 0)
		{
            playerData = GriefPreventionPlus.instance.dataStore.getPlayerData(claim.ownerID);
            
            //if the owner has been gone at least a week, and if he has ONLY the new player claim, it will be removed
	        Calendar sevenDaysAgo = Calendar.getInstance();
	        sevenDaysAgo.add(Calendar.DATE, -GriefPreventionPlus.instance.config_claims_chestClaimExpirationDays);
	        boolean newPlayerClaimsExpired = sevenDaysAgo.getTime().after(playerData.getLastLogin());
			if(newPlayerClaimsExpired && playerData.getClaims().size() == 1)
			{
				claim.removeSurfaceFluids(null);
				GriefPreventionPlus.instance.dataStore.deleteClaim(claim, true);
				cleanupChunks = true;
				
				//if configured to do so, restore the land to natural
				if(GriefPreventionPlus.instance.creativeRulesApply(claim.world) || GriefPreventionPlus.instance.config_claims_survivalAutoNatureRestoration)
				{
					GriefPreventionPlus.instance.restoreClaim(claim, 0);
				}
				
				GriefPreventionPlus.AddLogEntry(" " + claim.getOwnerName() + "'s new player claim expired.");
			}
		}
		
		//if configured to always remove claims after some inactivity period without exceptions...
		else if(GriefPreventionPlus.instance.config_claims_expirationDays > 0)
		{
			if(playerData == null) playerData = GriefPreventionPlus.instance.dataStore.getPlayerData(claim.ownerID);
		    Calendar earliestPermissibleLastLogin = Calendar.getInstance();
			earliestPermissibleLastLogin.add(Calendar.DATE, -GriefPreventionPlus.instance.config_claims_expirationDays);
			
			if(earliestPermissibleLastLogin.getTime().after(playerData.getLastLogin()))
			{
				//make a copy of this player's claim list
				Vector<Claim> claims = new Vector<Claim>();
				for(int i = 0; i < playerData.getClaims().size(); i++)
				{
					claims.add(playerData.getClaims().get(i));
				}
				
				//delete them
				GriefPreventionPlus.instance.dataStore.deleteClaimsForPlayer(claim.ownerID, true);
				GriefPreventionPlus.AddLogEntry(" All of " + claim.getOwnerName() + "'s claims have expired.");
				
				for(int i = 0; i < claims.size(); i++)
				{
					//if configured to do so, restore the land to natural
					if(GriefPreventionPlus.instance.creativeRulesApply(claims.get(i).world) || GriefPreventionPlus.instance.config_claims_survivalAutoNatureRestoration)
					{
						GriefPreventionPlus.instance.restoreClaim(claims.get(i), 0);
						cleanupChunks = true;				
					}
				}
			}
		}
		
		else if(GriefPreventionPlus.instance.config_claims_unusedClaimExpirationDays > 0 && GriefPreventionPlus.instance.creativeRulesApply(claim.world))
		{		
			//avoid scanning large claims and administrative claims
			if(claim.isAdminClaim() || claim.getWidth() > 25 || claim.getHeight() > 25) return;
			
			//otherwise scan the claim content
			int minInvestment = 400;
			
			long investmentScore = claim.getPlayerInvestmentScore();
			cleanupChunks = true;
			
			if(investmentScore < minInvestment)
			{
			    playerData = GriefPreventionPlus.instance.dataStore.getPlayerData(claim.ownerID);
	            
	            //if the owner has been gone at least a week, and if he has ONLY the new player claim, it will be removed
	            Calendar sevenDaysAgo = Calendar.getInstance();
	            sevenDaysAgo.add(Calendar.DATE, -GriefPreventionPlus.instance.config_claims_unusedClaimExpirationDays);
	            boolean claimExpired = sevenDaysAgo.getTime().after(playerData.getLastLogin());
	            if(claimExpired)
	            {
    			    GriefPreventionPlus.instance.dataStore.deleteClaim(claim, true);
    				GriefPreventionPlus.AddLogEntry("Removed " + claim.getOwnerName() + "'s unused claim @ " + GriefPreventionPlus.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
    				
    				//restore the claim area to natural state
    				GriefPreventionPlus.instance.restoreClaim(claim, 0);
	            }
			}
		}
		
		if(playerData != null) GriefPreventionPlus.instance.dataStore.clearCachedPlayerData(claim.ownerID);
		
		//since we're potentially loading a lot of chunks to scan parts of the world where there are no players currently playing, be mindful of memory usage
		if(cleanupChunks)
		{
			World world = claim.getLesserBoundaryCorner().getWorld();
			Chunk lesserChunk = world.getChunkAt(claim.getLesserBoundaryCorner());
			Chunk greaterChunk = world.getChunkAt(claim.getGreaterBoundaryCorner());
			for(int x = lesserChunk.getX(); x <= greaterChunk.getX(); x++)
			{
			    for(int z = lesserChunk.getZ(); z <= greaterChunk.getZ(); z++)
			    {
			        Chunk chunk = world.getChunkAt(x, z);
			        if(chunk.isLoaded())
			        {
			            chunk.unload(true, true);
			        }
			    }
			}
		}
	}
}
