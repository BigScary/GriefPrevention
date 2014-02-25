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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import me.ryanhamshire.GriefPrevention.Debugger.DebugLevel;
import me.ryanhamshire.GriefPrevention.exceptions.WorldNotFoundException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

//manages data stored in the file system
public class DatabaseDataStore extends DataStore {
	private Connection databaseConnection = null;

	private String databaseUrl;
	private String password;
	private String userName;

	public DatabaseDataStore(ConfigurationSection Source, ConfigurationSection Target) throws Exception {
		initialize(Source, Target);

	}

	@Override
	synchronized void close() {
		// System.out.println("DatabaseStore closing: Claims #" +
		// this.claims.size());
		super.close();
		if (this.databaseConnection != null) {
			try {
				if (!this.databaseConnection.isClosed()) {
					this.databaseConnection.close();
				}
			} catch (SQLException e) {
			}
			;
		}

		this.databaseConnection = null;
	}

	// deletes a top level claim from the database
	@Override
	synchronized void deleteClaimFromSecondaryStorage(Claim claim) {
		try {
			this.refreshDataConnection();

			Statement statement = this.databaseConnection.createStatement();
			statement.execute("DELETE FROM griefprevention_claimdata WHERE id=" + claim.id + ";");
			statement.execute("DELETE FROM griefprevention_claimdata WHERE parentid=" + claim.id + ";");
		} catch (SQLException e) {
			GriefPrevention.AddLogEntry("Unable to delete data for claim at " + this.locationToString(claim.lesserBoundaryCorner) + ".  Details:");
			GriefPrevention.AddLogEntry(e.getMessage());
		}
	}

	@Override
	public boolean deletePlayerData(String playerName) {
		// TODO Auto-generated method stub
		try {
			this.refreshDataConnection();
			Statement statement = this.databaseConnection.createStatement();
			return statement.execute("DELETE FROM griefprevention_playerdata WHERE name='" + playerName + "';");
		} catch (Exception exx) {
			exx.printStackTrace();
		}

		return true;

	}

	@Override
	public List<PlayerData> getAllPlayerData() {
		// TODO Auto-generated method stub
		super.ForceLoadAllClaims(this);
		List<PlayerData> generateList = new ArrayList<PlayerData>();
		try {

			this.refreshDataConnection();
			Statement statement = databaseConnection.createStatement();
			ResultSet gotplayers = statement.executeQuery("SELECT * FROM griefprevention_playerdata");
			while (gotplayers.next()) {
				// name,lastlogin,accruedblocks,bonusblocks
				String pname = gotplayers.getString("name");
				Date lastlog = gotplayers.getTimestamp("lastlogin");
				int accrued = gotplayers.getInt("accruedblocks");
				int bonus = gotplayers.getInt("bonusblocks");
				PlayerData pd = new PlayerData();
				pd.playerName = pname;
				pd.lastLogin = lastlog;
				pd.accruedClaimBlocks = accrued;
				pd.bonusClaimBlocks = bonus;
				generateList.add(pd);

			}
			return generateList;

		} catch (Exception exx) {

		}
		return new ArrayList<PlayerData>();
	}

	@Override
	synchronized PlayerData getPlayerDataFromStorage(String playerName) {
		PlayerData playerData = new PlayerData();
		playerData.playerName = playerName;

		try {
			this.refreshDataConnection();

			Statement statement = this.databaseConnection.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM griefprevention_playerdata WHERE name='" + playerName + "';");

			// if there's no data for this player, create it with defaults
			if (!results.next()) {
				this.savePlayerData(playerName, playerData);
			}
			//
			// otherwise, just read from the database
			else {
				playerData.lastLogin = results.getTimestamp("lastlogin");
				playerData.accruedClaimBlocks = results.getInt("accruedblocks");
				playerData.bonusClaimBlocks = results.getInt("bonusblocks");
                playerData.ClearInventoryOnJoin = results.getBoolean("clearonjoin");
			}
		} catch (SQLException e) {
			GriefPrevention.AddLogEntry("Unable to retrieve data for player " + playerName + ".  Details:");
			GriefPrevention.AddLogEntry(e.getMessage());
		}

		return playerData;
	}

	@Override
	public boolean hasPlayerData(String pName) {
		try {
			this.refreshDataConnection();
			Statement statement = this.databaseConnection.createStatement();
			ResultSet results = statement.executeQuery("SELECT COUNT(*) FROM griefprevention_playerdata WHERE name='" + pName + "';");
			results.next();
			int gotcount = results.getInt("Count");
			if (gotcount > 0)
				return true;

		} catch (SQLException e) {

		}
		return false;
	}

