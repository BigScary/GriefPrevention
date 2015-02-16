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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockIterator;

public class GriefPreventionPlus extends JavaPlugin
{
	//for convenience, a reference to the instance of this plugin
	public static GriefPreventionPlus instance;
	
	//for logging to the console and log file
	private static Logger log = Logger.getLogger("Minecraft");
	
	//this handles data storage, like player and region data
	public DataStore dataStore;
	
	//this tracks item stacks expected to drop which will need protection
    ArrayList<PendingItemProtection> pendingItemWatchList = new ArrayList<PendingItemProtection>();
	
	//configuration variables, loaded/saved from a config.yml
	
	//claim mode for each world
	public ConcurrentHashMap<World, ClaimsMode> config_claims_worldModes;     
	
	public boolean config_claims_preventTheft;						//whether containers and crafting blocks are protectable
	public boolean config_claims_protectCreatures;					//whether claimed animals may be injured by players without permission
	public boolean config_claims_protectFires;                      //whether open flint+steel flames should be protected - optional because it's expensive
	public boolean config_claims_protectHorses;						//whether horses on a claim should be protected by that claim's rules
	public boolean config_claims_preventButtonsSwitches;			//whether buttons and switches are protectable
	public boolean config_claims_lockWoodenDoors;					//whether wooden doors should be locked by default (require /accesstrust)
	public boolean config_claims_lockTrapDoors;						//whether trap doors should be locked by default (require /accesstrust)
	public boolean config_claims_lockFenceGates;					//whether fence gates should be locked by default (require /accesstrust)
	public boolean config_claims_enderPearlsRequireAccessTrust;		//whether teleporting into a claim with a pearl requires access trust
	public int config_claims_maxClaimsPerPlayer;                    //maximum number of claims per player
	public boolean config_claims_respectWorldGuard;                 //whether claim creations requires WG build permission in creation area
	public boolean config_claims_portalsRequirePermission;          //whether nether portals require permission to generate.  defaults to off for performance reasons
	
	public int config_claims_initialBlocks;							//the number of claim blocks a new player starts with
	public double config_claims_abandonReturnRatio;                 //the portion of claim blocks returned to a player when a claim is abandoned
	public int config_claims_blocksAccruedPerHour;					//how many additional blocks players get each hour of play (can be zero)
	public int config_claims_maxAccruedBlocks;						//the limit on accrued blocks (over time).  doesn't limit purchased or admin-gifted blocks 
	public int config_claims_maxDepth;								//limit on how deep claims can go
	public int config_claims_expirationDays;						//how many days of inactivity before a player loses his claims
	
	public int config_claims_automaticClaimsForNewPlayersRadius;	//how big automatic new player claims (when they place a chest) should be.  0 to disable
	//public int config_claims_claimsExtendIntoGroundDistance;		//how far below the shoveled block a new claim will reach
	public int config_claims_minSize;								//minimum width and height for non-admin claims
	
	public int config_claims_chestClaimExpirationDays;				//number of days of inactivity before an automatic chest claim will be deleted
	public int config_claims_unusedClaimExpirationDays;				//number of days of inactivity before an unused (nothing build) claim will be deleted
	public boolean config_claims_survivalAutoNatureRestoration;		//whether survival claims will be automatically restored to nature when auto-deleted
	
	public Material config_claims_investigationTool;				//which material will be used to investigate claims with a right click
	public Material config_claims_modificationTool;	  				//which material will be used to create/resize claims with a right click
	
	public ArrayList<World> config_siege_enabledWorlds;				//whether or not /siege is enabled on this server
	public ArrayList<Material> config_siege_blocks;					//which blocks will be breakable in siege mode
		
	public boolean config_spam_enabled;								//whether or not to monitor for spam
	public int config_spam_loginCooldownSeconds;					//how long players must wait between logins.  combats login spam.
	public ArrayList<String> config_spam_monitorSlashCommands;  	//the list of slash commands monitored for spam
	public boolean config_spam_banOffenders;						//whether or not to ban spammers automatically
	public String config_spam_banMessage;							//message to show an automatically banned player
	public String config_spam_warningMessage;						//message to show a player who is close to spam level
	public String config_spam_allowedIpAddresses;					//IP addresses which will not be censored
	public int config_spam_deathMessageCooldownSeconds;				//cooldown period for death messages (per player) in seconds
	
	public ArrayList<World> config_pvp_enabledWorlds;				//list of worlds where pvp anti-grief rules apply
	public boolean config_pvp_protectFreshSpawns;					//whether to make newly spawned players immune until they pick up an item
	public boolean config_pvp_punishLogout;						    //whether to kill players who log out during PvP combat
	public int config_pvp_combatTimeoutSeconds;						//how long combat is considered to continue after the most recent damage
	public boolean config_pvp_allowCombatItemDrop;					//whether a player can drop items during combat to hide them
	public ArrayList<String> config_pvp_blockedCommands;			//list of commands which may not be used during pvp combat
	public boolean config_pvp_noCombatInPlayerLandClaims;			//whether players may fight in player-owned land claims
	public boolean config_pvp_noCombatInAdminLandClaims;			//whether players may fight in admin-owned land claims
	public boolean config_pvp_noCombatInAdminSubdivisions;          //whether players may fight in subdivisions of admin-owned land claims
	
	public boolean config_lockDeathDropsInPvpWorlds;                //whether players' dropped on death items are protected in pvp worlds
	public boolean config_lockDeathDropsInNonPvpWorlds;             //whether players' dropped on death items are protected in non-pvp worlds
	
	public double config_economy_claimBlocksPurchaseCost;			//cost to purchase a claim block.  set to zero to disable purchase.
	public double config_economy_claimBlocksSellValue;				//return on a sold claim block.  set to zero to disable sale.
	
	public boolean config_blockSurfaceCreeperExplosions;			//whether creeper explosions near or above the surface destroy blocks
	public boolean config_blockSurfaceOtherExplosions;				//whether non-creeper explosions near or above the surface destroy blocks
	public boolean config_blockSkyTrees;							//whether players can build trees on platforms in the sky
	
	public boolean config_fireSpreads;								//whether fire spreads outside of claims
	public boolean config_fireDestroys;								//whether fire destroys blocks outside of claims
	
	public boolean config_whisperNotifications; 					//whether whispered messages will broadcast to administrators in game
	public boolean config_signNotifications;                        //whether sign content will broadcast to administrators in game
	public ArrayList<String> config_eavesdrop_whisperCommands;		//list of whisper commands to eavesdrop on
	
	public boolean config_smartBan;									//whether to ban accounts which very likely owned by a banned player
	
	public boolean config_endermenMoveBlocks;						//whether or not endermen may move blocks around
	public boolean config_silverfishBreakBlocks;					//whether silverfish may break blocks
	public boolean config_creaturesTrampleCrops;					//whether or not non-player entities may trample crops
	public boolean config_zombiesBreakDoors;						//whether or not hard-mode zombies may break down wooden doors
	
	public MaterialCollection config_mods_accessTrustIds;			//list of block IDs which should require /accesstrust for player interaction
	public MaterialCollection config_mods_containerTrustIds;		//list of block IDs which should require /containertrust for player interaction
	public List<String> config_mods_ignoreClaimsAccounts;			//list of player names which ALWAYS ignore claims
	public MaterialCollection config_mods_explodableIds;			//list of block IDs which can be destroyed by explosions, even in claimed areas

