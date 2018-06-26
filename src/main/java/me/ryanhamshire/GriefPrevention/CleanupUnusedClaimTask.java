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

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import me.ryanhamshire.GriefPrevention.events.ClaimExpirationEvent;

class CleanupUnusedClaimTask implements Runnable 
{	
    Claim claim;
    PlayerData ownerData;
    OfflinePlayer ownerInfo;
	
	CleanupUnusedClaimTask(Claim claim, PlayerData ownerData, OfflinePlayer ownerInfo)
	{
		this.claim = claim;
		this.ownerData = ownerData;
		this.ownerInfo = ownerInfo;
	}
	
	@Override
	public void run()
	{
	    //see if any other plugins don't want this claim deleted
	    ClaimExpirationEvent event = new ClaimExpirationEvent(this.claim);
	    Bukkit.getPluginManager().callEvent(event);
	    if(event.isCancelled()) return;
	    
	    //determine area of the default chest claim
		int areaOfDefaultClaim = 0;
		if(GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius >= 0)
		{
			areaOfDefaultClaim = (int)Math.pow(GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius * 2 + 1, 2);  
		}
		
		//if this claim is a chest claim and those are set to expire
        if(claim.getArea() <= areaOfDefaultClaim && GriefPrevention.instance.config_claims_chestClaimExpirationDays > 0)
		{
            //if the owner has been gone at least a week, and if he has ONLY the new player claim, it will be removed
	        Calendar sevenDaysAgo = Calendar.getInstance();
	        sevenDaysAgo.add(Calendar.DATE, -GriefPrevention.instance.config_claims_chestClaimExpirationDays);
	        boolean newPlayerClaimsExpired = sevenDaysAgo.getTime().after(new Date(ownerInfo.getLastPlayed()));
			if(newPlayerClaimsExpired && ownerData.getClaims().size() == 1)
			{
				claim.removeSurfaceFluids(null);
				GriefPrevention.instance.dataStore.deleteClaim(claim, true, true);
				
				//if configured to do so, restore the land to natural
				if(GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()) || GriefPrevention.instance.config_claims_survivalAutoNatureRestoration)
				{
					GriefPrevention.instance.restoreClaim(claim, 0);
				}
				
				GriefPrevention.AddLogEntry(" " + claim.getOwnerName() + "'s new player claim expired.", CustomLogEntryTypes.AdminActivity);
			}
		}
		
		//if configured to always remove claims after some inactivity period without exceptions...
		else if(GriefPrevention.instance.config_claims_expirationDays > 0)
		{
			Calendar earliestPermissibleLastLogin = Calendar.getInstance();
			earliestPermissibleLastLogin.add(Calendar.DATE, -GriefPrevention.instance.config_claims_expirationDays);
			
			if(earliestPermissibleLastLogin.getTime().after(new Date(ownerInfo.getLastPlayed())))
			{
				//make a copy of this player's claim list
				Vector<Claim> claims = new Vector<Claim>();
				for(int i = 0; i < ownerData.getClaims().size(); i++)
				{
					claims.add(ownerData.getClaims().get(i));
				}
				
				//delete them
				GriefPrevention.instance.dataStore.deleteClaimsForPlayer(claim.ownerID, true);
				GriefPrevention.AddLogEntry(" All of " + claim.getOwnerName() + "'s claims have expired.", CustomLogEntryTypes.AdminActivity);
				
				for(int i = 0; i < claims.size(); i++)
				{
					//if configured to do so, restore the land to natural
					if(GriefPrevention.instance.creativeRulesApply(claims.get(i).getLesserBoundaryCorner()) || GriefPrevention.instance.config_claims_survivalAutoNatureRestoration)
					{
						GriefPrevention.instance.restoreClaim(claims.get(i), 0);
					}
				}
			}
		}
		
		else if(GriefPrevention.instance.config_claims_unusedClaimExpirationDays > 0 && GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()))
		{		
			//avoid scanning large claims and administrative claims
			if(claim.isAdminClaim() || claim.getWidth() > 25 || claim.getHeight() > 25) return;
			
			//otherwise scan the claim content
			int minInvestment = 400;
			
			long investmentScore = claim.getPlayerInvestmentScore();
			
			if(investmentScore < minInvestment)
			{
			    //if the owner has been gone at least a week, and if he has ONLY the new player claim, it will be removed
	            Calendar sevenDaysAgo = Calendar.getInstance();
	            sevenDaysAgo.add(Calendar.DATE, -GriefPrevention.instance.config_claims_unusedClaimExpirationDays);
	            boolean claimExpired = sevenDaysAgo.getTime().after(new Date(ownerInfo.getLastPlayed()));
	            if(claimExpired)
	            {
    			    GriefPrevention.instance.dataStore.deleteClaim(claim, true, true);
    				GriefPrevention.AddLogEntry("Removed " + claim.getOwnerName() + "'s unused claim @ " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()), CustomLogEntryTypes.AdminActivity);
    				
    				//restore the claim area to natural state
    				GriefPrevention.instance.restoreClaim(claim, 0);
	            }
			}
		}
	}
}
