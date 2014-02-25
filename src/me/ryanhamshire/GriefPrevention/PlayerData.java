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

package me.ryanhamshire.GriefPrevention;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

//holds all of GriefPrevention's player-tied data
public class PlayerData {
	// we need to track when players step on pressure plates, since we only want
	// it to occur the first time it get's stepped on.
	public class PressurePlateData {
		Date LastStepTime;
		Location PlateLocation;

		public PressurePlateData(Location pPlateLocation) {
			PlateLocation = PlateLocation;
			LastStepTime = new Date();
		}

		public Date getLastStepTime() {
			return LastStepTime;
		}

		public Location getPlateLocation() {
			return PlateLocation;
		}

		public void setLastStepTime(Date value) {
			LastStepTime = value;
		}
	}

	// how many claim blocks the player has earned via play time
	public int accruedClaimBlocks = GriefPrevention.instance.config_claims_initialBlocks;
    public boolean ClearInventoryOnJoin= false;

	// visualization
	public List<Visualization> ActiveVisualizations = new ArrayList<Visualization>();

	// how many claim blocks the player has been gifted by admins, or purchased
	// via economy integration
	public int bonusClaimBlocks = 0;

	// accessor for PressurePlateData. the event occurs as long as the player is
	// standing on the pressure plate,
	// so we want it to only allow showing messages the "first time"

	// the claim this player is currently resizing
	public Claim claimResizing = null;

	// the player's claims
	public Vector<Claim> claims = new Vector<Claim>();

	// the claim this player is currently subdividing
	public Claim claimSubdividing = null;

    public static List<String> SoftMutedPlayers = new ArrayList<String>();


    public boolean getSoftMute(){
       return SoftMutedPlayers.contains(playerName);
    }
    public boolean setSoftMute(boolean value){
    if(getSoftMute()==value) return value; //no change.
    else if(getSoftMute()){
        SoftMutedPlayers.remove(playerName);

    }
    else{
        SoftMutedPlayers.add(playerName);
    }
    return value;
    }
    public boolean ToggleSoftMute(){
        return setSoftMute(!getSoftMute());
    }
    //track players on this Players "ignore list"
    private List<String> IgnoreList = new ArrayList<String>();

    public boolean isIgnored(Player p){
        for(String iterate:IgnoreList){
            if(iterate.equalsIgnoreCase(p.getName())) return true;
        }
        return false;
    }
    public void ignorePlayer(Player p){
        if(!isIgnored(p)) IgnoreList.add(p.getName());
    }
    public void unignorePlayer(Player p){
        if(isIgnored(p)) IgnoreList.remove(p.getName());
    }
    public List<String> getIgnoreList(){
        return IgnoreList;
    }
    /**
     * toggles the ignore value for a player by adding, or removing that player from this players ignore list.
     * @param p
     * @return
     */
    public boolean ToggleIgnored(Player p){
        if(isIgnored(p)) unignorePlayer(p); else ignorePlayer(p);
        return isIgnored(p);
    }
	// radius for restore nature fill mode
	public int fillRadius = 0;

	public boolean IgnoreClaimMessage = false; // using "claim" in a chat will
												// usually send a message
	// to that player about claiming. This is used to implement a cooldown-
	// after a configurable period the player will not
	// receive another message about Claims as a result of using the trigger
	// words.

	// ignore claims mode
	public boolean ignoreClaims = false;

	public boolean IgnoreStuckMessage = false; // save semantics as above, but
												// for Stuck Messages.
    public boolean IgnoreIgnoreMessage = false;
	public InetAddress ipAddress;

	// where this player was the last time we checked on him for earning claim
	// blocks
	public Location lastAfkCheckLocation = null;

	// last place the player damaged a chest
	public Location lastChestDamageLocation = null;

	// the last claim this player was in, that we know of
	public Claim lastClaim = null;

	// timestamp of last death, for use in preventing death message spam
	long lastDeathTimeStamp = 0;

	// spam
	public Date lastLogin; // when the player last logged into the server

	public String lastMessage = ""; // the player's last chat message, or slash

	public Date lastMessageTimestamp = new Date(); // last time the player sent

	public String lastPvpPlayer = "";

	// pvp
	public long lastPvpTimestamp = 0;

	// last place the player used the shovel, useful in creating and resizing
	// claims,
	// because the player must use the shovel twice in those instances
	public Location lastShovelLocation = null;

	public long lastSpawn = 0;

	// the timestamp for the last time the player used /trapped
	public Date lastTrappedUsage;
	// whether or not the player has a pending /trapped rescue
	public boolean pendingTrapped = false;
	public Map<String, PressurePlateData> PlateData = new HashMap<String, PressurePlateData>();
	// the player's name
	public String playerName;
	// anti-camping pvp protection
	public boolean pvpImmune = false;

