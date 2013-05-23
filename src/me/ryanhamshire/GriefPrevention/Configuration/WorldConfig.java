package me.ryanhamshire.GriefPrevention.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.MaterialCollection;
import me.ryanhamshire.GriefPrevention.MaterialInfo;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * represents the configuration settings of a single world.
 * 
 * @author BC_Programming
 *
 */
public class WorldConfig {

	
	//private members followed by their read-only accessor.
	private boolean claims_Seige_Enabled;
	/**
	 * Returns whether Seige is Enabled for this world.
	 * @return
	 */
	public boolean Seige_Enabled(){ return claims_Seige_Enabled;}
	private boolean claims_enabled;
	/**
	 * returns whether Claims are enabled. Most configuration Options, while still present and readable, become redundant when this is false.
	 * @return
	 */
	public boolean claims_enabled(){ return claims_enabled;}
	private boolean claims_creative_rules;
	/**
	 * returns whether Creative mode rules are being applied to this world.
	 * @return
	 */
	public boolean creative_rules(){ return claims_creative_rules;}
	private List<Material> config_trash_blocks=null;
	/**
	 * returns the List of Trash block materials for this world. These are Materials that can be 
	 * -placed in survival
	 * @return
	 */
	public List<Material> getTrashBlocks() { return config_trash_blocks;}
	private int config_message_cooldown_claims = 0; //claims cooldown. 0= no cooldown.
	public int message_cooldown_claims(){ return config_message_cooldown_claims;}
	private int config_message_cooldown_stuck = 0; //stuck cooldown. 0= no cooldown.
	public int message_cooldown_stuck(){return config_message_cooldown_stuck;}
	private int config_claimcleanup_maximumsize;  //maximum size of claims to cleanup. larger claims are not cleaned up.
	public int claimcleanup_maximumsize(){ return config_claimcleanup_maximumsize;}
	private int config_claimcleanup_maxinvestmentscore; //maximum investmentscore. claims with a higher score will not be cleaned up. if set to 0, claim cleanup will not have it's score calculated.
	public int claimcleanup_maxinvestmentscore() { return config_claimcleanup_maxinvestmentscore;}
	
	private boolean config_entitycleanup_enabled;
	public boolean entitycleanup_enabled(){ return config_entitycleanup_enabled;}
//	private boolean config_treecleanup_enabled;
	private boolean config_claimcleanup_enabled;                       //whether the cleanup task is activated.
	public boolean claimcleanup_enabled(){ return config_claimcleanup_enabled;}
	public boolean treecleanup_enabled(){return config_claimcleanup_enabled;}
//	private boolean config_naturerestorecleanup_enabled;
	private boolean config_claims_AllowEnvironmentalVehicleDamage;                 //whether Entities can take damage from the environment in a claim.
	public boolean claims_AllowEnvironmentalVehicleDamage(){return config_claims_AllowEnvironmentalVehicleDamage;}
	private double  config_claims_AbandonReturnRatio;                //return ratio when abandoning a claim- .80 will result in players getting 80% of the used claim blocks back.
	public double claims_AbandonReturnRatio(){ return config_claims_AbandonReturnRatio;}
	private boolean config_claims_preventTheft;						//whether containers and crafting blocks are protectable
	public boolean claims_preventTheft(){ return config_claims_preventTheft;}
	private boolean config_claims_protectCreatures;					//whether claimed animals may be injured by players without permission
	public boolean claims_protectCreatures(){ return config_claims_protectCreatures;}
	private boolean config_claims_preventButtonsSwitches;			//whether buttons and switches are protectable
	public boolean claims_preventButtonsSwitches(){ return config_claims_preventButtonsSwitches;}
	private boolean config_claims_lockWoodenDoors;					//whether wooden doors should be locked by default (require /accesstrust)
	public boolean claims_lockWoodenDoors(){ return config_claims_lockWoodenDoors;}
	private boolean config_claims_lockTrapDoors;						//whether trap doors should be locked by default (require /accesstrust)
	public boolean claims_lockTrapDoors(){ return config_claims_lockTrapDoors;}
	private boolean config_claims_lockFenceGates;					//whether fence gates should be locked by default (require /accesstrust)
	public boolean claims_lockFenceGates(){ return config_claims_lockFenceGates;}
	private boolean config_claims_enderPearlsRequireAccessTrust;		//whether teleporting into a claim with a pearl requires access trust
	public boolean claims_enderPearlsRequireAccessTrust(){ return config_claims_enderPearlsRequireAccessTrust;}
	
	
	private int config_claims_blocksAccruedPerHour;					//how many additional blocks players get each hour of play (can be zero)
	public int claims_blocksAccruedPerHour(){ return config_claims_blocksAccruedPerHour;}
	
	
	//public int claims_maxAccruedBlocks(){ return config_claims_maxAccruedBlocks;}
	private int config_claims_maxDepth;								//limit on how deep claims can go
	public int claims_maxDepth(){ return config_claims_maxDepth;}
	private int config_claims_expirationDays;						//how many days of inactivity before a player loses his claims
	public int claims_expirationDays(){ return config_claims_expirationDays;}
	private boolean config_claims_preventTrades;                     //prevent trades on claims players don't have permissions on
	public boolean claims_preventTrades(){ return config_claims_preventTrades;}
	private int config_claims_automaticClaimsForNewPlayersRadius;	//how big automatic new player claims (when they place a chest) should be.  0 to disable
	public int claims_automaticClaimsForNewPlayerRadius(){ return config_claims_automaticClaimsForNewPlayersRadius;}
	private boolean config_claims_creationRequiresPermission;		//whether creating claims with the shovel requires a permission
	public boolean claims_creationRequiresPermission(){ return config_claims_creationRequiresPermission;}
	private int config_claims_claimsExtendIntoGroundDistance;		//how far below the shoveled block a new claim will reach
	public int claims_claimsExtendIntoGroundDistance(){ return config_claims_claimsExtendIntoGroundDistance;}
	private int config_claims_minSize;								//minimum width and height for non-admin claims
	public int claims_minSize(){ return config_claims_minSize;}
	private boolean config_claims_allowWitherGrief;                   //when set, Withers do not respect ClaimExplosions, or set claims.
	public boolean claims_allowWitherGrief(){ return config_claims_allowWitherGrief;}
	private boolean config_claims_allowCreeperGrief;
	public boolean claims_allowCreeperGrief(){ return config_claims_allowCreeperGrief;}

