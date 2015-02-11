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
import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.Vector;

import net.kaikk.mc.gpp.Claim;
import net.kaikk.mc.gpp.GriefPreventionPlus;
import net.kaikk.mc.gpp.ShovelMode;
import net.kaikk.mc.gpp.SiegeData;
import net.kaikk.mc.gpp.Visualization;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

//holds all of GriefPrevention's player-tied data
public class PlayerData 
{
	//the player's ID
	public UUID playerID;
	
	//the player's claims
	private Vector<Claim> claims = null;
	
	//how many claim blocks the player has earned via play time
	private Integer accruedClaimBlocks = null;
	
	//temporary holding area to avoid opening data files too early
	private int newlyAccruedClaimBlocks = 0;
	
	//where this player was the last time we checked on him for earning claim blocks
	public Location lastAfkCheckLocation = null;
	
	//how many claim blocks the player has been gifted by admins, or purchased via economy integration 
	private Integer bonusClaimBlocks = null;
	
	//what "mode" the shovel is in determines what it will do when it's used
	public ShovelMode shovelMode = ShovelMode.Basic;
	
	//radius for restore nature fill mode
	int fillRadius = 0;
	
	//last place the player used the shovel, useful in creating and resizing claims, 
	//because the player must use the shovel twice in those instances
	public Location lastShovelLocation = null;	
	
	//the claim this player is currently resizing
	public Claim claimResizing = null;
	
	//the claim this player is currently subdividing
	public Claim claimSubdividing = null;
	
	//whether or not the player has a pending /trapped rescue
	public boolean pendingTrapped = false;
	
	//whether this player was recently warned about building outside land claims
	boolean warnedAboutBuildingOutsideClaims = false;
	
	//timestamp of last death, for use in preventing death message spam
	long lastDeathTimeStamp = 0;
	
	//whether the player was kicked (set and used during logout)
	boolean wasKicked = false;
	
	//spam
    public String lastMessage = "";					//the player's last chat message, or slash command complete with parameters 
	public Date lastMessageTimestamp = new Date();  //last time the player sent a chat message or used a monitored slash command
	public int spamCount = 0;						//number of consecutive "spams"
	public boolean spamWarned = false;				//whether the player recently received a warning
	
	//visualization
	public Visualization currentVisualization = null;
	
	//anti-camping pvp protection
	public boolean pvpImmune = false;
	public long lastSpawn = 0;
	
	//ignore claims mode
	public boolean ignoreClaims = false;
	
	//the last claim this player was in, that we know of
	public Claim lastClaim = null;
	
	//siege
	public SiegeData siegeData = null;
	
	//pvp
	public long lastPvpTimestamp = 0;
	public String lastPvpPlayer = "";
	
	//safety confirmation for deleting multi-subdivision claims
	public boolean warnedAboutMajorDeletion = false;

	public InetAddress ipAddress;

    //whether or not this player has received a message about unlocking death drops since his last death
	boolean receivedDropUnlockAdvertisement = false;

    //whether or not this player's dropped items (on death) are unlocked for other players to pick up
	boolean dropsAreUnlocked = false;

    //message to send to player after he respawns
	String messageOnRespawn = null;

    //player which a pet will be given to when it's right-clicked
	OfflinePlayer petGiveawayRecipient = null;
	
	//timestamp for last "you're building outside your land claims" message
	Long buildWarningTimestamp = null;
	
	//spot where a player can't talk, used to mute new players until they've moved a little
	//this is an anti-bot strategy.
	Location noChatLocation = null;
	
	PlayerData(UUID playerID) {
		this.playerID=playerID;
	}
	
	PlayerData(UUID playerID, Integer accruedClaimBlocks, Integer bonusClaimBlocks) {
		this.playerID=playerID;
		this.accruedClaimBlocks=accruedClaimBlocks;
		this.bonusClaimBlocks=bonusClaimBlocks;
	}
	
	//whether or not this player is "in" pvp combat
	public boolean inPvpCombat()
	{
		if(this.lastPvpTimestamp == 0) return false;
		
		long now = Calendar.getInstance().getTimeInMillis();
		
		long elapsed = now - this.lastPvpTimestamp;
		
		if(elapsed > GriefPreventionPlus.instance.config_pvp_combatTimeoutSeconds * 1000) //X seconds
		{
			this.lastPvpTimestamp = 0;
			return false;
		}
		
		return true;
	}
	
	//the number of claim blocks a player has available for claiming land
	public int getRemainingClaimBlocks()
	{
		// accrued blocks + bonus blocks + permission bonus blocks
		int remainingBlocks = this.getAccruedClaimBlocks() + this.getBonusClaimBlocks() + GriefPreventionPlus.instance.dataStore.getGroupBonusBlocks(this.playerID);
		for (Claim claim : this.getClaims()) {
			remainingBlocks-=claim.getArea();
		}
		
		return remainingBlocks;
	}
	
	//don't load data from secondary storage until it's needed
	public int getAccruedClaimBlocks()
	{
	    if(this.accruedClaimBlocks == null) this.loadDataFromSecondaryStorage();
        
	    //move any in the holding area
	    int newTotal = this.accruedClaimBlocks + this.newlyAccruedClaimBlocks;
	    this.newlyAccruedClaimBlocks = 0;
        
        //respect limits
        if(newTotal > GriefPreventionPlus.instance.config_claims_maxAccruedBlocks) newTotal = GriefPreventionPlus.instance.config_claims_maxAccruedBlocks;
	    this.accruedClaimBlocks = newTotal;
        
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
    
    public Date getLastLogin() {
    	return new Date(GriefPreventionPlus.instance.getServer().getOfflinePlayer(this.playerID).getLastPlayed());
    }
    
    private void loadDataFromSecondaryStorage()
    {
        //reach out to secondary storage to get any data there
        PlayerData storageData = GriefPreventionPlus.instance.dataStore.getPlayerDataFromStorage(this.playerID);
        
        if (storageData==null) {
        	// initialize new player data
        	storageData = new PlayerData(playerID);

			//shove that new player data into the hash map cache
			GriefPreventionPlus.instance.dataStore.playerNameToPlayerDataMap.put(playerID, storageData);
        }
        
        if(this.accruedClaimBlocks == null)
        {
            if(storageData.accruedClaimBlocks != null) {
                this.accruedClaimBlocks = storageData.accruedClaimBlocks;
            } else {
                this.accruedClaimBlocks = GriefPreventionPlus.instance.config_claims_initialBlocks;
            }
        }
        
        if(this.bonusClaimBlocks == null)
        {
            if(storageData.bonusClaimBlocks != null) {
                this.bonusClaimBlocks = storageData.bonusClaimBlocks;
            } else {
                this.bonusClaimBlocks = 0;
            }
        }
    }
    
    public Vector<Claim> getClaims()
    {
        if(this.claims == null)
        {
            this.claims = new Vector<Claim>();
            
            //find all the claims belonging to this player and note them for future reference
            for(Claim claim : GriefPreventionPlus.instance.dataStore.claims.values()) {
                if(playerID.equals(claim.ownerID)) {
                    this.claims.add(claim);
                }
            }
        }
        
        return this.claims;
    }
    
    public void accrueBlocks(int howMany)
    {
        this.newlyAccruedClaimBlocks += howMany;
    }
}