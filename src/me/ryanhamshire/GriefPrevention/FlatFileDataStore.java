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
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import me.ryanhamshire.GriefPrevention.exceptions.WorldNotFoundException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

//manages data stored in the file system
public class FlatFileDataStore extends DataStore {
	public final static String claimDataFolderPath = dataLayerFolderPath + File.separator + "ClaimData";
	public final static String nextClaimIdFilePath = claimDataFolderPath + File.separator + "_nextClaimID";
	public final static String playerDataFolderPath = dataLayerFolderPath + File.separator + "PlayerData";

	private static FileConfiguration getSourceCfg() {
		File f = new File(DataStore.dataLayerFolderPath + "flat.yml");
		if (f.exists())
			return YamlConfiguration.loadConfiguration(f);
		return new YamlConfiguration();
	}

	private static FileConfiguration getTargetCfg() {
		return new YamlConfiguration();
	}

	/**
	 * determines whether there is Player or claims data available. This
	 * determines if the folder exists.
	 * 
	 * @return
	 */
	static boolean hasData() {
		File playerDataFolder = new File(playerDataFolderPath);
		File claimsDataFolder = new File(claimDataFolderPath);

		return playerDataFolder.exists() || claimsDataFolder.exists();
	}

	public FlatFileDataStore() throws Exception {
		this(getSourceCfg(), getTargetCfg());
	}

	// initialization!
	public FlatFileDataStore(FileConfiguration Source, FileConfiguration Target) throws Exception {
		this.initialize(Source, Target);
	}

	@Override
	synchronized void close() {
		super.close();
	}

	// deletes a top level claim from the file system
	@Override
	synchronized void deleteClaimFromSecondaryStorage(Claim claim) {
		String claimID = String.valueOf(claim.id);

		// remove from disk
		File claimFile = new File(claimDataFolderPath + File.separator + claimID);
		if (claimFile.exists()) {
			// System.out.println("Deleting Claim ID #" + claimID);
			// temporary stack trace.
			// try { throw new Exception();}catch(Exception
			// exx){exx.printStackTrace();}
			if (!claimFile.delete()) {
				GriefPrevention.AddLogEntry("Error: Unable to delete claim file \"" + claimFile.getAbsolutePath() + "\".");
			}
		}
	}

	@Override
	public boolean deletePlayerData(String playerName) {
		new File(getPlayerDataFile(playerName)).delete();
		return !hasPlayerData(playerName);
	}

