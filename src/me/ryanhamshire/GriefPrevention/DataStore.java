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
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;

import me.ryanhamshire.GriefPrevention.events.DeniedMessageEvent;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.google.common.io.Files;

//singleton class which manages all GriefPrevention data (except for config options)
public abstract class DataStore 
{
	private GriefPrevention instance;

	//in-memory cache for player data
	protected ConcurrentHashMap<UUID, PlayerData> playerNameToPlayerDataMap = new ConcurrentHashMap<UUID, PlayerData>();
	
	//in-memory cache for group (permission-based) data
	protected ConcurrentHashMap<String, Integer> permissionToBonusBlocksMap = new ConcurrentHashMap<String, Integer>();
	
	//in-memory cache for claim data
	ArrayList<Claim> claims = new ArrayList<Claim>();
	ConcurrentHashMap<Long, ArrayList<Claim>> chunksToClaimsMap = new ConcurrentHashMap<Long, ArrayList<Claim>>();
	
	//in-memory cache for messages
	private String [] messages;
	
	//pattern for unique user identifiers (UUIDs)
	protected final static Pattern uuidpattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
	
	//next claim ID
	Long nextClaimID = (long)0;
	
	//path information, for where stuff stored on disk is well...  stored
	protected final static String dataLayerFolderPath = "plugins" + File.separator + "GriefPreventionData";
	final static String playerDataFolderPath = dataLayerFolderPath + File.separator + "PlayerData";
    final static String configFilePath = dataLayerFolderPath + File.separator + "config.yml";
	final static String messagesFilePath = dataLayerFolderPath + File.separator + "messages.yml";
	final static String bannedWordsFilePath = dataLayerFolderPath + File.separator + "bannedWords.txt";

    //the latest version of the data schema implemented here
	protected static final int latestSchemaVersion = 2;
	
	//reading and writing the schema version to the data store
	abstract int getSchemaVersionFromStorage();
    abstract void updateSchemaVersionInStorage(int versionToSet);
	
	//current version of the schema of data in secondary storage
    private int currentSchemaVersion = -1;  //-1 means not determined yet
    
    //video links
    static final String SURVIVAL_VIDEO_URL = "" + ChatColor.DARK_AQUA + ChatColor.UNDERLINE + "bit.ly/mcgpuser" + ChatColor.RESET;
    static final String CREATIVE_VIDEO_URL = "" + ChatColor.DARK_AQUA + ChatColor.UNDERLINE + "bit.ly/mcgpcrea" + ChatColor.RESET;
    static final String SUBDIVISION_VIDEO_URL = "" + ChatColor.DARK_AQUA + ChatColor.UNDERLINE + "bit.ly/mcgpsub" + ChatColor.RESET;
    
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
	
    //removes cached player data from memory
	synchronized void clearCachedPlayerData(UUID playerID)
	{
		this.playerNameToPlayerDataMap.remove(playerID);
	}
	
	//gets the number of bonus blocks a player has from his permissions
	//Bukkit doesn't allow for checking permissions of an offline player.
	//this will return 0 when he's offline, and the correct number when online.
	synchronized public int getGroupBonusBlocks(UUID playerID)
	{
		int bonusBlocks = 0;
		Set<String> keys = permissionToBonusBlocksMap.keySet();
		Iterator<String> iterator = keys.iterator();
		while(iterator.hasNext())
		{
			String groupName = iterator.next();
			Player player = GriefPrevention.instance.getServer().getPlayer(playerID);
			if(player != null && player.hasPermission(groupName))
			{
				bonusBlocks += this.permissionToBonusBlocksMap.get(groupName);
			}
		}
		
		return bonusBlocks;
	}
	
	//grants a group (players with a specific permission) bonus claim blocks as long as they're still members of the group
	synchronized public int adjustGroupBonusBlocks(String groupName, int amount)
	{
		Integer currentValue = this.permissionToBonusBlocksMap.get(groupName);
		if(currentValue == null) currentValue = 0;
		
		currentValue += amount;
		this.permissionToBonusBlocksMap.put(groupName, currentValue);
		
		//write changes to storage to ensure they don't get lost
		this.saveGroupBonusBlocks(groupName, currentValue);
		
		return currentValue;		
	}
	