	@Override
	synchronized void incrementNextClaimID() {
		this.setNextClaimID(this.nextClaimID + 1);
	}

	@Override
	void initialize(ConfigurationSection Source, ConfigurationSection Target) throws Exception {

		// "jdbc:mysql://<hostname>/database"
		String FormatString = "jdbc:mysql://%s:%s/%s";



			String grabhost = Source.getString("Host", "localhost");
            String grabport = Source.getString("Port","3306");
			String grabdbname = Source.getString("Database", "GriefPrevention");

			databaseUrl = String.format(FormatString, grabhost,grabport, grabdbname);
			Target.set("Host", grabhost);
            Target.set("Port",grabport);
			Target.set("Database", grabdbname);



		userName = Source.getString("Username", "");
		this.password = Source.getString("Password", "");

		//Target.set("URL", databaseUrl);
		Target.set("Username", userName);
		Target.set("Password", password);

		try {
			// load the java driver for mySQL
			Class.forName("com.mysql.jdbc.Driver");
		} catch (Exception e) {
			GriefPrevention.AddLogEntry("ERROR: Unable to load Java's mySQL database driver.  Check to make sure you've installed it properly.");
			e.printStackTrace();
            throw e;
		}

		try {
			this.refreshDataConnection();
		} catch (Exception e2) {
			GriefPrevention.AddLogEntry("ERROR: Unable to connect to database.  Check your config file settings.");
            e2.printStackTrace();
            throw e2;
		}
		GriefPrevention.AddLogEntry("Java MySQL driver loaded and connection established.");
		try {
			// ensure the data tables exist
			Statement statement = databaseConnection.createStatement();

			if (this.databaseUrl.startsWith("jdbc:postgresql")) {
				statement.execute("CREATE TABLE IF NOT EXISTS griefprevention_nextclaimid (nextid INTEGER);");

				statement.execute("CREATE TABLE IF NOT EXISTS griefprevention_claimdata (id INTEGER, owner VARCHAR(50), lessercorner VARCHAR(100), greatercorner VARCHAR(100), builders TEXT, containers TEXT, accessors TEXT, managers TEXT, parentid INTEGER, neverdelete BOOLEAN NOT NULL DEFAULT false);");

				statement.execute("CREATE TABLE IF NOT EXISTS griefprevention_playerdata (name VARCHAR(50), lastlogin TIMESTAMP WITH TIME ZONE, accruedblocks INTEGER, bonusblocks INTEGER);");
			} else {
				statement.execute("CREATE TABLE IF NOT EXISTS griefprevention_nextclaimid (nextid INT(15));");

				statement.execute("CREATE TABLE IF NOT EXISTS griefprevention_claimdata (id INT(15), owner VARCHAR(50), lessercorner VARCHAR(100), greatercorner VARCHAR(100), builders VARCHAR(1000), containers VARCHAR(1000), accessors VARCHAR(1000), managers VARCHAR(1000), parentid INT(15), neverdelete BOOLEAN NOT NULL DEFAULT 0);");
				// in case it's a previous schema, change it to auto_increment.
				// statement.execute("ALTER TABLE griefprevention_claimdata modify column id INT AUTO_INCREMENT");

				// IF EXISTS(SELECT * FROM sys.indexes WHERE object_id =
				// object_id('schema.tablename') AND NAME ='indexname')
				// DROP INDEX indexname ON SCHEMA.tablename;

				statement.execute("CREATE TABLE IF NOT EXISTS griefprevention_playerdata (name VARCHAR(50), lastlogin DATETIME, accruedblocks INT(15), bonusblocks INT(15));");

                ResultSet tempresult = statement.executeQuery("SHOW COLUMNS FROM griefprevention_playerdata LIKE 'clearonjoin';");
                if(!tempresult.next()){
                    statement.execute("ALTER TABLE griefprevention_playerdata ADD clearonjoin BOOLEAN NOT NULL DEFAULT 0;");
                }
				tempresult = statement.executeQuery("SHOW COLUMNS FROM griefprevention_claimdata LIKE 'neverdelete';");
				if (!tempresult.next()) {
					statement.execute("ALTER TABLE griefprevention_claimdata ADD neverdelete BOOLEAN NOT NULL DEFAULT 0;");
				}
				
				
			}
		} catch (Exception e3) {
			GriefPrevention.AddLogEntry("ERROR: Unable to create the necessary database table.  Details:");
			GriefPrevention.AddLogEntry(e3.getMessage());
            e3.printStackTrace();
            throw e3;
		}

		// load group data into memory
		Statement statement = databaseConnection.createStatement();
		ResultSet results = statement.executeQuery("SELECT * FROM griefprevention_playerdata;");

		while (results.next()) {
			String name = results.getString("name");

			// ignore non-groups. all group names start with a dollar sign.
			if (!name.startsWith("$"))
				continue;

			String groupName = name.substring(1);
			if (groupName == null || groupName.isEmpty())
				continue; // defensive coding, avoid unlikely cases

			int groupBonusBlocks = results.getInt("bonusblocks");

			this.permissionToBonusBlocksMap.put(groupName, groupBonusBlocks);
		}

		// load next claim number into memory
		results = statement.executeQuery("SELECT * FROM griefprevention_nextclaimid;");

		// if there's nothing yet, add it
		if (!results.next()) {
			statement.execute("INSERT INTO griefprevention_nextclaimid VALUES(0);");
			this.nextClaimID = (long) 0;
		}

		// otherwise load it
		else {
			this.nextClaimID = results.getLong("nextid");
		}

		// load claims data into memory
		//results = statement.executeQuery("SELECT * FROM griefprevention_claimdata;");

		super.initialize(Source, Target);
	}

