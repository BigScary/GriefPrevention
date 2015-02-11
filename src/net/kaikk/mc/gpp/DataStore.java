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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

//singleton class which manages all GriefPrevention data (except for config options)
public class DataStore 
{
	//in-memory cache for player data
	protected ConcurrentHashMap<UUID, PlayerData> playerNameToPlayerDataMap = new ConcurrentHashMap<UUID, PlayerData>();
	
	//in-memory cache for group (permission-based) data
	protected ConcurrentHashMap<String, Integer> permissionToBonusBlocksMap = new ConcurrentHashMap<String, Integer>();
	
	//in-memory cache for claim data
	//ArrayList<Claim> claims = new ArrayList<Claim>();
	ConcurrentHashMap<Integer, Claim> claims = new ConcurrentHashMap<Integer, Claim>();
	ConcurrentHashMap<String, ArrayList<Claim>> chunksToClaimsMap = new ConcurrentHashMap<String, ArrayList<Claim>>();
	
	//in-memory cache for messages
	private String [] messages;
	
	//pattern for unique user identifiers (UUIDs)
	//protected final static Pattern uuidpattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
	
	//path information, for where stuff stored on disk is well...  stored
	protected final static String dataLayerFolderPath = "plugins" + File.separator + "GriefPreventionData";
	final static String configFilePath = dataLayerFolderPath + File.separator + "config.yml";
	final static String messagesFilePath = dataLayerFolderPath + File.separator + "messages.yml";
	final static String softMuteFilePath = dataLayerFolderPath + File.separator + "softMute.txt";
    
    //video links
    static final String SURVIVAL_VIDEO_URL = "" + ChatColor.DARK_AQUA + ChatColor.UNDERLINE + "bit.ly/mcgpuser";
    static final String CREATIVE_VIDEO_URL = "" + ChatColor.DARK_AQUA + ChatColor.UNDERLINE + "bit.ly/mcgpcrea";
    static final String SUBDIVISION_VIDEO_URL = "" + ChatColor.DARK_AQUA + ChatColor.UNDERLINE + "bit.ly/mcgpsub";
    
    //list of UUIDs which are soft-muted
    ConcurrentHashMap<UUID, Boolean> softMuteMap = new ConcurrentHashMap<UUID, Boolean>();
    
    //world guard reference, if available
    private WorldGuardWrapper worldGuard = null;
    
    
	private Connection databaseConnection = null;
	
	private String databaseUrl;
	private String userName;
	private String password;
	
	
	DataStore(String url, String userName, String password) throws Exception
	{
		this.databaseUrl = url;
		this.userName = userName;
		this.password = password;
		
		this.initialize();
	}

	//initialization!
	void initialize() throws Exception
	{
		try
		{
			//load the java driver for mySQL
			Class.forName("com.mysql.jdbc.Driver");
		}
		catch(Exception e)
		{
			GriefPreventionPlus.AddLogEntry("ERROR: Unable to load Java's mySQL database driver.  Check to make sure you've installed it properly.");
			throw e;
		}
		
		try
		{
			this.refreshDataConnection();
		}
		catch(Exception e2)
		{
			GriefPreventionPlus.AddLogEntry("ERROR: Unable to connect to database.  Check your config file settings.");
			throw e2;
		}
		
		try
		{
			Statement statement = databaseConnection.createStatement();
			
			ResultSet results = statement.executeQuery("SHOW TABLES LIKE 'gpp_claims'");
			if (!results.next()) {
				statement.execute("CREATE TABLE IF NOT EXISTS gpp_claims ("
						+ "id int(11) NOT NULL AUTO_INCREMENT,"
						+ "owner binary(16) NOT NULL COMMENT 'UUID',"
						+ "world binary(16) NOT NULL COMMENT 'UUID',"
						+ "lesserX mediumint(9) NOT NULL,"
						+ "lesserZ mediumint(9) NOT NULL,"
						+ "greaterX mediumint(9) NOT NULL,"
						+ "greaterZ mediumint(9) NOT NULL,"
						+ "parentid int(11) NOT NULL,"
						+ "PRIMARY KEY (id));");
				
				statement.execute("CREATE TABLE IF NOT EXISTS gpp_groupdata ("
						+ "gname varchar(100) NOT NULL,"
						+ "blocks int(11) NOT NULL,"
						+ "UNIQUE KEY gname (gname));");
				
				statement.execute("CREATE TABLE IF NOT EXISTS gpp_permsbukkit ("
						+ "claimid int(11) NOT NULL,"
						+ "pname varchar(80) NOT NULL,"
						+ "perm tinyint(4) NOT NULL,"
						+ "PRIMARY KEY (claimid,pname),"
						+ "KEY claimid (claimid));");
				
				statement.execute("CREATE TABLE IF NOT EXISTS gpp_permsplayer ("
						+ "claimid int(11) NOT NULL,"
						+ "player binary(16) NOT NULL COMMENT 'UUID',"
						+ "perm tinyint(4) NOT NULL,"
						+ "PRIMARY KEY (claimid,player),"
						+ "KEY claimid (claimid));");
				
				statement.execute("CREATE TABLE IF NOT EXISTS gpp_playerdata ("
						+ "player binary(16) NOT NULL COMMENT 'UUID',"
						+ "accruedblocks int(11) NOT NULL,"
						+ "bonusblocks int(11) NOT NULL,"
						+ "PRIMARY KEY (player));");
				
				results = statement.executeQuery("SHOW TABLES LIKE 'griefprevention_claimdata';");
				if (results.next()) {
					// migration from griefprevention
					GriefPreventionPlus.AddLogEntry("Migrating data from Grief Prevention. It may take some time.");
					
					// claims
					results = statement.executeQuery("SELECT * FROM griefprevention_claimdata ORDER BY parentid ASC;");
					Statement statement2 = databaseConnection.createStatement();
					
					String tString;
					String playerId;
					long i=0;
					long j=0;
					long k=0;
					
					long claimId=1;
					Long nextParentId;
					
					HashMap<Long, Long> migratedClaims = new HashMap<Long, Long>();
					while (results.next()) {
						String ownerString = results.getString(2);
						playerId="0";

						if (ownerString.length()==36 && (tString=ownerString.replace("-", "")).length()==32) {
							playerId=tString;
						}
					
						String[] lesser = results.getString(3).split(";");
						String[] greater = results.getString(4).split(";");
						if (lesser.length!=4 || greater.length!=4) { // wrong corners, skip this claim
							GriefPreventionPlus.AddLogEntry("Skipping claim "+results.getLong(1)+": wrong corners");
							continue;
						}
						
						World world = GriefPreventionPlus.instance.getServer().getWorld(lesser[0]);
						if (world==null) { // this world doesn't exist, skip this claim
							GriefPreventionPlus.AddLogEntry("Skipping claim "+results.getLong(1)+": world "+lesser[0]+" doesn't exist");
							continue;
						}
						
						// insert this claim in new claims table

						if (results.getLong(9)==-1) { // claims
							migratedClaims.put(results.getLong(1), claimId++);
							nextParentId=(long)-1;
							if (playerId.equals("0")) {
								playerId=UUIDtoHexString(GriefPreventionPlus.UUID1); // administrative claims
							}
						} else { // subclaims
							nextParentId=migratedClaims.get(results.getLong(9));
						}
						
						if (nextParentId==null) {
							GriefPreventionPlus.AddLogEntry("Skipping orphan subclaim (parentid: "+results.getLong(9)+").");
							continue;
						}
						
						statement2.executeUpdate("INSERT INTO gpp_claims (owner, world, lesserX, lesserZ, greaterX, greaterZ, parentid) VALUES (0x"+playerId+", "+UUIDtoHexString(world.getUID())+", "+lesser[1]+", "+lesser[3]+", "+greater[1]+", "+greater[3]+", "+nextParentId+");");
						
						i++;
						
						// convert permissions for this claim
						// builders
						if (!results.getString(5).isEmpty()) {
							for(String s : results.getString(5).split(";")) {
								if (s.startsWith("[")) {
									statement2.executeUpdate("INSERT INTO gpp_permsbukkit VALUES("+i+", '"+s.substring(1, s.length()-1)+"', 2) ON DUPLICATE KEY UPDATE perm = perm | 2;");
								} else {
									if (s.length()==36 && (tString=s.replace("-", "")).length()==32) {
										statement2.executeUpdate("INSERT INTO gpp_permsplayer VALUES("+i+", 0x"+tString+", 2) ON DUPLICATE KEY UPDATE perm = perm | 2;");
									}
								}
								j++;
							}
						}
						// containers
						if (!results.getString(6).isEmpty()) {
							for(String s : results.getString(6).split(";")) {
								if (s.startsWith("[")) {
									statement2.executeUpdate("INSERT INTO gpp_permsbukkit VALUES("+i+", '"+s.substring(1, s.length()-1)+"', 4) ON DUPLICATE KEY UPDATE perm = perm | 4;");
								} else {
									if (s.length()==36 && (tString=s.replace("-", "")).length()==32) {
										statement2.executeUpdate("INSERT INTO gpp_permsplayer VALUES("+i+", 0x"+tString+", 4) ON DUPLICATE KEY UPDATE perm = perm | 4;");
									}
								}
								j++;
							}
						}
						// accessors
						if (!results.getString(7).isEmpty()) {
							for(String s : results.getString(7).split(";")) {
								if (s.startsWith("[")) {
									statement2.executeUpdate("INSERT INTO gpp_permsbukkit VALUES("+i+", '"+s.substring(1, s.length()-1)+"', 8) ON DUPLICATE KEY UPDATE perm = perm | 8;");
								} else {
									if (s.length()==36 && (tString=s.replace("-", "")).length()==32) {
										statement2.executeUpdate("INSERT INTO gpp_permsplayer VALUES("+i+", 0x"+tString+", 8) ON DUPLICATE KEY UPDATE perm = perm | 8;");
									}
								}
								j++;
							}
						}
						// managers
						if (!results.getString(8).isEmpty()) {
							for(String s : results.getString(8).split(";")) {
								if (s.startsWith("[")) {
									statement2.executeUpdate("INSERT INTO gpp_permsbukkit VALUES("+i+", '"+s.substring(1, s.length()-1)+"', 1) ON DUPLICATE KEY UPDATE perm = perm | 1;");
								} else {
									if (s.length()==36 && (tString=s.replace("-", "")).length()==32) {
										statement2.executeUpdate("INSERT INTO gpp_permsplayer VALUES("+i+", 0x"+tString+", 1) ON DUPLICATE KEY UPDATE perm = perm | 1;");
									}
								}
								j++;
							}
						}
					}
					
					results = statement.executeQuery("SELECT name, accruedblocks, bonusblocks FROM griefprevention_playerdata;");

					while(results.next()) {
						String ownerString = results.getString(1);

						if (ownerString.length()==36 && (tString=ownerString.replace("-", "")).length()==32) {
							playerId=tString;
						} else {
							GriefPreventionPlus.AddLogEntry("Skipping GriefPrevention data for user "+ownerString+": no UUID.");
							continue;
						}
						
						statement2.executeUpdate("INSERT INTO gpp_playerdata VALUES (0x"+playerId+", "+results.getInt(2)+", "+results.getInt(3)+");");
						k++;
					}
					
					statement.close();
					statement2.close();
					GriefPreventionPlus.AddLogEntry("Migration complete. Claims: "+i+" - Permissions: "+j+" - PlayerData: "+k);
				}
			}
		} catch(Exception e3) {
			GriefPreventionPlus.AddLogEntry("ERROR: Unable to create the necessary database table.  Details:");
			GriefPreventionPlus.AddLogEntry(e3.getMessage());
			e3.printStackTrace();
			throw e3;
		}
		
		//load group data into memory
		Statement statement = databaseConnection.createStatement();
		ResultSet results = statement.executeQuery("SELECT gname, blocks FROM gpp_groupdata;");
		
		while(results.next()) {
			this.permissionToBonusBlocksMap.put(results.getString(1), results.getInt(2));			
		}
		
		//load claims data into memory		
		results = statement.executeQuery("SELECT * FROM gpp_claims;");
		Statement statementPerms = databaseConnection.createStatement();
		ResultSet resultsPerms;
		
		while(results.next()) {
			int id=results.getInt(1);
			int parentid=results.getInt(8);
			UUID owner=null;
			HashMap<UUID, Integer> permissionMapPlayers = new HashMap<UUID, Integer>();
			HashMap<String, Integer> permissionMapBukkit = new HashMap<String, Integer>();

			World world = GriefPreventionPlus.instance.getServer().getWorld(toUUID(results.getBytes(3)));
			if (world==null) { // This world doesn't exist. Skip this claim.
				GriefPreventionPlus.AddLogEntry("Skipping claim id "+id+" (world doesn't exist)");
				continue;
			}
			
			if (results.getBytes(2)!=null) {
				owner = toUUID(results.getBytes(2));
			}
			
			resultsPerms = statementPerms.executeQuery("SELECT player, perm FROM gpp_permsplayer WHERE claimid="+id+";");
			while(resultsPerms.next()) {
				permissionMapPlayers.put(toUUID(resultsPerms.getBytes(1)), resultsPerms.getInt(2));
			}
			
			resultsPerms = statementPerms.executeQuery("SELECT pname, perm FROM gpp_permsbukkit WHERE claimid="+id+";");
			while(resultsPerms.next()) {
				permissionMapBukkit.put(resultsPerms.getString(1), resultsPerms.getInt(2));
			}

			Claim claim = new Claim(world, results.getInt(4), results.getInt(5), results.getInt(6), results.getInt(7), owner, permissionMapPlayers, permissionMapBukkit, id);
			
			if (parentid==-1) {
				this.addClaim(claim, false);
			} else {
				Claim topClaim = this.claims.get(parentid);
				if (topClaim==null) {
					// parent claim doesn't exist, skip this subclaim
					GriefPreventionPlus.AddLogEntry("Orphan subclaim: "+claim.locationToString());
					continue;
				}
				claim.parent=topClaim;
				topClaim.children.add(claim);
				claim.inDataStore=true;
			}
		}

		GriefPreventionPlus.AddLogEntry(this.claims.size() + " total claims loaded.");
		
		//load up all the messages from messages.yml
		this.loadMessages();
		GriefPreventionPlus.AddLogEntry("Customizable messages loaded.");
		
		//load list of soft mutes
        this.loadSoftMutes();
		
		//try to hook into world guard
		try {
		    this.worldGuard = new WorldGuardWrapper();
		    GriefPreventionPlus.AddLogEntry("Successfully hooked into WorldGuard.");
		}
		//if failed, world guard compat features will just be disabled.
		catch(ClassNotFoundException exception){ }
		catch(NoClassDefFoundError exception){ }
	}
	