	// what "mode" the shovel is in determines what it will do when it's used
	public ShovelMode shovelMode = ShovelMode.Basic;

	// siege
	public SiegeData siegeData = null;
	// a chat message or used a
	// monitored slash command
	public int spamCount = 0; // number of consecutive "spams"

	public boolean spamWarned = false; // whether the player recently received a
										// warning

	public int Confirmtimeoutseconds = 20;
	// number of blocks placed outside claims before next warning
	int unclaimedBlockPlacementsUntilWarning = 1;

	//
	private HashMap<String,Boolean> ConfirmationWarnings = new HashMap<String,Boolean>();
	public boolean getWarned(String pName){
		return ConfirmationWarnings.containsKey(pName);
	}
	public void setWarned(final String pName){
		setWarned(pName,true);
	}
	public void setWarned(final String pName,final boolean pValue){
		if(!pValue) ConfirmationWarnings.remove(pName);
		if(pValue){
			ConfirmationWarnings.put(pName, pValue);
			//setup a reset delegate.
			Bukkit.getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, new Runnable(){
				public void run(){
					if(!ConfirmationWarnings.containsKey(pName)) return;
					ConfirmationWarnings.remove(pName);

					Player p = Bukkit.getPlayer(playerName);
					if(p!=null){
						GriefPrevention.sendMessage(p,TextMode.Info, Messages.ConfirmationReset,pName);
					}
					
				}
				
				
			}
			,Confirmtimeoutseconds*20);
		}
	}
	
	// safety confirmation for deleting multi-subdivision claims
	//public boolean warnedAboutMajorDeletion = false;

	PlayerData() {
		// default last login date value to a year ago to ensure a brand new
		// player can log in
		// see login cooldown feature, PlayerEventHandler.onPlayerLogin()
		// if the player successfully logs in, this value will be overwritten
		// with the current date and time
		Calendar lastYear = Calendar.getInstance();
		lastYear.add(Calendar.YEAR, -1);
		this.lastLogin = lastYear.getTime();
		this.lastTrappedUsage = lastYear.getTime();
	}

	public Date getLastSteppedOn(Location pLocation) {

		if (PlateData.containsKey(pLocation.toString())) {
			return PlateData.get(pLocation.toString()).getLastStepTime();
		} else {
			Date yearago = new Date();
			yearago = new Date(yearago.getTime() - (1000 * 4400));
			return yearago;
		}
	}

	// the number of claim blocks a player has available for claiming land
	public int getRemainingClaimBlocks() {
		int remainingBlocks = this.accruedClaimBlocks + this.bonusClaimBlocks;
		for (int i = 0; i < this.claims.size(); i++) {
			Claim claim = this.claims.get(i);
			remainingBlocks -= claim.getArea();
		}
		// add any blocks this player might have based on group membership
		// (permissions)
		remainingBlocks += GriefPrevention.instance.dataStore.getGroupBonusBlocks(this.playerName);

		return remainingBlocks;
	}

	public int getRemainingClaimBlocks(World p) {

		WorldConfig wc = GriefPrevention.instance.getWorldCfg(p);
		if (wc.getClaims_maxBlocks() == 0)
			return Integer.MAX_VALUE; // easy break...
		if (wc == null)
			return 0;
		// get the total claim blocks this player has in this world.
		int WorldClaimBlocks = this.getTotalClaimBlocksinWorld(p);
		// return the maximum sans what they have.
		return wc.getClaims_maxBlocks() - WorldClaimBlocks;

	}

	public int getTotalClaimBlocksinWorld(World p) {
		int accum = 0;
		for (Claim iterate : getWorldClaims(p)) {
			accum += iterate.getArea();
		}
		return accum;
	}

	public Vector<Claim> getWorldClaims(World p) {
		Vector<Claim> makeresult = new Vector<Claim>();
		for (Claim cc : claims) {
			if (cc.getLesserBoundaryCorner().getWorld().equals(p)) {
				makeresult.add(cc);
			}
		}
		return makeresult;

	}

	// whether or not this player is "in" pvp combat
	public boolean inPvpCombat() {
		if (this.lastPvpTimestamp == 0)
			return false;

		WorldConfig wc = wc();
		long now = Calendar.getInstance().getTimeInMillis();

		long elapsed = now - this.lastPvpTimestamp;

		if (elapsed > wc.getPvPCombatTimeoutSeconds() * 1000) // X seconds
		{
			this.lastPvpTimestamp = 0;
			return false;
		}

		return true;
	}

	public void setLastSteppedOn(Location pLocation) {
		PressurePlateData newvalue = new PressurePlateData(pLocation);
		PlateData.put(pLocation.toString(), newvalue);

	}

	private WorldConfig wc() {
		Player p = Bukkit.getPlayer(playerName);
		return GriefPrevention.instance.getWorldCfg(p.getWorld());
	}
}