	private void refreshDataConnection() throws SQLException {
		if (this.databaseConnection == null || this.databaseConnection.isClosed()) {
			// set username/pass properties
			Properties connectionProps = new Properties();
			connectionProps.put("user", this.userName);
			connectionProps.put("password", this.password);

			// establish connection
			this.databaseConnection = DriverManager.getConnection(this.databaseUrl, connectionProps);
		}
	}

	// updates the database with a group's bonus blocks
	@Override
	synchronized void saveGroupBonusBlocks(String groupName, int currentValue) {
		// group bonus blocks are stored in the player data table, with player
		// name = $groupName
		String playerName = "$" + groupName;
		PlayerData playerData = new PlayerData();
		playerData.bonusClaimBlocks = currentValue;

		this.savePlayerData(playerName, playerData);
	}

	// saves changes to player data. MUST be called after you're done making
	// changes, otherwise a reload will lose them
	@Override
	synchronized public void savePlayerData(String playerName, PlayerData playerData) {
		// never save data for the "administrative" account. an empty string for
		// player name indicates administrative account

		if (playerName.length() == 0)
			return;

		try {
			this.refreshDataConnection();

			SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String dateString = sqlFormat.format(playerData.lastLogin);
            String Existsquery = "SELECT true from griefprevention_playerdata WHERE name='%1$s'";
            String UpdateFormat = "UPDATE griefprevention_playerdata SET name='%1$s',lastlogin='%2$s',accruedblocks='%3$s',bonusblocks='%4$s',clearonjoin='%5$s' WHERE name='%1$s'";
            String insertFormat =
                    "INSERT INTO griefprevention_playerdata (name,lastlogin,accruedblocks,bonusblocks,clearonjoin)  VALUES " +
                            "('%1$s', '%2$s', '%3$s','%4$s','%5$s');";

            Statement statement = databaseConnection.createStatement();
			//statement.execute("DELETE FROM griefprevention_playerdata WHERE name='" + playerName + "';");
            String buildexists = String.format(Existsquery,playerName);
            String useSaveQuery = null;
            ResultSet rs = statement.executeQuery(buildexists);
            if(rs.next()){
                useSaveQuery = String.format(UpdateFormat,playerName,dateString,playerData.accruedClaimBlocks,playerData.bonusClaimBlocks,playerData.ClearInventoryOnJoin?1:0);
            }
            else {
                useSaveQuery = String.format(insertFormat,playerName,dateString,playerData.accruedClaimBlocks,playerData.bonusClaimBlocks,playerData.ClearInventoryOnJoin?1:0);
            }

             statement.execute(useSaveQuery);


		} catch (SQLException e) {
			GriefPrevention.AddLogEntry("Unable to save data for player " + playerName + ".  Details:");
			e.printStackTrace();
		}
	}