	private void loadSoftMutes()
	{
	    File softMuteFile = new File(softMuteFilePath);
        if(softMuteFile.exists())
        {
            BufferedReader inStream = null;
            try
            {
                //open the file
                inStream = new BufferedReader(new FileReader(softMuteFile.getAbsolutePath()));
                
                //while there are lines left
                String nextID = inStream.readLine();
                while(nextID != null)
                {                
                    //parse line into a UUID
                    UUID playerID;
                    try
                    {
                        playerID = UUID.fromString(nextID);
                    }
                    catch(Exception e)
                    {
                        playerID = null;
                        GriefPreventionPlus.AddLogEntry("Failed to parse soft mute entry as a UUID: " + nextID);
                    }
                    
                    //push it into the map
                    if(playerID != null)
                    {
                        this.softMuteMap.put(playerID, true);
                    }
                    
                    //move to the next
                    nextID = inStream.readLine();
                }
            }
            catch(Exception e)
            {
                GriefPreventionPlus.AddLogEntry("Failed to read from the soft mute data file: " + e.toString());
                e.printStackTrace();
            }
            
            try
            {
                if(inStream != null) inStream.close();                  
            }
            catch(IOException exception) {}
        }        
    }
	
	//updates soft mute map and data file
	boolean toggleSoftMute(UUID playerID)
	{
	    boolean newValue = !this.isSoftMuted(playerID);
	    
	    this.softMuteMap.put(playerID, newValue);
	    this.saveSoftMutes();
	    
	    return newValue;
	}
	
	boolean isSoftMuted(UUID playerID)
	{
	    Boolean mapEntry = this.softMuteMap.get(playerID);
	    if(mapEntry == null || mapEntry == Boolean.FALSE)
	    {
	        return false;
	    }
	    
	    return true;
	}
	
	private void saveSoftMutes()
	{
	    BufferedWriter outStream = null;
        
        try
        {
            //open the file and write the new value
            File softMuteFile = new File(softMuteFilePath);
            softMuteFile.createNewFile();
            outStream = new BufferedWriter(new FileWriter(softMuteFile));
            
            for(Map.Entry<UUID, Boolean> entry : softMuteMap.entrySet())
            {
                if(entry.getValue().booleanValue())
                {
                    outStream.write(entry.getKey().toString());
                    outStream.newLine();
                }
            }
            
        }       
        
        //if any problem, log it
        catch(Exception e)
        {
            GriefPreventionPlus.AddLogEntry("Unexpected exception saving soft mute data: " + e.getMessage());
            e.printStackTrace();
        }
        
        //close the file
        try
        {
            if(outStream != null) outStream.close();
        }
        catch(IOException exception) {}
	}
	
    //removes cached player data from memory
	synchronized void clearCachedPlayerData(UUID playerID)
	{
		this.playerNameToPlayerDataMap.remove(playerID);
	}
	
	//gets the number of bonus blocks a player has from his permissions
	//Bukkit doesn't allow for checking permissions of an offline player.
	//this will return 0 when he's offline, and the correct number when online.
	synchronized int getGroupBonusBlocks(UUID playerID)
	{
		Player player = GriefPreventionPlus.instance.getServer().getPlayer(playerID);
		if (player!=null) {
			int bonusBlocks=0;
			for (Entry<String,Integer> e : permissionToBonusBlocksMap.entrySet()) {
				if (player!=null && player.hasPermission(e.getKey())) {
					bonusBlocks+=e.getValue();
				}
			}
			return bonusBlocks;
		} else {
			return 0;
		}
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
	
	//updates the database with a group's bonus blocks
	synchronized void saveGroupBonusBlocks(String groupName, int currentValue)
	{
		//group bonus blocks are stored in the player data table, with player name = $groupName
	    try
        {
            this.refreshDataConnection();
      
            Statement statement = databaseConnection.createStatement();
            statement.executeUpdate("INSERT INTO gpp_groupdata VALUES (\""+groupName+"\", "+currentValue+") ON DUPLICATE KEY UPDATE blocks="+currentValue+";");
        }
        catch(SQLException e)
        {
            GriefPreventionPlus.AddLogEntry("Unable to save data for group " + groupName + ".  Details:");
            GriefPreventionPlus.AddLogEntry(e.getMessage());
        }
	}
	
	synchronized public void changeClaimOwner(Claim claim, UUID newOwnerID) throws Exception
	{
		//if it's a subdivision, throw an exception
		if(claim.parent != null) {
			throw new Exception("Subdivisions can't be transferred.  Only top-level claims may change owners.");
		}
		
		//otherwise update information
		
		//determine current claim owner
		PlayerData ownerData = null;
		if(!claim.isAdminClaim()) {
			ownerData = this.getPlayerData(claim.ownerID);
		}
		
		//determine new owner
		PlayerData newOwnerData = this.getPlayerData(newOwnerID);
		
		//transfer
		claim.ownerID = newOwnerID;
		this.dbUpdateOwner(claim);
		
		//adjust blocks and other records
		if(ownerData != null) {
			ownerData.getClaims().remove(claim);
			ownerData.setBonusClaimBlocks(ownerData.getBonusClaimBlocks() - claim.getArea());
			this.savePlayerData(claim.ownerID, ownerData);
		}
		
		newOwnerData.getClaims().add(claim);
		newOwnerData.setBonusClaimBlocks(newOwnerData.getBonusClaimBlocks() + claim.getArea());
		this.savePlayerData(newOwnerID, newOwnerData);
	}

	//adds a claim to the datastore, making it an effective claim
	synchronized void addClaim(Claim newClaim, boolean writeToStorage)
	{
		//subdivisions are easy
		if(newClaim.parent != null)
		{
			newClaim.parent.children.add(newClaim);
			newClaim.inDataStore = true;
			if(writeToStorage) {
			    this.dbNewClaim(newClaim);
			}
			return;
		}
		
		if(writeToStorage) { // write the new claim on the db, so we get the id generated by the database
		    this.dbNewClaim(newClaim);
		    GriefPreventionPlus.AddLogEntry(newClaim.getOwnerName()+" made a new claim (id "+newClaim.id+") at "+newClaim.locationToString());
		}
		
		this.claims.put(newClaim.id, newClaim);
		ArrayList<String> chunkStrings = newClaim.getChunkStrings();
		for(String chunkString : chunkStrings)
		{
		    ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkString);
		    if(claimsInChunk == null)
		    {
		        claimsInChunk = new ArrayList<Claim>();
		        this.chunksToClaimsMap.put(chunkString, claimsInChunk);
		    }

		    claimsInChunk.add(newClaim);
		}
		
		newClaim.inDataStore = true;
		
		//except for administrative claims (which have no owner), update the owner's playerData with the new claim
		if(!newClaim.isAdminClaim() && writeToStorage)
		{
			PlayerData ownerData = this.getPlayerData(newClaim.ownerID);
			ownerData.getClaims().add(newClaim);
			this.savePlayerData(newClaim.ownerID, ownerData);
		}
	}
	