	abstract void saveGroupBonusBlocks(String groupName, int amount);
	
	class NoTransferException extends Exception
	{
        private static final long serialVersionUID = 1L;

        NoTransferException(String message)
	    {
	        super(message);
	    }
	}

	//saves any changes to a claim to secondary storage
	synchronized public void saveClaim(Claim claim)
	{
		//ensure a unique identifier for the claim which will be used to name the file on disk
		if(claim.id == null || claim.id == -1)
		{
			claim.id = this.nextClaimID;
			this.incrementNextClaimID();
		}
		
		this.writeClaimToStorage(claim);
	}
	
	abstract void writeClaimToStorage(Claim claim);
	
	//increments the claim ID and updates secondary storage to be sure it's saved
	abstract void incrementNextClaimID();
	
	//retrieves player data from memory or secondary storage, as necessary
	//if the player has never been on the server before, this will return a fresh player data with default values
	synchronized public PlayerData getPlayerData(UUID playerID)
	{
		//first, look in memory
		PlayerData playerData = this.playerNameToPlayerDataMap.get(playerID);
		
		//if not there, build a fresh instance with some blanks for what may be in secondary storage
		if(playerData == null)
		{
			playerData = new PlayerData();
			playerData.playerID = playerID;
			
			//shove that new player data into the hash map cache
			this.playerNameToPlayerDataMap.put(playerID, playerData);
		}
		
		return playerData;
	}
	
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

    //educates a player about /adminclaims and /acb, if he can use them
    void tryAdvertiseAdminAlternatives(Player player)
    {
        if(player.hasPermission("griefprevention.adminclaims") && player.hasPermission("griefprevention.adjustclaimblocks"))
        {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.AdvertiseACandACB);
        }
        else if(player.hasPermission("griefprevention.adminclaims"))
        {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.AdvertiseAdminClaims);
        }
        else if(player.hasPermission("griefprevention.adjustclaimblocks"))
        {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.AdvertiseACB);
        }
    }
	


	private void addDefault(HashMap<String, CustomizableMessage> defaults,
			Messages id, String text, String notes)
	{
		CustomizableMessage message = new CustomizableMessage(id, text, notes);
		defaults.put(id.name(), message);		
	}

	synchronized public String getMessage(Messages messageID, String... args)
	{
		String message = messages[messageID.ordinal()];
		
		for(int i = 0; i < args.length; i++)
		{
			String param = args[i];
			message = message.replace("{" + i + "}", param);
		}

		if (Bukkit.isPrimaryThread())
        {
            DeniedMessageEvent event = new DeniedMessageEvent(messageID, message);
            Bukkit.getPluginManager().callEvent(event);
            return event.getMessage();
        }
		return message;
	}
	
	//used in updating the data schema from 0 to 1.
	//converts player names in a list to uuids
	protected List<String> convertNameListToUUIDList(List<String> names)
	{
	    //doesn't apply after schema has been updated to version 1
	    if(this.getSchemaVersion() >= 1) return names;
	    
	    //list to build results
	    List<String> resultNames = new ArrayList<String>();
	    
	    for(String name : names)
	    {
	        //skip non-player-names (groups and "public"), leave them as-is
	        if(name.startsWith("[") || name.equals("public"))
            {
	            resultNames.add(name);
	            continue;
            }
	        
	        //otherwise try to convert to a UUID
	        UUID playerID = null;
	        try
	        {
	            playerID = UUIDFetcher.getUUIDOf(name);
	        }
	        catch(Exception ex){ }
	        
	        //if successful, replace player name with corresponding UUID
	        if(playerID != null)
	        {
	            resultNames.add(playerID.toString());
	        }
	    }
	    
	    return resultNames;
    }
	
	abstract void close();
	
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