	public synchronized long getNextClaimID(){
		
		return this.nextClaimID;
		
	}
	// sets the next claim ID. used by incrementNextClaimID() above, and also
	// while migrating data from a flat file data store
	@Override
	public synchronized void setNextClaimID(long nextID) {
		this.nextClaimID = nextID;

		try {
			this.refreshDataConnection();

			Statement statement = databaseConnection.createStatement();
			statement.execute("DELETE FROM griefprevention_nextclaimid;");
			statement.execute("INSERT INTO griefprevention_nextclaimid VALUES (" + nextID + ");");
		} catch (SQLException e) {
			GriefPrevention.AddLogEntry("Unable to set next claim ID to " + nextID + ".  Details:");
			GriefPrevention.AddLogEntry(e.getMessage());
		}
	}

	@Override
	void WorldLoaded(World loading) {
		try {
            Debugger.Write("Database:Loading claims in world:" + loading.getName(),DebugLevel.Verbose);
			Statement statement = databaseConnection.createStatement();

            ResultSet results = statement.executeQuery("SELECT * FROM griefprevention_claimdata where parentid=-1 AND lessercorner LIKE \"" + loading.getName() + ";%\";");
			ArrayList<Claim> claimsToRemove = new ArrayList<Claim>();

			while (results.next()) {
				try {
					// skip subdivisions

					long parentId = results.getLong("parentid");
					if (parentId != -1)
						continue;

					long claimID = results.getLong("id");

					String lesserCornerString = results.getString("lessercorner");

					String greaterCornerString = results.getString("greatercorner");

					String ownerName = results.getString("owner");

					String buildersString = results.getString("builders");
					String[] builderNames = buildersString.split(";");

					String containersString = results.getString("containers");
					String[] containerNames = containersString.split(";");

					String accessorsString = results.getString("accessors");
					String[] accessorNames = accessorsString.split(";");

					String managersString = results.getString("managers");
					String[] managerNames = managersString.split(";");

					boolean neverdelete = results.getBoolean("neverdelete");

					Location lesserBoundaryCorner = this.locationFromString(lesserCornerString);
					Location greaterBoundaryCorner = this.locationFromString(greaterCornerString);

					
					Claim topLevelClaim = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, ownerName, builderNames, containerNames, accessorNames, managerNames, claimID, neverdelete);

					// search for another claim overlapping this one
					Claim conflictClaim = this.getClaimAt(topLevelClaim.lesserBoundaryCorner, true);

					// if there is such a claim, mark it for later removal
					if (conflictClaim != null) {
						claimsToRemove.add(conflictClaim);
						continue;
					}

					// otherwise, add this claim to the claims collection
					else {
                        addClaim(topLevelClaim);
						topLevelClaim.inDataStore = true;
					}

					// look for any subdivisions for this claim
					Statement statement2 = this.databaseConnection.createStatement();

					ResultSet childResults = statement2.executeQuery("SELECT * FROM griefprevention_claimdata WHERE parentid=" + topLevelClaim.id + ";");

					while (childResults.next()) {

						lesserCornerString = childResults.getString("lessercorner");
						lesserBoundaryCorner = this.locationFromString(lesserCornerString);
						Long subid = childResults.getLong("id");
						greaterCornerString = childResults.getString("greatercorner");
						greaterBoundaryCorner = this.locationFromString(greaterCornerString);

						buildersString = childResults.getString("builders");
						builderNames = buildersString.split(";");

						containersString = childResults.getString("containers");
						containerNames = containersString.split(";");

						accessorsString = childResults.getString("accessors");
						accessorNames = accessorsString.split(";");

						managersString = childResults.getString("managers");
						managerNames = managersString.split(";");

						neverdelete = results.getBoolean("neverdelete");

						Claim childClaim = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, ownerName, builderNames, containerNames, accessorNames, managerNames, subid, neverdelete);

						// add this claim to the list of children of the current
						// top level claim
						childClaim.parent = topLevelClaim;
						topLevelClaim.children.add(childClaim);

						childClaim.inDataStore = true;
					}
				} catch (SQLException e) {
					GriefPrevention.AddLogEntry("Unable to load a claim.  Details: " + e.getMessage() + " ... " + results.toString());
					e.printStackTrace();
				} catch (WorldNotFoundException e) {
					// We don't need to worry about this exception.
					// This is just here to catch it so that the plugin
					// can load without erroring out.
				}
			}

