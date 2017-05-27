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

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import me.ryanhamshire.GriefPrevention.claim.Claim;

import me.ryanhamshire.GriefPrevention.events.DeniedMessageEvent;
import me.ryanhamshire.GriefPrevention.player.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Player;

//singleton class which manages all GriefPrevention data (except for config options)
public abstract class DataStore 
{
	private GriefPrevention instance;


	
	//pattern for unique user identifiers (UUIDs)
	protected final static Pattern uuidpattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
	
	//path information, for where stuff stored on disk is well...  stored
	protected final static String dataLayerFolderPath = "plugins" + File.separator + "GriefPreventionData";
	final static String playerDataFolderPath = dataLayerFolderPath + File.separator + "PlayerData";
    final static String configFilePath = dataLayerFolderPath + File.separator + "config.yml";

    //the latest version of the data schema implemented here
	protected static final int latestSchemaVersion = 2;
	
	//reading and writing the schema version to the data store
	abstract int getSchemaVersionFromStorage();
    abstract void updateSchemaVersionInStorage(int versionToSet);
	
	//current version of the schema of data in secondary storage
    private int currentSchemaVersion = -1;  //-1 means not determined yet

    protected int getSchemaVersion()
    {
        if(this.currentSchemaVersion >= 0)
        {
            return this.currentSchemaVersion;
        }
        else
        {
            this.currentSchemaVersion = this.getSchemaVersionFromStorage(); 
            return this.currentSchemaVersion;
        }
    }
	
    protected void setSchemaVersion(int versionToSet)
    {
        this.currentSchemaVersion = versionToSet;
        this.updateSchemaVersionInStorage(versionToSet);
    }
	
	//initialization!
	void initialize() throws Exception
	{
		GriefPrevention.AddLogEntry(this.claims.size() + " total claims loaded.");
		
		//ensure data folders exist
        File playerDataFolder = new File(playerDataFolderPath);
        if(!playerDataFolder.exists())
        {
            playerDataFolder.mkdirs();
        }
		
		//load up all the messages from messages.yml
		this.loadMessages();
		GriefPrevention.AddLogEntry("Customizable messages loaded.");
		
		//if converting up from an earlier schema version, write all claims back to storage using the latest format
        if(this.getSchemaVersion() < latestSchemaVersion)
        {
            GriefPrevention.AddLogEntry("Please wait.  Updating data format.");
            
            for(Claim claim : this.claims)
            {
                this.saveClaim(claim);
            }
            
            //clean up any UUID conversion work
            if(UUIDFetcher.lookupCache != null)
            {
                UUIDFetcher.lookupCache.clear();
                UUIDFetcher.correctedNames.clear();
            }
            
            GriefPrevention.AddLogEntry("Update finished.");
        }
        
        //make a note of the data store schema version
		this.setSchemaVersion(latestSchemaVersion);
	}


	
	abstract void writeClaimToStorage(Claim claim);
	
	//increments the claim ID and updates secondary storage to be sure it's saved
	//As far as I know, 2 billion claim IDs _should_ be enough
	abstract void setNextClaimID(int ID);

	//Used on startup
	abstract void getNextClaimID();
	
	abstract PlayerData getPlayerDataFromStorage(UUID playerID);
	
	abstract void deleteClaimFromSecondaryStorage(Claim claim);
	
	//saves changes to player data to secondary storage.  MUST be called after you're done making changes, otherwise a reload will lose them
    public void savePlayerDataSync(UUID playerID, PlayerData playerData)
    {
        //ensure player data is already read from file before trying to save
        playerData.getAccruedClaimBlocks();
        playerData.getClaims();
        
        this.asyncSavePlayerData(playerID, playerData);
    }
	
	//saves changes to player data to secondary storage.  MUST be called after you're done making changes, otherwise a reload will lose them
	public void savePlayerData(UUID playerID, PlayerData playerData)
	{
	    new SavePlayerDataThread(playerID, playerData).start();
	}
	
	public void asyncSavePlayerData(UUID playerID, PlayerData playerData)
	{
	    //save everything except the ignore list
	    this.overrideSavePlayerData(playerID, playerData);
	}
	
	abstract void overrideSavePlayerData(UUID playerID, PlayerData playerData);


	



	
	private class SavePlayerDataThread extends Thread
	{
	    private UUID playerID;
	    private PlayerData playerData;
	    
	    SavePlayerDataThread(UUID playerID, PlayerData playerData)
	    {
	        this.playerID = playerID;
	        this.playerData = playerData;
	    }
	    
	    public void run()
	    {
	        //ensure player data is already read from file before trying to save
	        playerData.getAccruedClaimBlocks();
	        playerData.getClaims();
	        asyncSavePlayerData(this.playerID, this.playerData);
	    }
	}
}