	public HashMap<String, Integer> config_seaLevelOverride;		//override for sea level, because bukkit doesn't report the right value for all situations
	
	public boolean config_limitTreeGrowth;                          //whether trees should be prevented from growing into a claim from outside
	public boolean config_pistonsInClaimsOnly;                      //whether pistons are limited to only move blocks located within the piston's land claim
	
	private String databaseUrl;
	private String databaseUserName;
	private String databasePassword;
	
	/** UUID 0 is used for "public" permission and subclaims */ 
	public final static UUID UUID0 = new UUID(0,0);
	
	/** UUID 1 is used for administrative claims */
	public final static UUID UUID1 = new UUID(0,1);
	
	//reference to the economy plugin, if economy integration is enabled
	public static Economy economy = null;					
	
	//how far away to search from a tree trunk for its branch blocks
	public static final int TREE_RADIUS = 5;
	
	//how long to wait before deciding a player is staying online or staying offline, for notication messages
	public static final int NOTIFICATION_SECONDS = 20;
	
	//adds a server log entry
	public static synchronized void AddLogEntry(String entry) {
		log.info("[GriefPreventionPlus] " + entry);
	}
	
	public void onLoad() {
		// check if Grief Prevention is loaded
		if (this.getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
			AddLogEntry("-- WARNING  --");
			AddLogEntry("-- SHUTDOWN --");
			AddLogEntry("Remove GriefPrevention.jar (do not delete data folder)");
			AddLogEntry("--------------");
			this.getServer().shutdown();
			this.getServer().getPluginManager().clearPlugins();
			return;
		}
	}
	
	//initializes well...   everything
	public void onEnable() { 		
		AddLogEntry("boot start.");
		instance = this;

		this.loadConfig();
		
		AddLogEntry("Finished loading configuration.");
		
		//when datastore initializes, it loads player and claim data, and posts some stats to the log
		if(this.databaseUrl.length() > 0)
		{
			try
			{
				DataStore databaseStore = new DataStore(this.databaseUrl, this.databaseUserName, this.databasePassword);

				this.dataStore = databaseStore;
			} catch(Exception e) {
				AddLogEntry(e.getMessage());
				e.printStackTrace();
				AddLogEntry("-- WARNING  --");
				AddLogEntry("-- SHUTDOWN --");
				AddLogEntry("I can't connect to the database! Update the database config settings to resolve the issue. The server will shutdown to avoid claim griefing.");
				AddLogEntry("--------------");
				this.getServer().shutdown();
				this.getServer().getPluginManager().clearPlugins();
				return;
			}			
		} else {
			AddLogEntry("-- WARNING  --");
			AddLogEntry("Database settings are required! Update the database config settings to resolve the issue. Grief Prevention Plus disabled.");
			AddLogEntry("--------------");
			return;
		}

		AddLogEntry("Finished loading data.");
		
		//unless claim block accrual is disabled, start the recurring per 5 minute event to give claim blocks to online players
		//20L ~ 1 second
		if(this.config_claims_blocksAccruedPerHour > 0)
		{
			DeliverClaimBlocksTask task = new DeliverClaimBlocksTask(null);
			this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task, 20L * 60 * 5, 20L * 60 * 5);
		}
		
		//start the recurring cleanup event for entities in creative worlds
		EntityCleanupTask task = new EntityCleanupTask(0);
		this.getServer().getScheduler().scheduleSyncDelayedTask(GriefPreventionPlus.instance, task, 20L);
		
		//register for events
		PluginManager pluginManager = this.getServer().getPluginManager();
		
		//player events
		PlayerEventHandler playerEventHandler = new PlayerEventHandler(this.dataStore, this);
		pluginManager.registerEvents(playerEventHandler, this);

		//block events
		BlockEventHandler blockEventHandler = new BlockEventHandler(this.dataStore);
		pluginManager.registerEvents(blockEventHandler, this);
				
		//entity events
		EntityEventHandler entityEventHandler = new EntityEventHandler(this.dataStore);
		pluginManager.registerEvents(entityEventHandler, this);
		
		//if economy is enabled
		if(this.config_economy_claimBlocksPurchaseCost > 0 || this.config_economy_claimBlocksSellValue > 0)
		{
			//try to load Vault
			GriefPreventionPlus.AddLogEntry("GriefPrevention requiresP Vault for economy integration.");
			GriefPreventionPlus.AddLogEntry("Attempting to load Vault...");
			RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
			GriefPreventionPlus.AddLogEntry("Vault loaded successfully!");
			
			//ask Vault to hook into an economy plugin
			GriefPreventionPlus.AddLogEntry("Looking for a Vault-compatible economy plugin...");
			if (economyProvider != null) 
	        {
	        	GriefPreventionPlus.economy = economyProvider.getProvider();
	            
	            //on success, display success message
				if(GriefPreventionPlus.economy != null)
		        {
	            	GriefPreventionPlus.AddLogEntry("Hooked into economy: " + GriefPreventionPlus.economy.getName() + ".");  
	            	GriefPreventionPlus.AddLogEntry("Ready to buy/sell claim blocks!");
		        }
		        
				//otherwise error message
				else
		        {
		        	GriefPreventionPlus.AddLogEntry("ERROR: Vault was unable to find a supported economy plugin.  Either install a Vault-compatible economy plugin, or set both of the economy config variables to zero.");
		        }	            
	        }
			
			//another error case
			else
			{
				GriefPreventionPlus.AddLogEntry("ERROR: Vault was unable to find a supported economy plugin.  Either install a Vault-compatible economy plugin, or set both of the economy config variables to zero.");
			}
		}
		
		int playersCached = 0;
		OfflinePlayer [] offlinePlayers = this.getServer().getOfflinePlayers();
		long now = System.currentTimeMillis();
		final long millisecondsPerDay = 1000 * 60 * 60 * 24;
		for(OfflinePlayer player : offlinePlayers) {
		    //if the player has been seen in the last 30 days, cache his name/UUID pair
		    if((now - player.getLastPlayed())/millisecondsPerDay <= 30) {
		        this.playerNameToIDMap.put(player.getName().toLowerCase(), player.getUniqueId());
		        playersCached++;
		    }
		}
		
		AddLogEntry("Cached " + playersCached + " recent players.");
		