			for (int i = 0; i < claimsToRemove.size(); i++) {
				this.deleteClaimFromSecondaryStorage(claimsToRemove.get(i));
			}

		} catch (Exception exx) {
			System.out.println("Exception from databaseDataStore handling of WorldLoad-");
			exx.printStackTrace();
		}

	}

	// actually writes claim data to the database
	synchronized private void writeClaimData(Claim claim) throws SQLException {
		String lesserCornerString = this.locationToString(claim.getLesserBoundaryCorner());
		String greaterCornerString = this.locationToString(claim.getGreaterBoundaryCorner());
		String owner = claim.claimOwnerName; //we need the direct name, so Admin Claims aren't lost.

		ArrayList<String> builders = new ArrayList<String>();
		ArrayList<String> containers = new ArrayList<String>();
		ArrayList<String> accessors = new ArrayList<String>();
		ArrayList<String> managers = new ArrayList<String>();

		claim.getPermissions(builders, containers, accessors, managers);

		String buildersString = "";
		for (int i = 0; i < builders.size(); i++) {
			buildersString += builders.get(i) + ";";
		}

		String containersString = "";
		for (int i = 0; i < containers.size(); i++) {
			containersString += containers.get(i) + ";";
		}

		String accessorsString = "";
		for (int i = 0; i < accessors.size(); i++) {
			accessorsString += accessors.get(i) + ";";
		}

		String managersString = "";
		for (int i = 0; i < managers.size(); i++) {
			managersString += managers.get(i) + ";";
		}

		long parentId;
		long id;
		if (claim.parent == null) {
			parentId = -1;
		} else {
			parentId = claim.parent.id;
		}

		if (claim.id == null) {
			claim.id = id = getNextClaimID();
		} else {
			id = claim.id;
		}

		try {
			this.refreshDataConnection();
			// if the ID is in the database, then
			Statement idexists = databaseConnection.createStatement();
			ResultSet itexists = idexists.executeQuery("SELECT COUNT(*) AS total FROM griefprevention_claimdata WHERE id=" + id);
			itexists.next();
			boolean useupdate = itexists.getInt("total") > 0;
			//
			if (useupdate) {
				Statement updatestatement = databaseConnection.createStatement();
				// statement.execute("CREATE TABLE IF NOT EXISTS
				// griefprevention_claimdata
				// (id INTEGER, owner VARCHAR(50),
				// lessercorner VARCHAR(100),
				// greatercorner VARCHAR(100),
				// builders TEXT,
				// containers TEXT,
				// accessors TEXT,
				// managers TEXT,
				// parentid INTEGER,
				// neverdelete BOOLEAN NOT NULL DEFAULT false);");
				updatestatement.execute("UPDATE griefprevention_claimdata " + "SET id='" + id + "'" + ",owner='" + owner + "'" + ",lessercorner='" + lesserCornerString + "'" + ",greatercorner='" + greaterCornerString + "'" + ",builders='" + buildersString + "'" + ",containers='" + containersString + "'" + ",accessors='" + accessorsString + "'" + ",managers='" + managersString + "'" + ",parentid='" + parentId + "'" + ",neverdelete='" + (claim.neverdelete ? 1 : 0) + "' " + "WHERE id=" + id);
				Debugger.Write("updated data into griefprevention_claimdata- ID:" + claim.getID(), DebugLevel.Verbose);

			} else {
				Statement statement = databaseConnection.createStatement();
				statement.execute("INSERT INTO griefprevention_claimdata VALUES(" + id + ", '" + owner + "', '" + lesserCornerString + "', '" + greaterCornerString + "', '" + buildersString + "', '" + containersString + "', '" + accessorsString + "', '" + managersString + "', " + parentId + ", " + claim.neverdelete + ");");
				Debugger.Write("Successfully inserted data into griefprevention_claimdata- ID:" + claim.getID(), DebugLevel.Verbose);
			}
		} catch (SQLException e) {

			GriefPrevention.AddLogEntry("Unable to save data for claim at " + this.locationToString(claim.lesserBoundaryCorner) + ".  Details:");
			e.printStackTrace();
		}
	}

	@Override
	synchronized void writeClaimToStorage(Claim claim) // see datastore.java. this
														// will ALWAYS be a top
														// level claim
	{
		try {
			this.refreshDataConnection();

			// wipe out any existing data about this claim
			// this.deleteClaimFromSecondaryStorage(claim);

			// write top level claim data to the database
			this.writeClaimData(claim);

			// for each subdivision
			for (int i = 0; i < claim.children.size(); i++) {
				// write the subdivision's data to the database
				this.writeClaimData(claim.children.get(i));
			}
		} catch (SQLException e) {
			GriefPrevention.AddLogEntry("Unable to save data for claim at " + this.locationToString(claim.lesserBoundaryCorner) + ".  Details:");
			e.printStackTrace();
		}
	}

}