	//turns a location into a string FIXME
	private String locationStringDelimiter = ";";	
	String locationToString(Location location)
	{
		StringBuilder stringBuilder = new StringBuilder(location.getWorld().getName());
		stringBuilder.append(locationStringDelimiter);
		stringBuilder.append(location.getBlockX());
		stringBuilder.append(locationStringDelimiter);
		stringBuilder.append(location.getBlockY());
		stringBuilder.append(locationStringDelimiter);
		stringBuilder.append(location.getBlockZ());
		
		return stringBuilder.toString();
	}
	
	//turns a location string back into a location
	Location locationFromString(String string) throws Exception
	{
		//split the input string on the space
		String [] elements = string.split(locationStringDelimiter);
	    
		//expect four elements - world name, X, Y, and Z, respectively
		if(elements.length < 4)
		{
			throw new Exception("Expected four distinct parts to the location string: \"" + string + "\"");
		}
		
		String worldName = elements[0];
		String xString = elements[1];
		String yString = elements[2];
		String zString = elements[3];
	    
		//identify world the claim is in
		World world = GriefPreventionPlus.instance.getServer().getWorld(worldName);
		if(world == null)
		{
			throw new Exception("World not found: \"" + worldName + "\"");
		}
		
		//convert those numerical strings to integer values
	    int x = Integer.parseInt(xString);
	    int y = Integer.parseInt(yString);
	    int z = Integer.parseInt(zString);
	    
	    return new Location(world, x, y, z);
	}
	