	private boolean config_blockSurfaceOtherExplosions;
	public boolean blockSurfaceOtherExplosions(){ return config_blockSurfaceOtherExplosions;}
	
	private boolean config_claims_creativeRules;
	public boolean claims_creativeRules(){ return config_claims_creativeRules;}
	
	private boolean config_claims_allowUnclaim;			//whether players may unclaim land (resize or abandon) 
	public boolean claims_allowUnclaim(){ return config_claims_allowUnclaim;}
	private boolean config_claims_autoRestoreUnclaimed; 	//whether unclaimed land in creative worlds is automatically /restorenature-d
	public boolean claims_autoRestoreUnclaimed(){ return config_claims_autoRestoreUnclaimed;}
	private boolean config_claims_noBuildOutsideClaims;				//whether players can build in survival worlds outside their claimed areas
	public boolean claims_noBuildOutsideClaims(){ return config_claims_noBuildOutsideClaims;}
	
	private int config_claims_chestClaimExpirationDays;				//number of days of inactivity before an automatic chest claim will be deleted
	public int claims_chestClaimExpirationDays(){ return config_claims_chestClaimExpirationDays;}
	private int config_claims_unusedClaimExpirationDays;				//number of days of inactivity before an unused (nothing build) claim will be deleted
	public int claims_unusedClaimExpirationDays(){ return config_claims_unusedClaimExpirationDays;}
	private boolean config_claims_AutoNatureRestoration;		//whether survival claims will be automatically restored to nature when auto-deleted
	public boolean claims_AutoNatureRestoration(){ return config_claims_AutoNatureRestoration;}
	private boolean config_claims_Abandon_NatureRestoration; //whether survival claims will be automatically restored to nature when abandoned.
	public boolean claims_AbandonNatureRestoration(){ return config_claims_Abandon_NatureRestoration;}
	
	private int config_claims_trappedCooldownHours;					//number of hours between uses of the /trapped command
	public int claims_trappedCooldownHours(){ return config_claims_trappedCooldownHours;}
	
	private Material config_claims_investigationTool;				//which material will be used to investigate claims with a right click
	public Material claims_investigationTool(){ return config_claims_investigationTool;}
	private Material config_claims_modificationTool;	  				//which material will be used to create/resize claims with a right click
	public Material claims_modificationTool(){ return config_claims_modificationTool;}
	
	
	private ArrayList<Material> config_siege_blocks;					//which blocks will be breakable in siege mode
	public List<Material> siege_blocks() { return config_siege_blocks;}
	private boolean config_spam_enabled;								//whether or not to monitor for spam
	public boolean spam_enabled(){ return config_spam_enabled;}
	private int config_spam_loginCooldownMinutes;					//how long players must wait between logins.  combats login spam.
	public int spam_loginCooldownMinutes(){ return config_spam_loginCooldownMinutes;}
	private ArrayList<String> config_spam_monitorSlashCommands;  	//the list of slash commands monitored for spam
	public List<String> spam_monitorSlashCommands(){ return config_spam_monitorSlashCommands;}
	private boolean config_spam_banOffenders;						//whether or not to ban spammers automatically
	public boolean spam_banOffenders(){ return config_spam_banOffenders;}
	private String config_spam_banMessage;							//message to show an automatically banned player
	public String spam_banMessage(){ return config_spam_banMessage;}
	private String config_spam_warningMessage;						//message to show a player who is close to spam level
	public String spam_warningMessage(){return config_spam_warningMessage;}
	private String config_spam_allowedIpAddresses;					//IP addresses which will not be censored
	public String spam_allowedIpAddresses(){ return config_spam_allowedIpAddresses;}
	private int config_spam_deathMessageCooldownSeconds;				//cooldown period for death messages (per player) in seconds
	public int spam_deathMessageCooldownSeconds(){ return config_spam_deathMessageCooldownSeconds;}
	private String config_spam_bancommand;                           //command to run when spam detector triggers. {0} will be player name.
	public String spam_bancommand(){ return config_spam_bancommand;}
	private String config_spam_kickcommand;                          //command(s) to run when spam detector triggers and kicks. {0} will be player name.
	public String spam_kickcommand(){ return config_spam_kickcommand;}
	//private ArrayList<World> config_pvp_enabledWorlds;				//list of worlds where pvp anti-grief rules apply
	private boolean config_pvp_protectFreshSpawns;					//whether to make newly spawned players immune until they pick up an item
	public boolean protectFreshSpawns(){ return config_pvp_protectFreshSpawns;}
	private boolean config_pvp_punishLogout;						    //whether to kill players who log out during PvP combat
	public boolean pvp_punishLogout() { return config_pvp_punishLogout;}
	private int config_pvp_combatTimeoutSeconds;						//how long combat is considered to continue after the most recent damage
	public int pvp_combatTimeoutSeconds(){ return config_pvp_combatTimeoutSeconds;}
	private boolean config_pvp_allowCombatItemDrop;					//whether a player can drop items during combat to hide them
	public boolean pvp_allowCombatItemDrop(){ return config_pvp_allowCombatItemDrop;}
	private ArrayList<String> config_pvp_blockedCommands;			//list of commands which may not be used during pvp combat
	public List<String> pvp_blockedCommands(){ return config_pvp_blockedCommands;}
	private boolean config_pvp_noCombatInPlayerLandClaims;			//whether players may fight in player-owned land claims
	public boolean pvp_noCombatinPlayerClaims(){ return config_pvp_noCombatInPlayerLandClaims;}
	private boolean config_pvp_noCombatInAdminLandClaims;			//whether players may fight in admin-owned land claims
	public boolean pvp_noCombatinAdminClaims(){ return config_pvp_noCombatInAdminLandClaims;}
	