		this.getCommand("abandonclaim").setExecutor(new CommandExec());
		this.getCommand("abandontoplevelclaim").setExecutor(new CommandExec());
		this.getCommand("ignoreclaims").setExecutor(new CommandExec());
		this.getCommand("abandonallclaims").setExecutor(new CommandExec());
		this.getCommand("restorenature").setExecutor(new CommandExec());
		this.getCommand("restorenatureaggressive").setExecutor(new CommandExec());
		this.getCommand("restorenaturefill").setExecutor(new CommandExec());
		this.getCommand("trust").setExecutor(new CommandExec());
		this.getCommand("transferclaim").setExecutor(new CommandExec());
		this.getCommand("trustlist").setExecutor(new CommandExec());
		this.getCommand("untrust").setExecutor(new CommandExec());
		this.getCommand("accesstrust").setExecutor(new CommandExec());
		this.getCommand("containertrust").setExecutor(new CommandExec());
		this.getCommand("permissiontrust").setExecutor(new CommandExec());
		this.getCommand("buyclaimblocks").setExecutor(new CommandExec());
		this.getCommand("sellclaimblocks").setExecutor(new CommandExec());
		this.getCommand("adminclaims").setExecutor(new CommandExec());
		this.getCommand("basicclaims").setExecutor(new CommandExec());
		this.getCommand("subdivideclaims").setExecutor(new CommandExec());
		this.getCommand("deleteclaim").setExecutor(new CommandExec());
		this.getCommand("claimexplosions").setExecutor(new CommandExec());
		this.getCommand("deleteallclaims").setExecutor(new CommandExec());
		this.getCommand("claimslist").setExecutor(new CommandExec());
		this.getCommand("unlockdrops").setExecutor(new CommandExec());
		this.getCommand("deletealladminclaims").setExecutor(new CommandExec());
		this.getCommand("adjustbonusclaimblocks").setExecutor(new CommandExec());
		this.getCommand("trapped").setExecutor(new CommandExec());
		this.getCommand("siege").setExecutor(new CommandExec());
		this.getCommand("softmute").setExecutor(new CommandExec());
		this.getCommand("gpreload").setExecutor(new CommandExec());
		this.getCommand("givepet").setExecutor(new CommandExec());
		this.getCommand("gpblockinfo").setExecutor(new CommandExec());

		
		//start recurring cleanup scan for unused claims belonging to inactive players
		CleanupUnusedClaimsTask task2 = new CleanupUnusedClaimsTask();
		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task2, 20L * 60 * 2, 20L * 60 * 5);
		
		AddLogEntry("Boot finished.");
	}
	
	void loadConfig()
	{
	  //load the config if it exists
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(DataStore.configFilePath));
        FileConfiguration outConfig = new YamlConfiguration();
        
        //read configuration settings (note defaults)
        
        //get (deprecated node) claims world names from the config file
        List<World> worlds = this.getServer().getWorlds();
        List<String> deprecated_claimsEnabledWorldNames = config.getStringList("GriefPrevention.Claims.Worlds");
        
        //validate that list
        for(int i = 0; i < deprecated_claimsEnabledWorldNames.size(); i++)
        {
            String worldName = deprecated_claimsEnabledWorldNames.get(i);
            World world = this.getServer().getWorld(worldName);
            if(world == null)
            {
                deprecated_claimsEnabledWorldNames.remove(i--);
            }
        }
        
        //get (deprecated node) creative world names from the config file
        List<String> deprecated_creativeClaimsEnabledWorldNames = config.getStringList("GriefPrevention.Claims.CreativeRulesWorlds");
        
        //validate that list
        for(int i = 0; i < deprecated_creativeClaimsEnabledWorldNames.size(); i++)
        {
            String worldName = deprecated_creativeClaimsEnabledWorldNames.get(i);
            World world = this.getServer().getWorld(worldName);
            if(world == null)
            {
                deprecated_claimsEnabledWorldNames.remove(i--);
            }
        }
        
        //decide claim mode for each world
        this.config_claims_worldModes = new ConcurrentHashMap<World, ClaimsMode>();
        for(World world : worlds)
        {
            //is it specified in the config file?
            String configSetting = config.getString("GriefPrevention.Claims.Mode." + world.getName());
            if(configSetting != null)
            {
                ClaimsMode claimsMode = this.configStringToClaimsMode(configSetting);
                if(claimsMode != null)
                {
                    this.config_claims_worldModes.put(world, claimsMode);
                    continue;
                }
                else
                {
                    GriefPreventionPlus.AddLogEntry("Error: Invalid claim mode \"" + configSetting + "\".  Options are Survival, Creative, and Disabled.");
                    this.config_claims_worldModes.put(world, ClaimsMode.Creative);
                }
            }
            
            //was it specified in a deprecated config node?
            if(deprecated_creativeClaimsEnabledWorldNames.contains(world.getName()))
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Creative);
            }
            
            else if(deprecated_claimsEnabledWorldNames.contains(world.getName()))
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Survival);
            }
            
            //does the world's name indicate its purpose?
            else if(world.getName().toLowerCase().contains("survival"))
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Survival);
            }
            
            else if(world.getName().toLowerCase().contains("creative"))
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Creative);
            }
            
            //decide a default based on server type and world type
            else if(this.getServer().getDefaultGameMode() == GameMode.CREATIVE)
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Creative);
            }
            
            else if(world.getEnvironment() == Environment.NORMAL)
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Survival);
            }
            
            else
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Disabled);
            }
            
            //if the setting WOULD be disabled but this is a server upgrading from the old config format,
            //then default to survival mode for safety's sake (to protect any admin claims which may 
            //have been created there)
            if(this.config_claims_worldModes.get(world) == ClaimsMode.Disabled &&
               deprecated_claimsEnabledWorldNames.size() > 0)
            {
                this.config_claims_worldModes.put(world, ClaimsMode.Survival);
            }
        }
        
        //pvp worlds list
        this.config_pvp_enabledWorlds = new ArrayList<World>();
        for(World world : worlds)          
        {
            boolean pvpWorld = config.getBoolean("GriefPrevention.PvP.RulesEnabledInWorld." + world.getName(), world.getPVP());
            if(pvpWorld)
            {
                this.config_pvp_enabledWorlds.add(world);
            }
        }
        
        //sea level
        this.config_seaLevelOverride = new HashMap<String, Integer>();
        for(int i = 0; i < worlds.size(); i++)
        {
            int seaLevelOverride = config.getInt("GriefPrevention.SeaLevelOverrides." + worlds.get(i).getName(), -1);
            outConfig.set("GriefPrevention.SeaLevelOverrides." + worlds.get(i).getName(), seaLevelOverride);
            this.config_seaLevelOverride.put(worlds.get(i).getName(), seaLevelOverride);
        }
        
        this.config_claims_preventTheft = config.getBoolean("GriefPrevention.Claims.PreventTheft", true);
        this.config_claims_protectCreatures = config.getBoolean("GriefPrevention.Claims.ProtectCreatures", true);
        this.config_claims_protectFires = config.getBoolean("GriefPrevention.Claims.ProtectFires", false);
        this.config_claims_protectHorses = config.getBoolean("GriefPrevention.Claims.ProtectHorses", true);
        this.config_claims_preventButtonsSwitches = config.getBoolean("GriefPrevention.Claims.PreventButtonsSwitches", true);
        this.config_claims_lockWoodenDoors = config.getBoolean("GriefPrevention.Claims.LockWoodenDoors", false);
        this.config_claims_lockTrapDoors = config.getBoolean("GriefPrevention.Claims.LockTrapDoors", false);
        this.config_claims_lockFenceGates = config.getBoolean("GriefPrevention.Claims.LockFenceGates", true);
        this.config_claims_enderPearlsRequireAccessTrust = config.getBoolean("GriefPrevention.Claims.EnderPearlsRequireAccessTrust", true);
        this.config_claims_initialBlocks = config.getInt("GriefPrevention.Claims.InitialBlocks", 100);
        this.config_claims_blocksAccruedPerHour = config.getInt("GriefPrevention.Claims.BlocksAccruedPerHour", 100);
        this.config_claims_maxAccruedBlocks = config.getInt("GriefPrevention.Claims.MaxAccruedBlocks", 80000);
        this.config_claims_abandonReturnRatio = config.getDouble("GriefPrevention.Claims.AbandonReturnRatio", 1);
        this.config_claims_automaticClaimsForNewPlayersRadius = config.getInt("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", 4);
        //this.config_claims_claimsExtendIntoGroundDistance = Math.abs(config.getInt("GriefPrevention.Claims.ExtendIntoGroundDistance", 5));
        this.config_claims_minSize = config.getInt("GriefPrevention.Claims.MinimumSize", 10);
        this.config_claims_maxDepth = config.getInt("GriefPrevention.Claims.MaximumDepth", 0);
        this.config_claims_chestClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.ChestClaimDays", 7);
        this.config_claims_unusedClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.UnusedClaimDays", 14);
        this.config_claims_expirationDays = config.getInt("GriefPrevention.Claims.Expiration.AllClaimDays", 0);
        this.config_claims_survivalAutoNatureRestoration = config.getBoolean("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration.SurvivalWorlds", false);
        this.config_claims_maxClaimsPerPlayer = config.getInt("GriefPrevention.Claims.MaximumNumberOfClaimsPerPlayer", 0);
        this.config_claims_respectWorldGuard = config.getBoolean("GriefPrevention.Claims.CreationRequiresWorldGuardBuildPermission", true);
        this.config_claims_portalsRequirePermission = config.getBoolean("GriefPrevention.Claims.PortalGenerationRequiresPermission", false);
        
        this.config_spam_enabled = config.getBoolean("GriefPrevention.Spam.Enabled", true);
        this.config_spam_loginCooldownSeconds = config.getInt("GriefPrevention.Spam.LoginCooldownSeconds", 60);
        this.config_spam_warningMessage = config.getString("GriefPrevention.Spam.WarningMessage", "Please reduce your noise level.  Spammers will be banned.");
        this.config_spam_allowedIpAddresses = config.getString("GriefPrevention.Spam.AllowedIpAddresses", "1.2.3.4; 5.6.7.8");
        this.config_spam_banOffenders = config.getBoolean("GriefPrevention.Spam.BanOffenders", true);       
        this.config_spam_banMessage = config.getString("GriefPrevention.Spam.BanMessage", "Banned for spam.");
        String slashCommandsToMonitor = config.getString("GriefPrevention.Spam.MonitorSlashCommands", "/me;/tell;/global;/local;/w;/msg;/r;/t");
        this.config_spam_deathMessageCooldownSeconds = config.getInt("GriefPrevention.Spam.DeathMessageCooldownSeconds", 60);       
        
        this.config_pvp_protectFreshSpawns = config.getBoolean("GriefPrevention.PvP.ProtectFreshSpawns", true);
        this.config_pvp_punishLogout = config.getBoolean("GriefPrevention.PvP.PunishLogout", true);
        this.config_pvp_combatTimeoutSeconds = config.getInt("GriefPrevention.PvP.CombatTimeoutSeconds", 15);
        this.config_pvp_allowCombatItemDrop = config.getBoolean("GriefPrevention.PvP.AllowCombatItemDrop", false);
        String bannedPvPCommandsList = config.getString("GriefPrevention.PvP.BlockedSlashCommands", "/home;/vanish;/spawn;/tpa");
        
        this.config_economy_claimBlocksPurchaseCost = config.getDouble("GriefPrevention.Economy.ClaimBlocksPurchaseCost", 0);
        this.config_economy_claimBlocksSellValue = config.getDouble("GriefPrevention.Economy.ClaimBlocksSellValue", 0);
        
        this.config_lockDeathDropsInPvpWorlds = config.getBoolean("GriefPrevention.ProtectItemsDroppedOnDeath.PvPWorlds", false);
        this.config_lockDeathDropsInNonPvpWorlds = config.getBoolean("GriefPrevention.ProtectItemsDroppedOnDeath.NonPvPWorlds", true);
        
        this.config_blockSurfaceCreeperExplosions = config.getBoolean("GriefPrevention.BlockSurfaceCreeperExplosions", true);
        this.config_blockSurfaceOtherExplosions = config.getBoolean("GriefPrevention.BlockSurfaceOtherExplosions", true);
        this.config_blockSkyTrees = config.getBoolean("GriefPrevention.LimitSkyTrees", true);
        this.config_limitTreeGrowth = config.getBoolean("GriefPrevention.LimitTreeGrowth", false);
        this.config_pistonsInClaimsOnly = config.getBoolean("GriefPrevention.LimitPistonsToLandClaims", true);
                
        this.config_fireSpreads = config.getBoolean("GriefPrevention.FireSpreads", false);
        this.config_fireDestroys = config.getBoolean("GriefPrevention.FireDestroys", false);
        
        this.config_whisperNotifications = config.getBoolean("GriefPrevention.AdminsGetWhispers", true);
        this.config_signNotifications = config.getBoolean("GriefPrevention.AdminsGetSignNotifications", true);
        String whisperCommandsToMonitor = config.getString("GriefPrevention.WhisperCommands", "/tell;/pm;/r;/w;/whisper;/t;/msg");
        
        this.config_smartBan = config.getBoolean("GriefPrevention.SmartBan", true);
        
        this.config_endermenMoveBlocks = config.getBoolean("GriefPrevention.EndermenMoveBlocks", false);
        this.config_silverfishBreakBlocks = config.getBoolean("GriefPrevention.SilverfishBreakBlocks", false);
        this.config_creaturesTrampleCrops = config.getBoolean("GriefPrevention.CreaturesTrampleCrops", false);
        this.config_zombiesBreakDoors = config.getBoolean("GriefPrevention.HardModeZombiesBreakDoors", false);
        
        this.config_mods_ignoreClaimsAccounts = config.getStringList("GriefPrevention.Mods.PlayersIgnoringAllClaims");
        
        if(this.config_mods_ignoreClaimsAccounts == null) this.config_mods_ignoreClaimsAccounts = new ArrayList<String>();
        
        this.config_mods_accessTrustIds = new MaterialCollection();
        List<String> accessTrustStrings = config.getStringList("GriefPrevention.Mods.BlockIdsRequiringAccessTrust");
        
        this.parseMaterialListFromConfig(accessTrustStrings, this.config_mods_accessTrustIds);
        
        this.config_mods_containerTrustIds = new MaterialCollection();
        List<String> containerTrustStrings = config.getStringList("GriefPrevention.Mods.BlockIdsRequiringContainerTrust");
        
        //default values for container trust mod blocks
        if(containerTrustStrings == null || containerTrustStrings.size() == 0)
        {
            containerTrustStrings.add(new MaterialInfo(99999, "Example - ID 99999, all data values.").toString());
        }
        
        //parse the strings from the config file
        this.parseMaterialListFromConfig(containerTrustStrings, this.config_mods_containerTrustIds);
        
        this.config_mods_explodableIds = new MaterialCollection();
        List<String> explodableStrings = config.getStringList("GriefPrevention.Mods.BlockIdsExplodable");
        
        //parse the strings from the config file
        this.parseMaterialListFromConfig(explodableStrings, this.config_mods_explodableIds);
        
        //default for claim investigation tool
        String investigationToolMaterialName = Material.STICK.name();
        
        //get investigation tool from config
        investigationToolMaterialName = config.getString("GriefPrevention.Claims.InvestigationTool", investigationToolMaterialName);
        
        //validate investigation tool
        this.config_claims_investigationTool = Material.getMaterial(investigationToolMaterialName);
        if(this.config_claims_investigationTool == null)
        {
            GriefPreventionPlus.AddLogEntry("ERROR: Material " + investigationToolMaterialName + " not found.  Defaulting to the stick.  Please update your config.yml.");
            this.config_claims_investigationTool = Material.STICK;
        }
        
        //default for claim creation/modification tool
        String modificationToolMaterialName = Material.GOLD_SPADE.name();
        
        //get modification tool from config
        modificationToolMaterialName = config.getString("GriefPrevention.Claims.ModificationTool", modificationToolMaterialName);
        
        //validate modification tool
        this.config_claims_modificationTool = Material.getMaterial(modificationToolMaterialName);
        if(this.config_claims_modificationTool == null)
        {
            GriefPreventionPlus.AddLogEntry("ERROR: Material " + modificationToolMaterialName + " not found.  Defaulting to the golden shovel.  Please update your config.yml.");
            this.config_claims_modificationTool = Material.GOLD_SPADE;
        }
        
        //default for siege worlds list
        ArrayList<String> defaultSiegeWorldNames = new ArrayList<String>();
        
        //get siege world names from the config file
        List<String> siegeEnabledWorldNames = config.getStringList("GriefPrevention.Siege.Worlds");
        if(siegeEnabledWorldNames == null)
        {           
            siegeEnabledWorldNames = defaultSiegeWorldNames;
        }
        
        //validate that list
        this.config_siege_enabledWorlds = new ArrayList<World>();
        for(int i = 0; i < siegeEnabledWorldNames.size(); i++)
        {
            String worldName = siegeEnabledWorldNames.get(i);
            World world = this.getServer().getWorld(worldName);
            if(world == null)
            {
                AddLogEntry("Error: Siege Configuration: There's no world named \"" + worldName + "\".  Please update your config.yml.");
            }
            else
            {
                this.config_siege_enabledWorlds.add(world);
            }
        }
        
        //default siege blocks
        this.config_siege_blocks = new ArrayList<Material>();
        this.config_siege_blocks.add(Material.DIRT);
        this.config_siege_blocks.add(Material.GRASS);
        this.config_siege_blocks.add(Material.LONG_GRASS);
        this.config_siege_blocks.add(Material.COBBLESTONE);
        this.config_siege_blocks.add(Material.GRAVEL);
        this.config_siege_blocks.add(Material.SAND);
        this.config_siege_blocks.add(Material.GLASS);
        this.config_siege_blocks.add(Material.THIN_GLASS);
        this.config_siege_blocks.add(Material.WOOD);
        this.config_siege_blocks.add(Material.WOOL);
        this.config_siege_blocks.add(Material.SNOW);
        
        //build a default config entry
        ArrayList<String> defaultBreakableBlocksList = new ArrayList<String>();
        for(int i = 0; i < this.config_siege_blocks.size(); i++)
        {
            defaultBreakableBlocksList.add(this.config_siege_blocks.get(i).name());
        }
        
        //try to load the list from the config file
        List<String> breakableBlocksList = config.getStringList("GriefPrevention.Siege.BreakableBlocks");
        
        //if it fails, use default list instead
        if(breakableBlocksList == null || breakableBlocksList.size() == 0)
        {
            breakableBlocksList = defaultBreakableBlocksList;
        }
        
        //parse the list of siege-breakable blocks
        this.config_siege_blocks = new ArrayList<Material>();
        for(int i = 0; i < breakableBlocksList.size(); i++)
        {
            String blockName = breakableBlocksList.get(i);
            Material material = Material.getMaterial(blockName);
            if(material == null)
            {
                GriefPreventionPlus.AddLogEntry("Siege Configuration: Material not found: " + blockName + ".");
            }
            else
            {
                this.config_siege_blocks.add(material);
            }
        }
        
        this.config_pvp_noCombatInPlayerLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims", this.config_siege_enabledWorlds.size() == 0);
        this.config_pvp_noCombatInAdminLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", this.config_siege_enabledWorlds.size() == 0);
        this.config_pvp_noCombatInAdminSubdivisions = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeSubdivisions", this.config_siege_enabledWorlds.size() == 0);
        
        //optional database settings
        this.databaseUrl = config.getString("GriefPrevention.Database.URL", "");
        this.databaseUserName = config.getString("GriefPrevention.Database.UserName", "");
        this.databasePassword = config.getString("GriefPrevention.Database.Password", "");
        
        //claims mode by world
        for(World world : this.config_claims_worldModes.keySet())
        {
            outConfig.set(
                "GriefPrevention.Claims.Mode." + world.getName(), 
                this.config_claims_worldModes.get(world).name());
        }
        
        outConfig.set("GriefPrevention.Claims.PreventTheft", this.config_claims_preventTheft);
        outConfig.set("GriefPrevention.Claims.ProtectCreatures", this.config_claims_protectCreatures);
        outConfig.set("GriefPrevention.Claims.PreventButtonsSwitches", this.config_claims_preventButtonsSwitches);
        outConfig.set("GriefPrevention.Claims.LockWoodenDoors", this.config_claims_lockWoodenDoors);
        outConfig.set("GriefPrevention.Claims.LockTrapDoors", this.config_claims_lockTrapDoors);
        outConfig.set("GriefPrevention.Claims.LockFenceGates", this.config_claims_lockFenceGates);
        outConfig.set("GriefPrevention.Claims.EnderPearlsRequireAccessTrust", this.config_claims_enderPearlsRequireAccessTrust);
        outConfig.set("GriefPrevention.Claims.ProtectFires", this.config_claims_protectFires);
        outConfig.set("GriefPrevention.Claims.ProtectHorses", this.config_claims_protectHorses);
        outConfig.set("GriefPrevention.Claims.InitialBlocks", this.config_claims_initialBlocks);
        outConfig.set("GriefPrevention.Claims.BlocksAccruedPerHour", this.config_claims_blocksAccruedPerHour);
        outConfig.set("GriefPrevention.Claims.MaxAccruedBlocks", this.config_claims_maxAccruedBlocks);
        outConfig.set("GriefPrevention.Claims.AbandonReturnRatio", this.config_claims_abandonReturnRatio);
        outConfig.set("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", this.config_claims_automaticClaimsForNewPlayersRadius);
        //outConfig.set("GriefPrevention.Claims.ExtendIntoGroundDistance", this.config_claims_claimsExtendIntoGroundDistance);
        outConfig.set("GriefPrevention.Claims.MinimumSize", this.config_claims_minSize);
        outConfig.set("GriefPrevention.Claims.MaximumDepth", this.config_claims_maxDepth);
        outConfig.set("GriefPrevention.Claims.InvestigationTool", this.config_claims_investigationTool.name());
        outConfig.set("GriefPrevention.Claims.ModificationTool", this.config_claims_modificationTool.name());
        outConfig.set("GriefPrevention.Claims.Expiration.ChestClaimDays", this.config_claims_chestClaimExpirationDays);
        outConfig.set("GriefPrevention.Claims.Expiration.UnusedClaimDays", this.config_claims_unusedClaimExpirationDays);       
        outConfig.set("GriefPrevention.Claims.Expiration.AllClaimDays", this.config_claims_expirationDays);
        outConfig.set("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration.SurvivalWorlds", this.config_claims_survivalAutoNatureRestoration);
        outConfig.set("GriefPrevention.Claims.MaximumNumberOfClaimsPerPlayer", this.config_claims_maxClaimsPerPlayer);
        outConfig.set("GriefPrevention.Claims.CreationRequiresWorldGuardBuildPermission", this.config_claims_respectWorldGuard);
        outConfig.set("GriefPrevention.Claims.PortalGenerationRequiresPermission", this.config_claims_portalsRequirePermission);
        
        outConfig.set("GriefPrevention.Spam.Enabled", this.config_spam_enabled);
        outConfig.set("GriefPrevention.Spam.LoginCooldownSeconds", this.config_spam_loginCooldownSeconds);
        outConfig.set("GriefPrevention.Spam.MonitorSlashCommands", slashCommandsToMonitor);
        outConfig.set("GriefPrevention.Spam.WarningMessage", this.config_spam_warningMessage);
        outConfig.set("GriefPrevention.Spam.BanOffenders", this.config_spam_banOffenders);      
        outConfig.set("GriefPrevention.Spam.BanMessage", this.config_spam_banMessage);
        outConfig.set("GriefPrevention.Spam.AllowedIpAddresses", this.config_spam_allowedIpAddresses);
        outConfig.set("GriefPrevention.Spam.DeathMessageCooldownSeconds", this.config_spam_deathMessageCooldownSeconds);
        
        for(World world : worlds)
        {
            outConfig.set("GriefPrevention.PvP.RulesEnabledInWorld." + world.getName(), this.config_pvp_enabledWorlds.contains(world));
        }
        outConfig.set("GriefPrevention.PvP.ProtectFreshSpawns", this.config_pvp_protectFreshSpawns);
        outConfig.set("GriefPrevention.PvP.PunishLogout", this.config_pvp_punishLogout);
        outConfig.set("GriefPrevention.PvP.CombatTimeoutSeconds", this.config_pvp_combatTimeoutSeconds);
        outConfig.set("GriefPrevention.PvP.AllowCombatItemDrop", this.config_pvp_allowCombatItemDrop);
        outConfig.set("GriefPrevention.PvP.BlockedSlashCommands", bannedPvPCommandsList);
        outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims", this.config_pvp_noCombatInPlayerLandClaims);
        outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", this.config_pvp_noCombatInAdminLandClaims);
        outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeSubdivisions", this.config_pvp_noCombatInAdminSubdivisions);
        
        outConfig.set("GriefPrevention.Economy.ClaimBlocksPurchaseCost", this.config_economy_claimBlocksPurchaseCost);
        outConfig.set("GriefPrevention.Economy.ClaimBlocksSellValue", this.config_economy_claimBlocksSellValue);
        
        outConfig.set("GriefPrevention.ProtectItemsDroppedOnDeath.PvPWorlds", this.config_lockDeathDropsInPvpWorlds);
        outConfig.set("GriefPrevention.ProtectItemsDroppedOnDeath.NonPvPWorlds", this.config_lockDeathDropsInNonPvpWorlds);
        
        outConfig.set("GriefPrevention.BlockSurfaceCreeperExplosions", this.config_blockSurfaceCreeperExplosions);
        outConfig.set("GriefPrevention.BlockSurfaceOtherExplosions", this.config_blockSurfaceOtherExplosions);
        outConfig.set("GriefPrevention.LimitSkyTrees", this.config_blockSkyTrees);
        outConfig.set("GriefPrevention.LimitTreeGrowth", this.config_limitTreeGrowth);
        outConfig.set("GriefPrevention.LimitPistonsToLandClaims", this.config_pistonsInClaimsOnly);
        
        outConfig.set("GriefPrevention.FireSpreads", this.config_fireSpreads);
        outConfig.set("GriefPrevention.FireDestroys", this.config_fireDestroys);
        
        outConfig.set("GriefPrevention.AdminsGetWhispers", this.config_whisperNotifications);
        outConfig.set("GriefPrevention.AdminsGetSignNotifications", this.config_signNotifications);
        
        outConfig.set("GriefPrevention.WhisperCommands", whisperCommandsToMonitor);     
        outConfig.set("GriefPrevention.SmartBan", this.config_smartBan);
        
        outConfig.set("GriefPrevention.Siege.Worlds", siegeEnabledWorldNames);
        outConfig.set("GriefPrevention.Siege.BreakableBlocks", breakableBlocksList);
        
        outConfig.set("GriefPrevention.EndermenMoveBlocks", this.config_endermenMoveBlocks);
        outConfig.set("GriefPrevention.SilverfishBreakBlocks", this.config_silverfishBreakBlocks);      
        outConfig.set("GriefPrevention.CreaturesTrampleCrops", this.config_creaturesTrampleCrops);
        outConfig.set("GriefPrevention.HardModeZombiesBreakDoors", this.config_zombiesBreakDoors);      
        
        outConfig.set("GriefPrevention.Database.URL", this.databaseUrl);
        outConfig.set("GriefPrevention.Database.UserName", this.databaseUserName);
        outConfig.set("GriefPrevention.Database.Password", this.databasePassword);       
        
        outConfig.set("GriefPrevention.Mods.BlockIdsRequiringAccessTrust", this.config_mods_accessTrustIds);
        outConfig.set("GriefPrevention.Mods.BlockIdsRequiringContainerTrust", this.config_mods_containerTrustIds);
        outConfig.set("GriefPrevention.Mods.BlockIdsExplodable", this.config_mods_explodableIds);
        outConfig.set("GriefPrevention.Mods.PlayersIgnoringAllClaims", this.config_mods_ignoreClaimsAccounts);
        outConfig.set("GriefPrevention.Mods.BlockIdsRequiringAccessTrust", accessTrustStrings);
        outConfig.set("GriefPrevention.Mods.BlockIdsRequiringContainerTrust", containerTrustStrings);
        outConfig.set("GriefPrevention.Mods.BlockIdsExplodable", explodableStrings);
        
        try
        {
            outConfig.save(DataStore.configFilePath);
        }
        catch(IOException exception)
        {
            AddLogEntry("Unable to write to the configuration file at \"" + DataStore.configFilePath + "\"");
        }
        
        //try to parse the list of commands which should be monitored for spam
        this.config_spam_monitorSlashCommands = new ArrayList<String>();
        String [] commands = slashCommandsToMonitor.split(";");
        for(int i = 0; i < commands.length; i++)
        {
            this.config_spam_monitorSlashCommands.add(commands[i].trim());
        }
        
        //try to parse the list of commands which should be included in eavesdropping
        this.config_eavesdrop_whisperCommands  = new ArrayList<String>();
        commands = whisperCommandsToMonitor.split(";");
        for(int i = 0; i < commands.length; i++)
        {
            this.config_eavesdrop_whisperCommands.add(commands[i].trim());
        }       
        
        //try to parse the list of commands which should be banned during pvp combat
        this.config_pvp_blockedCommands = new ArrayList<String>();
        commands = bannedPvPCommandsList.split(";");
        for(int i = 0; i < commands.length; i++)
        {
            this.config_pvp_blockedCommands.add(commands[i].trim());
        }
    }

    private ClaimsMode configStringToClaimsMode(String configSetting)
    {
        if(configSetting.equalsIgnoreCase("Survival"))
        {
            return ClaimsMode.Survival;
        }
        else if(configSetting.equalsIgnoreCase("Creative"))
        {
            return ClaimsMode.Creative;
        }
        else if(configSetting.equalsIgnoreCase("Disabled"))
        {
            return ClaimsMode.Disabled;
        }
        else
        {
            return null;
        }
    }

    public static String getfriendlyLocationString(Location location) 
	{
		return location.getWorld().getName() + ": x" + location.getBlockX() + ", z" + location.getBlockZ();
	}    
	

	//helper method to resolve a player by name
	public ConcurrentHashMap<String, UUID> playerNameToIDMap = new ConcurrentHashMap<String, UUID>();
	

    public OfflinePlayer resolvePlayer(String name)  {
		//try online players first
		Player targetPlayer = this.getServer().getPlayer(name);
        if(targetPlayer != null) return targetPlayer;

        UUID bestMatchID = this.playerNameToIDMap.get(name.toLowerCase());
        
        if(bestMatchID == null) {
            return null;
        }

		return this.getServer().getOfflinePlayer(bestMatchID);
	}
    
    public UUID resolvePlayerId(String name)  {
		//try online players first
        return this.playerNameToIDMap.get(name.toLowerCase());
	}

	//helper method to resolve a player name from the player's UUID
    static String lookupPlayerName(UUID playerID) 
    {
        //parameter validation
        if(playerID == null || playerID == UUID0) return "somebody";
        if(playerID == UUID1) return "an administrator";
        
        //check the cache
        OfflinePlayer player = GriefPreventionPlus.instance.getServer().getOfflinePlayer(playerID);
        if(player.hasPlayedBefore() || player.isOnline()) {
            return player.getName();
        } else {
            return "someone";
        }
    }
    
    //cache for player name lookups, to save searches of all offline players
    static void cacheUUIDNamePair(UUID playerID, String playerName)
    {
        //store the reverse mapping
        GriefPreventionPlus.instance.playerNameToIDMap.put(playerName.toLowerCase(), playerID);
    }
	
	public void onDisable()
	{ 
		if (this.dataStore!=null) {
			//save data for any online players
			Player [] players = this.getServer().getOnlinePlayers();
			for(int i = 0; i < players.length; i++)
			{
				UUID playerID = players[i].getUniqueId();
				PlayerData playerData = this.dataStore.getPlayerData(playerID);
				this.dataStore.savePlayerDataSync(playerID, playerData);
			}
			this.dataStore.close();
		}
		
		AddLogEntry("GriefPreventionPlus disabled.");
	}
	
	//called when a player spawns, applies protection for that player if necessary
	public void checkPvpProtectionNeeded(Player player)
	{
	    //if anti spawn camping feature is not enabled, do nothing
        if(!this.config_pvp_protectFreshSpawns) return;
        
	    //if pvp is disabled, do nothing
		if(!this.config_pvp_enabledWorlds.contains(player.getWorld())) return;
		
		//if player is in creative mode, do nothing
		if(player.getGameMode() == GameMode.CREATIVE) return;
		
		//if the player has the damage any player permission enabled, do nothing
		if(player.hasPermission("griefprevention.nopvpimmunity")) return;
		
		//check inventory for well, anything
		PlayerInventory inventory = player.getInventory();
		ItemStack [] armorStacks = inventory.getArmorContents();
		
		//check armor slots, stop if any items are found
		for(int i = 0; i < armorStacks.length; i++)
		{
			if(!(armorStacks[i] == null || armorStacks[i].getType() == Material.AIR)) return;
		}
		
		//check other slots, stop if any items are found
		ItemStack [] generalStacks = inventory.getContents();
		for(int i = 0; i < generalStacks.length; i++)
		{
			if(!(generalStacks[i] == null || generalStacks[i].getType() == Material.AIR)) return;
		}
			
		//otherwise, apply immunity
		PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
		playerData.pvpImmune = true;
		
		//inform the player after he finishes respawning
		GriefPreventionPlus.sendMessage(player, TextMode.Success, Messages.PvPImmunityStart, 5L);
	}
	
	//checks whether players siege in a world
	public boolean siegeEnabledForWorld(World world)
	{
		return this.config_siege_enabledWorlds.contains(world);
	}

	//moves a player from the claim he's in to a nearby wilderness location
	public Location ejectPlayer(Player player)
	{
		//look for a suitable location
		Location candidateLocation = player.getLocation();
		while(true)
		{
			Claim claim = null;
			claim = GriefPreventionPlus.instance.dataStore.getClaimAt(candidateLocation, false, null);
			
			//if there's a claim here, keep looking
			if(claim != null)
			{
				candidateLocation = new Location(claim.world, claim.lesserX - 1, 0, claim.lesserZ - 1);
				continue;
			}
			
			//otherwise find a safe place to teleport the player
			else
			{
				//find a safe height, a couple of blocks above the surface
				GuaranteeChunkLoaded(candidateLocation);
				Block highestBlock = candidateLocation.getWorld().getHighestBlockAt(candidateLocation.getBlockX(), candidateLocation.getBlockZ());
				Location destination = new Location(highestBlock.getWorld(), highestBlock.getX(), highestBlock.getY() + 2, highestBlock.getZ());
				player.teleport(destination);			
				return destination;
			}			
		}
	}
	
	//ensures a piece of the managed world is loaded into server memory
	//(generates the chunk if necessary)
	private static void GuaranteeChunkLoaded(Location location)
	{
		Chunk chunk = location.getChunk();
		while(!chunk.isLoaded() || !chunk.load(true));
	}
	
	//sends a color-coded message to a player
	static void sendMessage(Player player, ChatColor color, Messages messageID, String... args)
	{
		sendMessage(player, color, messageID, 0, args);
	}
	
	//sends a color-coded message to a player
	static void sendMessage(Player player, ChatColor color, Messages messageID, long delayInTicks, String... args)
	{
		String message = GriefPreventionPlus.instance.dataStore.getMessage(messageID, args);
		sendMessage(player, color, message, delayInTicks);
	}
	
	//sends a color-coded message to a player
	static void sendMessage(Player player, ChatColor color, String message)
	{
		if(message == null || message.length() == 0) return;
		
	    if(player == null)
		{
			GriefPreventionPlus.AddLogEntry(color + message);
		}
		else
		{
			player.sendMessage(color + message);
		}
	}
	
	static void sendMessage(Player player, ChatColor color, String message, long delayInTicks)
	{
		SendPlayerMessageTask task = new SendPlayerMessageTask(player, color, message);
		if(delayInTicks > 0)
		{
			GriefPreventionPlus.instance.getServer().getScheduler().runTaskLater(GriefPreventionPlus.instance, task, delayInTicks);
		}
		else
		{
			task.run();
		}
	}
	
	//checks whether players can create claims in a world
    public boolean claimsEnabledForWorld(World world)
    {
        return this.config_claims_worldModes.get(world) != ClaimsMode.Disabled;
    }
    
    //determines whether creative anti-grief rules apply at a location
	boolean creativeRulesApply(World world)
	{
		return this.config_claims_worldModes.get((world)) == ClaimsMode.Creative;
	}
	
	public String allowBuild(Player player, Location location)
	{
	    return this.allowBuild(player, location, location.getBlock().getType());
	}
	
	public String allowBuild(Player player, Location location, Material material)
	{
		PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
		Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);
		
		//exception: administrators in ignore claims mode and special player accounts created by server mods
		if(playerData.ignoreClaims || GriefPreventionPlus.instance.config_mods_ignoreClaimsAccounts.contains(player.getName())) return null;
		
		//wilderness rules
		if(claim == null)
		{
			//no building in the wilderness in creative mode
			if(this.creativeRulesApply(location.getWorld()))
			{
				String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims);
				if(player.hasPermission("griefprevention.ignoreclaims"))
					reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
				reason += "  " + this.dataStore.getMessage(Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
				return reason;
			}
			
		    //but it's fine in survival mode
		    else
			{
				return null;
			}			
		}
		
		//if not in the wilderness, then apply claim rules (permissions, etc)
		else
		{
			//cache the claim for later reference
			playerData.lastClaim = claim;
			return claim.allowBuild(player, material);
		}
	}
	
	public String allowBreak(Player player, Block block, Location location)
	{
		PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
		Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);
		
		//exception: administrators in ignore claims mode, and special player accounts created by server mods
		if(playerData.ignoreClaims || GriefPreventionPlus.instance.config_mods_ignoreClaimsAccounts.contains(player.getName())) return null;
		
		//wilderness rules
		if(claim == null)
		{
			//no building in the wilderness in creative mode
			if(this.creativeRulesApply(location.getWorld()))
			{
				String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims);
				if(player.hasPermission("griefprevention.ignoreclaims"))
					reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
				reason += "  " + this.dataStore.getMessage(Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL);
				return reason;
			}
			
			//but it's fine in survival mode
			else
			{
				return null;
			}
		}
		else
		{
			//cache the claim for later reference
			playerData.lastClaim = claim;
		
			//if not in the wilderness, then apply claim rules (permissions, etc)
			return claim.allowBreak(player, block.getType());
		}
	}

	//restores nature in multiple chunks, as described by a claim instance
	//this restores all chunks which have ANY number of claim blocks from this claim in them
	//if the claim is still active (in the data store), then the claimed blocks will not be changed (only the area bordering the claim)
	public void restoreClaim(Claim claim, long delayInTicks)
	{
		//admin claims aren't automatically cleaned up when deleted or abandoned
		if(claim.isAdminClaim()) return;
		
		//it's too expensive to do this for huge claims
		if(claim.getArea() > 10000) return;
		
		ArrayList<Chunk> chunks = claim.getChunks();
        for(Chunk chunk : chunks)
        {
			this.restoreChunk(chunk, this.getSeaLevel(chunk.getWorld()) - 15, false, delayInTicks, null);
        }
	}
	
	public void restoreChunk(Chunk chunk, int miny, boolean aggressiveMode, long delayInTicks, Player playerReceivingVisualization)
	{
		//build a snapshot of this chunk, including 1 block boundary outside of the chunk all the way around
		int maxHeight = chunk.getWorld().getMaxHeight();
		BlockSnapshot[][][] snapshots = new BlockSnapshot[18][maxHeight][18];
		Block startBlock = chunk.getBlock(0, 0, 0);
		Location startLocation = new Location(chunk.getWorld(), startBlock.getX() - 1, 0, startBlock.getZ() - 1);
		for(int x = 0; x < snapshots.length; x++)
		{
			for(int z = 0; z < snapshots[0][0].length; z++)
			{
				for(int y = 0; y < snapshots[0].length; y++)
				{
					Block block = chunk.getWorld().getBlockAt(startLocation.getBlockX() + x, startLocation.getBlockY() + y, startLocation.getBlockZ() + z);
					snapshots[x][y][z] = new BlockSnapshot(block.getLocation(), block.getTypeId(), block.getData());
				}
			}
		}
		
		//create task to process those data in another thread
		Location lesserBoundaryCorner = chunk.getBlock(0,  0, 0).getLocation();
		Location greaterBoundaryCorner = chunk.getBlock(15, 0, 15).getLocation();
		
		//create task
		//when done processing, this task will create a main thread task to actually update the world with processing results
		RestoreNatureProcessingTask task = new RestoreNatureProcessingTask(snapshots, miny, chunk.getWorld().getEnvironment(), lesserBoundaryCorner.getBlock().getBiome(), lesserBoundaryCorner, greaterBoundaryCorner, this.getSeaLevel(chunk.getWorld()), aggressiveMode, GriefPreventionPlus.instance.creativeRulesApply(lesserBoundaryCorner.getWorld()), playerReceivingVisualization);
		GriefPreventionPlus.instance.getServer().getScheduler().runTaskLaterAsynchronously(GriefPreventionPlus.instance, task, delayInTicks);
	}
	
	private void parseMaterialListFromConfig(List<String> stringsToParse, MaterialCollection materialCollection)
	{
		materialCollection.clear();
		
		//for each string in the list
		for(int i = 0; i < stringsToParse.size(); i++)
		{
			//try to parse the string value into a material info
			MaterialInfo materialInfo = MaterialInfo.fromString(stringsToParse.get(i));
			
			//null value returned indicates an error parsing the string from the config file
			if(materialInfo == null)
			{
				//show error in log
				GriefPreventionPlus.AddLogEntry("ERROR: Unable to read a material entry from the config file.  Please update your config.yml.");
				
				//update string, which will go out to config file to help user find the error entry
				if(!stringsToParse.get(i).contains("can't"))
				{
					stringsToParse.set(i, stringsToParse.get(i) + "     <-- can't understand this entry, see BukkitDev documentation");
				}
			}
			
			//otherwise store the valid entry in config data
			else
			{
				materialCollection.Add(materialInfo);
			}
		}		
	}
	
	public int getSeaLevel(World world)
	{
		Integer overrideValue = this.config_seaLevelOverride.get(world.getName());
		if(overrideValue == null || overrideValue == -1)
		{
			return world.getSeaLevel();
		}
		else
		{
			return overrideValue;
		}		
	}
	
	static Block getTargetNonAirBlock(Player player, int maxDistance) throws IllegalStateException
    {
        BlockIterator iterator = new BlockIterator(player.getLocation(), player.getEyeHeight(), maxDistance);
        Block result = player.getLocation().getBlock().getRelative(BlockFace.UP);
        while (iterator.hasNext())
        {
            result = iterator.next();
            if(result.getType() != Material.AIR) return result;
        }
        
        return result;
    }
}