	@Override
	public List<PlayerData> getAllPlayerData() {
		File playerDataFolder = new File(playerDataFolderPath);
		File[] files = playerDataFolder.listFiles();
		ArrayList<PlayerData> Buildresult = new ArrayList<PlayerData>();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (!file.isFile())
				continue; // avoids folders

			// all group data files start with a dollar sign. ignoring those,
			// already handled above
			if (file.getName().startsWith("$"))
				continue;

			String playerName = file.getName();
			Buildresult.add(this.getPlayerData(playerName));

		}
		return Buildresult;
	}

	String getClaimWorld(File SourceFile) {
		// retrieves the World the Claim contained in the given file is a part
		// of.
		// steps: Open, read first line, close, parse, return.
		if (!SourceFile.exists())
			return "";
		try {
			BufferedReader fr = new BufferedReader(new FileReader(SourceFile.getAbsolutePath()));
			String firstline = fr.readLine();
			fr.close();
			String[] splitresult = firstline.split(";");
			return splitresult[0];

		} catch (Exception exx) {
			return "";
		}
	}
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        Debugger.Write("Copying File:" + sourceFile.toString() + " To " + destFile.toString(), Debugger.DebugLevel.Verbose);
        BufferedInputStream bsin = new BufferedInputStream(new FileInputStream(sourceFile));
        BufferedOutputStream bsout = new BufferedOutputStream(new FileOutputStream(destFile));
        int chunksize = 16*1024;
        int readamount=0;
        byte[] buffer = new byte[chunksize];
        try {
            //write in chunks of chunksize.
            while((readamount=bsin.read(buffer,0,chunksize))==chunksize){
                bsout.write(buffer,0,chunksize);
            }
            //write out the remainder.
            bsout.write(buffer,0,readamount);

        }
        catch(Exception exx){
            exx.printStackTrace();
        }
        finally {
            bsin.close();
            bsout.close();
        }
    }


	private String getPlayerDataFile(String sPlayerName) {
        String retval=null;
        try {


        String strPath = playerDataFolderPath + File.separator;

        String scaseSensitive = strPath + sPlayerName;
        String scaseInsensitive = strPath +sPlayerName.toLowerCase();

        File examinepath = new File(strPath);

        File CaseSensitive = null;
        File CaseInsensitive=null;

        //search for file.
        for(File iterate:examinepath.listFiles()){
            //if it equals the name case-sensitively,

            if(iterate.getName().equals(sPlayerName)){
                //assign our case sensitive name

                CaseSensitive=iterate;
            }
            else if(iterate.getName().equalsIgnoreCase(sPlayerName)
                    && iterate.getName().toLowerCase().equals(iterate.getName())){
                //otherwise assign our case insensitive name.

                CaseInsensitive = iterate;
                scaseInsensitive = CaseInsensitive.getName();
            }
        }


        if(CaseSensitive!=null && CaseInsensitive!=null &&  !scaseInsensitive.equals(scaseSensitive)){
            try {

                //delete caseinsensitive file...
                new File(scaseInsensitive).delete();
                //copy case sensitive version in it's place.

           copyFile(CaseSensitive, CaseInsensitive);
               //CaseInsensitive.renameTo(new File(scaseInsensitive + "-backup"));
                //delete the case sensitive file.

                CaseSensitive.delete();

            }
            catch(IOException iox){
                //ignore
            }
        }
        return retval=scaseInsensitive;


        }
        finally {
            Debugger.Write("Flat: Player Data retrieved for " + sPlayerName + ":" + retval, Debugger.DebugLevel.Verbose ) ;
        }
	}

	@Override
	synchronized PlayerData getPlayerDataFromStorage(String playerName) {

        //if the file exists when we check for the specific casing, use that file.
        //On Windows machines the FS will not be case sensitive, however, for *nix based machines
        //the file systems and the file I/O API are case sensitive. We save data lowercase now
        //however previous installations may have upper-cased filenames. Thus we will
        //look for the filename for the file that it would be named if we create the path
        //with a case-insensitive player name.

        File CaseInsensitive = new File(getPlayerDataFile(playerName));
        File playerFile;
		playerFile = CaseInsensitive;
		PlayerData playerData = new PlayerData();
		playerData.playerName = playerName;

		// if it doesn't exist as a file
		if (!playerFile.exists()) {

			// create a file with defaults, but only if the player has been
			// online before.
			Player playerobj = Bukkit.getPlayer(playerName);
			if (playerobj == null) {
				this.savePlayerData(playerName, playerData);
			}

			else if (playerobj.hasPlayedBefore() || playerobj.isOnline()) {
				this.savePlayerData(playerName, playerData);
			}
		}

		// otherwise, read the file
		else {
			BufferedReader inStream = null;
			try {
                inStream = new BufferedReader(new FileReader(playerFile.getAbsolutePath()));

				// first line is last login timestamp
				String lastLoginTimestampString = inStream.readLine();

				// convert that to a date and store it
				DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
				try {
					playerData.lastLogin = dateFormat.parse(lastLoginTimestampString);
				} catch (ParseException parseException) {
					GriefPrevention.AddLogEntry("Unable to load last login for \"" + playerFile.getName() + "\".");
					playerData.lastLogin = null;
				}

				// second line is accrued claim blocks
				String accruedBlocksString = inStream.readLine();

				// convert that to a number and store it
				playerData.accruedClaimBlocks = Integer.parseInt(accruedBlocksString);

				// third line is any bonus claim blocks granted by
				// administrators
				String bonusBlocksString = inStream.readLine();

				// convert that to a number and store it
				playerData.bonusClaimBlocks = Integer.parseInt(bonusBlocksString);

				// fourth line is a double-semicolon-delimited list of claims,
				// which is currently ignored

                try {
				inStream.readLine();
                String playerinventoryclear = inStream.readLine();
                playerData.ClearInventoryOnJoin = Boolean.parseBoolean(playerinventoryclear);
                }
                catch(Exception exx){} //do nothing, seems like there was no value. Oh well.
				inStream.close();
			}

			// if there's any problem with the file's content, log an error
			// message
			catch (Exception e) {
				GriefPrevention.AddLogEntry("Unable to load data for player \"" + playerName + "\": ");

			}

			try {
				if (inStream != null)
					inStream.close();
			} catch (IOException exception) {
			}
		}

		return playerData;
	}

	@Override
	public boolean hasPlayerData(String playerName) {
		// TODO Auto-generated method stub
		return new File(getPlayerDataFile(playerName)).exists();
	}

	@Override
	synchronized void incrementNextClaimID() {
		// increment in memory
		this.nextClaimID++;

		BufferedWriter outStream = null;

		try {
			// open the file and write the new value
			File nextClaimIdFile = new File(nextClaimIdFilePath);
			nextClaimIdFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(nextClaimIdFile));

			outStream.write(String.valueOf(this.nextClaimID));
		}

		// if any problem, log it
		catch (Exception e) {
			GriefPrevention.AddLogEntry("Unexpected exception saving next claim ID: " + e.getMessage());
		}

		// close the file
		try {
			if (outStream != null)
				outStream.close();
		} catch (IOException exception) {
		}
	}

	@Override
	void initialize(ConfigurationSection Source, ConfigurationSection Target) throws Exception {

		// ensure data folders exist
		new File(playerDataFolderPath).mkdirs();
		new File(claimDataFolderPath).mkdirs();

		// load group data into memory
		File playerDataFolder = new File(playerDataFolderPath);
		File[] files = playerDataFolder.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (!file.isFile())
				continue; // avoids folders

			// all group data files start with a dollar sign. ignoring the rest,
			// which are player data files.
			if (!file.getName().startsWith("$"))
				continue;

			String groupName = file.getName().substring(1);
			if (groupName == null || groupName.isEmpty())
				continue; // defensive coding, avoid unlikely cases

			BufferedReader inStream = null;
			try {
				inStream = new BufferedReader(new FileReader(file.getAbsolutePath()));
				String line = inStream.readLine();

				int groupBonusBlocks = Integer.parseInt(line);

				this.permissionToBonusBlocksMap.put(groupName, groupBonusBlocks);
			} catch (Exception e) {
				GriefPrevention.AddLogEntry("Unable to load group bonus block data from file \"" + file.getName() + "\": " + e.getMessage());
			}

			try {
				if (inStream != null)
					inStream.close();
			} catch (IOException exception) {
			}
		}

		// load next claim number from file
		File nextClaimIdFile = new File(nextClaimIdFilePath);
		if (nextClaimIdFile.exists()) {
			BufferedReader inStream = null;
			try {
				inStream = new BufferedReader(new FileReader(nextClaimIdFile.getAbsolutePath()));

				// read the id
				String line = inStream.readLine();

				// try to parse into a long value
				this.nextClaimID = Long.parseLong(line);
			} catch (Exception e) {
			}

			try {
				if (inStream != null)
					inStream.close();
			} catch (IOException exception) {
			}
		}

		// load claims data into memory
		// get a list of all the files in the claims data folder
		File claimDataFolder = new File(claimDataFolderPath);
		files = claimDataFolder.listFiles();

		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) // avoids folders
			{
				// skip any file starting with an underscore, to avoid the
				// _nextClaimID file.
				if (files[i].getName().startsWith("_"))
					continue;

				/*
				 * try { claimID = Long.parseLong(files[i].getName()); }
				 * 
				 * //because some older versions used a different file name
				 * pattern before claim IDs were introduced, //those files need
				 * to be "converted" by renaming them to a unique ID
				 * catch(Exception e) { claimID = this.nextClaimID;
				 * this.incrementNextClaimID(); File newFile = new
				 * File(claimDataFolderPath + File.separator +
				 * String.valueOf(this.nextClaimID));
				 * files[i].renameTo(newFile); files[i] = newFile; }
				 */
				// readClaim(files[i]);
			}
		}

		super.initialize(Source, Target);
	}

	synchronized void migrateData(DataStore targetStore) {
		ForceLoadAllClaims(this);
        targetStore.ClearInventoryOnJoinPlayers= ClearInventoryOnJoinPlayers;
		// migrate claims
		for (Claim c : this.claims) {
			GriefPrevention.AddLogEntry("Migrating Claim #" + c.getID());
			targetStore.addClaim(c);
		}

		// migrate groups
		Iterator<String> groupNamesEnumerator = this.permissionToBonusBlocksMap.keySet().iterator();
		while (groupNamesEnumerator.hasNext()) {
			String groupName = groupNamesEnumerator.next();
			targetStore.saveGroupBonusBlocks(groupName, this.permissionToBonusBlocksMap.get(groupName));
		}

		// migrate players
		for (PlayerData pdata : getAllPlayerData()) {

			targetStore.playerNameToPlayerDataMap.put(pdata.playerName, pdata);
            targetStore.savePlayerData(pdata.playerName,pdata);

		}

		// migrate next claim ID
		if (this.nextClaimID > targetStore.nextClaimID) {
			targetStore.setNextClaimID(this.nextClaimID);
		}

		// rename player and claim data folders so the migration won't run again
		int i = 0;
		File claimsBackupFolder;
		File playersBackupFolder;
		do {
			String claimsFolderBackupPath = claimDataFolderPath;
			if (i > 0)
				claimsFolderBackupPath += String.valueOf(i);
			claimsBackupFolder = new File(claimsFolderBackupPath);

			String playersFolderBackupPath = playerDataFolderPath;
			if (i > 0)
				playersFolderBackupPath += String.valueOf(i);
			playersBackupFolder = new File(playersFolderBackupPath);
			i++;
		} while (claimsBackupFolder.exists() || playersBackupFolder.exists());

		File claimsFolder = new File(claimDataFolderPath);
		File playersFolder = new File(playerDataFolderPath);

		claimsFolder.renameTo(claimsBackupFolder);
		playersFolder.renameTo(playersBackupFolder);

		GriefPrevention.AddLogEntry("Backed your file system data up to " + claimsBackupFolder.getName() + " and " + playersBackupFolder.getName() + ".");
		GriefPrevention.AddLogEntry("If your migration encountered any problems, you can restore those data with a quick copy/paste.");
		GriefPrevention.AddLogEntry("When you're satisfied that all your data have been safely migrated, consider deleting those folders.");
	}

	void readClaim(File SourceFile) {
		// reads a single Claim.
		// loads this claim from the given file.
		if (SourceFile.getPath().startsWith("_"))
			return;
		Long claimID;
		try {
			claimID = Long.parseLong(SourceFile.getName());
		}

		// because some older versions used a different file name pattern before
		// claim IDs were introduced,
		// those files need to be "converted" by renaming them to a unique ID
		catch (Exception e) {
			claimID = this.nextClaimID;
			this.incrementNextClaimID();
			File newFile = new File(claimDataFolderPath + File.separator + String.valueOf(this.nextClaimID));
			SourceFile.renameTo(newFile);
			SourceFile = newFile;
		}
		BufferedReader inStream = null;
		try {
			Claim topLevelClaim = null;
			FileReader fr = new FileReader(SourceFile.getAbsolutePath());
			inStream = new BufferedReader(fr);
			String line = inStream.readLine();
            String subclaimtext = null;
			while (line != null) {

				if (line.toUpperCase().startsWith("SUB:")) {
                    subclaimtext=line.substring(4);
					line = inStream.readLine(); // read to the next line.
				}
				// first line is lesser boundary corner location
				String splitentry = line.split(";")[0];
				// if the world doesn't exist yet, we need to create a
				// DeferredWorldClaim instance and
				// add it to the dataStore hashMap.
				Location lesserBoundaryCorner = null;
				Location greaterBoundaryCorner = null;
				lesserBoundaryCorner = this.locationFromString(line);
				// second line is greater boundary corner location
				line = inStream.readLine();
				greaterBoundaryCorner = this.locationFromString(line);

				// third line is owner name
				line = inStream.readLine();
				String ownerName = line;

				// is there PlayerData for this gai?

				if (!hasPlayerData(ownerName) && GriefPrevention.instance.config_claims_deleteclaimswithunrecognizedowners) {
					// PlayerData not found, don't load this claim.
					GriefPrevention.AddLogEntry("discarded Claim belonging to " + ownerName + " Because there is no PlayerData for that Player.");
					return;
				}

				// fourth line is list of builders
				line = inStream.readLine();
				String[] builderNames = line.split(";");

				// fifth line is list of players who can access containers
				line = inStream.readLine();
				String[] containerNames = line.split(";");

				// sixth line is list of players who can use buttons and
				// switches
				line = inStream.readLine();
				String[] accessorNames = line.split(";");

				// seventh line is list of players who can grant permissions
				line = inStream.readLine();
				if (line == null)
					line = "";
				String[] managerNames = line.split(";");

				// Eighth line either contains whether the claim can ever be
				// deleted, or the divider for the subclaims
				boolean neverdelete = false;
				line = inStream.readLine();
				if (line == null)
					line = "";
				if (!line.contains("==========")) {
					neverdelete = Boolean.parseBoolean(line);
				}

				// Sub claims below this line
				while (line != null && !line.contains("=========="))
					line = inStream.readLine();

				// build a claim instance from those data
				// if this is the first claim loaded from this file, it's the
				// top level claim

				if (topLevelClaim == null) {
					// instantiate
					topLevelClaim = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, ownerName, builderNames, containerNames, accessorNames, managerNames, claimID, neverdelete);

					// search for another claim overlapping this one

					Claim conflictClaim = this.getClaimAt(topLevelClaim.lesserBoundaryCorner, true);

					// if there is such a claim, delete this file and move on to
					// the next
					if (conflictClaim != null) {
						// System.out.println("Deleting Claim File:" +
						// SourceFile.getAbsolutePath() +
						// " as it is overlapped by ID #" +
						// conflictClaim.getID());
						inStream.close();
						SourceFile.delete();
						line = null;
						continue;
					}

					// otherwise, add this claim to the claims collection
					else {
						topLevelClaim.modifiedDate = new Date(SourceFile.lastModified());

						addClaim(topLevelClaim);

						topLevelClaim.inDataStore = true;
					}
				}

				// otherwise there's already a top level claim, so this must be
				// a subdivision of that top level claim
				else {

					// if it starts with "sub:" then it is a subid.

					// as such, try to read in the subclaim ID.

					Claim subdivision = new Claim(lesserBoundaryCorner, greaterBoundaryCorner,topLevelClaim.getOwnerName() , builderNames, containerNames, accessorNames, managerNames, claimID, neverdelete);
                    try {subdivision.id = Long.parseLong(subclaimtext);}
                    catch(NumberFormatException nfe){
                        subdivision.id=new Long(-1);
                    }
					subdivision.modifiedDate = new Date(SourceFile.lastModified());
					subdivision.parent = topLevelClaim;
					topLevelClaim.children.add(subdivision);
					subdivision.inDataStore = true;
				}

				// move up to the first line in the next subdivision
				line = inStream.readLine();
			}

			inStream.close();
		}
		// We don't need to log any additional error messages for this error.
		catch (WorldNotFoundException e) {
			// Nothing to do here.
		}

		// if there's any problem with the file's content, log an error message
		// and skip it
		catch (Exception e) {
			GriefPrevention.AddLogEntry("Unable to load data for claim \"" + SourceFile.getName() + "\": " + e.getClass().getName() + "-" + e.getMessage());
			e.printStackTrace();
		}

		try {
			if (inStream != null)
				inStream.close();
		} catch (IOException exception) {
		}

	}

	// grants a group (players with a specific permission) bonus claim blocks as
	// long as they're still members of the group
	@Override
	synchronized void saveGroupBonusBlocks(String groupName, int currentValue) {
		// write changes to file to ensure they don't get lost
		BufferedWriter outStream = null;
		try {
			// open the group's file
			File groupDataFile = new File(playerDataFolderPath + File.separator + "$" + groupName);
			groupDataFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(groupDataFile));

			// first line is number of bonus blocks
			outStream.write(String.valueOf(currentValue));
			outStream.newLine();
		}

		// if any problem, log it
		catch (Exception e) {
			GriefPrevention.AddLogEntry("Unexpected exception saving data for group \"" + groupName + "\": " + e.getMessage());
		}

		try {
			// close the file
			if (outStream != null) {
				outStream.close();
			}
		} catch (IOException exception) {
		}
	}

	// saves changes to player data. MUST be called after you're done making
	// changes, otherwise a reload will lose them
	@Override
	synchronized public void savePlayerData(String playerName, PlayerData playerData) {
		// never save data for the "administrative" account. an empty string for
		// claim owner indicates administrative account
		if (playerName.length() == 0)
			return;

		BufferedWriter outStream = null;
		try {
			// open the player's file
			File playerDataFile = new File(playerDataFolderPath + File.separator + playerName.toLowerCase());
			playerDataFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(playerDataFile));

			// first line is last login timestamp
			if (playerData.lastLogin == null)
				playerData.lastLogin = new Date();
			DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
			outStream.write(dateFormat.format(playerData.lastLogin));
			outStream.newLine();

			// second line is accrued claim blocks
			outStream.write(String.valueOf(playerData.accruedClaimBlocks));
			outStream.newLine();

			// third line is bonus claim blocks
			outStream.write(String.valueOf(playerData.bonusClaimBlocks));
			outStream.newLine();

			// fourth line is a double-semicolon-delimited list of claims
			/*if (playerData.claims.size() > 0) {
				outStream.write(this.locationToString(playerData.claims.get(0).getLesserBoundaryCorner()));
				for (int i = 1; i < playerData.claims.size(); i++) {
					outStream.write(";;" + this.locationToString(playerData.claims.get(i).getLesserBoundaryCorner()));
				}
			} */

            //write out wether the player's inventory needs to be cleared on join.
            outStream.newLine();
            outStream.write(String.valueOf(playerData.ClearInventoryOnJoin));
			outStream.newLine();
		}

		// if any problem, log it
		catch (Exception e) {
			GriefPrevention.AddLogEntry("GriefPrevention: Unexpected exception saving data for player \"" + playerName + "\": " + e.getMessage());
		}

		try {
			// close the file
			if (outStream != null) {
				outStream.close();
			}
		} catch (IOException exception) {
		}
	}

	public long getNextClaimID(){
		return this.nextClaimID;
	}
	@Override
	public void setNextClaimID(long nextClaimID2) {
		// TODO Auto-generated method stub

		File nextClaimIdFile = new File(nextClaimIdFilePath);
		if (nextClaimIdFile.exists()) {
			BufferedWriter outStream = null;
			try {
				outStream = new BufferedWriter(new FileWriter(nextClaimIdFile.getAbsolutePath()));

				// read the id
				outStream.write(Long.toString(nextClaimID2));

				// try to parse into a long value
				this.nextClaimID = nextClaimID2;
			} catch (Exception e) {
			}

			try {
				if (outStream != null)
					outStream.close();
			} catch (IOException exception) {
			}
		}

	}

	@Override
	void WorldLoaded(World loaded) {

		int claimsread = 0;
		File claimDataFolder = new File(claimDataFolderPath);
		File[] files = claimDataFolder.listFiles();
		for (File iterate : files) {

			if (getClaimWorld(iterate).equals(loaded.getName())) {
				claimsread++;
				readClaim(iterate);
			}

		}
		// System.out.println("Read in " + claimsread + " Claims for world:" +
		// loaded.getName());

	}

	
	synchronized public static void migrateData(DataStore Source,DataStore Target){
		
		//before we migrate, we want to FORCE all worlds to be loaded, to make sure all data is in memory to be transferred.
		//we do this by iterating through the names of configuration files and forcing all those worlds to exist.
		
		
		
		
		
	}
	
	// actually writes claim data to an output stream
	synchronized private void writeClaimData(Claim claim, BufferedWriter outStream) throws IOException {
		if (claim.parent != null) {
            if(claim.id<0-1) claim.id = getNextClaimID();
			Long ChildID = claim.getID();
			outStream.write("SUB:" + String.valueOf(ChildID));
			outStream.newLine();

		}
		// first line is lesser boundary corner location
		outStream.write(this.locationToString(claim.getLesserBoundaryCorner()));
		outStream.newLine();

		// second line is greater boundary corner location
		outStream.write(this.locationToString(claim.getGreaterBoundaryCorner()));
		outStream.newLine();

		// third line is owner name
		outStream.write(claim.claimOwnerName);
		outStream.newLine();

		ArrayList<String> builders = new ArrayList<String>();
		ArrayList<String> containers = new ArrayList<String>();
		ArrayList<String> accessors = new ArrayList<String>();
		ArrayList<String> managers = new ArrayList<String>();
		
		claim.getPermissions(builders, containers, accessors, managers);
		
		// fourth line is list of players with build permission
		for (int i = 0; i < builders.size(); i++) {
			outStream.write(builders.get(i) + ";");
		}
		outStream.newLine();

		// fifth line is list of players with container permission
		for (int i = 0; i < containers.size(); i++) {
			outStream.write(containers.get(i) + ";");
		}
		outStream.newLine();

		// sixth line is list of players with access permission
		for (int i = 0; i < accessors.size(); i++) {
			outStream.write(accessors.get(i) + ";");
		}
		outStream.newLine();

		// seventh line is list of players who may grant permissions for others
		for (int i = 0; i < managers.size(); i++) {
			outStream.write(managers.get(i) + ";");
		}
		outStream.newLine();

		// eighth line has the never delete variable
		outStream.write(Boolean.toString(claim.neverdelete));
		outStream.newLine();

		// cap each claim with "=========="
		outStream.write("==========");
		outStream.newLine();
	}

	@Override
	synchronized void writeClaimToStorage(Claim claim) {
        if(claim.id<0) claim.id = getNextClaimID();
		String claimID = String.valueOf(claim.id);

		BufferedWriter outStream = null;

		try {
			// open the claim's file
			File claimFile = new File(claimDataFolderPath + File.separator + claimID);
			claimFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(claimFile));

			// write top level claim data to the file
			this.writeClaimData(claim, outStream);

			// for each subdivision
			for (int i = 0; i < claim.children.size(); i++) {

				// write the subdivision's data to the file
				// write it's unique ID.
				Claim childclaim = claim.children.get(i);
				// Long childid = childclaim.getSubClaimID();
				// childid = childid==null?childclaim.getID():childid;
				// System.out.println("Attempting to write child claim: SubID: "
				// + childid);

				this.writeClaimData(childclaim, outStream);
				// outStream.write("Sub:" + String.valueOf(childid));
			}
		}

		// if any problem, log it
		catch (Exception e) {
			GriefPrevention.AddLogEntry("Unexpected exception saving data for claim \"" + claimID + "\": " + e.getMessage());
		}

		// close the file
		try {
			if (outStream != null)
				outStream.close();
		} catch (IOException exception) {
		}
	}

}
