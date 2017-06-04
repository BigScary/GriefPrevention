/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2011 Ryan Hamshire

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

package me.ryanhamshire.GriefPrevention.player;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import me.ryanhamshire.GriefPrevention.CustomLogEntryTypes;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.ShovelMode;
import me.ryanhamshire.GriefPrevention.Visualization;
import me.ryanhamshire.GriefPrevention.claim.Claim;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

//holds all of GriefPrevention's player-tied data
public class PlayerData 
{
	//the player's ID
	private UUID playerID;
	
	//the player's claims
	private Vector<Claim> claims = null;
	
	//how many claim blocks the player has earned via play time
	private Integer accruedClaimBlocks = null;
	
	//temporary holding area to avoid opening data files too early
	private int newlyAccruedClaimBlocks = 0;
	
	//where this player was the last time we checked on him for earning claim blocks
    private Location lastAfkCheckLocation = null;
	
	//how many claim blocks the player has been gifted/purchased
	private Integer bonusClaimBlocks = null;
	
	//what "mode" the shovel is in determines what it will do when it's used
    private ShovelMode shovelMode = ShovelMode.Basic;
	
	//last place the player used the shovel, useful in creating and resizing claims, 
	//because the player must use the shovel twice in those instances
    private Location lastShovelLocation = null;
	
	//the claim this player is currently resizing
    private Claim claimResizing = null;
	
	//the claim this player is currently subdividing
    private Claim claimSubdividing = null;
	
	//whether or not the player has a pending /trapped rescue
    private boolean pendingTrapped = false;
	
	//whether this player was recently warned about building outside land claims
	boolean warnedAboutBuildingOutsideClaims = false;
    
	//visualization
    private Visualization currentVisualization = null;
	
	//the last claim this player was in, that we know of
    private Claim lastClaim = null;

    private InetAddress ipAddress;

    //for addons to set per-player claim limits. Any negative value will use config's value
    private int AccruedClaimBlocksLimit = -1;

    //player which a pet will be given to when it's right-clicked
	OfflinePlayer petGiveawayRecipient = null;
	
	//timestamp for last "you're building outside your land claims" message
	Long buildWarningTimestamp = null;

	public boolean ignoreListChanged = false;
	
	//the number of claim blocks a player has available for claiming land
	public int getRemainingClaimBlocks()
	{
		int remainingBlocks = this.getAccruedClaimBlocks() + this.getBonusClaimBlocks();
		for(int i = 0; i < this.getClaims().size(); i++)
		{
			Claim claim = this.getClaims().get(i);
			remainingBlocks -= claim.getArea();
		}
		
		//add any blocks this player might have based on group membership (permissions)
		remainingBlocks += GriefPrevention.instance.dataStore.getGroupBonusBlocks(this.playerID);
		
		return remainingBlocks;
	}
	
	//don't load data from secondary storage until it's needed
	public int getAccruedClaimBlocks()
	{
	    if(this.accruedClaimBlocks == null) this.loadDataFromSecondaryStorage();
        
	    //update claim blocks with any he has accrued during his current play session
	    if(this.newlyAccruedClaimBlocks > 0)
	    {
	        int accruedLimit = this.getAccruedClaimBlocksLimit();
	        
	        //if over the limit before adding blocks, leave it as-is, because the limit may have changed AFTER he accrued the blocks
	        if(this.accruedClaimBlocks < accruedLimit)
	        {
	            //move any in the holding area
	            int newTotal = this.accruedClaimBlocks + this.newlyAccruedClaimBlocks;
	            
	            //respect limits
	            this.accruedClaimBlocks = Math.min(newTotal, accruedLimit);
	        }
	        
	        this.newlyAccruedClaimBlocks = 0;
	        return this.accruedClaimBlocks;
	    }
	    
	    return accruedClaimBlocks;
    }

    public void setAccruedClaimBlocks(Integer accruedClaimBlocks)
    {
        this.accruedClaimBlocks = accruedClaimBlocks;
        this.newlyAccruedClaimBlocks = 0;
    }

    public int getBonusClaimBlocks()
    {
        if(this.bonusClaimBlocks == null) this.loadDataFromSecondaryStorage();
        return bonusClaimBlocks;
    }

    public void setBonusClaimBlocks(Integer bonusClaimBlocks)
    {
        this.bonusClaimBlocks = bonusClaimBlocks;
    }
    

    
    public Vector<Claim> getClaims()
    {
        if(this.claims == null)
        {
            this.claims = new Vector<Claim>();
            
            //find all the claims belonging to this player and note them for future reference
            DataStore dataStore = GriefPrevention.instance.dataStore;
            int totalClaimsArea = 0;
            for(int i = 0; i < dataStore.claims.size(); i++)
            {
                Claim claim = dataStore.claims.get(i);
                if(!claim.inDataStore)
                {
                    dataStore.claims.remove(i--);
                    continue;
                }
                if(playerID.equals(claim.ownerID))
                {
                    this.claims.add(claim);
                    totalClaimsArea += claim.getArea();
                }
            }
            
            //ensure player has claim blocks for his claims, and at least the minimum accrued
            this.loadDataFromSecondaryStorage();
            
            //if total claimed area is more than total blocks available
            int totalBlocks = this.accruedClaimBlocks + this.getBonusClaimBlocks() + GriefPrevention.instance.dataStore.getGroupBonusBlocks(this.playerID);
            if(totalBlocks < totalClaimsArea)
            {
                OfflinePlayer player = GriefPrevention.instance.getServer().getOfflinePlayer(this.playerID);
                GriefPrevention.AddLogEntry(player.getName() + " has more claimed land than blocks available.  Adding blocks to fix.", CustomLogEntryTypes.Debug, true);
                GriefPrevention.AddLogEntry("Total blocks: " + totalBlocks + " Total claimed area: " + totalClaimsArea, CustomLogEntryTypes.Debug, true);
                for(Claim claim : this.claims)
                {
                    if(!claim.inDataStore) continue;
                    GriefPrevention.AddLogEntry(
                            GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " // "
                            + GriefPrevention.getfriendlyLocationString(claim.getGreaterBoundaryCorner()) + " = "
                            + claim.getArea()
                            , CustomLogEntryTypes.Debug, true);
                }
                
                //try to fix it by adding to accrued blocks
                this.accruedClaimBlocks = totalClaimsArea;
                int accruedLimit = this.getAccruedClaimBlocksLimit();
                this.accruedClaimBlocks = Math.min(accruedLimit, this.accruedClaimBlocks);
                
                //if that didn't fix it, then make up the difference with bonus blocks
                totalBlocks = this.accruedClaimBlocks + this.getBonusClaimBlocks() + GriefPrevention.instance.dataStore.getGroupBonusBlocks(this.playerID);
                if(totalBlocks < totalClaimsArea)
                {
                    this.bonusClaimBlocks += totalClaimsArea - totalBlocks;
                }
            }
        }
        
        for(int i = 0; i < this.claims.size(); i++)
        {
            if(!claims.get(i).inDataStore)
            {
                claims.remove(i--);
            }
        }
        
        return claims;
    }
    
    //Limit can be changed by addons
    public int getAccruedClaimBlocksLimit()
    {
        if (this.AccruedClaimBlocksLimit < 0)
            return GriefPrevention.instance.config_claims_maxAccruedBlocks_default;
        return this.AccruedClaimBlocksLimit;
    }

    public void setAccruedClaimBlocksLimit(int limit)
    {
        this.AccruedClaimBlocksLimit = limit;
    }

    public void accrueBlocks(int howMany)
    {
        this.newlyAccruedClaimBlocks += howMany;
    }
}