	synchronized public void dbNewClaim(Claim claim) {
		try {
			this.refreshDataConnection();
			Statement statement = databaseConnection.createStatement();

			statement.executeUpdate("INSERT INTO gpp_claims (owner, world, lesserX, lesserZ, greaterX, greaterZ, parentid) "
					+ "VALUES (" + UUIDtoHexString(claim.ownerID) + ", "
					+ UUIDtoHexString(claim.getLesserBoundaryCorner().getWorld().getUID()) + ", "
					+ claim.getLesserBoundaryCorner().getBlockX()+", "
					+ claim.getLesserBoundaryCorner().getBlockZ()+", "
					+ claim.getGreaterBoundaryCorner().getBlockX()+", "
					+ claim.getGreaterBoundaryCorner().getBlockZ()+", "
					+ (claim.parent!=null ? claim.parent.id : -1)+");", Statement.RETURN_GENERATED_KEYS);
			
			ResultSet result = statement.getGeneratedKeys();
			result.next();
			claim.id = result.getInt(1);
		} catch (SQLException e) {
			GriefPreventionPlus.AddLogEntry("Unable to insert data for new claim at " + claim.locationToString() + ".  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
		}
	}
	
	synchronized public void dbUpdateOwner(Claim claim) {
		try {
			this.refreshDataConnection();
			Statement statement = databaseConnection.createStatement();

			statement.executeUpdate("UPDATE gpp_claims SET owner="+UUIDtoHexString(claim.ownerID)+" WHERE id="+claim.id);
		} catch (SQLException e) {
			GriefPreventionPlus.AddLogEntry("Unable to update owner for claim id " + claim.id + ".  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
		}
	}
	
	synchronized public void dbUpdateLocation(Claim claim) {
		try {
			this.refreshDataConnection();
			Statement statement = databaseConnection.createStatement();

			statement.executeUpdate("UPDATE gpp_claims SET lesserX="+claim.getLesserBoundaryCorner().getBlockX()+", lesserZ="+claim.getLesserBoundaryCorner().getBlockZ()+", greaterX="+claim.getGreaterBoundaryCorner().getBlockX()+", greaterZ="+claim.getGreaterBoundaryCorner().getBlockZ()+" WHERE id="+claim.id);
		} catch (SQLException e) {
			GriefPreventionPlus.AddLogEntry("Unable to update location for claim id " + claim.id + ".  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
		}
	}
	
	synchronized public void dbSetPerm(Integer claimId, UUID playerId, int perm) {
		try {
			this.refreshDataConnection();
			Statement statement = databaseConnection.createStatement();

			statement.executeUpdate("INSERT INTO gpp_permsplayer VALUES ("+claimId+", "+UUIDtoHexString(playerId)+", "+perm+") ON DUPLICATE KEY UPDATE perm=perm | "+perm+";");
		} catch (SQLException e) {
			GriefPreventionPlus.AddLogEntry("Unable to set perms for claim id " + claimId + " player {"+playerId.toString()+"}.  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
		}
	}
	
	synchronized public void dbSetPerm(Integer claimId, String permString, int perm) {
		try {
			this.refreshDataConnection();
			Statement statement = databaseConnection.createStatement();

			statement.executeUpdate("INSERT INTO gpp_permsbukkit VALUES ("+claimId+", \""+permString+"\", "+perm+") ON DUPLICATE KEY UPDATE perm=perm | "+perm+";");
		} catch (SQLException e) {
			GriefPreventionPlus.AddLogEntry("Unable to set perms for claim id " + claimId + " perm ["+permString+"].  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
		}
	}
	
	/** Unset all claim's perms */
	synchronized public void dbUnsetPerm(Integer claimId) {
		try {
			this.refreshDataConnection();
			Statement statement = databaseConnection.createStatement();

			statement.executeUpdate("DELETE FROM gpp_permsplayer WHERE claimid="+claimId+";");
			statement.executeUpdate("DELETE FROM gpp_permsbukkit WHERE claimid="+claimId+";");
		} catch (SQLException e) {
			GriefPreventionPlus.AddLogEntry("Unable to unset perms for claim id " + claimId + ".  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
		}
	}
	/** Unset all player claims' perms */
	synchronized public void dbUnsetPerm(UUID playerId) {
		try {
			this.refreshDataConnection();
			Statement statement = databaseConnection.createStatement();

			statement.executeUpdate("DELETE p FROM gpp_permsplayer AS p INNER JOIN gpp_claims AS c ON p.claimid = c.id WHERE c.owner="+UUIDtoHexString(playerId)+";");
			statement.executeUpdate("DELETE p FROM gpp_permsbukkit AS p INNER JOIN gpp_claims AS c ON p.claimid = c.id WHERE c.owner="+UUIDtoHexString(playerId)+";");
		} catch (SQLException e) {
			GriefPreventionPlus.AddLogEntry("Unable to unset perms for " + playerId.toString() + "'s claims.  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
		}
	}
	
	/** Unset playerId perms from all owner's claim */
	synchronized public void dbUnsetPerm(UUID owner, UUID playerId) {
		try {
			this.refreshDataConnection();
			Statement statement = databaseConnection.createStatement();

			statement.executeUpdate("DELETE p FROM gpp_permsplayer AS p INNER JOIN gpp_claims AS c ON p.claimid = c.id WHERE c.owner="+UUIDtoHexString(owner)+" AND p.player="+UUIDtoHexString(playerId)+";");
		} catch (SQLException e) {
			GriefPreventionPlus.AddLogEntry("Unable to unset {"+playerId.toString()+"} perms from {" + owner.toString() + "}'s claims.  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
		}
	}
	
	/** Unset permbukkit perms from all owner's claim */
	synchronized public void dbUnsetPerm(UUID owner, String permString) {
		try {
			this.refreshDataConnection();
			Statement statement = databaseConnection.createStatement();

			statement.executeUpdate("DELETE p FROM gpp_permsbukkit AS p INNER JOIN gpp_claims AS c ON p.claimid = c.id WHERE c.owner="+UUIDtoHexString(owner)+" AND p.pname=\""+permString+"\";");
		} catch (SQLException e) {
			GriefPreventionPlus.AddLogEntry("Unable to unset ["+permString+"] perms from {" + owner.toString() + "}'s claims.  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
		}
	}
	
	/** Unset playerId's perm from claim */
	synchronized public void dbUnsetPerm(Integer claimId, UUID playerId) {
		try {
			this.refreshDataConnection();
			Statement statement = databaseConnection.createStatement();

			statement.executeUpdate("DELETE FROM gpp_permsplayer WHERE claimid="+claimId+" AND player="+UUIDtoHexString(playerId)+";");
		} catch (SQLException e) {
			GriefPreventionPlus.AddLogEntry("Unable to unset perms for claim id " + claimId + " player {"+playerId.toString()+"}.  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
		}
	}	
	
	/** Unset permBukkit's perm from claim */
	synchronized public void dbUnsetPerm(Integer claimId, String permString) {
		try {
			this.refreshDataConnection();
			Statement statement = databaseConnection.createStatement();

			statement.executeUpdate("DELETE FROM gpp_permsbukkit WHERE claimid="+claimId+" AND pname=\""+permString+"\";");
		} catch (SQLException e) {
			GriefPreventionPlus.AddLogEntry("Unable to unset perms for claim id " + claimId + " perm ["+permString+"].  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
		}
	}
	
	//retrieves player data from memory or secondary storage, as necessary
	//if the player has never been on the server before, this will return a fresh player data with default values
	synchronized public PlayerData getPlayerData(UUID playerID)
	{
		//first, look in memory
		PlayerData playerData = this.playerNameToPlayerDataMap.get(playerID);
		
		//if not there, build a fresh instance with some blanks for what may be in secondary storage
		if(playerData == null) {
			playerData = new PlayerData(playerID);
			
			//shove that new player data into the hash map cache
			this.playerNameToPlayerDataMap.put(playerID, playerData);
		}
		
		return playerData;
	}
	
	synchronized PlayerData getPlayerDataFromStorage(UUID playerID)
	{
		try
		{
			this.refreshDataConnection();
			
			Statement statement = this.databaseConnection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM gpp_playerdata WHERE player=" + UUIDtoHexString(playerID) + ";");
		
			//if data for this player exists, use it
			if(results.next()){			
				return new PlayerData(playerID, results.getInt(2), results.getInt(3));
			}
		}
		catch(SQLException e)
		{
			GriefPreventionPlus.AddLogEntry("Unable to retrieve data for player " + playerID.toString() + ".  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
			e.printStackTrace();
		}
		
		return null;
	}
	
	//deletes a claim or subdivision
    synchronized public void deleteClaim(Claim claim)
    {
        this.deleteClaim(claim, true);
    }
	synchronized public void deleteClaim(Claim claim, boolean fireEvent)
	{
		if(claim.parent != null) { // subdivision
			Claim parentClaim = claim.parent;
			parentClaim.children.remove(claim);
			claim.inDataStore = false;
			this.deleteClaimFromSecondaryStorage(claim);
	        return;
		}
        
        //mark as deleted so any references elsewhere can be ignored
        claim.inDataStore = false;
        
		for (Claim subclaim : claim.children) {
			subclaim.inDataStore=false;
		}
		
		//remove from memory
        this.claims.remove(claim.id);

		
		ArrayList<String> chunkStrings = claim.getChunkStrings();
        for(String chunkString : chunkStrings) {
            ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkString);
            for(int j = 0; j < claimsInChunk.size(); j++) {
                if(claimsInChunk.get(j).id.equals(claim.id)) {
                    claimsInChunk.remove(j);
                    break;
                }
            }
        }
		
		//remove from secondary storage
		this.deleteClaimFromSecondaryStorage(claim);
		
		//update player data, except for administrative claims, which have no owner
		if(!claim.isAdminClaim()) {
			PlayerData ownerData = this.getPlayerData(claim.ownerID);
			for(int i = 0; i < ownerData.getClaims().size(); i++) {
				if(ownerData.getClaims().get(i).id.equals(claim.id)) {
					ownerData.getClaims().remove(i);
					break;
				}
			}
			this.savePlayerData(claim.ownerID, ownerData);
		}
		
		if(fireEvent) {
		    ClaimDeletedEvent ev = new ClaimDeletedEvent(claim);
            Bukkit.getPluginManager().callEvent(ev);
		}
	}
	
	
	//deletes a claim from the database (this delete subclaims too)
	synchronized void deleteClaimFromSecondaryStorage(Claim claim) {
	    try {
			this.refreshDataConnection();

			Statement statement = this.databaseConnection.createStatement();
			if (claim.children.isEmpty()) {
				statement.execute("DELETE p FROM gpp_claims AS c RIGHT JOIN gpp_permsbukkit AS p ON c.id = p.claimid WHERE c.id="+claim.id+";");
				statement.execute("DELETE p FROM gpp_claims AS c RIGHT JOIN gpp_permsplayer AS p ON c.id = p.claimid WHERE c.id="+claim.id+";");
				statement.execute("DELETE FROM gpp_claims WHERE id="+claim.id+";");			
			} else {
				statement.execute("DELETE p FROM gpp_claims AS c RIGHT JOIN gpp_permsbukkit AS p ON c.id = p.claimid WHERE c.id="+claim.id+" OR c.parentid="+claim.id+";");
				statement.execute("DELETE p FROM gpp_claims AS c RIGHT JOIN gpp_permsplayer AS p ON c.id = p.claimid WHERE c.id="+claim.id+" OR c.parentid="+claim.id+";");
				statement.execute("DELETE FROM gpp_claims WHERE id="+claim.id+" OR parentid="+claim.id+";");
			}
		} catch(SQLException e) {
			GriefPreventionPlus.AddLogEntry("Unable to delete data for claim " + claim.id + ".  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
			e.printStackTrace();
		}
	}
	
	//gets the claim at a specific location
	//ignoreHeight = TRUE means that a location UNDER an existing claim will return the claim
	//cachedClaim can be NULL, but will help performance if you have a reasonable guess about which claim the location is in
	synchronized public Claim getClaimAt(Location location, boolean ignoreHeight, Claim cachedClaim)
	{
		//check cachedClaim guess first.  if it's in the datastore and the location is inside it, we're done
		if(cachedClaim != null && cachedClaim.inDataStore && cachedClaim.contains(location, ignoreHeight, true)) return cachedClaim;
		
		//find a top level claim
		String chunkID = this.getChunkString(location);
		ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkID);
		if(claimsInChunk == null) return null;
		
		for(Claim claim : claimsInChunk)
		{
		    if(claim.contains(location, ignoreHeight, false))
		    {
		        //when we find a top level claim, if the location is in one of its subdivisions,
                //return the SUBDIVISION, not the top level claim
                for(int j = 0; j < claim.children.size(); j++)
                {
                    Claim subdivision = claim.children.get(j);
                    if(subdivision.contains(location, ignoreHeight, false)) return subdivision;
                }                       
                    
                return claim;
		    }
		}
		
		//if no claim found, return null
		return null;
	}
	
	/**get a claim by ID*/
	public synchronized Claim getClaim(long id) {
	    return this.claims.get(id);
	}
	
	
	//gets a unique, persistent identifier string for a chunk
	private String getChunkString(Location location)
	{
        return (location.getBlockX() >> 4) + location.getWorld().getName() + (location.getBlockZ() >> 4);
    }

    //creates a claim.
	//if the new claim would overlap an existing claim, returns a failure along with a reference to the existing claim
	//if the new claim would overlap a WorldGuard region where the player doesn't have permission to build, returns a failure with NULL for claim
	//otherwise, returns a success along with a reference to the new claim
	//use ownerName == "" for administrative claims
	//for top level claims, pass parent == NULL
	//DOES adjust claim blocks available on success (players can go into negative quantity available)
	//DOES check for world guard regions where the player doesn't have permission
	//does NOT check a player has permission to create a claim, or enough claim blocks.
	//does NOT check minimum claim size constraints
	//does NOT visualize the new claim for any players	
	synchronized public ClaimResult createClaim(World world, int x1, int x2, int z1, int z2, UUID ownerID, Claim parent, Integer id, Player creatingPlayer)
	{
		ClaimResult result = new ClaimResult();
		
		int smallx, bigx, smallz, bigz;

		//determine small versus big inputs
		if(x1 < x2) {
			smallx = x1;
			bigx = x2;
		} else {
			smallx = x2;
			bigx = x1;
		}
		
		if(z1 < z2) {
			smallz = z1;
			bigz = z2;
		} else {
			smallz = z2;
			bigz = z1;
		}
		
		//creative mode claims always go to bedrock FIXME wat?
		/*if(GriefPreventionPlus.instance.config_claims_worldModes.get(world) == ClaimsMode.Creative) {
			smally = 2;
		}*/
		
		//create a new claim instance (but don't save it, yet)
		Claim newClaim = new Claim(
			world, smallx, smallz,
			bigx, bigz,
			ownerID,
			null,
			null,
			id);
		
		newClaim.parent = parent;
		
		//ensure this new claim won't overlap any existing claims
		ArrayList<Claim> claimsToCheck;
		if(newClaim.parent != null) {
			claimsToCheck = newClaim.parent.children;			
		} else {
			claimsToCheck = new ArrayList<Claim>(this.claims.values());
		}

		for(int i = 0; i < claimsToCheck.size(); i++)
		{
			Claim otherClaim = claimsToCheck.get(i);
			
			//if we find an existing claim which will be overlapped
			if(otherClaim.overlaps(newClaim)) {
				//result = fail, return conflicting claim
				result.succeeded = false;
				result.claim = otherClaim;
				return result;
			}
		}
		
		//if worldguard is installed, also prevent claims from overlapping any worldguard regions
		if(GriefPreventionPlus.instance.config_claims_respectWorldGuard && this.worldGuard != null && creatingPlayer != null)
		{
		    if(!this.worldGuard.canBuild(newClaim.world, newClaim.lesserX, newClaim.lesserZ, newClaim.greaterX, newClaim.greaterZ, creatingPlayer)) {
                result.succeeded = false;
                result.claim = null;
                return result;
            }
		}

		//otherwise add this new claim to the data store to make it effective
		this.addClaim(newClaim, true);
		
		//then return success along with reference to new claim
		result.succeeded = true;
		result.claim = newClaim;
		return result;
	}
	
	/** This method checks if a claim overlaps an existing claim. subdivision are accepted too
	 @return the overlapped claim (or itself if it would overlap a worldguard region), null if it doesn't overlap! */
	public synchronized Claim overlapsClaims(Claim claim, Claim excludedClaim, Player creatingPlayer) {
		if (claim.parent!=null) {
			// top claim contains this subclaim
			if (!claim.parent.contains(claim.getLesserBoundaryCorner(), true, false) || !claim.parent.contains(claim.getGreaterBoundaryCorner(), true, false)) {
				return claim.parent;
			}

			//check parent's subclaims
			for(Claim otherClaim : claim.parent.children) {
				if (otherClaim==claim || otherClaim==excludedClaim) { // exclude this claim
					continue;
				}
				
				if(otherClaim.overlaps(claim)) {
					return otherClaim;
				}
			}
		} else {
			// if this claim has subclaims, check that every subclaim is within the top claim
			for (Claim otherClaim : claim.children) {
				if (!claim.contains(otherClaim.getGreaterBoundaryCorner(), true, false) ||  !claim.contains(otherClaim.getLesserBoundaryCorner(), true, false)) {
					return otherClaim;
				}
			}

			// Check for other claims
			for(Claim otherClaim : this.claims.values()) {
				if (otherClaim==claim || otherClaim==excludedClaim) { 
					continue; // exclude this claim
				}
				
				if(otherClaim.overlaps(claim)) {
					return otherClaim;
				}
			}

			//if worldguard is installed, also prevent claims from overlapping any worldguard regions
			if(GriefPreventionPlus.instance.config_claims_respectWorldGuard && this.worldGuard != null && creatingPlayer != null) {
			    if(!this.worldGuard.canBuild(claim.world, claim.lesserX, claim.lesserZ, claim.greaterX, claim.greaterZ, creatingPlayer)) {
			    	return claim;
	            }
			}
		}
		return null;
	}
	
	
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
	
	//saves changes to player data.  MUST be called after you're done making changes, otherwise a reload will lose them
	public void asyncSavePlayerData(UUID playerID, PlayerData playerData)
	{
		//never save data for the "administrative" account.  an empty string for player name indicates administrative account
		if(playerID == null) return;
		
		try {
			this.refreshDataConnection();
			
			Statement statement = databaseConnection.createStatement();
			statement.executeUpdate("INSERT INTO gpp_playerdata VALUES ("+UUIDtoHexString(playerData.playerID)+", \""+playerData.getAccruedClaimBlocks()+"\", "+playerData.getBonusClaimBlocks()+") ON DUPLICATE KEY UPDATE accruedblocks="+playerData.getAccruedClaimBlocks()+", bonusblocks="+playerData.getBonusClaimBlocks()+";");
		} catch(SQLException e) {
			GriefPreventionPlus.AddLogEntry("Unable to save data for player " + playerID.toString() + ".  Details:");
			GriefPreventionPlus.AddLogEntry(e.getMessage());
		}
	}

	//starts a siege on a claim
	//does NOT check siege cooldowns, see onCooldown() below
	synchronized public void startSiege(Player attacker, Player defender, Claim defenderClaim)
	{
		//fill-in the necessary SiegeData instance
		SiegeData siegeData = new SiegeData(attacker, defender, defenderClaim);
		PlayerData attackerData = this.getPlayerData(attacker.getUniqueId());
		PlayerData defenderData = this.getPlayerData(defender.getUniqueId());
		attackerData.siegeData = siegeData;
		defenderData.siegeData = siegeData;
		defenderClaim.siegeData = siegeData;
		
		//start a task to monitor the siege
		//why isn't this a "repeating" task?
		//because depending on the status of the siege at the time the task runs, there may or may not be a reason to run the task again
		SiegeCheckupTask task = new SiegeCheckupTask(siegeData);
		siegeData.checkupTaskID = GriefPreventionPlus.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPreventionPlus.instance, task, 20L * 30);
	}
	
	//ends a siege
	//either winnerName or loserName can be null, but not both
	synchronized public void endSiege(SiegeData siegeData, String winnerName, String loserName, boolean death)
	{
		boolean grantAccess = false;
		
		//determine winner and loser
		if(winnerName == null && loserName != null)
		{
			if(siegeData.attacker.getName().equals(loserName))
			{
				winnerName = siegeData.defender.getName();
			}
			else
			{
				winnerName = siegeData.attacker.getName();
			}
		}
		else if(winnerName != null && loserName == null)
		{
			if(siegeData.attacker.getName().equals(winnerName))
			{
				loserName = siegeData.defender.getName();
			}
			else
			{
				loserName = siegeData.attacker.getName();
			}
		}
		
		//if the attacker won, plan to open the doors for looting
		if(siegeData.attacker.getName().equals(winnerName))
		{
			grantAccess = true;
		}
		
		PlayerData attackerData = this.getPlayerData(siegeData.attacker.getUniqueId());
		attackerData.siegeData = null;
		
		PlayerData defenderData = this.getPlayerData(siegeData.defender.getUniqueId());	
		defenderData.siegeData = null;

		//start a cooldown for this attacker/defender pair
		Long now = System.currentTimeMillis();
		Long cooldownEnd = now + 1000 * 60 * 60;  //one hour from now
		this.siegeCooldownRemaining.put(siegeData.attacker.getName() + "_" + siegeData.defender.getName(), cooldownEnd);
		
		//start cooldowns for every attacker/involved claim pair
		for(int i = 0; i < siegeData.claims.size(); i++)
		{
			Claim claim = siegeData.claims.get(i);
			claim.siegeData = null;
			this.siegeCooldownRemaining.put(siegeData.attacker.getName() + "_" + claim.getOwnerName(), cooldownEnd);
			
			//if doors should be opened for looting, do that now
			if(grantAccess)
			{
				claim.doorsOpen = true;
			}
		}

		//cancel the siege checkup task
		GriefPreventionPlus.instance.getServer().getScheduler().cancelTask(siegeData.checkupTaskID);
		
		//notify everyone who won and lost
		if(winnerName != null && loserName != null)
		{
			GriefPreventionPlus.instance.getServer().broadcastMessage(winnerName + " defeated " + loserName + " in siege warfare!");
		}
		
		//if the claim should be opened to looting
		if(grantAccess)
		{
			Player winner = GriefPreventionPlus.instance.getServer().getPlayer(winnerName);
			if(winner != null)
			{
				//notify the winner
				GriefPreventionPlus.sendMessage(winner, TextMode.Success, Messages.SiegeWinDoorsOpen);
				
				//schedule a task to secure the claims in about 5 minutes
				SecureClaimTask task = new SecureClaimTask(siegeData);
				GriefPreventionPlus.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPreventionPlus.instance, task, 20L * 60 * 5);
			}
		}
		
		//if the siege ended due to death, transfer inventory to winner
		if(death)
		{
			Player winner = GriefPreventionPlus.instance.getServer().getPlayer(winnerName);
			Player loser = GriefPreventionPlus.instance.getServer().getPlayer(loserName);
			if(winner != null && loser != null)
			{
				//get loser's inventory, then clear it
				ItemStack [] loserItems = loser.getInventory().getContents();
				loser.getInventory().clear();
				
				//try to add it to the winner's inventory
				for(int j = 0; j < loserItems.length; j++)
				{
					if(loserItems[j] == null || loserItems[j].getType() == Material.AIR || loserItems[j].getAmount() == 0) continue;
					
					HashMap<Integer, ItemStack> wontFitItems = winner.getInventory().addItem(loserItems[j]);
					
					//drop any remainder on the ground at his feet
					Object [] keys = wontFitItems.keySet().toArray();
					Location winnerLocation = winner.getLocation(); 
					for(int i = 0; i < keys.length; i++)
					{
						Integer key = (Integer)keys[i];
						winnerLocation.getWorld().dropItemNaturally(winnerLocation, wontFitItems.get(key));
					}
				}
			}
		}
	}
	
	//timestamp for each siege cooldown to end
	private HashMap<String, Long> siegeCooldownRemaining = new HashMap<String, Long>();

	//whether or not a sieger can siege a particular victim or claim, considering only cooldowns
	synchronized public boolean onCooldown(Player attacker, Player defender, Claim defenderClaim)
	{
		Long cooldownEnd = null;
		
		//look for an attacker/defender cooldown
		if(this.siegeCooldownRemaining.get(attacker.getName() + "_" + defender.getName()) != null)
		{
			cooldownEnd = this.siegeCooldownRemaining.get(attacker.getName() + "_" + defender.getName());
			
			if(System.currentTimeMillis() < cooldownEnd)
			{
				return true;
			}
			
			//if found but expired, remove it
			this.siegeCooldownRemaining.remove(attacker.getName() + "_" + defender.getName());
		}
		
		//look for an attacker/claim cooldown
		if(cooldownEnd == null && this.siegeCooldownRemaining.get(attacker.getName() + "_" + defenderClaim.getOwnerName()) != null)
		{
			cooldownEnd = this.siegeCooldownRemaining.get(attacker.getName() + "_" + defenderClaim.getOwnerName());
			
			if(System.currentTimeMillis() < cooldownEnd)
			{
				return true;
			}
			
			//if found but expired, remove it
			this.siegeCooldownRemaining.remove(attacker.getName() + "_" + defenderClaim.getOwnerName());			
		}
		
		return false;
	}

	//extend a siege, if it's possible to do so
	synchronized void tryExtendSiege(Player player, Claim claim)
	{
		PlayerData playerData = this.getPlayerData(player.getUniqueId());
		
		//player must be sieged
		if(playerData.siegeData == null) return;
		
		//claim isn't already under the same siege
		if(playerData.siegeData.claims.contains(claim)) return;
		
		//admin claims can't be sieged
		if(claim.isAdminClaim()) return;
		
		//player must have some level of permission to be sieged in a claim
		if(claim.allowAccess(player) != null) return;
		
		//otherwise extend the siege
		playerData.siegeData.claims.add(claim);
		claim.siegeData = playerData.siegeData;
	}		
	
	//deletes all claims owned by a player
	synchronized public void deleteClaimsForPlayer(UUID playerID, boolean deleteCreativeClaims)
	{
		for(Claim claim : this.claims.values()) {
			if((playerID == claim.ownerID || (playerID != null && playerID.equals(claim.ownerID))) && (deleteCreativeClaims || !GriefPreventionPlus.instance.creativeRulesApply(claim.world))) {
				claim.removeSurfaceFluids(null);
				
				this.deleteClaim(claim, true);
				
				//if in a creative mode world, delete the claim
				if(GriefPreventionPlus.instance.creativeRulesApply(claim.world)) {
					GriefPreventionPlus.instance.restoreClaim(claim, 0);
				}
			}
		}				
	}

	/**tries to resize a claim
	see CreateClaim() for details on return value*/
	synchronized public ClaimResult resizeClaim(Claim claim, int newx1, int newz1, int newx2, int newz2, Player resizingPlayer) {
		//create a fake claim with new coords
		Claim newClaim = new Claim(claim.world, newx1, newz1, newx2, newz2, claim.ownerID, null, null, claim.id);
		newClaim.parent=claim.parent;
		newClaim.children=claim.children;

		GriefPreventionPlus.AddLogEntry("resizeClaim "+newClaim.id+" cs:"+newClaim.children.size());

		Claim claimCheck = this.overlapsClaims(newClaim, claim, resizingPlayer);

		if (claimCheck==null) {
			// let's update this claim
			String oldLoc = claim.locationToString();
			claim.setLocation(claim.world, newx1, newz1, newx2, newz2);
			this.dbUpdateLocation(claim);
			GriefPreventionPlus.AddLogEntry(claim.getOwnerName()+" resized claim id "+claim.id+" from "+oldLoc+" to "+claim.locationToString());
			return new ClaimResult(true, claim);
		} else {
			return new ClaimResult(false, claimCheck);
		}
	}
	
	private void loadMessages() 
	{
		Messages [] messageIDs = Messages.values();
		this.messages = new String[Messages.values().length];
		
		HashMap<String, CustomizableMessage> defaults = new HashMap<String, CustomizableMessage>();
		
		//initialize defaults
		this.addDefault(defaults, Messages.RespectingClaims, "Now respecting claims.", null);
		this.addDefault(defaults, Messages.IgnoringClaims, "Now ignoring claims.", null);
		this.addDefault(defaults, Messages.NoCreativeUnClaim, "You can't unclaim this land.  You can only make this claim larger or create additional claims.", null);
		this.addDefault(defaults, Messages.SuccessfulAbandon, "Claims abandoned.  You now have {0} available claim blocks.", "0: remaining blocks");
		this.addDefault(defaults, Messages.RestoreNatureActivate, "Ready to restore some nature!  Right click to restore nature, and use /BasicClaims to stop.", null);
		this.addDefault(defaults, Messages.RestoreNatureAggressiveActivate, "Aggressive mode activated.  Do NOT use this underneath anything you want to keep!  Right click to aggressively restore nature, and use /BasicClaims to stop.", null);
		this.addDefault(defaults, Messages.FillModeActive, "Fill mode activated with radius {0}.  Right click an area to fill.", "0: fill radius");
		this.addDefault(defaults, Messages.TransferClaimPermission, "That command requires the administrative claims permission.", null);
		this.addDefault(defaults, Messages.TransferClaimMissing, "There's no claim here.  Stand in the administrative claim you want to transfer.", null);
		this.addDefault(defaults, Messages.TransferClaimAdminOnly, "Only administrative claims may be transferred to a player.", null);
		this.addDefault(defaults, Messages.PlayerNotFound2, "No player by that name has logged in recently.", null);
		this.addDefault(defaults, Messages.TransferTopLevel, "Only top level claims (not subdivisions) may be transferred.  Stand outside of the subdivision and try again.", null);
		this.addDefault(defaults, Messages.TransferSuccess, "Claim transferred.", null);
		this.addDefault(defaults, Messages.TrustListNoClaim, "Stand inside the claim you're curious about.", null);
		this.addDefault(defaults, Messages.ClearPermsOwnerOnly, "Only the claim owner can clear all permissions.", null);
		this.addDefault(defaults, Messages.UntrustIndividualAllClaims, "Revoked {0}'s access to ALL your claims.  To set permissions for a single claim, stand inside it.", "0: untrusted player");
		this.addDefault(defaults, Messages.UntrustEveryoneAllClaims, "Cleared permissions in ALL your claims.  To set permissions for a single claim, stand inside it.", null);
		this.addDefault(defaults, Messages.NoPermissionTrust, "You don't have {0}'s permission to manage permissions here.", "0: claim owner's name");
		this.addDefault(defaults, Messages.ClearPermissionsOneClaim, "Cleared permissions in this claim.  To set permission for ALL your claims, stand outside them.", null);
		this.addDefault(defaults, Messages.UntrustIndividualSingleClaim, "Revoked {0}'s access to this claim.  To set permissions for a ALL your claims, stand outside them.", "0: untrusted player");
		this.addDefault(defaults, Messages.OnlySellBlocks, "Claim blocks may only be sold, not purchased.", null);
		this.addDefault(defaults, Messages.BlockPurchaseCost, "Each claim block costs {0}.  Your balance is {1}.", "0: cost of one block; 1: player's account balance");
		this.addDefault(defaults, Messages.ClaimBlockLimit, "You've reached your claim block limit.  You can't purchase more.", null);
		this.addDefault(defaults, Messages.InsufficientFunds, "You don't have enough money.  You need {0}, but you only have {1}.", "0: total cost; 1: player's account balance");
		this.addDefault(defaults, Messages.PurchaseConfirmation, "Withdrew {0} from your account.  You now have {1} available claim blocks.", "0: total cost; 1: remaining blocks");
		this.addDefault(defaults, Messages.OnlyPurchaseBlocks, "Claim blocks may only be purchased, not sold.", null);
		this.addDefault(defaults, Messages.BlockSaleValue, "Each claim block is worth {0}.  You have {1} available for sale.", "0: block value; 1: available blocks");
		this.addDefault(defaults, Messages.NotEnoughBlocksForSale, "You don't have that many claim blocks available for sale.", null);
		this.addDefault(defaults, Messages.BlockSaleConfirmation, "Deposited {0} in your account.  You now have {1} available claim blocks.", "0: amount deposited; 1: remaining blocks");
		this.addDefault(defaults, Messages.AdminClaimsMode, "Administrative claims mode active.  Any claims created will be free and editable by other administrators.", null);
		this.addDefault(defaults, Messages.BasicClaimsMode, "Returned to basic claim creation mode.", null);
		this.addDefault(defaults, Messages.SubdivisionMode, "Subdivision mode.  Use your shovel to create subdivisions in your existing claims.  Use /basicclaims to exit.", null);
		this.addDefault(defaults, Messages.SubdivisionVideo2, "Click for Subdivision Help: {0}", "0:video URL");
		this.addDefault(defaults, Messages.DeleteClaimMissing, "There's no claim here.", null);
		this.addDefault(defaults, Messages.DeletionSubdivisionWarning, "This claim includes subdivisions.  If you're sure you want to delete it, use /DeleteClaim again.", null);
		this.addDefault(defaults, Messages.DeleteSuccess, "Claim deleted.", null);
		this.addDefault(defaults, Messages.CantDeleteAdminClaim, "You don't have permission to delete administrative claims.", null);
		this.addDefault(defaults, Messages.DeleteAllSuccess, "Deleted all of {0}'s claims.", "0: owner's name");
		this.addDefault(defaults, Messages.NoDeletePermission, "You don't have permission to delete claims.", null);
		this.addDefault(defaults, Messages.AllAdminDeleted, "Deleted all administrative claims.", null);
		this.addDefault(defaults, Messages.AdjustBlocksSuccess, "Adjusted {0}'s bonus claim blocks by {1}.  New total bonus blocks: {2}.", "0: player; 1: adjustment; 2: new total");
		this.addDefault(defaults, Messages.NotTrappedHere, "You can build here.  Save yourself.", null);
		this.addDefault(defaults, Messages.RescuePending, "If you stay put for 10 seconds, you'll be teleported out.  Please wait.", null);
		this.addDefault(defaults, Messages.NonSiegeWorld, "Siege is disabled here.", null);
		this.addDefault(defaults, Messages.AlreadySieging, "You're already involved in a siege.", null);
		this.addDefault(defaults, Messages.AlreadyUnderSiegePlayer, "{0} is already under siege.  Join the party!", "0: defending player");
		this.addDefault(defaults, Messages.NotSiegableThere, "{0} isn't protected there.", "0: defending player");
		this.addDefault(defaults, Messages.SiegeTooFarAway, "You're too far away to siege.", null);
		this.addDefault(defaults, Messages.NoSiegeDefenseless, "That player is defenseless.  Go pick on somebody else.", null);
		this.addDefault(defaults, Messages.AlreadyUnderSiegeArea, "That area is already under siege.  Join the party!", null);
		this.addDefault(defaults, Messages.NoSiegeAdminClaim, "Siege is disabled in this area.", null);
		this.addDefault(defaults, Messages.SiegeOnCooldown, "You're still on siege cooldown for this defender or claim.  Find another victim.", null);
		this.addDefault(defaults, Messages.SiegeAlert, "You're under siege!  If you log out now, you will die.  You must defeat {0}, wait for him to give up, or escape.", "0: attacker name");
		this.addDefault(defaults, Messages.SiegeConfirmed, "The siege has begun!  If you log out now, you will die.  You must defeat {0}, chase him away, or admit defeat and walk away.", "0: defender name");
		this.addDefault(defaults, Messages.AbandonClaimMissing, "Stand in the claim you want to delete, or consider /AbandonAllClaims.", null);
		this.addDefault(defaults, Messages.NotYourClaim, "This isn't your claim.", null);
		this.addDefault(defaults, Messages.DeleteTopLevelClaim, "To delete a subdivision, stand inside it.  Otherwise, use /AbandonTopLevelClaim to delete this claim and all subdivisions.", null);		
		this.addDefault(defaults, Messages.AbandonSuccess, "Claim abandoned.  You now have {0} available claim blocks.", "0: remaining claim blocks");
		this.addDefault(defaults, Messages.CantGrantThatPermission, "You can't grant a permission you don't have yourself.", null);
		this.addDefault(defaults, Messages.GrantPermissionNoClaim, "Stand inside the claim where you want to grant permission.", null);
		this.addDefault(defaults, Messages.GrantPermissionConfirmation, "Granted {0} permission to {1} {2}.", "0: target player; 1: permission description; 2: scope (changed claims)");
		this.addDefault(defaults, Messages.ManageUniversalPermissionsInstruction, "To manage permissions for ALL your claims, stand outside them.", null);
		this.addDefault(defaults, Messages.ManageOneClaimPermissionsInstruction, "To manage permissions for a specific claim, stand inside it.", null);
		this.addDefault(defaults, Messages.CollectivePublic, "the public", "as in 'granted the public permission to...'");
		this.addDefault(defaults, Messages.BuildPermission, "build", null);
		this.addDefault(defaults, Messages.ContainersPermission, "access containers and animals", null);
		this.addDefault(defaults, Messages.AccessPermission, "use buttons and levers", null);
		this.addDefault(defaults, Messages.PermissionsPermission, "manage permissions", null);
		this.addDefault(defaults, Messages.LocationCurrentClaim, "in this claim", null);
		this.addDefault(defaults, Messages.LocationAllClaims, "in all your claims", null);
		this.addDefault(defaults, Messages.PvPImmunityStart, "You're protected from attack by other players as long as your inventory is empty.", null);
		this.addDefault(defaults, Messages.SiegeNoDrop, "You can't give away items while involved in a siege.", null);
		this.addDefault(defaults, Messages.DonateItemsInstruction, "To give away the item(s) in your hand, left-click the chest again.", null);
		this.addDefault(defaults, Messages.ChestFull, "This chest is full.", null);
		this.addDefault(defaults, Messages.DonationSuccess, "Item(s) transferred to chest!", null);
		this.addDefault(defaults, Messages.PlayerTooCloseForFire, "You can't start a fire this close to {0}.", "0: other player's name");
		this.addDefault(defaults, Messages.TooDeepToClaim, "This chest can't be protected because it's too deep underground.  Consider moving it.", null);
		this.addDefault(defaults, Messages.ChestClaimConfirmation, "This chest is protected.", null);
		this.addDefault(defaults, Messages.AutomaticClaimNotification, "This chest and nearby blocks are protected from breakage and theft.", null);
		this.addDefault(defaults, Messages.UnprotectedChestWarning, "This chest is NOT protected.  Consider using a golden shovel to expand an existing claim or to create a new one.", null);
		this.addDefault(defaults, Messages.ThatPlayerPvPImmune, "You can't injure defenseless players.", null);
		this.addDefault(defaults, Messages.CantFightWhileImmune, "You can't fight someone while you're protected from PvP.", null);
		this.addDefault(defaults, Messages.NoDamageClaimedEntity, "That belongs to {0}.", "0: owner name");
		this.addDefault(defaults, Messages.ShovelBasicClaimMode, "Shovel returned to basic claims mode.", null);
		this.addDefault(defaults, Messages.RemainingBlocks, "You may claim up to {0} more blocks.", "0: remaining blocks");
		this.addDefault(defaults, Messages.CreativeBasicsVideo2, "Click for Land Claim Help: {0}", "{0}: video URL");
		this.addDefault(defaults, Messages.SurvivalBasicsVideo2, "Click for Land Claim Help: {0}", "{0}: video URL");
		this.addDefault(defaults, Messages.TrappedChatKeyword, "trapped", "When mentioned in chat, players get information about the /trapped command.");
		this.addDefault(defaults, Messages.TrappedInstructions, "Are you trapped in someone's land claim?  Try the /trapped command.", null);
		this.addDefault(defaults, Messages.PvPNoDrop, "You can't drop items while in PvP combat.", null);
		this.addDefault(defaults, Messages.SiegeNoTeleport, "You can't teleport out of a besieged area.", null);
		this.addDefault(defaults, Messages.BesiegedNoTeleport, "You can't teleport into a besieged area.", null);
		this.addDefault(defaults, Messages.SiegeNoContainers, "You can't access containers while involved in a siege.", null);
		this.addDefault(defaults, Messages.PvPNoContainers, "You can't access containers during PvP combat.", null);
		this.addDefault(defaults, Messages.PvPImmunityEnd, "Now you can fight with other players.", null);
		this.addDefault(defaults, Messages.NoBedPermission, "{0} hasn't given you permission to sleep here.", "0: claim owner");
		this.addDefault(defaults, Messages.NoWildernessBuckets, "You may only dump buckets inside your claim(s) or underground.", null);
		this.addDefault(defaults, Messages.NoLavaNearOtherPlayer, "You can't place lava this close to {0}.", "0: nearby player");
		this.addDefault(defaults, Messages.TooFarAway, "That's too far away.", null);
		this.addDefault(defaults, Messages.BlockNotClaimed, "No one has claimed this block.", null);
		this.addDefault(defaults, Messages.BlockClaimed, "That block has been claimed by {0}.", "0: claim owner");
		this.addDefault(defaults, Messages.SiegeNoShovel, "You can't use your shovel tool while involved in a siege.", null);
		this.addDefault(defaults, Messages.RestoreNaturePlayerInChunk, "Unable to restore.  {0} is in that chunk.", "0: nearby player");
		this.addDefault(defaults, Messages.NoCreateClaimPermission, "You don't have permission to claim land.", null);
		this.addDefault(defaults, Messages.ResizeClaimTooSmall, "This new size would be too small.  Claims must be at least {0} x {0}.", "0: minimum claim size");
		this.addDefault(defaults, Messages.ResizeNeedMoreBlocks, "You don't have enough blocks for this size.  You need {0} more.", "0: how many needed");
		this.addDefault(defaults, Messages.ClaimResizeSuccess, "Claim resized.  {0} available claim blocks remaining.", "0: remaining blocks");
		this.addDefault(defaults, Messages.ResizeFailOverlap, "Can't resize here because it would overlap another nearby claim.", null);
		this.addDefault(defaults, Messages.ResizeStart, "Resizing claim.  Use your shovel again at the new location for this corner.", null);
		this.addDefault(defaults, Messages.ResizeFailOverlapSubdivision, "You can't create a subdivision here because it would overlap another subdivision.  Consider /abandonclaim to delete it, or use your shovel at a corner to resize it.", null);
		this.addDefault(defaults, Messages.SubdivisionStart, "Subdivision corner set!  Use your shovel at the location for the opposite corner of this new subdivision.", null);
		this.addDefault(defaults, Messages.CreateSubdivisionOverlap, "Your selected area overlaps another subdivision.", null);
		this.addDefault(defaults, Messages.SubdivisionSuccess, "Subdivision created!  Use /trust to share it with friends.", null);
		this.addDefault(defaults, Messages.CreateClaimFailOverlap, "You can't create a claim here because it would overlap your other claim.  Use /abandonclaim to delete it, or use your shovel at a corner to resize it.", null);
		this.addDefault(defaults, Messages.CreateClaimFailOverlapOtherPlayer, "You can't create a claim here because it would overlap {0}'s claim.", "0: other claim owner");
		this.addDefault(defaults, Messages.ClaimsDisabledWorld, "Land claims are disabled in this world.", null);
		this.addDefault(defaults, Messages.ClaimStart, "Claim corner set!  Use the shovel again at the opposite corner to claim a rectangle of land.  To cancel, put your shovel away.", null);
		this.addDefault(defaults, Messages.NewClaimTooSmall, "This claim would be too small.  Any claim must be at least {0} x {0}.", "0: minimum claim size");
		this.addDefault(defaults, Messages.CreateClaimInsufficientBlocks, "You don't have enough blocks to claim that entire area.  You need {0} more blocks.", "0: additional blocks needed");
		this.addDefault(defaults, Messages.AbandonClaimAdvertisement, "To delete another claim and free up some blocks, use /AbandonClaim.", null);
		this.addDefault(defaults, Messages.CreateClaimFailOverlapShort, "Your selected area overlaps an existing claim.", null);
		this.addDefault(defaults, Messages.CreateClaimSuccess, "Claim created!  Use /trust to share it with friends.", null);
		this.addDefault(defaults, Messages.SiegeWinDoorsOpen, "Congratulations!  Buttons and levers are temporarily unlocked (five minutes).", null);
		this.addDefault(defaults, Messages.RescueAbortedMoved, "You moved!  Rescue cancelled.", null);
		this.addDefault(defaults, Messages.SiegeDoorsLockedEjection, "Looting time is up!  Ejected from the claim.", null);
		this.addDefault(defaults, Messages.NoModifyDuringSiege, "Claims can't be modified while under siege.", null);
		this.addDefault(defaults, Messages.OnlyOwnersModifyClaims, "Only {0} can modify this claim.", "0: owner name");
		this.addDefault(defaults, Messages.NoBuildUnderSiege, "This claim is under siege by {0}.  No one can build here.", "0: attacker name");
		this.addDefault(defaults, Messages.NoBuildPvP, "You can't build in claims during PvP combat.", null);
		this.addDefault(defaults, Messages.NoBuildPermission, "You don't have {0}'s permission to build here.", "0: owner name");
		this.addDefault(defaults, Messages.NonSiegeMaterial, "That material is too tough to break.", null);
		this.addDefault(defaults, Messages.NoOwnerBuildUnderSiege, "You can't make changes while under siege.", null);
		this.addDefault(defaults, Messages.NoAccessPermission, "You don't have {0}'s permission to use that.", "0: owner name.  access permission controls buttons, levers, and beds");
		this.addDefault(defaults, Messages.NoContainersSiege, "This claim is under siege by {0}.  No one can access containers here right now.", "0: attacker name");
		this.addDefault(defaults, Messages.NoContainersPermission, "You don't have {0}'s permission to use that.", "0: owner's name.  containers also include crafting blocks");
		this.addDefault(defaults, Messages.OwnerNameForAdminClaims, "an administrator", "as in 'You don't have an administrator's permission to build here.'");
		this.addDefault(defaults, Messages.ClaimTooSmallForEntities, "This claim isn't big enough for that.  Try enlarging it.", null);
		this.addDefault(defaults, Messages.TooManyEntitiesInClaim, "This claim has too many entities already.  Try enlarging the claim or removing some animals, monsters, paintings, or minecarts.", null);
		this.addDefault(defaults, Messages.YouHaveNoClaims, "You don't have any land claims.", null);
		this.addDefault(defaults, Messages.ConfirmFluidRemoval, "Abandoning this claim will remove lava inside the claim.  If you're sure, use /AbandonClaim again.", null);
		this.addDefault(defaults, Messages.AutoBanNotify, "Auto-banned {0}({1}).  See logs for details.", null);
		this.addDefault(defaults, Messages.AdjustGroupBlocksSuccess, "Adjusted bonus claim blocks for players with the {0} permission by {1}.  New total: {2}.", "0: permission; 1: adjustment amount; 2: new total bonus");
		this.addDefault(defaults, Messages.InvalidPermissionID, "Please specify a player name, or a permission in [brackets].", null);
		this.addDefault(defaults, Messages.UntrustOwnerOnly, "Only {0} can revoke permissions here.", "0: claim owner's name");
		this.addDefault(defaults, Messages.HowToClaimRegex, "(^|.*\\W)how\\W.*\\W(claim|protect|lock)(\\W.*|$)", "This is a Java Regular Expression.  Look it up before editing!  It's used to tell players about the demo video when they ask how to claim land.");
		this.addDefault(defaults, Messages.NoBuildOutsideClaims, "You can't build here unless you claim some land first.", null);
		this.addDefault(defaults, Messages.PlayerOfflineTime, "  Last login: {0} days ago.", "0: number of full days since last login");
		this.addDefault(defaults, Messages.BuildingOutsideClaims, "Other players can build here, too.  Consider creating a land claim to protect your work!", null);
		this.addDefault(defaults, Messages.TrappedWontWorkHere, "Sorry, unable to find a safe location to teleport you to.  Contact an admin, or consider /kill if you don't want to wait.", null);
		this.addDefault(defaults, Messages.CommandBannedInPvP, "You can't use that command while in PvP combat.", null);
		this.addDefault(defaults, Messages.UnclaimCleanupWarning, "The land you've unclaimed may be changed by other players or cleaned up by administrators.  If you've built something there you want to keep, you should reclaim it.", null);
		this.addDefault(defaults, Messages.BuySellNotConfigured, "Sorry, buying anhd selling claim blocks is disabled.", null);
		this.addDefault(defaults, Messages.NoTeleportPvPCombat, "You can't teleport while fighting another player.", null);
		this.addDefault(defaults, Messages.NoTNTDamageAboveSeaLevel, "Warning: TNT will not destroy blocks above sea level.", null);
		this.addDefault(defaults, Messages.NoTNTDamageClaims, "Warning: TNT will not destroy claimed blocks.", null);
		this.addDefault(defaults, Messages.IgnoreClaimsAdvertisement, "To override, use /IgnoreClaims.", null);		
		this.addDefault(defaults, Messages.NoPermissionForCommand, "You don't have permission to do that.", null);
		this.addDefault(defaults, Messages.ClaimsListNoPermission, "You don't have permission to get information about another player's land claims.", null);
		this.addDefault(defaults, Messages.ExplosivesDisabled, "This claim is now protected from explosions.  Use /ClaimExplosions again to disable.", null);
		this.addDefault(defaults, Messages.ExplosivesEnabled, "This claim is now vulnerable to explosions.  Use /ClaimExplosions again to re-enable protections.", null);
		this.addDefault(defaults, Messages.ClaimExplosivesAdvertisement, "To allow explosives to destroy blocks in this land claim, use /ClaimExplosions.", null);
		this.addDefault(defaults, Messages.PlayerInPvPSafeZone, "That player is in a PvP safe zone.", null);		
		this.addDefault(defaults, Messages.NoPistonsOutsideClaims, "Warning: Pistons won't move blocks outside land claims.", null);
		this.addDefault(defaults, Messages.SoftMuted, "Soft-muted {0}.", "0: The changed player's name.");
		this.addDefault(defaults, Messages.UnSoftMuted, "Un-soft-muted {0}.", "0: The changed player's name.");
		this.addDefault(defaults, Messages.DropUnlockAdvertisement, "Other players can't pick up your dropped items unless you /UnlockDrops first.", null);
		this.addDefault(defaults, Messages.PickupBlockedExplanation, "You can't pick this up unless {0} uses /UnlockDrops.", "0: The item stack's owner.");
		this.addDefault(defaults, Messages.DropUnlockConfirmation, "Unlocked your drops.  Other players may now pick them up (until you die again).", null);
		this.addDefault(defaults, Messages.AdvertiseACandACB, "You may use /ACB to give yourself more claim blocks, or /AdminClaims to create a free administrative claim.", null);
		this.addDefault(defaults, Messages.AdvertiseAdminClaims, "You could create an administrative land claim instead using /AdminClaims, which you'd share with other administrators.", null);
		this.addDefault(defaults, Messages.AdvertiseACB, "You may use /ACB to give yourself more claim blocks.", null);
		this.addDefault(defaults, Messages.NotYourPet, "That belongs to {0} until it's given to you with /GivePet.", "0: owner name");
		this.addDefault(defaults, Messages.PetGiveawayConfirmation, "Pet transferred.", null);
		this.addDefault(defaults, Messages.PetTransferCancellation, "Pet giveaway cancelled.", null);
		this.addDefault(defaults, Messages.ReadyToTransferPet, "Ready to transfer!  Right-click the pet you'd like to give away, or cancel with /GivePet cancel.", null);
		this.addDefault(defaults, Messages.AvoidGriefClaimLand, "Prevent grief!  If you claim your land, you will be grief-proof.", null);
		this.addDefault(defaults, Messages.BecomeMayor, "Subdivide your land claim and become a mayor!", null);
		this.addDefault(defaults, Messages.ClaimCreationFailedOverClaimCountLimit, "You've reached your limit on land claims.  Use /AbandonClaim to remove one before creating another.", null);
		this.addDefault(defaults, Messages.CreateClaimFailOverlapRegion, "You can't claim all of this because you're not allowed to build here.", null);
		this.addDefault(defaults, Messages.ResizeFailOverlapRegion, "You don't have permission to build there, so you can't claim that area.", null);
		this.addDefault(defaults, Messages.NoBuildPortalPermission, "You can't use this portal because you don't have {0}'s permission to build an exit portal in the destination land claim.", "0: Destination land claim owner's name.");
		this.addDefault(defaults, Messages.ShowNearbyClaims, "Found {0} land claims.", "0: Number of claims found.");
		this.addDefault(defaults, Messages.NoChatUntilMove, "Sorry, but you have to move a little more before you can chat.  We get lots of spam bots here.  :)", null);
		
		//load the config file
		FileConfiguration config = YamlConfiguration.loadConfiguration(new File(messagesFilePath));
		
		//for each message ID
		for(int i = 0; i < messageIDs.length; i++)
		{
			//get default for this message
			Messages messageID = messageIDs[i];
			CustomizableMessage messageData = defaults.get(messageID.name());
			
			//if default is missing, log an error and use some fake data for now so that the plugin can run
			if(messageData == null)
			{
				GriefPreventionPlus.AddLogEntry("Missing message for " + messageID.name() + ".  Please contact the developer.");
				messageData = new CustomizableMessage(messageID, "Missing message!  ID: " + messageID.name() + ".  Please contact a server admin.", null);
			}
			
			//read the message from the file, use default if necessary
			this.messages[messageID.ordinal()] = config.getString("Messages." + messageID.name() + ".Text", messageData.text);
			config.set("Messages." + messageID.name() + ".Text", this.messages[messageID.ordinal()]);
			
			if(messageData.notes != null)
			{
				messageData.notes = config.getString("Messages." + messageID.name() + ".Notes", messageData.notes);
				config.set("Messages." + messageID.name() + ".Notes", messageData.notes);
			}
		}
		
		//save any changes
		try
		{
			config.save(DataStore.messagesFilePath);
		}
		catch(IOException exception)
		{
			GriefPreventionPlus.AddLogEntry("Unable to write to the configuration file at \"" + DataStore.messagesFilePath + "\"");
		}
		
		defaults.clear();
		System.gc();				
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
		
		return message;		
	}
	
	//used in updating the data schema from 0 to 1.
	//converts player names in a list to uuids
	protected String[] convertNameListToUUIDList(String[] names)
	{
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
	    
	    //return final result of conversion
	    String [] resultArray = new String [resultNames.size()];
	    for(int i = 0; i < resultNames.size(); i++)
	    {
	        resultArray[i] = resultNames.get(i);
	    }
	    
	    return resultArray;
    }
	

	synchronized void close()
	{
		if(this.databaseConnection != null)
		{
			try
			{
				if(!this.databaseConnection.isClosed())
				{
					this.databaseConnection.close();
				}
			}
			catch(SQLException e){};
		}
		
		this.databaseConnection = null;
	}
	
	synchronized void refreshDataConnection() throws SQLException
	{
		if(this.databaseConnection == null || this.databaseConnection.isClosed())
		{
			//set username/pass properties
			Properties connectionProps = new Properties();
			connectionProps.put("user", this.userName);
			connectionProps.put("password", this.password);
			connectionProps.put("autoReconnect", "true");
			connectionProps.put("maxReconnects", "4");
			
			//establish connection
			this.databaseConnection = DriverManager.getConnection(this.databaseUrl, connectionProps); 
		}
	}
	
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

    //gets all the claims "near" a location
	Set<Claim> getNearbyClaims(Location location)
    {
        Set<Claim> claims = new HashSet<Claim>();
        
        Chunk lesserChunk = location.getWorld().getChunkAt(location.subtract(150, 0, 150));
        Chunk greaterChunk = location.getWorld().getChunkAt(location.add(300, 0, 300));
        
        for(int chunk_x = lesserChunk.getX(); chunk_x <= greaterChunk.getX(); chunk_x++)
        {
            for(int chunk_z = lesserChunk.getZ(); chunk_z <= greaterChunk.getZ(); chunk_z++)
            {
                Chunk chunk = location.getWorld().getChunkAt(chunk_x, chunk_z);
                String chunkID = this.getChunkString(chunk.getBlock(0,  0,  0).getLocation());
                ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkID);
                if(claimsInChunk != null)
                {
                    claims.addAll(claimsInChunk);
                }
            }
        }
        
        return claims;
    }
	
	public static UUID toUUID(byte[] bytes) {
	    if (bytes.length != 16) {
	        throw new IllegalArgumentException();
	    }
	    int i = 0;
	    long msl = 0;
	    for (; i < 8; i++) {
	        msl = (msl << 8) | (bytes[i] & 0xFF);
	    }
	    long lsl = 0;
	    for (; i < 16; i++) {
	        lsl = (lsl << 8) | (bytes[i] & 0xFF);
	    }
	    return new UUID(msl, lsl);
	}
	
	public static String UUIDtoHexString(UUID uuid) {
		if (uuid==null) return "0";
		return "0x"+org.apache.commons.lang.StringUtils.leftPad(Long.toHexString(uuid.getMostSignificantBits()), 16, "0")+org.apache.commons.lang.StringUtils.leftPad(Long.toHexString(uuid.getLeastSignificantBits()), 16, "0");
	}
}