	private boolean config_trees_removeFloatingTreetops;				//whether to automatically remove partially cut trees
	public boolean trees_removeFloatingTreetops(){ return config_trees_removeFloatingTreetops;}
	private boolean config_trees_regrowGriefedTrees;					//whether to automatically replant partially cut trees
	public boolean trees_regrowGriefedTrees(){ return config_trees_regrowGriefedTrees;}
	
	
	
	
	private boolean config_blockWildernessWaterBuckets;				//whether players can dump water buckets outside their claims
	public boolean blockWildernessWaterBuckets(){ return config_blockWildernessWaterBuckets;}
	private boolean config_blockSkyTrees;							//whether players can build trees on platforms in the sky
	public boolean blockSkyTrees(){ return config_blockSkyTrees;}
	
	private boolean config_fireSpreads;								//whether fire spreads outside of claims
	public boolean fireSpreads(){ return config_fireSpreads;}
	private boolean config_fireDestroys;								//whether fire destroys blocks outside of claims
	public boolean fireDestroys(){ return config_fireDestroys;}
	
	private boolean config_addItemsToClaimedChests;					//whether players may add items to claimed chests by left-clicking them
	public boolean addItemsToClaimedChests(){ return config_addItemsToClaimedChests;}
	private boolean config_sign_Eavesdrop;                           //whether to allow sign eavesdropping at all.
	public boolean sign_Eavesdrop(){ return config_sign_Eavesdrop;}
	private boolean config_eavesdrop; 								//whether whispered messages will be visible to administrators
	public boolean EavesDrop(){ return config_eavesdrop;}
	private ArrayList<String> config_eavesdrop_whisperCommands;		//list of whisper commands to eavesdrop on
	public List<String> eavesdrop_whisperCommands(){ return config_eavesdrop_whisperCommands;}
	
	private boolean config_smartBan;									//whether to ban accounts which very likely owned by a banned player
	public boolean smartBan(){ return config_smartBan;}
	
	private boolean config_endermenMoveBlocks;						//whether or not endermen may move blocks around
	public boolean endermenMoveBlocks(){ return config_endermenMoveBlocks;}
	private boolean config_silverfishBreakBlocks;					//whether silverfish may break blocks
	public boolean silverfishBreakBlocks(){ return config_silverfishBreakBlocks;}
	private boolean config_creaturesTrampleCrops;					//whether or not non-player entities may trample crops
	public boolean creaturesTrampleCrops(){ return config_creaturesTrampleCrops;}
	private boolean config_zombiesBreakDoors;						//whether or not hard-mode zombies may break down wooden doors
	public boolean zombiesBreakDoors(){ return config_zombiesBreakDoors;}
	
	private MaterialCollection config_mods_accessTrustIds;			//list of block IDs which should require /accesstrust for player interaction
	public MaterialCollection mods_accessTrustIds(){ return config_mods_accessTrustIds;}
	private MaterialCollection config_mods_containerTrustIds;		//list of block IDs which should require /containertrust for player interaction
	public MaterialCollection mods_containerTrustIds(){ return config_mods_containerTrustIds;}
	private List<String> config_mods_ignoreClaimsAccounts;			//list of player names which ALWAYS ignore claims
	public List<String> mods_ignoreClaimsAccounts(){ return config_mods_ignoreClaimsAccounts;}
	private MaterialCollection config_mods_explodableIds;			//list of block IDs which can be destroyed by explosions, even in claimed areas
	public MaterialCollection mods_explodableIds() {return config_mods_explodableIds;}

	private boolean config_claims_warnOnBuildOutside;				//whether players should be warned when they're building in an unclaimed area
	public boolean claims_warnOnBuildOutside(){ return config_claims_warnOnBuildOutside;}
	private int config_seaLevelOverride;
	//private HashMap<String, Integer> config_seaLevelOverride;		//override for sea level, because bukkit doesn't report the right value for all situations
	public Integer seaLevelOverride(){ return config_seaLevelOverride;}
	//configuration option changes the number of non-trash blocks that can be placed before
	//the plugin warns about being in the wilderness and all that guff about
	//players being able to undo your work. 0 disables the display entirely.
	private int config_claims_wildernessBlocksDelay;
	public int claims_wildernessBlocksDelay(){ return config_claims_wildernessBlocksDelay;}
	private int config_claims_perplayer_claim_limit;                        //maximum number of claims a user can have.
	public int claims_perplayer_claims_limit(){ return config_claims_perplayer_claim_limit;}

	
	//config options for explosions.
	private boolean BlockSurfaceWildCreeperExplosions; //creeper Explosions above Sea level are blocked? ClaimExplosions will override this.
	private boolean BlockSurfaceWildTNTExplosions; //TNT explosions above SeaLevel are blocked. Claimexplosions will override this.
	private boolean BlockSurfaceWildOtherExplosions; //block explosions not covered by medicare, I mean the above.
	
	
	
	private String WorldName;
	private boolean config_siege_enabled;
	public String getWorldName(){ return WorldName;}
	
	//constructor accepts a Name and a FileConfiguration.
	public WorldConfig(String pName,FileConfiguration config,FileConfiguration outConfig){
		
		//determine defaults based on the world itself (isCreative, isPvP)
		boolean isCreative=false,isPvP=false;
		World getworld = Bukkit.getWorld(pName);
		if(getworld!=null){
			isCreative = Bukkit.getServer().getDefaultGameMode()==GameMode.CREATIVE;
			isPvP = getworld.getPVP();
		}
		
		
		GriefPrevention.instance.getLogger().log(Level.INFO,"Reading Configuration for World:" + pName);
		this.config_seaLevelOverride = config.getInt("GriefPrevention.SeaLevelOverride");
		
		
		
		//read trash blocks.
		//Cobblestone,Torch,Dirt,Sapling,Gravel,Sand,TNT,Workbench
		this.config_trash_blocks = new ArrayList<Material>();
		for(Material trashblock:new Material[]{Material.COBBLESTONE,
				Material.TORCH,Material.DIRT,Material.SAPLING,Material.GRAVEL,Material.SAND,Material.TNT,Material.WORKBENCH}){
		this.config_trash_blocks.add(trashblock);
		}
		List<String> trashblocks= config.getStringList("GriefPrevention.Claims.TrashBlocks");
		if(trashblocks==null || trashblocks.size()==0){
		//go with the default, which we already set.	
			trashblocks = new ArrayList<String>();
			for(String iterate:new String[] {"COBBLESTONE","TORCH","DIRT","SAPLING","GRAVEL","SAND","TNT","WORKBENCH"}){
				trashblocks.add(iterate);
			}
			//set trashblocks, since we save it to outConfig later, so save out this default. This makes it easier to 
			//edit.
			
		}
		else {
			//reset...
			this.config_trash_blocks=new ArrayList<Material>();
			for(String trashmaterial:trashblocks){
				 try {
					 //replace spaces with underscores...
					 trashmaterial = trashmaterial.replace(" ", "_");
				Material parsed = Material.valueOf(trashmaterial.toUpperCase());
				config_trash_blocks.add(parsed);
				 }
				 catch(IllegalArgumentException iae){
					 //nothing special, log though.
					 GriefPrevention.AddLogEntry("failed to parse trashmaterial Entry:" + trashmaterial.toUpperCase());
				 }
			}
		}

		this.claims_enabled = config.getBoolean("GriefPrevention.Claims.Enabled",true);
		outConfig.set("GriefPrevention.Claims.Enabled", claims_enabled);
		this.config_entitycleanup_enabled = config.getBoolean("GriefPrevention.CleanupTasks.Claims",true);
		outConfig.set("GriefPrevention.CleanupTasks.Entity", this.config_entitycleanup_enabled);
		//this.config_treecleanup_enabled = config.getBoolean("GriefPrevention.CleanupTasks.Trees",true);
		//this.config_naturerestorecleanup_enabled = config.getBoolean("GriefPrevention.CleanupTasks.NatureRestore",true);
		this.config_claimcleanup_enabled = config.getBoolean("GriefPrevention.ClaimCleanup.Enabled",true); 
		this.config_claimcleanup_maximumsize = config.getInt("GriefPrevention.ClaimCleanup.MaximumSize",25);
		//max investment score, defaults to 400 for creative worlds.
		this.config_claimcleanup_maxinvestmentscore = 
				config.getInt("GriefPrevention.ClaimCleanup.MaxInvestmentScore",isCreative?400:100);
		
		config_claims_blocksAccruedPerHour = config.getInt("GriefPrevention.BlocksAccruedPerHour",100);
		outConfig.set("GriefPrevention.BlocksAccruedPerHour",config_claims_blocksAccruedPerHour);
		outConfig.set("GriefPrevention.ClaimCleanup.MaximumSize", config_claimcleanup_maximumsize);
		outConfig.set("GriefPrevention.ClaimCleanup.MaxInvestmentScore", this.config_claimcleanup_maxinvestmentscore);
		outConfig.set("GriefPrevention.ClaimCleanup.Enabled",this.config_claimcleanup_enabled);
		this.config_message_cooldown_claims = config.getInt("GriefPrevention.Expiration.MessageCooldown.Claim",0);
		this.config_message_cooldown_stuck = config.getInt("GriefPrevention.Expiration.MessageCooldown.Stuck",0);
		outConfig.set("GriefPrevention.Expiration.MessageCooldown.Claim", config_message_cooldown_claims);
		outConfig.set("GriefPrevention.Expiration.MessageCooldown.Stuck", config_message_cooldown_stuck);
		
		this.config_claims_wildernessBlocksDelay = config.getInt("GriefPrevention.Claims.WildernessWarningBlockCount",15); //number of blocks,0 will disable the wilderness warning.
		this.config_claims_creativeRules = config.getBoolean("GriefPrevention.CreativeRules",Bukkit.getServer().getDefaultGameMode()==GameMode.CREATIVE);
		this.config_claims_AbandonReturnRatio = config.getDouble("GriefPrevention.Claims.AbandonReturnRatio",1);
		outConfig.set("GriefPrevention.Claims.AbandonReturnRatio", this.config_claims_AbandonReturnRatio);
		outConfig.set("GriefPrevention.CreativeRules",config_claims_creativeRules);
		this.config_sign_Eavesdrop = config.getBoolean("GriefPrevention.SignEavesDrop",true);
		outConfig.set("GriefPrevention.SignEavesDrop", this.config_sign_Eavesdrop);
		
		config_claims_preventTrades = config.getBoolean("GriefPrevention.Claims.PreventTrades",false);
		outConfig.set("GriefPrevention.Claims.PreventTrades", config_claims_preventTrades);
						
		this.config_claims_perplayer_claim_limit = config.getInt("GriefPrevention.Claims.PerPlayerLimit",0);
		
		outConfig.set("GriefPrevention.Claims.PerPlayerLimit",config_claims_perplayer_claim_limit);
		
		this.config_claims_AllowEnvironmentalVehicleDamage = config.getBoolean("GriefPrevention.Claims.EnvironmentalVehicleDamage",true);
		outConfig.set("GriefPrevention.Claims.EnvironmentalVehicleDamage",this.config_claims_AllowEnvironmentalVehicleDamage);
		
		this.config_claims_allowWitherGrief = config.getBoolean("GriefPrevention.Claims.AllowWitherGrief",false);
		outConfig.set("GriefPrevention.Claims.AllowWitherGrief",this.config_claims_allowWitherGrief);
		
		this.config_claims_allowCreeperGrief = config.getBoolean("GriefPrevention.Claims.AllowCreeperGrief",false);
		outConfig.set("GriefPrevention.Claims.AllowCreeperGrief",this.config_claims_allowCreeperGrief);
		
		this.config_claims_preventTheft = config.getBoolean("GriefPrevention.Claims.PreventTheft", true);
		this.config_claims_protectCreatures = config.getBoolean("GriefPrevention.Claims.ProtectCreatures", true);
		this.config_claims_preventButtonsSwitches = config.getBoolean("GriefPrevention.Claims.PreventButtonsSwitches", true);
		this.config_claims_lockWoodenDoors = config.getBoolean("GriefPrevention.Claims.LockWoodenDoors", false);
		this.config_claims_lockTrapDoors = config.getBoolean("GriefPrevention.Claims.LockTrapDoors", false);
		this.config_claims_lockFenceGates = config.getBoolean("GriefPrevention.Claims.LockFenceGates", true);
		this.config_claims_enderPearlsRequireAccessTrust = config.getBoolean("GriefPrevention.Claims.EnderPearlsRequireAccessTrust", true);
		this.config_claims_blocksAccruedPerHour = config.getInt("GriefPrevention.Claims.BlocksAccruedPerHour", 100);
		this.config_claims_automaticClaimsForNewPlayersRadius = config.getInt("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", 4);
		this.config_claims_claimsExtendIntoGroundDistance = config.getInt("GriefPrevention.Claims.ExtendIntoGroundDistance", 5);
		this.config_claims_creationRequiresPermission = config.getBoolean("GriefPrevention.Claims.CreationRequiresPermission", false);
		this.config_claims_minSize = config.getInt("GriefPrevention.Claims.MinimumSize", 10);
		this.config_claims_maxDepth = config.getInt("GriefPrevention.Claims.MaximumDepth", 0);
		this.config_claims_trappedCooldownHours = config.getInt("GriefPrevention.Claims.TrappedCommandCooldownHours", 8);
		this.config_claims_noBuildOutsideClaims = config.getBoolean("GriefPrevention.Claims.NoSurvivalBuildingOutsideClaims", false);
		this.config_claims_warnOnBuildOutside = config.getBoolean("GriefPrevention.Claims.WarnWhenBuildingOutsideClaims", true);
		this.config_claims_allowUnclaim = config.getBoolean("GriefPrevention.Claims.AllowUnclaimingLand", true);
		this.config_claims_autoRestoreUnclaimed = config.getBoolean("GriefPrevention.Claims.AutoRestoreUnclaimedLand", true);		

		this.config_claims_Abandon_NatureRestoration = config.getBoolean("GriefPrevention.Claims.AbandonAutoRestore",false);
		outConfig.set("GriefPrevention.Claims.AbandonAutoRestore",this.config_claims_Abandon_NatureRestoration);
		
		this.config_claims_Abandon_NatureRestoration = config.getBoolean("GriefPrevention.Claims.AbandonAutoRestore",false);
		outConfig.set("GriefPrevention.Claims.AbandonAutoRestore",this.config_claims_Abandon_NatureRestoration);
		
		this.config_claims_chestClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.ChestClaimDays", 7);
		outConfig.set("GriefPrevention.Claims.Expiration.ChestClaimDays", this.config_claims_chestClaimExpirationDays);
		
		this.config_claims_unusedClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.UnusedClaimDays", 14);
		outConfig.set("GriefPrevention.Claims.Expiration.UnusedClaimDays", this.config_claims_unusedClaimExpirationDays);		
		
		this.config_claims_expirationDays = config.getInt("GriefPrevention.Claims.Expiration.AllClaimDays", 0);
		outConfig.set("GriefPrevention.Claims.Expiration.AllClaimDays", this.config_claims_expirationDays);
		
		this.config_claims_AutoNatureRestoration = config.getBoolean("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration", false);
		outConfig.set("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration", this.config_claims_AutoNatureRestoration);		
		
				
		
		this.config_spam_enabled = config.getBoolean("GriefPrevention.Spam.Enabled", true);
		this.config_spam_loginCooldownMinutes = config.getInt("GriefPrevention.Spam.LoginCooldownMinutes", 2);
		this.config_spam_warningMessage = config.getString("GriefPrevention.Spam.WarningMessage", "Please reduce your noise level.  Spammers will be banned.");
		this.config_spam_allowedIpAddresses = config.getString("GriefPrevention.Spam.AllowedIpAddresses", "1.2.3.4; 5.6.7.8");
		this.config_spam_banOffenders = config.getBoolean("GriefPrevention.Spam.BanOffenders", true);		
		this.config_spam_banMessage = config.getString("GriefPrevention.Spam.BanMessage", "Banned for spam.");
		String slashCommandsToMonitor = config.getString("GriefPrevention.Spam.MonitorSlashCommands", "/me;/tell;/global;/local");
		this.config_spam_deathMessageCooldownSeconds = config.getInt("GriefPrevention.Spam.DeathMessageCooldownSeconds", 60);		
		
		
		this.config_spam_kickcommand = config.getString("GriefPrevention.Spam.KickCommand","kick {0}");
		this.config_spam_bancommand = config.getString("GriefPrevention.Spam.BanCommand","ban {0};kick {0}");
		outConfig.set("GriefPrevention.Spam.KickCommand", this.config_spam_kickcommand);
		outConfig.set("GriefPrevention.Spam.BanCommand", config_spam_bancommand);
		
		this.config_pvp_protectFreshSpawns = config.getBoolean("GriefPrevention.PvP.ProtectFreshSpawns", true);
		this.config_pvp_punishLogout = config.getBoolean("GriefPrevention.PvP.PunishLogout", true);
		this.config_pvp_combatTimeoutSeconds = config.getInt("GriefPrevention.PvP.CombatTimeoutSeconds", 15);
		this.config_pvp_allowCombatItemDrop = config.getBoolean("GriefPrevention.PvP.AllowCombatItemDrop", false);
		String bannedPvPCommandsList = config.getString("GriefPrevention.PvP.BlockedSlashCommands", "/home;/vanish;/spawn;/tpa");
		
		this.config_trees_removeFloatingTreetops = config.getBoolean("GriefPrevention.Trees.RemoveFloatingTreetops", true);
		this.config_trees_regrowGriefedTrees = config.getBoolean("GriefPrevention.Trees.RegrowGriefedTrees", true);
		
		
		
		this.BlockSurfaceWildCreeperExplosions = config.getBoolean("GriefPrevention.Explosions.Surface.BlockCreeper", true);
		this.BlockSurfaceWildTNTExplosions = config.getBoolean("GriefPrevention.Explosions.Surface.BlockTNT", true);
		this.BlockSurfaceWildOtherExplosions = config.getBoolean("GriefPrevention.Explosions.Surface.BlockOther",true);
		this.config_blockWildernessWaterBuckets = config.getBoolean("GriefPrevention.LimitSurfaceWaterBuckets", true);
		this.config_blockSkyTrees = config.getBoolean("GriefPrevention.LimitSkyTrees", true);
				
		this.config_fireSpreads = config.getBoolean("GriefPrevention.FireSpreads", false);
		this.config_fireDestroys = config.getBoolean("GriefPrevention.FireDestroys", false);
		
		this.config_addItemsToClaimedChests = config.getBoolean("GriefPrevention.AddItemsToClaimedChests", true);
		this.config_eavesdrop = config.getBoolean("GriefPrevention.EavesdropEnabled", false);
		String whisperCommandsToMonitor = config.getString("GriefPrevention.WhisperCommands", "/tell;/pm;/r");
		
		this.config_smartBan = config.getBoolean("GriefPrevention.SmartBan", true);
		
		this.config_endermenMoveBlocks = config.getBoolean("GriefPrevention.EndermenMoveBlocks", false);
		this.config_silverfishBreakBlocks = config.getBoolean("GriefPrevention.SilverfishBreakBlocks", false);
		this.config_creaturesTrampleCrops = config.getBoolean("GriefPrevention.CreaturesTrampleCrops", false);
		this.config_zombiesBreakDoors = config.getBoolean("GriefPrevention.HardModeZombiesBreakDoors", false);
		
		this.config_mods_ignoreClaimsAccounts = config.getStringList("GriefPrevention.Mods.PlayersIgnoringAllClaims");
		
		if(this.config_mods_ignoreClaimsAccounts == null) this.config_mods_ignoreClaimsAccounts = new ArrayList<String>();
		
		this.config_mods_accessTrustIds = new MaterialCollection();
		List<String> accessTrustStrings = config.getStringList("GriefPrevention.Mods.BlockIdsRequiringAccessTrust");
		
		//default values for access trust mod blocks
		if(accessTrustStrings == null || accessTrustStrings.size() == 0)
		{
			//none by default
		}
		
		GriefPrevention.instance.parseMaterialListFromConfig(accessTrustStrings, this.config_mods_accessTrustIds);
		
		this.config_mods_containerTrustIds = new MaterialCollection();
		List<String> containerTrustStrings = config.getStringList("GriefPrevention.Mods.BlockIdsRequiringContainerTrust");
		
		//default values for container trust mod blocks
		if(containerTrustStrings == null || containerTrustStrings.size() == 0)
		{
			containerTrustStrings.add(new MaterialInfo(227, "Battery Box").toString());
			containerTrustStrings.add(new MaterialInfo(130, "Transmutation Tablet").toString());
			containerTrustStrings.add(new MaterialInfo(128, "Alchemical Chest and Energy Condenser").toString());
			containerTrustStrings.add(new MaterialInfo(181, "Various Chests").toString());
			containerTrustStrings.add(new MaterialInfo(178, "Ender Chest").toString());
			containerTrustStrings.add(new MaterialInfo(150, "Various BuildCraft Gadgets").toString());
			containerTrustStrings.add(new MaterialInfo(155, "Filler").toString());
			containerTrustStrings.add(new MaterialInfo(157, "Builder").toString());
			containerTrustStrings.add(new MaterialInfo(158, "Template Drawing Table").toString());
			containerTrustStrings.add(new MaterialInfo(126, "Various EE Gadgets").toString());
			containerTrustStrings.add(new MaterialInfo(138, "Various RedPower Gadgets").toString());
			containerTrustStrings.add(new MaterialInfo(137, "BuildCraft Project Table and Furnaces").toString());
			containerTrustStrings.add(new MaterialInfo(250, "Various IC2 Machines").toString());
			containerTrustStrings.add(new MaterialInfo(161, "BuildCraft Engines").toString());
			containerTrustStrings.add(new MaterialInfo(169, "Automatic Crafting Table").toString());
			containerTrustStrings.add(new MaterialInfo(177, "Wireless Components").toString());
			containerTrustStrings.add(new MaterialInfo(183, "Solar Arrays").toString());
			containerTrustStrings.add(new MaterialInfo(187, "Charging Benches").toString());
			containerTrustStrings.add(new MaterialInfo(188, "More IC2 Machines").toString());
			containerTrustStrings.add(new MaterialInfo(190, "Generators, Fabricators, Strainers").toString());
			containerTrustStrings.add(new MaterialInfo(194, "More Gadgets").toString());
			containerTrustStrings.add(new MaterialInfo(207, "Computer").toString());
			containerTrustStrings.add(new MaterialInfo(208, "Computer Peripherals").toString());
			containerTrustStrings.add(new MaterialInfo(246, "IC2 Generators").toString());
			containerTrustStrings.add(new MaterialInfo(24303, "Teleport Pipe").toString());
			containerTrustStrings.add(new MaterialInfo(24304, "Waterproof Teleport Pipe").toString());
			containerTrustStrings.add(new MaterialInfo(24305, "Power Teleport Pipe").toString());
			containerTrustStrings.add(new MaterialInfo(4311, "Diamond Sorting Pipe").toString());
			containerTrustStrings.add(new MaterialInfo(216, "Turtle").toString());
			
		}
		
		//parse the strings from the config file
		GriefPrevention.instance.parseMaterialListFromConfig(containerTrustStrings, this.config_mods_containerTrustIds);
		
		this.config_mods_explodableIds = new MaterialCollection();
		List<String> explodableStrings = config.getStringList("GriefPrevention.Mods.BlockIdsExplodable");
		
		//default values for explodable mod blocks
		if(explodableStrings == null || explodableStrings.size() == 0)
		{
			explodableStrings.add(new MaterialInfo(161, "BuildCraft Engines").toString());			
			explodableStrings.add(new MaterialInfo(246, (byte)5 ,"Nuclear Reactor").toString());
		}
		
		//parse the strings from the config file
		GriefPrevention.instance.parseMaterialListFromConfig(explodableStrings, this.config_mods_explodableIds);
		
		//default for claim investigation tool
		String investigationToolMaterialName = Material.STICK.name();
		
		//get investigation tool from config
		investigationToolMaterialName = config.getString("GriefPrevention.Claims.InvestigationTool", investigationToolMaterialName);
		
		//validate investigation tool
		this.config_claims_investigationTool = Material.getMaterial(investigationToolMaterialName);
		if(this.config_claims_investigationTool == null)
		{
			GriefPrevention.AddLogEntry("ERROR: Material " + investigationToolMaterialName + " not found.  Defaulting to the stick.  Please update your config.yml.");
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
			GriefPrevention.AddLogEntry("ERROR: Material " + modificationToolMaterialName + " not found.  Defaulting to the golden shovel.  Please update your config.yml.");
			this.config_claims_modificationTool = Material.GOLD_SPADE;
		}
		
		//default for siege worlds list
		ArrayList<String> defaultSiegeWorldNames = new ArrayList<String>();
		this.config_siege_enabled = config.getBoolean("GriefPrevention.Siege.Enabled",isPvP);
		//get siege world names from the config file
				
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
				GriefPrevention.AddLogEntry("Siege Configuration: Material not found: " + blockName + ".");
			}
			else
			{
				this.config_siege_blocks.add(material);
			}
		}
		
		this.config_pvp_noCombatInPlayerLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims", !this.config_siege_enabled);
		this.config_pvp_noCombatInAdminLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", !this.config_siege_enabled);
		
		
		
		//outConfig.set("GriefPrevention.Claims.Worlds", claimsEnabledWorldNames);
		//outConfig.set("GriefPrevention.Claims.CreativeRulesWorlds", creativeClaimsEnabledWorldNames);
		outConfig.set("GriefPrevention.Claims.PreventTheft", this.config_claims_preventTheft);
		outConfig.set("GriefPrevention.Claims.ProtectCreatures", this.config_claims_protectCreatures);
		outConfig.set("GriefPrevention.Claims.PreventButtonsSwitches", this.config_claims_preventButtonsSwitches);
		outConfig.set("GriefPrevention.Claims.LockWoodenDoors", this.config_claims_lockWoodenDoors);
		outConfig.set("GriefPrevention.Claims.LockTrapDoors", this.config_claims_lockTrapDoors);
		outConfig.set("GriefPrevention.Claims.LockFenceGates", this.config_claims_lockFenceGates);
		outConfig.set("GriefPrevention.Claims.EnderPearlsRequireAccessTrust", this.config_claims_enderPearlsRequireAccessTrust);
		
		outConfig.set("GriefPrevention.Claims.BlocksAccruedPerHour", this.config_claims_blocksAccruedPerHour);
		
		outConfig.set("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", this.config_claims_automaticClaimsForNewPlayersRadius);
		outConfig.set("GriefPrevention.Claims.ExtendIntoGroundDistance", this.config_claims_claimsExtendIntoGroundDistance);
		outConfig.set("GriefPrevention.Claims.CreationRequiresPermission", this.config_claims_creationRequiresPermission);
		outConfig.set("GriefPrevention.Claims.MinimumSize", this.config_claims_minSize);
		outConfig.set("GriefPrevention.Claims.MaximumDepth", this.config_claims_maxDepth);
		outConfig.set("GriefPrevention.Claims.TrappedCommandCooldownHours", this.config_claims_trappedCooldownHours);
		outConfig.set("GriefPrevention.Claims.InvestigationTool", this.config_claims_investigationTool.name());
		outConfig.set("GriefPrevention.Claims.ModificationTool", this.config_claims_modificationTool.name());
		outConfig.set("GriefPrevention.Claims.NoSurvivalBuildingOutsideClaims", this.config_claims_noBuildOutsideClaims);
		outConfig.set("GriefPrevention.Claims.WarnWhenBuildingOutsideClaims", this.config_claims_warnOnBuildOutside);
		outConfig.set("GriefPrevention.Claims.AllowUnclaimingLand", this.config_claims_allowUnclaim);
		outConfig.set("GriefPrevention.Claims.AutoRestoreUnclaimedLand", this.config_claims_autoRestoreUnclaimed);
		
		outConfig.set("GriefPrevention.Claims.TrashBlocks",trashblocks);
		outConfig.set("GriefPrevention.Claims.WildernessWarningBlockCount", this.config_claims_wildernessBlocksDelay);
		
		outConfig.set("GriefPrevention.Spam.Enabled", this.config_spam_enabled);
		
		outConfig.set("GriefPrevention.Spam.LoginCooldownMinutes", this.config_spam_loginCooldownMinutes);
		outConfig.set("GriefPrevention.Spam.MonitorSlashCommands", slashCommandsToMonitor);
		outConfig.set("GriefPrevention.Spam.WarningMessage", this.config_spam_warningMessage);
		outConfig.set("GriefPrevention.Spam.BanOffenders", this.config_spam_banOffenders);		
		outConfig.set("GriefPrevention.Spam.BanMessage", this.config_spam_banMessage);
		outConfig.set("GriefPrevention.Spam.AllowedIpAddresses", this.config_spam_allowedIpAddresses);
		outConfig.set("GriefPrevention.Spam.DeathMessageCooldownSeconds", this.config_spam_deathMessageCooldownSeconds);
		
		//outConfig.set("GriefPrevention.PvP.Worlds", pvpEnabledWorldNames);
		outConfig.set("GriefPrevention.PvP.ProtectFreshSpawns", this.config_pvp_protectFreshSpawns);
		outConfig.set("GriefPrevention.PvP.PunishLogout", this.config_pvp_punishLogout);
		outConfig.set("GriefPrevention.PvP.CombatTimeoutSeconds", this.config_pvp_combatTimeoutSeconds);
		outConfig.set("GriefPrevention.PvP.AllowCombatItemDrop", this.config_pvp_allowCombatItemDrop);
		outConfig.set("GriefPrevention.PvP.BlockedSlashCommands", bannedPvPCommandsList);
		outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims", this.config_pvp_noCombatInPlayerLandClaims);
		outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", this.config_pvp_noCombatInAdminLandClaims);
		
		outConfig.set("GriefPrevention.Trees.RemoveFloatingTreetops", this.config_trees_removeFloatingTreetops);
		outConfig.set("GriefPrevention.Trees.RegrowGriefedTrees", this.config_trees_regrowGriefedTrees);
		
		
		
		outConfig.set("GriefPrevention.Explosions.blockSurfaceOtherExplosions", config_blockSurfaceOtherExplosions);
		
		
		//outConfig.set("GriefPrevention.BlockSurfaceWildCreeperExplosions", this.config_blockSurfaceWildCreeperExplosions);
		//outConfig.set("GriefPrevention.BlockSurfaceWildOtherExplosions", this.config_blockSurfaceWildOtherExplosions);
		outConfig.set("GriefPrevention.LimitSurfaceWaterBuckets", this.config_blockWildernessWaterBuckets);
		outConfig.set("GriefPrevention.LimitSkyTrees", this.config_blockSkyTrees);
		
		outConfig.set("GriefPrevention.FireSpreads", this.config_fireSpreads);
		outConfig.set("GriefPrevention.FireDestroys", this.config_fireDestroys);
		
		outConfig.set("GriefPrevention.AddItemsToClaimedChests", this.config_addItemsToClaimedChests);
		
		outConfig.set("GriefPrevention.EavesdropEnabled", this.config_eavesdrop);		
		outConfig.set("GriefPrevention.WhisperCommands", whisperCommandsToMonitor);		
		outConfig.set("GriefPrevention.SmartBan", this.config_smartBan);
		
		//outConfig.set("GriefPrevention.Siege.Worlds", siegeEnabledWorldNames);
		outConfig.set("GriefPrevention.Siege.BreakableBlocks", breakableBlocksList);
		
		outConfig.set("GriefPrevention.EndermenMoveBlocks", this.config_endermenMoveBlocks);
		outConfig.set("GriefPrevention.SilverfishBreakBlocks", this.config_silverfishBreakBlocks);		
		outConfig.set("GriefPrevention.CreaturesTrampleCrops", this.config_creaturesTrampleCrops);
		outConfig.set("GriefPrevention.HardModeZombiesBreakDoors", this.config_zombiesBreakDoors);		
		
			
		
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
			GriefPrevention.AddLogEntry("Unable to write to the configuration file at \"" + DataStore.configFilePath + "\"");
		}
		
		
		
	}
	public WorldConfig(String worldname){
		this(worldname,new YamlConfiguration(),ConfigData.createTargetConfiguration(worldname) );
	}
	public WorldConfig(World grabfor) {
		// //construct WorldConfig with default settings.
		//we construct a default FileConfiguration and call ourselves...
		this(grabfor.getName());

	}
	
	
	
}
