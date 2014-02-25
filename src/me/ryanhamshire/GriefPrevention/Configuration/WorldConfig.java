package me.ryanhamshire.GriefPrevention.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.Debugger;
import me.ryanhamshire.GriefPrevention.Debugger.DebugLevel;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.MaterialCollection;
import me.ryanhamshire.GriefPrevention.MaterialInfo;
import me.ryanhamshire.GriefPrevention.PermNodes;
import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData.ClaimBehaviourMode;
import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData.SiegePVPOverrideConstants;
import me.ryanhamshire.GriefPrevention.Configuration.PlacementRules.SeaLevelOverrideTypes;
import me.ryanhamshire.GriefPrevention.tasks.DeliverClaimBlocksTask;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * represents the configuration settings of a single world.
 *
 * @author BC_Programming
 */
public class WorldConfig {


    // Explosion and similar effect information.
    // we've moved this to another class for brevity as well as to make it
    // easier to deal with and more flexible.
    // you know- all the standard reasons for moving things into a class. I'll
    // shut up now and get to writing the applicable code.

    public static WorldConfig fromFile(String templateFile) {
        // TODO Auto-generated method stub
        File grabfile = new File(templateFile);
        if (!grabfile.exists())
            return null;
        YamlConfiguration Source = YamlConfiguration.loadConfiguration(new File(templateFile));
        YamlConfiguration Target = new YamlConfiguration();
        WorldConfig created = new WorldConfig(grabfile.getName(), Source, Target);
        try {
            Target.save(grabfile.getPath());
        } catch (Exception exx) {
        }
        return created;
    }

    public static String getWorldConfig(String forWorld) {
        return DataStore.dataLayerFolderPath + File.separator + "WorldConfigs/" + forWorld + ".yml";
    }

    private int afkDistanceSquared = -1;

    private ClaimBehaviourData ArrowWoodenButtonRules;

    private ClaimBehaviourData ArrowWoodenTouchplateRules;

    private ClaimBehaviourData Beds;

    private ClaimBehaviourData BlockTweakRules; // rules for Noteblocks,
    // redstone repeaters, and
    // comparators.

    private ClaimBehaviourData BonemealGrass;

    private ArrayList<Material> BreakableArrowMaterials;

    private ClaimBehaviourData BreedingRules;

    private boolean claims_creative_rules;
    private boolean griefprevention_enabled;

    private boolean claims_enabled;

    // private members followed by their read-only accessor.
    private boolean claims_Siege_Enabled;

    private boolean config_addItemsToClaimedChests; // whether players may add
    // items to claimed chests
    // by left-clicking them

    private Material config_administration_tool;

    private int config_afkDistanceCheck;

    private boolean config_allowItemSpawn = true;

    private List<BlockPlacementRules> config_BlockBreakRules;

    private List<BlockPlacementRules> config_BlockPlacementRules;

    // private boolean config_blockWildernessWaterBuckets; //whether players can
    // dump water buckets outside their claims
    // public boolean blockWildernessWaterBuckets(){ return
    // config_blockWildernessWaterBuckets;}
    private boolean config_blockSkyTrees; // whether players can build trees on
    // platforms in the sky

    // private boolean config_treecleanup_enabled;
    private boolean config_claimcleanup_enabled; // whether the cleanup task is
    // activated.

    private int config_claimcleanup_maximumsize; // maximum size of claims to
    // cleanup. larger claims
    // are not cleaned up.

    private int config_claimcleanup_maxinvestmentscore; // maximum
    // investmentscore. claims with a higher score will not be cleaned up. if set to 0, claim cleanup will not have it's score calculated.

    private int config_claimcleanup_interval = 5*60;
    public int getClaimCleanupInterval(){ return config_claimcleanup_interval;}

    private int config_claimcleanup_quantity = 5;
    public int getClaimCleanupQuantity(){ return config_claimcleanup_quantity;}



    private boolean config_claims_Abandon_NatureRestoration; // whether survival
    // claims will
    // be
    // automatically
    // restored to
    // nature when
    // abandoned.


    private double config_claims_AbandonReturnRatio; // return ratio when
    // abandoning a claim-
    // .80 will result in
    // players getting 80%
    // of the used claim
    // blocks back.

    private boolean config_claims_allowUnclaim; // whether players may unclaim
    // land (resize or abandon)

    //private boolean config_claims_ApplyTrashBlockRules; // whether players can
    // build in survival
    // worlds outside their
    // claimed areas
    private int config_claims_automaticClaimsForNewPlayersRadius; // how big
    // automatic
    // new
    // player
    // claims
    // (when
    // they
    // place a
    // chest)
    // should
    // be. 0 to
    // disable
    private boolean config_claims_AutoNatureRestoration; // whether survival
    // claims will be
    // automatically
    // restored to
    // nature when
    // auto-deleted
    private boolean config_claims_autoRestoreUnclaimed; // whether unclaimed
    // land in creative
    // worlds is
    // automatically
    // /restorenature-d
    private float config_claims_blocksAccruedPerHour; // how many additional
    // blocks players get
    // each hour of play
    // (can be zero)
    private int config_claims_chestClaimExpirationDays; // number of days of
    // inactivity before an
    // automatic chest claim
    // will be deleted
    private int config_claims_claimsExtendIntoGroundDistance; // how far below
    // the shoveled
    // block a new
    // claim will
    // reach
    private boolean config_claims_creationRequiresPermission; // whether
    // creating
    // claims with
    // the shovel
    // requires a
    // permission
    private boolean config_claims_creativeRules;
    private int config_claims_expirationDays; // how many days of inactivity
    // before a player loses his
    // claims
    private Material config_claims_investigationTool; // which material will be
    // used to investigate
    // claims with a right
    // click
    private int config_claims_maxblocks; // maximum blocks a player can claim in
    // this world. This does not change
    // and is essentially a cap to keep
    // a player
    // from 'taking over' an entire world. 0 indicates there is no limit.

    // public int claims_maxAccruedBlocks(){ return
    // config_claims_maxAccruedBlocks;}
    private int config_claims_maxDepth; // limit on how deep claims can go

    private int config_claims_minBlocks;

    private int config_claims_minSize; // minimum width and height for non-admin
    // claims

    private Material config_claims_modificationTool; // which material will be
    // used to create/resize
    // claims with a right
    // click

    private int config_claims_perplayer_claim_limit; // maximum number of claims
    // a user can have.

    private int config_claims_showsurroundingsRadius;

    private int config_claims_trappedCooldownMinutes; // number of minutes
    // between uses of the
    // /trapped command

    private boolean config_claims_TrashBlocksWithoutPermission; // whether
    // players can
    // place/break
    // trashblocks
    // inside claims
    // for which
    // they do not
    // have perms.

    private int config_claims_unusedClaimExpirationDays; // number of days of
    // inactivity before
    // an unused
    // (nothing build)
    // claim will be
    // deleted

    private boolean config_claims_warnOnBuildOutside; // whether players should
    // be warned when
    // they're building in
    // an unclaimed area

    // configuration option changes the number of non-trash blocks that can be
    // placed before
    // the plugin warns about being in the wilderness and all that guff about
    // players being able to undo your work. 0 disables the display entirely.
    private int config_claims_wildernessBlocksDelay;

    private boolean config_creaturesTrampleCrops; // whether or not non-player
    // entities may trample
    // crops

    private boolean config_deny_all_explosions = false;
    // whether whispered messages will be visible to administrators
    private boolean config_eavesdrop;

    private boolean config_eavesdrop_bookdrop;
    private boolean config_eavesdrop_books;
    public boolean getEavesDropBooks(){ return config_eavesdrop_books;}
    // list of whisper commands to eavesdrop on
    private ArrayList<String> config_eavesdrop_whisperCommands;

    private int config_entityClaimLimit = 0; // 0 is no limit.

    private boolean config_entitycleanup_enabled;
    public enum HorseTrustConstants
    {
        Standard,
        Extended,
        Disabled,

    }
    private HorseTrustConstants HorseTrust=HorseTrustConstants.Disabled;
    public HorseTrustConstants getHorseTrust(){ return HorseTrust;}
    private int config_message_cooldown_claims = 0; // claims cooldown. 0= no cooldown.

    private int config_message_cooldown_stuck = 0; // stuck cooldown. 0= no cooldown.
    private int config_message_cooldown_ignore = 0;
    private MaterialCollection config_mods_accessTrustIds; // list of block IDs which should require /accesstrust for player interaction

    private MaterialCollection config_mods_containerTrustIds;
    // list of block IDs which should require /containertrust for player interaction

    private MaterialCollection config_mods_explodableIds;
    // list of block IDs which can be destroyed by explosions, even
    // in claimed areas
    // public MaterialCollection getModsExplodableIds() {return
    // config_mods_explodableIds;}

    private List<String> config_mods_ignoreClaimsAccounts;
    // list of player names which ALWAYS ignore claims

    private boolean config_pvp_allowCombatItemDrop;
    // whether a player can drop items during combat to hide them

    private boolean config_pvp_blockContainers;


    private ArrayList<String> config_pvp_blockedCommands = new ArrayList<String>();
    // list of commands which may not be used during pvp combat

    //how long combat is considered to continue after the most recent damage.
    private int config_pvp_combatTimeoutSeconds;
    //whether PVP is enabled.
    private boolean config_pvp_enabled;
    //Whether players may fight in administrator claims.
    private boolean config_pvp_noCombatInAdminLandClaims;

    //whether players may fight in player-owned claims. Setting this to off and having siege enabled makes things awkward.
    private boolean config_pvp_noCombatInPlayerLandClaims;


    //whether newly spawned players are protected.
    private boolean SpawnProtectEnabled;
    //Timeout value; if this timeout is non-zero than after that number of ticks expires, a newly spawned player
    //will lose their PVP protection.
    private int SpawnProtectTimeout;
    //Whether Spawn Protection is disabled when a player picks up an item.
    private boolean SpawnProtectDisableonItemPickup;
    //whether spawn protection is disabled for a player when that player attacks another player.
    private boolean SpawnProtectDisableonInstigate;
    //value during which a newly spawned player may not pick up items.
    private int SpawnProtectPickupTimeout;

    public int getSpawnProtectTimeout() {
        return SpawnProtectTimeout;
    }

    public boolean getSpawnProtectDisableonItemPickup() {
        return SpawnProtectDisableonItemPickup;
    }

    public boolean getSpawnProtectDisableonInstigate() {
        return SpawnProtectDisableonInstigate;
    }

    public int getSpawnProtectPickupTimeout() {
        return SpawnProtectPickupTimeout;
    }


    private boolean config_pvp_punishLogout; // whether to kill players who log
    // out during PvP combat

    private int config_pvp_Siege_Loot_Chests; // defaults to 0, above zero means
    // that a player is allowed to
    // look into and take items from
    // X chests on a claim they
    // Siege.

    private int config_seaLevelOverride;

    private List<SiegeableData> config_siege_blocks; // which blocks will be
    // breakable in siege
    // mode
    private List<SiegeableData> config_TNTSiege_blocks; //which blocks are breakable during a siege from explosions.

    public List<SiegeableData> getTNTSiegeBlocks() {
        return config_TNTSiege_blocks;
    }

    private boolean config_siege_enabled;

    private boolean config_sign_Eavesdrop; // whether to allow sign
    // eavesdropping at all.

    private boolean config_smartBan; // whether to ban accounts which very
    // likely owned by a banned player

    private String config_spam_allowedIpAddresses; // IP addresses which will
    // not be censored

    private String config_spam_bancommand; // command to run when spam detector
    // triggers. {0} will be player
    // name.

    private String config_spam_banMessage; // message to show an automatically
    // banned player

    private int config_spam_deathMessageCooldownSeconds; // cooldown period for
    // death messages
    // (per player) in
    // seconds

    private boolean config_spam_enabled; // whether or not to monitor for spam

    private String config_spam_kickcommand; // command(s) to run when spam
    // detector triggers and kicks. {0}
    // will be player name.

    private String config_spam_kickMessage;

    private int config_spam_loginCooldownSeconds; // how long players must wait
    // between logins. combats
    // login spam.

    private List<String> config_spam_monitorSlashCommands; // the list of slash
    // commands
    // monitored for
    // spam

    private String config_spam_warningMessage; // message to show a player who
    // is close to spam level

    private int config_SpamASCIIArtMinLength;

    private int config_SpamBanThreshold;

    private int config_SpamCapsMinLength;

    private int config_SpamDelayThreshold;

    private int config_SpamKickThreshold;

    private int config_SpamMuteThreshold;

    private int config_SpamNonAlphaNumMinLength;

    private int config_SpamShortMessageMaxLength;

    private int config_SpamShortMessageTimeout;

    private MaterialCollection config_trash_blocks = null;

    private boolean config_trees_regrowGriefedTrees; // whether to automatically
    // replant partially cut
    // trees

    private boolean config_trees_removeFloatingTreetops; // whether to
    // automatically
    // remove partially
    // cut trees
    private PlaceBreakOverrides BlockBreakOverrides = PlaceBreakOverrides.Default;

    public PlaceBreakOverrides getBlockBreakOverrides() {
        return BlockBreakOverrides;
    }

    private PlaceBreakOverrides BlockPlaceOverrides = PlaceBreakOverrides.Default;

    public PlaceBreakOverrides getBlockPlaceOverrides() {
        return BlockPlaceOverrides;
    }

    private ClaimBehaviourMode SiegeDefender = ClaimBehaviourMode.RequireOwner; //applicable Trust level of defender.
    private ClaimBehaviourData PlaceBlockRules;
    private ClaimBehaviourData BreakBlockRules;

    public ClaimBehaviourData getPlaceBlockRules() {
        return PlaceBlockRules;
    }

    public ClaimBehaviourData getBreakBlockRules() {
        return BreakBlockRules;
    }

    public ClaimBehaviourMode getSiegeDefendable() {
        return SiegeDefender;
    }

    private ClaimBehaviourData DragonEggRules = ClaimBehaviourData.getAll("Dragon Egg").setBehaviourMode(ClaimBehaviourMode.RequireBuild);

    public ClaimBehaviourData getDragonEggRules() {
        return DragonEggRules;
    }

    private ClaimBehaviourData EnderEyePortalRules;
    private ClaimBehaviourData FlowerPotRules;
    private ClaimBehaviourData ItemFrameItemRules;

    public ClaimBehaviourData getFlowerPotRules() {
        return FlowerPotRules;
    }

    public ClaimBehaviourData getEnderEyePortalRules() {
        return EnderEyePortalRules;
    }

    public ClaimBehaviourData getItemFrameRules() {
        return ItemFrameItemRules;
    }

    private ClaimBehaviourData ContainersRules;

    private ClaimBehaviourData CreatureDamage;

    // data for Creeper Explosions. This indicates where they can occur.
    private ClaimBehaviourData CreeperExplosionBlockDamageBehaviour;

    private ClaimBehaviourData CreeperExplosionsBehaviour;

    private ClaimBehaviourData EnderDragonDamageBehaviour;

    private ClaimBehaviourData EnderDragonDamageBlockBehaviour;

    private ClaimBehaviourData DispenserArrowBehaviour;

    private ClaimBehaviourData DispenserEggBehaviour;

    private ClaimBehaviourData DispenserFireChargeBehaviour;

    private ClaimBehaviourData DispenserFireworkBehaviour;

    private ClaimBehaviourData DispenserFlintandSteelBehaviour;

    private ClaimBehaviourData DispenserLavaBehaviour;

    private ClaimBehaviourData DispenserMiscBehaviour;

    private ClaimBehaviourData DispenserNegativePotionBehaviour;

    private ClaimBehaviourData DispenserPositivePotionBehaviour;

    private ClaimBehaviourData DispenserSnowballBehaviour;

    private ClaimBehaviourData DispenserSpawnEggBehaviour;

    // Dispenser rules.
    private ClaimBehaviourData DispenserWaterBehaviour;

    private ClaimBehaviourData EndermanPickupRules; // rules for enderman
    // picking up blocks.

    private ClaimBehaviourData EndermanPlacementRules;

    private ClaimBehaviourData EnderPearlOrigins;

    private ClaimBehaviourData EnderPearlTargets;

    //tweak: moved VehicleDamage to separate nodes.

    private ClaimBehaviourData VehicleDamage;

    private ClaimBehaviourData EnvironmentalVehicleDamage;

    private ClaimBehaviourData Riding_Boat;

    private ClaimBehaviourData Riding_Minecart;

    private ClaimBehaviourData Riding_Pig;

    private ClaimBehaviourData Riding_Horse;

    private ClaimBehaviourData Placement_Boat;
    private ClaimBehaviourData Placement_Minecart;

    public ClaimBehaviourData getBoatPlacement(){ return Placement_Boat;}
    public ClaimBehaviourData getMinecartPlacement() { return Placement_Minecart;}
    public ClaimBehaviourData getBoatRiding(){ return Riding_Boat;}
    public ClaimBehaviourData getMinecartRiding(){ return Riding_Minecart;}
    public ClaimBehaviourData getPigRiding(){ return Riding_Pig;}
    public ClaimBehaviourData getHorseRiding() { return Riding_Horse;}

    private ClaimBehaviourData EquineInventoryRules;

    private ClaimBehaviourData FeedingRules;

    private ClaimBehaviourData FenceGates;

    private ClaimBehaviourData FireExtinguishing;

    private ClaimBehaviourData FireSetting;

    private int InsufficientSneakResetBound; // when sneaking and trying to
    // finish a claim, if it
    // requires more

    private ClaimBehaviourData IronGolemSpawnBehaviour; // data for how
    // IronGolems can be
    // spawned.

    private ClaimBehaviourData SpawnEggBehaviour;
    public ClaimBehaviourData getSpawnEggBehaviour() { return SpawnEggBehaviour;}
    private List<ItemUsageRules> ItemRules;


    //Fire options

    //Option pertains to the <origin> of a fire spread event.
    private ClaimBehaviourData FireSpreadOriginBehaviour;

    public ClaimBehaviourData getFireSpreadOriginBehaviour() {
        return FireSpreadOriginBehaviour;
    }


    //target of a fire spread event.
    private ClaimBehaviourData FireSpreadTargetBehaviour;

    public ClaimBehaviourData getFireSpreadTargetBehaviour() {
        return FireSpreadTargetBehaviour;

    }

    //fire destroying a block.
    private ClaimBehaviourData FireDestroyBehaviour;

    public ClaimBehaviourData getFireDestroyBehaviour() {

        return FireDestroyBehaviour;

    }

    private ClaimBehaviourData LavaBucketEmptyBehaviour;

    private ClaimBehaviourData LavaBucketFillBehaviour;

    private ClaimBehaviourData LeadUsageRules;

    private ClaimBehaviourData Levers;

    private ClaimBehaviourData ModInteractables;

    private ClaimBehaviourData NameTagUsageRules;

    private ClaimBehaviourData OtherExplosionBehaviour;

    public ClaimBehaviourData OtherExplosionBlockDamageBehaviour;

    private ClaimBehaviourData PlayerTrampleRules;

    private ClaimBehaviourData SheepDyeing;

    private ClaimBehaviourData SheepShearingRules;

    private int Siege_TamedAnimalDistance;

    private boolean SiegeAutoTransfer;

    private boolean SiegeBlockRevert = false;

    private ClaimBehaviourData SilverfishBreakRules;

    private ClaimBehaviourData SnowGolemSpawnBehaviour; // data for now Snow
    // Golems can be
    // spawned.

    private ClaimBehaviourData StoneButton;

    private ClaimBehaviourData StonePressurePlates;

    private ClaimBehaviourData TamingRules;

    // data for TNT Explosions. this indicates where they can occur. Applies for
    // both TNT and TNT minecarts.
    private ClaimBehaviourData TNTExplosionBlockDamageBehaviour;

    private ClaimBehaviourData TNTExplosionsBehaviour;



    private ClaimBehaviourData TrapDoors;

    private ClaimBehaviourData TrashBlockPlacementBehaviour;

    private ClaimBehaviourData VillagerTrades; // prevent trades on claims
    // players don't have
    // permissions on

	/*
     * private boolean config_claims_preventTheft; //whether containers and
	 * crafting blocks are protectable public boolean getClaimsPreventTheft(){
	 * return config_claims_preventTheft;} private boolean
	 * config_claims_protectCreatures; //whether claimed animals may be injured
	 * by players without permission public boolean getClaimsProtectCreatures(){
	 * return config_claims_protectCreatures;} private boolean
	 * config_claims_preventButtonsSwitches; //whether buttons and switches are
	 * protectable public boolean getClaimsPreventButtonsSwitches(){ return
	 * config_claims_preventButtonsSwitches;} private boolean
	 * config_claims_lockWoodenDoors; //whether wooden doors should be locked by
	 * default (require /accesstrust) public boolean getClaimsLockWoodenDoors(){
	 * return config_claims_lockWoodenDoors;} private boolean
	 * config_claims_lockTrapDoors; //whether trap doors should be locked by
	 * default (require /accesstrust) public boolean getClaimsLockTrapDoors(){
	 * return config_claims_lockTrapDoors;} private boolean
	 * config_claims_lockFenceGates; //whether fence gates should be locked by
	 * default (require /accesstrust) public boolean getClaimsLockFenceGates(){
	 * return config_claims_lockFenceGates;} private boolean
	 * config_claims_enderPearlsRequireAccessTrust; //whether teleporting into a
	 * claim with a pearl requires access trust public boolean
	 * getEnderPearlsRequireAccessTrust(){ return
	 * config_claims_enderPearlsRequireAccessTrust;}
	 */

    private ClaimBehaviourData WaterBucketEmptyBehaviour;

    private ClaimBehaviourData WaterBucketFillBehaviour;

    private ClaimBehaviourData WitherEatBehaviour;

    private ClaimBehaviourData WitherExplosionBehaviour;

    private ClaimBehaviourData WitherExplosionBlockDamageBehaviour;

    private ClaimBehaviourData WitherSpawnBehaviour; // data for how Withers can
    // be spawned.

    private ClaimBehaviourData WoodenButton;

    private ClaimBehaviourData WoodenDoors;

    private ClaimBehaviourData WoodPressurePlates;

    private String WorldName;

    private ClaimBehaviourData ZombieDoorBreaking;

    private int MinLavaDistance = 10;
    public int getMinLavaDistance(){
        return MinLavaDistance;
    }
    public WorldConfig(String worldname) {
        this(worldname, new YamlConfiguration(), ConfigData.createTargetConfiguration(worldname));
    }
    private boolean isValidRule(FileConfiguration Source, String NodePath){

        ConfigurationSection wild = Source.getConfigurationSection(NodePath + ".wilderness");
        ConfigurationSection claim = Source.getConfigurationSection(NodePath + "claims");
        if(wild!=null && claim!=null)
        {
            return true; //if neither is null, it's likely a valid rule.
        }

     return false;
    }




    // constructor accepts a Name and a FileConfiguration.
    public WorldConfig(String pName, FileConfiguration config, FileConfiguration outConfig) {

        // determine defaults based on the world itself (isCreative, isPvP)
        boolean isCreative = false, isPvP = false;
        WorldName = pName;
        World getworld = Bukkit.getWorld(pName);
        if (getworld != null) {

            isCreative = Bukkit.getServer().getDefaultGameMode() == GameMode.CREATIVE;
            isPvP = getworld.getPVP();
            boolean gotpvp = config.getBoolean("GriefPrevention.PvP.Enabled", isPvP);
            outConfig.set("GriefPrevention.PvP.Enabled", gotpvp);
            getworld.setPVP(isPvP = gotpvp);
            config_pvp_enabled=gotpvp;
        }

        Debugger.Write("Reading Configuration for World:" + pName, DebugLevel.Verbose);
        this.griefprevention_enabled = config.getBoolean("GriefPrevention.Enabled", true);
        outConfig.set("GriefPrevention.Enabled", this.griefprevention_enabled);
        this.config_seaLevelOverride = config.getInt("GriefPrevention.SeaLevelOverride", -1);
        if (this.config_seaLevelOverride == -1) {

            // try to get new setting.
            int newsealevel = config.getInt("GriefPrevention.SeaLevelOverride.Setting", -1);
            boolean dooverride = config.getBoolean("GriefPrevention.SeaLevelOverride.Enabled", false);

            if (dooverride && newsealevel != -1)
                config_seaLevelOverride = newsealevel;

        }
        if (config_seaLevelOverride != -1) {
            outConfig.set("GriefPrevention.SeaLevelOverride.Setting", config_seaLevelOverride);
        }
        outConfig.set("GriefPrevention.SeaLevelOverride.Enabled", config_seaLevelOverride != -1);


        // read in the data for TNT explosions and Golem/Wither placements.
        this.config_afkDistanceCheck = config.getInt("GriefPrevention.AFKDistance", 3);
        if(outConfig.getConfigurationSection("GriefPrevention").contains("ExtendedHorseTrust")){
           boolean oldHorseTrust = config.getBoolean("GriefPrevention.ExtendedHorseTrust",false);
           HorseTrust = oldHorseTrust?HorseTrustConstants.Extended:HorseTrustConstants.Standard;
        }
        else {
           String newHorseTrust = config.getString("GriefPrevention.HorseTrust","");
            try {
            this.HorseTrust =  HorseTrustConstants.valueOf(newHorseTrust);
            }
            catch(Exception exx){
                if(newHorseTrust.equalsIgnoreCase("Enabled")) HorseTrust = HorseTrustConstants.Standard; else
                HorseTrust = HorseTrustConstants.Disabled;
            }
        }
        outConfig.set("GriefPrevention.HorseTrust",HorseTrust.name());
        String SiegeDefenderStr = config.getString("GriefPrevention.SiegeDefendable", "RequireOwner");
        ClaimBehaviourMode ccm = ClaimBehaviourMode.parseMode(SiegeDefenderStr);
        outConfig.set("GriefPrevention.SiegeDefendable",SiegeDefenderStr);
        if (ccm == null) SiegeDefender = ClaimBehaviourMode.RequireOwner;
        else SiegeDefender = ccm;

        this.MinLavaDistance = config.getInt("GriefPrevention.MinLavaPlacementDistance",10);
        outConfig.set("GriefPrevention.MinLavaPlacementDistance",MinLavaDistance);

        this.config_entityClaimLimit = config.getInt("GriefPrevention.Claims.EntityLimit", 0);
        outConfig.set("GriefPrevention.Claims.EntityLimit", 0);

        this.config_deny_all_explosions = config.getBoolean("GriefPrevention.DenyAllExplosions", false);
        outConfig.set("GriefPrevention.DenyAllExplosions", config_deny_all_explosions);

        this.SiegeAutoTransfer = config.getBoolean("GriefPrevention.Siege.ItemTransfer", true);
        outConfig.set("GriefPrevention.Siege.ItemTransfer", this.SiegeAutoTransfer);

        this.SiegeBlockRevert = config.getBoolean("GriefPrevention.Siege.BlockRevert", true);
        outConfig.set("GriefPrevention.Siege.BlockRevert", SiegeBlockRevert);
        // read trash blocks.
        // Cobblestone,Torch,Dirt,Sapling,Gravel,Sand,TNT,Workbench

        // SpamDelayThreshold=1500
        // SpamCapsMinLength=4
        // SpamAlphaNumMinLength=5
        // SpamASCIIArtLengthMinLength=15
        // SpamShortMessageMaxLength=5
        // SpamShortMessageTimeout=3000
        // SpamBanThreshold=8
        // SpamMuteThreshold=4
        this.config_SpamDelayThreshold = config.getInt("GriefPrevention.Spam.DelayThreshold", 1500);
        outConfig.set("GriefPrevention.Spam.DelayThreshold", config_SpamDelayThreshold);
        this.config_SpamCapsMinLength = config.getInt("GriefPrevention.Spam.CapsMinLength", 4);
        outConfig.set("GriefPrevention.Spam.CapsMinLength", config_SpamCapsMinLength);
        this.config_SpamNonAlphaNumMinLength = config.getInt("GriefPrevention.Spam.NonAlphaNumMinLength", 5);
        outConfig.set("GriefPrevention.Spam.AlphaNumMinLength", config_SpamNonAlphaNumMinLength);
        this.config_SpamASCIIArtMinLength = config.getInt("GriefPrevention.Spam.ASCIIArtMinLength", 15);
        outConfig.set("GriefPrevention.Spam.ASCIIArtMinLength", config_SpamASCIIArtMinLength);
        this.config_SpamShortMessageMaxLength = config.getInt("GriefPrevention.Spam.ShortMessageMaxLength", 5);
        outConfig.set("GriefPrevention.Spam.ShortMessageMaxLength", config_SpamShortMessageMaxLength);
        this.config_SpamKickThreshold = config.getInt("GriefPrevention.Spam.KickThreshold", 6);
        outConfig.set("GriefPrevention.Spam.KickThreshold", config_SpamKickThreshold);
        this.config_SpamBanThreshold = config.getInt("GriefPrevention.Spam.BanThreshold", 8);
        outConfig.set("GriefPrevention.Spam.BanThreshold", config_SpamBanThreshold);
        this.config_SpamMuteThreshold = config.getInt("GriefPrevention.Spam.MuteThreshold", 4);
        outConfig.set("GriefPrevention.Spam.MuteThreshold", config_SpamMuteThreshold);

        this.InsufficientSneakResetBound = config.getInt("GriefPrevention.Claims.InsufficientSneakResetBound", 0);
        outConfig.set("GriefPrevention.Claims.InsufficientSneakResetBound", this.InsufficientSneakResetBound);
        this.claims_Siege_Enabled = config.getBoolean("GriefPrevention.Siege.Enabled", isPvP);
        outConfig.set("GriefPrevention.Siege.Enabled", claims_Siege_Enabled);

        this.Siege_TamedAnimalDistance = config.getInt("GriefPrevention.Claims.SiegeTamedAnimalDistance", 20);
        outConfig.set("GriefPrevention.Claims.SiegeTamedAnimalDistance", Siege_TamedAnimalDistance);
        this.claims_enabled = config.getBoolean("GriefPrevention.Claims.Enabled", true);
        outConfig.set("GriefPrevention.Claims.Enabled", claims_enabled);
        this.config_entitycleanup_enabled = config.getBoolean("GriefPrevention.CleanupTasks.Entity", true);
        outConfig.set("GriefPrevention.CleanupTasks.Entity", this.config_entitycleanup_enabled);
        // this.config_treecleanup_enabled =
        // config.getBoolean("GriefPrevention.CleanupTasks.Trees",true);
        // this.config_naturerestorecleanup_enabled =
        // config.getBoolean("GriefPrevention.CleanupTasks.NatureRestore",true);
        this.config_claimcleanup_enabled = config.getBoolean("GriefPrevention.ClaimCleanup.Enabled", true);
        this.config_claimcleanup_interval = config.getInt("GriefPrevention.ClaimCleanup.Interval",5*60);
        this.config_claimcleanup_quantity = config.getInt("GriefPrevention.ClaimCleanup.Quantity",5);
        outConfig.set("GriefPrevention.ClaimCleanup.Interval",config_claimcleanup_interval);
        outConfig.set("GriefPrevention.ClaimCleanup.Quantity",config_claimcleanup_quantity);
        this.config_claimcleanup_maximumsize = config.getInt("GriefPrevention.ClaimCleanup.MaximumSize", 25);
        // max investment score, defaults to 400 for creative worlds.
        this.config_claimcleanup_maxinvestmentscore = config.getInt("GriefPrevention.ClaimCleanup.MaxInvestmentScore", isCreative ? 400 : 100);

        String blocksaccruedstr = config.getString("GriefPrevention.Claims.BlocksAccruedPerHour", "100");
        try {
            config_claims_blocksAccruedPerHour = Float.parseFloat(blocksaccruedstr);
        } catch (NumberFormatException nfe) {
            config_claims_blocksAccruedPerHour = 100;
        }

        this.config_claims_maxblocks = config.getInt("GriefPrevention.Claims.MaxClaimBlocks", 0);
        outConfig.set("GriefPrevention.Claims.MaxClaimBlocks", this.config_claims_maxblocks);

        outConfig.set("GriefPrevention.Claims.BlocksAccruedPerHour", config_claims_blocksAccruedPerHour);
        outConfig.set("GriefPrevention.ClaimCleanup.MaximumSize", config_claimcleanup_maximumsize);
        outConfig.set("GriefPrevention.ClaimCleanup.MaxInvestmentScore", this.config_claimcleanup_maxinvestmentscore);
        outConfig.set("GriefPrevention.ClaimCleanup.Enabled", this.config_claimcleanup_enabled);
        this.config_message_cooldown_claims = config.getInt("GriefPrevention.Expiration.MessageCooldown.Claim", 0);
        this.config_message_cooldown_stuck = config.getInt("GriefPrevention.Expiration.MessageCooldown.Stuck", 0);
        this.config_message_cooldown_ignore = config.getInt("GriefPrevention.Expiration.MessageCooldown.Ignore", 0);
        outConfig.set("GriefPrevention.Expiration.MessageCooldown.Claim", config_message_cooldown_claims);
        outConfig.set("GriefPrevention.Expiration.MessageCooldown.Stuck", config_message_cooldown_stuck);
        outConfig.set("GriefPrevention.Expiration.MessageCooldown.Ignore", config_message_cooldown_ignore);
        this.config_claims_wildernessBlocksDelay = config.getInt("GriefPrevention.Claims.WildernessWarningBlockCount", 15); // number
        // of
        // blocks,0
        // will
        // disable
        // the
        // wilderness
        // warning.
        this.config_claims_creativeRules = config.getBoolean("GriefPrevention.CreativeRules", Bukkit.getServer().getDefaultGameMode() == GameMode.CREATIVE);
        this.config_claims_AbandonReturnRatio = config.getDouble("GriefPrevention.Claims.AbandonReturnRatio", 1);
        outConfig.set("GriefPrevention.Claims.AbandonReturnRatio", this.config_claims_AbandonReturnRatio);
        outConfig.set("GriefPrevention.CreativeRules", config_claims_creativeRules);

        this.config_claims_showsurroundingsRadius = config.getInt("GriefPrevention.Claims.InvestigateSurroundingsRadius", 50);

        this.config_claims_perplayer_claim_limit = config.getInt("GriefPrevention.Claims.PerPlayerLimit", 0);

        outConfig.set("GriefPrevention.Claims.PerPlayerLimit", config_claims_perplayer_claim_limit);

        this.config_pvp_Siege_Loot_Chests = config.getInt("GriefPrevention.Claims.SiegeLootChests", 0);

        outConfig.set("GriefPrevention.Claims.SiegeLootChests", config_pvp_Siege_Loot_Chests);

        this.config_claims_automaticClaimsForNewPlayersRadius = config.getInt("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", 4);
        this.config_claims_claimsExtendIntoGroundDistance = config.getInt("GriefPrevention.Claims.ExtendIntoGroundDistance", 5);
        this.config_claims_creationRequiresPermission = config.getBoolean("GriefPrevention.Claims.CreationRequiresPermission", false);
        this.config_claims_minBlocks = config.getInt("GriefPrevention.Claims.MinimumBlocks", 100);
        this.config_claims_minSize = config.getInt("GriefPrevention.Claims.MinimumSize", 10);
        this.config_claims_maxDepth = config.getInt("GriefPrevention.Claims.MaximumDepth", 0);
        this.config_claims_trappedCooldownMinutes = config.getInt("GriefPrevention.Claims.TrappedCommandCooldownMinutes", 8 * 60);

        //this.config_claims_ApplyTrashBlockRules = config.getBoolean("GriefPrevention.Claims.NoSurvivalBuildingOutsideClaims", false);
        this.config_claims_warnOnBuildOutside = config.getBoolean("GriefPrevention.Claims.WarnWhenBuildingOutsideClaims", true);
        this.config_claims_allowUnclaim = config.getBoolean("GriefPrevention.Claims.AllowUnclaimingLand", true);
        this.config_claims_autoRestoreUnclaimed = config.getBoolean("GriefPrevention.Claims.AutoRestoreUnclaimedLand", true);

        this.config_claims_Abandon_NatureRestoration = config.getBoolean("GriefPrevention.Claims.AbandonAutoRestore", false);
        outConfig.set("GriefPrevention.Claims.AbandonAutoRestore", this.config_claims_Abandon_NatureRestoration);

        this.config_claims_Abandon_NatureRestoration = config.getBoolean("GriefPrevention.Claims.AbandonAutoRestore", false);
        outConfig.set("GriefPrevention.Claims.AbandonAutoRestore", this.config_claims_Abandon_NatureRestoration);

        this.config_claims_chestClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.ChestClaimDays", 7);
        outConfig.set("GriefPrevention.Claims.Expiration.ChestClaimDays", this.config_claims_chestClaimExpirationDays);

        this.config_claims_unusedClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.UnusedClaimDays", 14);
        outConfig.set("GriefPrevention.Claims.Expiration.UnusedClaimDays", this.config_claims_unusedClaimExpirationDays);

        this.config_claims_expirationDays = config.getInt("GriefPrevention.Claims.Expiration.AllClaimDays", 0);
        outConfig.set("GriefPrevention.Claims.Expiration.AllClaimDays", this.config_claims_expirationDays);

        this.config_claims_AutoNatureRestoration = config.getBoolean("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration", false);
        outConfig.set("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration", this.config_claims_AutoNatureRestoration);

        this.config_spam_enabled = config.getBoolean("GriefPrevention.Spam.Enabled", true);
        int cooldownminutes = config.getInt("GriefPrevention.Spam.LoginCooldownMinutes", -1);
        if (cooldownminutes == -1) {
            this.config_spam_loginCooldownSeconds = config.getInt("GriefPrevention.Spam.LoginCooldownSeconds", 60);
        } else
            this.config_spam_loginCooldownSeconds = cooldownminutes * 60;

        outConfig.set("GriefPrevention.Spam.LoginCooldownSeconds", config_spam_loginCooldownSeconds);

        this.config_spam_warningMessage = config.getString("GriefPrevention.Spam.WarningMessage", "Please reduce your noise level.  Spammers will be banned.");
        this.config_spam_allowedIpAddresses = config.getString("GriefPrevention.Spam.AllowedIpAddresses", "1.2.3.4; 5.6.7.8");
        this.config_spam_banMessage = config.getString("GriefPrevention.Spam.BanMessage", "Banned for spamming.");
        this.config_spam_kickMessage = config.getString("GriefPrevention.Spam.KickMessage", "Kicked for spamming.");
        String slashCommandsToMonitor = config.getString("GriefPrevention.Spam.MonitorSlashCommands", "/me;/tell;/global;/local");
        this.config_spam_monitorSlashCommands = Arrays.asList(slashCommandsToMonitor.split(";"));
        this.config_spam_deathMessageCooldownSeconds = config.getInt("GriefPrevention.Spam.DeathMessageCooldownSeconds", 60);

        this.config_spam_kickcommand = config.getString("GriefPrevention.Spam.KickCommand", "kick {0}");
        this.config_spam_bancommand = config.getString("GriefPrevention.Spam.BanCommand", "ban {0};kick {0}");

        outConfig.set("GriefPrevention.Spam.Enabled", this.config_spam_enabled);

        //
        outConfig.set("GriefPrevention.Spam.MonitorSlashCommands", slashCommandsToMonitor);
        outConfig.set("GriefPrevention.Spam.WarningMessage", this.config_spam_warningMessage);
        outConfig.set("GriefPrevention.Spam.KickMessage", this.config_spam_kickMessage);
        outConfig.set("GriefPrevention.Spam.BanMessage", this.config_spam_banMessage);
        outConfig.set("GriefPrevention.Spam.AllowedIpAddresses", this.config_spam_allowedIpAddresses);
        outConfig.set("GriefPrevention.Spam.DeathMessageCooldownSeconds", this.config_spam_deathMessageCooldownSeconds);

        outConfig.set("GriefPrevention.Spam.KickCommand", this.config_spam_kickcommand);
        outConfig.set("GriefPrevention.Spam.BanCommand", config_spam_bancommand);

        this.SpawnProtectEnabled = config.getBoolean("GriefPrevention.PvP.SpawnProtect.Enabled", true);
        this.SpawnProtectTimeout = config.getInt("GriefPrevention.PvP.SpawnProtect.Timeout", 20 * 120);

        this.SpawnProtectDisableonInstigate = config.getBoolean("GriefPrevention.PvP.SpawnProtect.DisableonInstigate", true);
        this.SpawnProtectDisableonItemPickup = config.getBoolean("GriefPrevention.PvP.SpawnProtect.DisableonItemPickup", true);
        this.SpawnProtectPickupTimeout = config.getInt("GriefPrevention.PvP.SpawnProtect.PickupTimeout", 10000);
        this.config_pvp_punishLogout = config.getBoolean("GriefPrevention.PvP.PunishLogout", true);
        this.config_pvp_combatTimeoutSeconds = config.getInt("GriefPrevention.PvP.CombatTimeoutSeconds", 15);
        this.config_pvp_allowCombatItemDrop = config.getBoolean("GriefPrevention.PvP.AllowCombatItemDrop", false);
        this.config_pvp_blockContainers = config.getBoolean("GriefPrevention.PvP.BlockContainers", true);


        outConfig.set("GriefPrevention.PvP.SpawnProtect.Enabled", SpawnProtectEnabled);
        outConfig.set("GriefPrevention.PvP.SpawnProtect.Timeout", SpawnProtectTimeout);
        outConfig.set("GriefPrevention.PvP.SpawnProtect.DisableonInstigate", SpawnProtectDisableonInstigate);
        outConfig.set("GriefPrevention.PvP.SpawnProtect.DisableonItemPickup", SpawnProtectDisableonItemPickup);
        outConfig.set("GriefPrevention.PvP.SpawnProtect.PickupTimeout", SpawnProtectPickupTimeout);


        String bannedPvPCommandsList = config.getString("GriefPrevention.PvP.BlockedSlashCommands", "/home;/vanish;/spawn;/tpa");
        String[] splitresult = bannedPvPCommandsList.split(";");
        config_pvp_blockedCommands = new ArrayList<String>();
        for (int i = 0; i < splitresult.length; i++) {
            if (splitresult[i].length() > 0)
                config_pvp_blockedCommands.add(splitresult[i]);
        }

        outConfig.set("GriefPrevention.PvP.ProtectFreshSpawns", this.SpawnProtectEnabled);
        outConfig.set("GriefPrevention.PvP.PunishLogout", this.config_pvp_punishLogout);
        outConfig.set("GriefPrevention.PvP.CombatTimeoutSeconds", this.config_pvp_combatTimeoutSeconds);
        outConfig.set("GriefPrevention.PvP.AllowCombatItemDrop", this.config_pvp_allowCombatItemDrop);
        outConfig.set("GriefPrevention.PvP.BlockedSlashCommands", bannedPvPCommandsList);

        this.config_trees_removeFloatingTreetops = config.getBoolean("GriefPrevention.Trees.RemoveFloatingTreetops", true);
        this.config_trees_regrowGriefedTrees = config.getBoolean("GriefPrevention.Trees.RegrowGriefedTrees", true);
        this.config_trees_cleanupDelay = config.getInt("GriefPrevention.Trees.CleanupTimeout",60*10)*20;
        outConfig.set("GriefPrevention.Trees.CleanupTimeout",config_trees_cleanupDelay/20);



        outConfig.set("GriefPrevention.PvP.BlockContainers", config_pvp_blockContainers);

        // this.config_blockWildernessWaterBuckets =
        // config.getBoolean("GriefPrevention.LimitSurfaceWaterBuckets", true);
        this.config_blockSkyTrees = config.getBoolean("GriefPrevention.LimitSkyTrees", true);

        //this.config_fireSpreads = config.getBoolean("GriefPrevention.FireSpreads", false);
        //this.config_fireDestroys = config.getBoolean("GriefPrevention.FireDestroys", false);

        this.config_addItemsToClaimedChests = config.getBoolean("GriefPrevention.AddItemsToClaimedChests", true);
        this.config_eavesdrop = config.getBoolean("GriefPrevention.Eavesdrop.Enabled", false);
        outConfig.set("GriefPrevention.Eavesdrop.Enabled", this.config_eavesdrop);
        String whisperCommandsToMonitor = config.getString("GriefPrevention.Eavesdrop.WhisperCommands", "/tell;/pm;/r");
        this.config_sign_Eavesdrop = config.getBoolean("GriefPrevention.Eavesdrop.Signs", true);
        outConfig.set("GriefPrevention.Eavesdrop.Signs", this.config_sign_Eavesdrop);
        this.config_eavesdrop_whisperCommands = new ArrayList<String>();
        for (String whispercommand : whisperCommandsToMonitor.split(";")) {
            config_eavesdrop_whisperCommands.add(whispercommand);
        }

        this.config_eavesdrop_bookdrop = config.getBoolean("GriefPrevention.Eavesdrop.BookDrop", false);
        outConfig.set("GriefPrevention.Eavesdrop.BookDrop", false);
        this.config_eavesdrop_books = config.getBoolean("GriefPrevention.Eavesdrop.Books",true);
        outConfig.set("GriefPrevention.Eavesdrop.Books",config_eavesdrop_books);

        outConfig.set("GriefPrevention.Eavesdrop.WhisperCommands", whisperCommandsToMonitor);

        this.config_smartBan = config.getBoolean("GriefPrevention.SmartBan", true);

        // this.config_endermenMoveBlocks =
        // config.getBoolean("GriefPrevention.EndermenMoveBlocks", false);
        // this.config_silverfishBreakBlocks =
        // config.getBoolean("GriefPrevention.SilverfishBreakBlocks", false);
        this.config_creaturesTrampleCrops = config.getBoolean("GriefPrevention.CreaturesTrampleCrops", false);
        this.config_mods_ignoreClaimsAccounts = config.getStringList("GriefPrevention.Mods.PlayersIgnoringAllClaims");

        if (this.config_mods_ignoreClaimsAccounts == null)
            this.config_mods_ignoreClaimsAccounts = new ArrayList<String>();
        // default the access trust IDs to found modded blocks, if any. Note
        // that the constructor
        // will ignore null parameters, so we shouldn't get a NullRef from
        // MaterialCollection if a Search was not performed.
        this.config_mods_accessTrustIds = new MaterialCollection(GriefPrevention.instance.ModdedBlocks.FoundAccess);
        // build on that list with any configured items.
        //
        List<String> accessTrustStrings = config.getStringList("GriefPrevention.Mods.BlockIdsRequiringAccessTrust");
        // add found items if applicable.
        if (GriefPrevention.instance.ModdedBlocks != null) {
            for (MaterialInfo mi : GriefPrevention.instance.ModdedBlocks.FoundAccess.getMaterials()) {
        	String miString = mi.toString();
        	if (miString != null){
        	    accessTrustStrings.add(miString);
        	}
            }
        }
        // parse the list we got from the cfg file. This will ADD to the list,
        // but not remove existing items.
        // it also will not add items that are already in the list.
        GriefPrevention.instance.parseMaterialListFromConfig(accessTrustStrings, this.config_mods_accessTrustIds);

        //

        // trash blocks.
        this.config_trash_blocks = new MaterialCollection();
        List<String> trashblockStrings = config.getStringList("GriefPrevention.TrashBlocks");
        outConfig.set("GriefPrevention.TrashBlocks", trashblockStrings);
        if (GriefPrevention.instance.ModdedBlocks != null) {
            for (MaterialInfo mi : GriefPrevention.instance.ModdedBlocks.FoundOres.getMaterials()) {
                trashblockStrings.add(mi.toString());
            }
        }
        GriefPrevention.instance.parseMaterialListFromConfig(trashblockStrings, this.config_trash_blocks);

        this.config_mods_containerTrustIds = new MaterialCollection();
        // Get the config setting....
        List<String> containerTrustStrings = config.getStringList("GriefPrevention.Mods.BlockIdsRequiringContainerTrust");

        // parse the strings from the config file

        if (GriefPrevention.instance.ModdedBlocks != null) {

            for (MaterialInfo mi : GriefPrevention.instance.ModdedBlocks.FoundContainers.getMaterials()) {
                containerTrustStrings.add(mi.toString());
            }
        }
        GriefPrevention.instance.parseMaterialListFromConfig(containerTrustStrings, this.config_mods_containerTrustIds);

        // default values for explodable mod blocks BC: Unneeded: Explosions
        // cannot be sourced from a block in vanilla, so Explosion
        // behaviour has been changed to allow the block at the epicenter of the
        // explosion to be broken.
        // parse the strings from the config file

        // default for claim investigation tool
        String investigationToolMaterialName = Material.STICK.name();

        // get investigation tool from config
        investigationToolMaterialName = config.getString("GriefPrevention.Claims.InvestigationTool", investigationToolMaterialName);

        // validate investigation tool
        this.config_claims_investigationTool = Material.getMaterial(investigationToolMaterialName);
        if (this.config_claims_investigationTool == null) {
            GriefPrevention.AddLogEntry("ERROR: Material " + investigationToolMaterialName + " not found.  Defaulting to the stick.  Please update your config.yml.");
            this.config_claims_investigationTool = Material.STICK;
        }

        // default for claim creation/modification tool
        String modificationToolMaterialName = Material.GOLD_SPADE.name();

        // get modification tool from config
        modificationToolMaterialName = config.getString("GriefPrevention.Claims.ModificationTool", modificationToolMaterialName);

        // validate modification tool
        this.config_claims_modificationTool = Material.getMaterial(modificationToolMaterialName);
        if (this.config_claims_modificationTool == null) {
            GriefPrevention.AddLogEntry("ERROR: Material " + modificationToolMaterialName + " not found.  Defaulting to the golden shovel. ");
            this.config_claims_modificationTool = Material.GOLD_SPADE;
        }
        config_administration_tool = Material.FIRE;
        String admintoolName = config.getString("GriefPrevention.AdministrationTool", config_administration_tool.name());
        config_administration_tool = Material.getMaterial(admintoolName);
        if (config_administration_tool == null) {
            GriefPrevention.AddLogEntry("ERROR: Material " + modificationToolMaterialName + " not found.  Defaulting to the chainmail helm.");
            this.config_claims_modificationTool = Material.FIRE;
        }



		/*
		 * // Removed this segment. It supports Access, Container, and Trust
		 * config settings for being able to hit other players with certain
		 * items to add them to a claim. It was removed because it "doesn't fit"
		 * and "should be in another plugin". I kept it here because...meh, may as well.
		 *
		 *
		 * String AccessTrustTool =
		 * config.getString("GriefPrevention.Claims.AccessTrustTool"
		 * ,Material.FEATHER.name()); String ContainerTrustTool =
		 * config.getString
		 * ("GriefPrevention.Claims.ContainerTrustTool",Material.STRING.name());
		 * String TrustTool =
		 * config.getString("GriefPrevention.Claims.TrustTool"
		 * ,Material.GOLD_NUGGET.name());
		 *
		 * this.config_claims_giveAccessTrustTool =
		 * Material.getMaterial(AccessTrustTool);
		 * this.config_claims_giveContainerTrustTool =
		 * Material.getMaterial(ContainerTrustTool);
		 * this.config_claims_giveTrustTool = Material.getMaterial(TrustTool);
		 *
		 *
		 * if(config_claims_giveAccessTrustTool==null){
		 * GriefPrevention.AddLogEntry("Error: Access Trust Tool " +
		 * modificationToolMaterialName + " Not valid. Defaulting to Feather.");
		 * config_claims_giveAccessTrustTool = Material.FEATHER; }
		 * if(config_claims_giveContainerTrustTool==null){
		 * GriefPrevention.AddLogEntry("Error: Container Trust Tool " +
		 * modificationToolMaterialName + " Not valid. Defaulting to String.");
		 * config_claims_giveContainerTrustTool = Material.STRING; }
		 * if(config_claims_giveTrustTool==null){
		 * GriefPrevention.AddLogEntry("Error: Trust Tool " +
		 * modificationToolMaterialName +
		 * " Not valid. Defaulting to Gold Nugget");
		 * config_claims_giveContainerTrustTool = Material.GOLD_NUGGET; }
		 *
		 * //save the tools.
		 * outConfig.set("GriefPrevention.Claims.AccessTrustTool",
		 * config_claims_giveAccessTrustTool);
		 * outConfig.set("GriefPrevention.Claims.ContainerTrustTool",
		 * config_claims_giveContainerTrustTool);
		 * outConfig.set("GriefPrevention.Claims.TrustTool",
		 * config_claims_giveTrustTool);
		 */
        // default for siege worlds list
        // ArrayList<String> defaultSiegeWorldNames = new ArrayList<String>();

        this.config_siege_enabled = config.getBoolean("GriefPrevention.Siege.Enabled", isPvP);
         outConfig.set("GriefPrevention.Siege.Enabled",config_siege_enabled);
        // default siege blocks
        this.config_siege_blocks = new ArrayList<SiegeableData>();
        this.config_siege_blocks.add(new SiegeableData(Material.DIRT.name()));
        this.config_siege_blocks.add(new SiegeableData(Material.GRASS.name()));
        this.config_siege_blocks.add(new SiegeableData(Material.LONG_GRASS.name()));
        this.config_siege_blocks.add(new SiegeableData(Material.COBBLESTONE.name()));
        this.config_siege_blocks.add(new SiegeableData(Material.GRAVEL.name()));
        this.config_siege_blocks.add(new SiegeableData(Material.SAND.name()));
        this.config_siege_blocks.add(new SiegeableData(Material.GLASS.name()));
        this.config_siege_blocks.add(new SiegeableData(Material.THIN_GLASS.name()));
        this.config_siege_blocks.add(new SiegeableData(Material.WOOD.name()));
        this.config_siege_blocks.add(new SiegeableData(Material.WOOL.name()));
        this.config_siege_blocks.add(new SiegeableData(Material.SNOW.name()));

        if(GriefPrevention.isMCVersionorLater(GriefPrevention.MinecraftVersions.MC17)){
            this.config_siege_blocks.add(new SiegeableData(Material.STAINED_GLASS.name()));
            this.config_siege_blocks.add(new SiegeableData(Material.STAINED_GLASS_PANE.name()));
        }


        this.config_siege_blocks = SiegeableData.readList(config, outConfig, "GriefPrevention.Siege.BreakableBlocks", config_siege_blocks);
        config_TNTSiege_blocks = new ArrayList<SiegeableData>();
        //go with cobble for now.


        for(SiegeableData sd:config_siege_blocks){
            config_TNTSiege_blocks.add(sd);
        }

        config_TNTSiege_blocks.add(new SiegeableData(Material.BRICK.name()));
        config_TNTSiege_blocks.add(new SiegeableData(Material.CLAY_BRICK.name()));
        config_TNTSiege_blocks.add(new SiegeableData(Material.NETHER_BRICK.name()));
        config_TNTSiege_blocks.add(new SiegeableData(Material.SANDSTONE.name()));
        config_TNTSiege_blocks.add(new SiegeableData(Material.STONE.name()));
        if(GriefPrevention.isMCVersionorLater(GriefPrevention.MinecraftVersions.MC16)){
            config_TNTSiege_blocks.add(new SiegeableData(Material.STAINED_CLAY.name()));
            config_TNTSiege_blocks.add(new SiegeableData(Material.HARD_CLAY.name()));
        }


        this.config_TNTSiege_blocks = SiegeableData.readList(config, outConfig, "GriefPrevention.Siege.ExplosionBreakableBlocks", config_TNTSiege_blocks);

        //List<String> breakableBlocksList = config.getStringList("GriefPrevention.Siege.BreakableBlocks");

        this.config_pvp_noCombatInPlayerLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims");

        Debugger.Write("\"GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims\" is " + String.valueOf(config_pvp_noCombatInPlayerLandClaims),DebugLevel.Verbose);

        this.config_pvp_noCombatInAdminLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", !this.config_siege_enabled);

        outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims", this.config_pvp_noCombatInPlayerLandClaims);
        outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", this.config_pvp_noCombatInAdminLandClaims);

        //this.SpawnProtectEnabled = config.getBoolean("GriefPrevention.PvP.ProtectFreshSpawns", true);
        // outConfig.set("GriefPrevention.Claims.Worlds",
        // claimsEnabledWorldNames);
        // outConfig.set("GriefPrevention.Claims.CreativeRulesWorlds",
        // creativeClaimsEnabledWorldNames);

        outConfig.set("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", this.config_claims_automaticClaimsForNewPlayersRadius);
        outConfig.set("GriefPrevention.Claims.ExtendIntoGroundDistance", this.config_claims_claimsExtendIntoGroundDistance);
        outConfig.set("GriefPrevention.Claims.CreationRequiresPermission", this.config_claims_creationRequiresPermission);
        outConfig.set("GriefPrevention.Claims.MinimumSize", this.config_claims_minSize);
        outConfig.set("GriefPrevention.Claims.MinimumBlocks", this.config_claims_minBlocks);
        outConfig.set("GriefPrevention.Claims.MaximumDepth", this.config_claims_maxDepth);
        outConfig.set("GriefPrevention.Claims.TrappedCommandCooldownMinutes", this.config_claims_trappedCooldownMinutes);
        outConfig.set("GriefPrevention.Claims.InvestigationTool", this.config_claims_investigationTool.name());
        outConfig.set("GriefPrevention.Claims.ModificationTool", this.config_claims_modificationTool.name());

        outConfig.set("GriefPrevention.Claims.WarnWhenBuildingOutsideClaims", this.config_claims_warnOnBuildOutside);
        outConfig.set("GriefPrevention.Claims.AllowUnclaimingLand", this.config_claims_allowUnclaim);
        outConfig.set("GriefPrevention.Claims.AutoRestoreUnclaimedLand", this.config_claims_autoRestoreUnclaimed);

        // outConfig.set("GriefPrevention.Claims.TrashBlocks",trashblocks);
        outConfig.set("GriefPrevention.Claims.WildernessWarningBlockCount", this.config_claims_wildernessBlocksDelay);


        outConfig.set("GriefPrevention.Trees.RemoveFloatingTreetops", this.config_trees_removeFloatingTreetops);
        outConfig.set("GriefPrevention.Trees.RegrowGriefedTrees", this.config_trees_regrowGriefedTrees);

        // outConfig.set("GriefPrevention.BlockSurfaceWildCreeperExplosions",
        // this.config_blockSurfaceWildCreeperExplosions);
        // outConfig.set("GriefPrevention.BlockSurfaceWildOtherExplosions",
        // this.config_blockSurfaceWildOtherExplosions);

        outConfig.set("GriefPrevention.LimitSkyTrees", this.config_blockSkyTrees);


        outConfig.set("GriefPrevention.AddItemsToClaimedChests", this.config_addItemsToClaimedChests);

        outConfig.set("GriefPrevention.SmartBan", this.config_smartBan);

        // outConfig.set("GriefPrevention.Siege.Worlds",
        // siegeEnabledWorldNames);


        outConfig.set("GriefPrevention.CreaturesTrampleCrops", this.config_creaturesTrampleCrops);

        outConfig.set("GriefPrevention.Mods.BlockIdsRequiringAccessTrust", this.config_mods_accessTrustIds);
        outConfig.set("GriefPrevention.Mods.BlockIdsRequiringContainerTrust", this.config_mods_containerTrustIds);
        outConfig.set("GriefPrevention.Mods.BlockIdsExplodable", this.config_mods_explodableIds);
        outConfig.set("GriefPrevention.Mods.PlayersIgnoringAllClaims", this.config_mods_ignoreClaimsAccounts);
        outConfig.set("GriefPrevention.Mods.BlockIdsRequiringAccessTrust", accessTrustStrings);
        outConfig.set("GriefPrevention.Mods.BlockIdsRequiringContainerTrust", containerTrustStrings);
        this.BlockBreakOverrides = new PlaceBreakOverrides(config, outConfig, "GriefPrevention.Rules.BreakOverrides", PlaceBreakOverrides.Default);
        this.BlockPlaceOverrides = new PlaceBreakOverrides(config, outConfig, "GriefPrevention.Rules.PlaceOverrides", PlaceBreakOverrides.Default);
        this.config_BlockPlacementRules = BlockPlacementRules.ParseRules(config, outConfig, "GriefPrevention.BlockPlacementRules");
        this.config_BlockBreakRules = BlockPlacementRules.ParseRules(config, outConfig, "GriefPrevention.BlockBreakRules");
        this.PlaceBlockRules = new ClaimBehaviourData("Block Placement", config, outConfig, "GriefPrevention.Rules.BlockPlacement", new ClaimBehaviourData("Block Placement", PlacementRules.Both, PlacementRules.Both, ClaimBehaviourMode.RequireBuild).setPVPOverride(SiegePVPOverrideConstants.Deny).setSiegeOverrides(SiegePVPOverrideConstants.Deny, SiegePVPOverrideConstants.Deny));
        this.BreakBlockRules = new ClaimBehaviourData("Block Breaking", config, outConfig, "GriefPrevention.Rules.BlockBreaking", new ClaimBehaviourData("Block Breaking", PlacementRules.Both, PlacementRules.Both, ClaimBehaviourMode.RequireBuild).setPVPOverride(SiegePVPOverrideConstants.Deny).setSiegeOverrides(SiegePVPOverrideConstants.Allow, SiegePVPOverrideConstants.Deny));

        boolean nosurvivalbuilding = config.getBoolean("GriefPrevention.NoSurvivalBuildingOutsideClaims",false);
        if(nosurvivalbuilding){
            outConfig.set("GriefPrevention.NoSurvivalBuildingOutsideClaims",true);
            GriefPrevention.AddLogEntry("NoSurvivalBuildingOutsideClaims option detected: attempting conversion to appropriate overrides.");
            //PlaceBlockRules = ClaimBehaviourData.getAll("Block Placement").setBehaviourMode(ClaimBehaviourMode.RequireBuild);
            PlaceBlockRules = new ClaimBehaviourData("Block Placement",
                    new PlacementRules(PlacementRules.BasicPermissionConstants.Force_Deny,PlacementRules.BasicPermissionConstants.Force_Deny),
                    PlacementRules.Both,
                    ClaimBehaviourMode.RequireBuild).setPVPOverride(SiegePVPOverrideConstants.Deny).setSiegeOverrides(SiegePVPOverrideConstants.Allow, SiegePVPOverrideConstants.Deny);
            BreakBlockRules =
                    new ClaimBehaviourData("Block Breaking",
                            new PlacementRules(PlacementRules.BasicPermissionConstants.Force_Deny,PlacementRules.BasicPermissionConstants.Force_Deny),
                            PlacementRules.Both,
                            ClaimBehaviourMode.RequireBuild).setPVPOverride(SiegePVPOverrideConstants.Deny).setSiegeOverrides(SiegePVPOverrideConstants.Allow, SiegePVPOverrideConstants.Deny);
            //create an override for chests and trapped chests.
            Material[] addforMaterials = new Material[]{Material.CHEST,Material.TRAPPED_CHEST};
            for(Material overridemat:addforMaterials){
                this.getBlockPlaceOverrides().AddOverride("Chests:" + overridemat.name(),overridemat.getId(),
                        ClaimBehaviourData.getOutsideClaims("Chests" + overridemat.name()).setBehaviourMode(ClaimBehaviourMode.RequireBuild).addSpecialRule(ClaimBehaviourData.SpecialRules.ClaimRule_RequireNoClaims));
            }


            Debugger.Write(this.getBlockPlaceOverrides().toString(),DebugLevel.Verbose);
        }

        this.SilverfishBreakRules = new ClaimBehaviourData("Silverfish Break", config, outConfig, "GriefPrevention.Rules.SilverfishBreak", new ClaimBehaviourData("Silverfish Break", PlacementRules.Both, PlacementRules.Neither, ClaimBehaviourMode.Disabled));



        // outConfig.set("GriefPrevention.Mods.BlockIdsExplodable",
        // explodableStrings);

        //placement requires build permission by default, and isn't allowed during PVP or Siege.


        this.FireDestroyBehaviour = new ClaimBehaviourData("Fire Destruction", config, outConfig, "GriefPrevention.Rules.FireDestroys", new ClaimBehaviourData("GriefPrevention.Rules.FireDestroys", PlacementRules.Neither, PlacementRules.Neither, ClaimBehaviourMode.Disabled));
        this.FireSpreadOriginBehaviour = new ClaimBehaviourData("Fire Spread Origin", config, outConfig, "GriefPrevention.Rules.FireSpreadOrigin", new ClaimBehaviourData("Fire Spread Origin", PlacementRules.Neither, PlacementRules.Neither, ClaimBehaviourMode.Disabled));
        this.FireSpreadTargetBehaviour = new ClaimBehaviourData("Fire Spread Target", config, outConfig, "GriefPrevention.Rules.FireSpreadTarget", new ClaimBehaviourData("Fire Spread Target", PlacementRules.Neither, PlacementRules.Neither, ClaimBehaviourMode.Disabled));
        this.CreeperExplosionsBehaviour = new ClaimBehaviourData("Creeper Explosions", config, outConfig, "GriefPrevention.Rules.CreeperExplosions", new ClaimBehaviourData("Creeper Explosions", PlacementRules.BelowOnly, PlacementRules.Both, ClaimBehaviourMode.Disabled).setSeaLevelOffsets(SeaLevelOverrideTypes.Offset, -1));
        this.WitherExplosionBehaviour = new ClaimBehaviourData("Wither Explosions", config, outConfig, "GriefPrevention.Rules.WitherExplosions", new ClaimBehaviourData("Wither Explosions", PlacementRules.Neither, PlacementRules.Neither, ClaimBehaviourMode.Disabled).setSeaLevelOffsets(SeaLevelOverrideTypes.Offset, -1));
        this.TNTExplosionsBehaviour = new ClaimBehaviourData("TNT Explosions", config, outConfig, "GriefPrevention.Rules.TNTExplosions",



                ClaimBehaviourData.getAll("TNT Explosions").setSeaLevelOffsets(SeaLevelOverrideTypes.Offset, -1));

        this.EnderDragonDamageBehaviour = new ClaimBehaviourData("Ender Dragon Damage",config,outConfig,"GriefPrevention.Rules.EnderDragonDamage",
                new ClaimBehaviourData("Ender Dragon Damage", PlacementRules.AboveOnly, PlacementRules.Both, ClaimBehaviourMode.Disabled).setSeaLevelOffsets(SeaLevelOverrideTypes.Offset, -1));
        this.EnderDragonDamageBlockBehaviour = new ClaimBehaviourData("Ender Dragon Block Damage",config,outConfig,"GriefPrevention.Rules.EnderDragonDamage",
                new ClaimBehaviourData("Ender Dragon Block Damage", PlacementRules.AboveOnly, PlacementRules.Both, ClaimBehaviourMode.Disabled).setSeaLevelOffsets(SeaLevelOverrideTypes.Offset, -1));

        this.EnderEyePortalRules = new ClaimBehaviourData("Ender Portal Fill", config, outConfig, "GriefPrevention.Rules.EnderPortalFill",
                ClaimBehaviourData.getAll("Ender Portal Fill").setBehaviourMode(ClaimBehaviourMode.RequireBuild));
        this.FlowerPotRules = new ClaimBehaviourData("Flower Pots", config, outConfig, "GriefPrevention.Rules.FlowerPots",
                ClaimBehaviourData.getAll("Flower Pots").setBehaviourMode(ClaimBehaviourMode.RequireBuild));
        this.ItemFrameItemRules = new ClaimBehaviourData("Item Frame Items", config, outConfig, "GriefPrevention.Rules.ItemFrameItems",
                ClaimBehaviourData.getAll("Item Frame").setBehaviourMode(ClaimBehaviourMode.RequireBuild));
        this.SpawnEggBehaviour = new ClaimBehaviourData("Spawn Eggs",config,outConfig,"GriefPrevention.Rules.SpawnEggs",
                ClaimBehaviourData.getAll("Monster Eggs").setBehaviourMode(ClaimBehaviourMode.RequireBuild));



        this.WitherExplosionBlockDamageBehaviour = new ClaimBehaviourData("Wither Explosion Damage", config, outConfig, "GriefPrevention.Rules.BlockDamageWitherExplosions", new ClaimBehaviourData("Wither Explosion Damage", PlacementRules.AboveOnly, PlacementRules.Neither, ClaimBehaviourMode.Disabled).setSeaLevelOffsets(SeaLevelOverrideTypes.Offset, -1));
        this.OtherExplosionBehaviour = new ClaimBehaviourData("Other Explosions", config, outConfig, "GriefPrevention.Rules.OtherExplosions", ClaimBehaviourData.getAll("Other Explosions").setSeaLevelOffsets(SeaLevelOverrideTypes.Offset, -1));
        this.WitherEatBehaviour = new ClaimBehaviourData("Wither Eating", config, outConfig, "GriefPrevention.Rules.WitherEating", new ClaimBehaviourData("Wither Eating", PlacementRules.AboveOnly, PlacementRules.Neither, ClaimBehaviourMode.Disabled));
        this.CreeperExplosionBlockDamageBehaviour = new ClaimBehaviourData("Creeper Explosion Damage", config, outConfig, "GriefPrevention.Rules.BlockDamageCreeperExplosion", new ClaimBehaviourData("Creeper Explosion Damage", PlacementRules.AboveOnly, PlacementRules.Neither, ClaimBehaviourMode.Disabled).setSeaLevelOffsets(SeaLevelOverrideTypes.Offset, -1).setSiegeNonPlayerOverride(SiegePVPOverrideConstants.Allow));
        this.TNTExplosionBlockDamageBehaviour = new ClaimBehaviourData("TNT Explosion Damage", config, outConfig, "GriefPrevention.Rules.BlockDamageTNTExplosions", ClaimBehaviourData.getAll("TNT Explosion Damage").setSeaLevelOffsets(SeaLevelOverrideTypes.Offset, -1).setSiegeNonPlayerOverride(SiegePVPOverrideConstants.Allow));
        this.OtherExplosionBlockDamageBehaviour = new ClaimBehaviourData("Other Explosion Damage", config, outConfig, "GriefPrevention.Rules.BlockDamageOtherExplosions", ClaimBehaviourData.getOutsideClaims("Other Explosion Damage").getAboveSeaLevel("Other Explosion Damage").setSeaLevelOffsets(SeaLevelOverrideTypes.Offset, -1).setSiegeNonPlayerOverride(SiegePVPOverrideConstants.Allow));
        ClaimBehaviourData WaterRequire = new ClaimBehaviourData("Water Placement", PlacementRules.BelowOnly, PlacementRules.Both, ClaimBehaviourMode.RequireBuild);
        ClaimBehaviourData LavaRequire = new ClaimBehaviourData("Lava Placement", PlacementRules.BelowOnly, PlacementRules.Both, ClaimBehaviourMode.RequireBuild).setWildernessRequiredPermission(PermNodes.LavaPermission);
        this.WaterBucketEmptyBehaviour = new ClaimBehaviourData("Water Placement", config, outConfig, "GriefPrevention.Rules.WaterBuckets.Place", WaterRequire).setWildernessRequiredPermission(PermNodes.WaterPermission);
        this.LavaBucketEmptyBehaviour = new ClaimBehaviourData("Lava Placement", config, outConfig, "GriefPrevention.Rules.LavaBuckets.Place", LavaRequire);
        this.WaterBucketFillBehaviour = new ClaimBehaviourData("Water Bucket Fill", config, outConfig, "GriefPrevention.Rules.WaterBuckets.Fill", ClaimBehaviourData.getAll("Water Bucket Fill").setBehaviourMode(ClaimBehaviourMode.RequireBuild));
        this.LavaBucketFillBehaviour = new ClaimBehaviourData("Lava Bucket Fill", config, outConfig, "GriefPrevention.Rules.LavaBuckets.Fill", ClaimBehaviourData.getAll("Lava Bucket Fill").setBehaviourMode(ClaimBehaviourMode.RequireBuild));
        // Snow golem spawn rules.
        this.IronGolemSpawnBehaviour = new ClaimBehaviourData("Iron Golem Spawning", config, outConfig, "GriefPrevention.Rules.BuildIronGolem", ClaimBehaviourData.getInsideClaims("Iron Golem Spawning"));
        this.SnowGolemSpawnBehaviour = new ClaimBehaviourData("Snow Golem Spawning", config, outConfig, "GriefPrevention.Rules.BuildSnowGolem", ClaimBehaviourData.getInsideClaims("Snow Golem Spawning"));
        this.WitherSpawnBehaviour = new ClaimBehaviourData("Wither Spawning", config, outConfig, "GriefPrevention.Rules.BuildWither", ClaimBehaviourData.getInsideClaims("Wither Spawning"));
        TrashBlockPlacementBehaviour = new ClaimBehaviourData("Trash Block Placement", config, outConfig, "GriefPrevention.Rules.TrashBlockPlacementRules", ClaimBehaviourData.getOutsideClaims("Trash Block Placement").setBehaviourMode(ClaimBehaviourMode.RequireBuild));
        VillagerTrades = new ClaimBehaviourData("Villager Trading", config, outConfig, "GriefPrevention.Rules.VillagerTrading", ClaimBehaviourData.getInsideClaims("Villager Trading").setBehaviourMode(ClaimBehaviourMode.RequireContainer));
        this.VehicleDamage = new ClaimBehaviourData("Vehicle Damage", config, outConfig, "GriefPrevention.Rules.VehicleDamage", ClaimBehaviourData.getAll("Vehicle Damage").setBehaviourMode(ClaimBehaviourMode.RequireContainer));
        this.EnvironmentalVehicleDamage = new ClaimBehaviourData("Environmental Vehicle Damage", config, outConfig, "GriefPrevention.Rules.EnvironmentalVehicleDamage", ClaimBehaviourData.getOutsideClaims("Environmental Vehicle Damage"));
        //WIP: not applicable for this commit...
        ClaimBehaviourData vehiclerule = new ClaimBehaviourData("Vehicle Boarding",config,outConfig,"GriefPrevention.Rules.VehicleBoarding",
                ClaimBehaviourData.getAll("Vehicle Boarding").setBehaviourMode(ClaimBehaviourMode.RequireContainer));
        if(isValidRule(config,"GriefPrevention.Rules.VehicleBoarding")){
           Riding_Pig = Riding_Horse = vehiclerule;

        }

        //Pig,Boat,Minecart, Horse.
        if(isValidRule(config,"GriefPrevention.Rules.Riding.Boat")){
            Riding_Boat = new ClaimBehaviourData("Boat Riding",config,outConfig,
                    "GriefPrevention.Rules.Riding.Boat",
                    ClaimBehaviourData.getAll("Boat Riding").setBehaviourMode(ClaimBehaviourMode.RequireContainer));
        }
        else {
            Riding_Boat = vehiclerule;
        }
        if(isValidRule(config,"GriefPrevention.Rules.Riding.Minecart")){
            Riding_Minecart = new ClaimBehaviourData("Minecart Riding",config,outConfig,
                    "GriefPrevention.Rules.Riding.Minecart",
            ClaimBehaviourData.getAll("Minecart Riding").setBehaviourMode(ClaimBehaviourMode.RequireContainer));
        }
        Placement_Boat =new ClaimBehaviourData("Boat Placement",config,outConfig,
                "GriefPrevention.Rules.VehiclePlacement.Boat",
                ClaimBehaviourData.getAll("Boat Placement").setBehaviourMode(ClaimBehaviourMode.RequireBuild));
        Placement_Minecart = new ClaimBehaviourData("Minecart Placement",config,outConfig,
                "GriefPrevention.Rules.VehiclePlacement.Minecart",
                ClaimBehaviourData.getAll("Minecart Placement").setBehaviourMode(ClaimBehaviourMode.RequireBuild));

        this.ZombieDoorBreaking = new ClaimBehaviourData("Zombie Door Breaking", config, outConfig, "GriefPrevention.Rules.ZombieDoorBreaking", ClaimBehaviourData.getNone("Zombie Door Breaking"));
        SheepShearingRules = new ClaimBehaviourData("Sheep Shearing", config, outConfig, "GriefPrevention.Rules.SheepShearing", ClaimBehaviourData.getAll("Sheep Shearing").setBehaviourMode(ClaimBehaviourMode.RequireContainer));
        SheepDyeing = new ClaimBehaviourData("Sheep Dyeing", config, outConfig, "GriefPrevention.Rules.SheepDyeing", ClaimBehaviourData.getAll("Sheep Dyeing").setBehaviourMode(ClaimBehaviourMode.RequireContainer));
        this.BonemealGrass = new ClaimBehaviourData("Bonemeal", config, outConfig, "GriefPrevention.Rules.BonemealGrass", ClaimBehaviourData.getAll("Bonemeal").setBehaviourMode(ClaimBehaviourMode.RequireBuild));
        this.PlayerTrampleRules = new ClaimBehaviourData("Crop Trampling", config, outConfig, "GriefPrevention.Rules.PlayerCropTrample", ClaimBehaviourData.getAll("Crop Trampling").setBehaviourMode(ClaimBehaviourMode.RequireBuild));
        this.EndermanPickupRules = new ClaimBehaviourData("Enderman Pickup", config, outConfig, "GriefPrevention.Rules.EndermanPickup", ClaimBehaviourData.getNone("Enderman Pickup"));
        this.EndermanPlacementRules = new ClaimBehaviourData("Enderman Placement", config, outConfig, "GriefPrevention.Rules.EndermanPlacement", ClaimBehaviourData.getNone("Enderman Placement"));
        this.ArrowWoodenButtonRules = new ClaimBehaviourData("Arrows Trigger Wood Buttons", config, outConfig, "GriefPrevention.Rules.ArrowsHitWoodButtons", ClaimBehaviourData.getAll("Arrows Trigger Wood Buttons").setBehaviourMode(ClaimBehaviourMode.RequireAccess));
        this.BlockTweakRules = new ClaimBehaviourData("Block Tweaking", config, outConfig, "GriefPrevention.Rules.BlockTweaking", ClaimBehaviourData.getAll("Block Tweaking").setBehaviourMode(ClaimBehaviourMode.RequireAccess));
        this.ContainersRules = new ClaimBehaviourData("Containers", config, outConfig, "GriefPrevention.Rules.Containers", ClaimBehaviourData.getAll("Containers").setBehaviourMode(ClaimBehaviourMode.RequireContainer).setPVPOverride(SiegePVPOverrideConstants.Deny).setSiegeOverrides(SiegePVPOverrideConstants.Deny, SiegePVPOverrideConstants.Deny));
        // defaults to only allowing theft outside claims.
        this.CreatureDamage = new ClaimBehaviourData("Creature Damage", config, outConfig, "GriefPrevention.Rules.CreatureDamage", ClaimBehaviourData.getAll("Creature Damage").setBehaviourMode(ClaimBehaviourMode.RequireContainer));
        this.WoodenDoors = new ClaimBehaviourData("Wooden Doors", config, outConfig, "GriefPrevention.Rules.WoodenDoors", ClaimBehaviourData.getAll("Wooden Doors").setBehaviourMode(ClaimBehaviourMode.RequireNone));
        this.TrapDoors = new ClaimBehaviourData("TrapDoors", config, outConfig, "GriefPrevention.Rules.TrapDoors", ClaimBehaviourData.getAll("TrapDoors").setBehaviourMode(ClaimBehaviourMode.RequireNone));
        this.FenceGates = new ClaimBehaviourData("Fence Gates", config, outConfig, "GriefPrevention.Rules.FenceGates", ClaimBehaviourData.getAll("Fence Gates").setBehaviourMode(ClaimBehaviourMode.RequireNone));
        this.EnderPearlOrigins = new ClaimBehaviourData("EnderPearl Origins", config, outConfig, "GriefPrevention.Rules.EnderPearlOrigin", ClaimBehaviourData.getAll("EnderPearl Origins").setBehaviourMode(ClaimBehaviourMode.RequireAccess));
        this.EnderPearlTargets = new ClaimBehaviourData("EnderPearl Targets", config, outConfig, "GriefPrevention.Rules.EnderPearlTarget", ClaimBehaviourData.getAll("EnderPearl Targets").setBehaviourMode(ClaimBehaviourMode.RequireAccess));
        this.StonePressurePlates = new ClaimBehaviourData("Stone Pressure Plates", config, outConfig, "GriefPrevention.Rules.StonePressurePlates", ClaimBehaviourData.getAll("Stone Pressure Plates").setBehaviourMode(ClaimBehaviourMode.RequireNone));
        this.WoodPressurePlates = new ClaimBehaviourData("Wooden Pressure Plates", config, outConfig, "GriefPrevention.Rules.WoodenPressurePlates", ClaimBehaviourData.getAll("Wooden Pressure Plates").setBehaviourMode(ClaimBehaviourMode.RequireNone));
        this.ArrowWoodenTouchplateRules = new ClaimBehaviourData("Wooden Touchplate", config, outConfig, "GriefPrevention.Rules.ArrowWoodenPressurePlate", ClaimBehaviourData.getAll("Wooden Pressure Plates Arrows").setBehaviourMode(ClaimBehaviourMode.RequireNone));
        this.StoneButton = new ClaimBehaviourData("Stone Button", config, outConfig, "GriefPrevention.Rules.StoneButton", ClaimBehaviourData.getAll("Stone Button").setBehaviourMode(ClaimBehaviourMode.RequireAccess));
        this.WoodenButton = new ClaimBehaviourData("Wooden Button", config, outConfig, "GriefPrevention.Rules.WoodenButton", ClaimBehaviourData.getAll("Wooden Button").setBehaviourMode(ClaimBehaviourMode.RequireAccess));
        this.Levers = new ClaimBehaviourData("Levers", config, outConfig, "GriefPrevention.Rules.Levers", ClaimBehaviourData.getAll("Levers").setBehaviourMode(ClaimBehaviourMode.RequireAccess));
        this.Beds = new ClaimBehaviourData("Beds", config, outConfig, "GriefPrevention.Rules.Beds", ClaimBehaviourData.getAll("Beds").setBehaviourMode(ClaimBehaviourMode.RequireAccess));
        this.FireExtinguishing = new ClaimBehaviourData("Fire Extinguishing", config, outConfig, "GriefPrevention.Rules.FireExtinguishing", ClaimBehaviourData.getAll("Fire Extinguishing").setBehaviourMode(ClaimBehaviourMode.RequireBuild));
        this.FireSetting = new ClaimBehaviourData("Fire Setting", config, outConfig, "GriefPrevention.Rules.FireSetting", ClaimBehaviourData.getInsideClaims("Fire Setting").setBehaviourMode(ClaimBehaviourMode.RequireBuild).setWildernessRequiredPermission("GriefPrevention.Lava"));
        // BreedingRules,EquineTamingRules,LeadUsageRules,
        // EquineInventoryRules,NameTagUsageRules
        // FeedingRules
        // breeding can only occur in claims and requires Access Trust by
        // default.
        this.BreedingRules = new ClaimBehaviourData("Animal Breeding", config, outConfig, "GriefPrevention.Rules.Breeding", ClaimBehaviourData.getInsideClaims("Animal Breeding").setBehaviourMode(ClaimBehaviourMode.RequireAccess)).setTameableAllowOwner(true);
        // TamingRules is for taming Ocelots and Wolves by using Fish and Bones
        // respectively,
        // as well as being a rule for attempting to ride a wild horse.
        // this defaults to working in the wild and in claims with access trust.
        this.TamingRules = new ClaimBehaviourData("Taming Rules", config, outConfig, "GriefPrevention.Rules.Taming", ClaimBehaviourData.getAll("Taming Rules").setBehaviourMode(ClaimBehaviourMode.RequireAccess));
        // Lead Usage rules
        // this is for using leads either on animals or on fences.
        // defaults to working everywhere but requires access trust within a
        // claim.
        this.LeadUsageRules = new ClaimBehaviourData("Lead Usage", config, outConfig, "GriefPrevention.Rules.LeadUsage", ClaimBehaviourData.getAll("Lead Usage").setBehaviourMode(ClaimBehaviourMode.RequireAccess)).setTameableAllowOwner(true);

        // EquineInventoryRules is for opening horse inventories.
        // This is a tricky one since horses and donkeys don't actually have
        // prescribed owners.
        // we will make this simply default to working everywhere but requiring
        // container trust within a claim.

        this.EquineInventoryRules = new ClaimBehaviourData("Equine Inventory", config, outConfig, "GriefPrevention.Rules.EquineInventory", ClaimBehaviourData.getNone("Equine Inventory")).setTameableAllowOwner(true);

        // Name Tag usage.
        // Will work in the wild. Requires Container trust inside of claims.
        this.NameTagUsageRules = new ClaimBehaviourData("Name Tags", config, outConfig, "GriefPrevention.Rules.NameTags", ClaimBehaviourData.getAll("Name Tags").setBehaviourMode(ClaimBehaviourMode.RequireContainer)).setTameableAllowOwner(true);

        // FeedingRules
        // this applies to feeding wheat, apples, and hay blocks to tamed
        // horses,
        // meat to tamed wolves and fish to tamed cats.

        this.FeedingRules = new ClaimBehaviourData("Feeding", config, outConfig, "GriefPrevention.Rules.Feeding", ClaimBehaviourData.getAll("Feeding").setBehaviourMode(ClaimBehaviourMode.RequireAccess)).setTameableAllowOwner(true);

        // Dispenser rules.
        this.DispenserLavaBehaviour = new ClaimBehaviourData("Lava Dispensing", config, outConfig, "GriefPrevention.Rules.Dispensers.Lava", ClaimBehaviourData.getInsideClaims("Lava Dispensing"));

        this.DispenserWaterBehaviour = new ClaimBehaviourData("Water Dispensing", config, outConfig, "GriefPrevention.Rules.Dispensers.Water", ClaimBehaviourData.getAll("Water Dispensing"));

        this.DispenserArrowBehaviour = new ClaimBehaviourData("Arrow Dispensing", config, outConfig, "GriefPrevention.Rules.Dispensers.Arrows", ClaimBehaviourData.getInsideClaims("Arrow Dispensing"));

        this.DispenserFireChargeBehaviour = new ClaimBehaviourData("Fire Charge Dispensing", config, outConfig, "GriefPrevention.Rules.Dispensers.FireCharges", ClaimBehaviourData.getInsideClaims("Fire Charge Dispensing"));

        this.DispenserFlintandSteelBehaviour = new ClaimBehaviourData("Flint and Steel Dispensing", config, outConfig, "GriefPrevention.Rules.Dispensers.FlintandSteel", ClaimBehaviourData.getInsideClaims("Flint and Steel Dispensing"));

        this.DispenserPositivePotionBehaviour = new ClaimBehaviourData("Positive Potion Dispensing", config, outConfig, "GriefPrevention.Rules.Dispensers.PositivePotions", ClaimBehaviourData.getAll("Positive Potion Dispensing"));

        this.DispenserNegativePotionBehaviour = new ClaimBehaviourData("Negative Potion Dispensing", config, outConfig, "GriefPrevention.Rules.Dispensers.NegativePotions", ClaimBehaviourData.getAll("Negative Potion Dispensing"));

        this.DispenserSpawnEggBehaviour = new ClaimBehaviourData("Spawn Egg Dispensing", config, outConfig, "GriefPrevention.Rules.Dispensers.SpawnEggs", ClaimBehaviourData.getAll("Spawn Egg Dispensing"));

        this.DispenserFireworkBehaviour = new ClaimBehaviourData("Firework Dispensing", config, outConfig, "GriefPrevention.Rules.Dispensers.Fireworks", ClaimBehaviourData.getAll("Firework Dispensing"));

        this.DispenserSnowballBehaviour = new ClaimBehaviourData("Snowball Dispensing", config, outConfig, "GriefPrevention.Rules.Dispensers.Snowballs", ClaimBehaviourData.getAll("Snowball Dispensing"));
        this.DispenserEggBehaviour = new ClaimBehaviourData("Egg Dispensing", config, outConfig, "GriefPrevention.Rules.Dispensers.Eggs", ClaimBehaviourData.getInsideClaims("Egg Dispensing"));
        this.DispenserMiscBehaviour = new ClaimBehaviourData("Misc Dispensing", config, outConfig, "GriefPrevention.Rules.Dispensers.Misc", ClaimBehaviourData.getAll("Misc Dispensing"));


        // Task startup.
        // if we have a blockaccrued value and the ClaimTask for delivering
        // claim blocks is null,
        // create and schedule it to run.
        if (config_claims_blocksAccruedPerHour > 0 && GriefPrevention.instance.ClaimTask == null) {
            Debugger.Write("Starting Claim Block Delivery Task.",DebugLevel.Verbose);
            GriefPrevention.instance.ClaimTask = new DeliverClaimBlocksTask();
            GriefPrevention.instance.getServer().getScheduler().scheduleSyncRepeatingTask(GriefPrevention.instance, GriefPrevention.instance.ClaimTask, 60L * 20 * 2, 60L * 20 * 5);
        }
        // similar logic for ClaimCleanup: if claim cleanup is enabled and there
        // isn't a cleanup task, start it.

    }

    public WorldConfig(World grabfor) {
        // //construct WorldConfig with default settings.
        // we construct a default FileConfiguration and call ourselves...
        this(grabfor.getName());

    }

    public boolean claims_warnOnBuildOutside() {
        return config_claims_warnOnBuildOutside;
    }

    public boolean creaturesTrampleCrops() {
        return config_creaturesTrampleCrops;
    }

    public List<String> eavesdrop_whisperCommands() {
        return config_eavesdrop_whisperCommands;
    }

    public boolean getAddItemsToClaimedChests() {
        return config_addItemsToClaimedChests;
    }

    public Material getAdministrationTool() {
        return config_administration_tool;
    }

    public int getafkDistanceCheck() {
        return config_afkDistanceCheck;
    }

    public int getafkDistanceSquared() {
        // if the distance squared variable is set, retrieve it. if it is the
        // default, set it and return that assignment result.

        return afkDistanceSquared != -1 ? afkDistanceSquared : (afkDistanceSquared = (config_afkDistanceCheck * config_afkDistanceCheck));
    }

    public boolean getAllowCombatItemDrop() {
        return config_pvp_allowCombatItemDrop;
    }

    public boolean getAllowItemSpawn() {
        return config_allowItemSpawn;
    }

    public boolean getAllowUnclaim() {
        return config_claims_allowUnclaim;
    }


    public ClaimBehaviourData getArrowWoodenButtonRules() {
        return ArrowWoodenButtonRules;
    }

    public ClaimBehaviourData getArrowWoodenTouchPlateRules() {
        return ArrowWoodenTouchplateRules;
    }

    public int getAutomaticClaimsForNewPlayerRadius() {
        return config_claims_automaticClaimsForNewPlayersRadius;
    }

    public boolean getAutoRestoreUnclaimed() {
        return config_claims_autoRestoreUnclaimed;
    }

    public ClaimBehaviourData getBeds() {
        return Beds;
    }

    public List<BlockPlacementRules> getBlockBreakRules() {
        return config_BlockBreakRules;
    }

    public List<BlockPlacementRules> getBlockPlacementRules() {
        return config_BlockPlacementRules;
    }

    public boolean getBlockSkyTrees() {
        return config_blockSkyTrees;
    }

    public ClaimBehaviourData getBlockTweakRules() {
        return BlockTweakRules;
    }

    public ClaimBehaviourData getBonemealGrassRules() {
        return BonemealGrass;
    }

    public ClaimBehaviourData getBreedingRules() {
        return BreedingRules;
    }

    public int getChestClaimExpirationDays() {
        return config_claims_chestClaimExpirationDays;
    }

    public float getClaimBlocksAccruedPerHour() {
        return config_claims_blocksAccruedPerHour;
    }

    public boolean getClaimCleanupEnabled() {
        return config_claimcleanup_enabled;
    }

    public int getClaimCleanupMaximumSize() {
        return config_claimcleanup_maximumsize;
    }

    public int getClaimCleanupMaxInvestmentScore() {
        return config_claimcleanup_maxinvestmentscore;
    }

    public int getClaims_maxBlocks() {
        return config_claims_maxblocks;
    }

    public boolean getClaimsAbandonNatureRestoration() {
        return config_claims_Abandon_NatureRestoration;
    }

    public double getClaimsAbandonReturnRatio() {
        return config_claims_AbandonReturnRatio;
    }

    public boolean getClaimsAutoNatureRestoration() {
        return config_claims_AutoNatureRestoration;
    }

    public boolean Enabled() {
        return  griefprevention_enabled;
    }

    /**
     * returns whether Claims are enabled. Most configuration Options, while
     * still present and readable, become redundant when this is false.
     *
     * @return
     */
    public boolean getClaimsEnabled() {
        return GriefPrevention.instance.Configuration.getGlobalClaimsEnabled() && claims_enabled && this.Enabled(); //Enabled also causes this to be set.
    }

    public int getClaimsExpirationDays() {
        return config_claims_expirationDays;
    }

    public int getClaimsExtendIntoGroundDistance() {
        return config_claims_claimsExtendIntoGroundDistance;
    }

    public Material getClaimsInvestigationTool() {
        return config_claims_investigationTool;
    }

    public int getClaimsMaxDepth() {
        return config_claims_maxDepth;
    }

    public Material getClaimsModificationTool() {
        return config_claims_modificationTool;
    }

    public int getClaimsPerPlayerLimit() {
        return config_claims_perplayer_claim_limit;
    }

    public int getClaimsTrappedCooldownMinutes() {
        return config_claims_trappedCooldownMinutes;
    }

    public int getClaimsWildernessBlocksDelay() {
        return config_claims_wildernessBlocksDelay;
    }

    public int getConfigShowSurroundingsRadius() {
        return config_claims_showsurroundingsRadius;
    }

    /**
     * Returns Behaviour Information on Containers. This defaults to requiring
     * Container trust inside claims, and is disabled outside claims. This
     *
     * @return
     */
    public ClaimBehaviourData getContainersRules() {
        return ContainersRules;
    }

    public boolean getCreateClaimRequiresPermission() {
        return config_claims_creationRequiresPermission;
    }

    public boolean getCreativeRules() {
        return config_claims_creativeRules;
    }

    public ClaimBehaviourData getCreatureDamage() {
        return CreatureDamage;
    }

    public ClaimBehaviourData getCreeperExplosionBehaviour() {
        return CreeperExplosionsBehaviour;
    }

    public ClaimBehaviourData getCreeperExplosionBlockDamageBehaviour() {
        return CreeperExplosionBlockDamageBehaviour;
    }
    public ClaimBehaviourData getEnderDragonDamageBehaviour(){
      return EnderDragonDamageBehaviour;
    }
    public ClaimBehaviourData getEnderDragonDamageBlocksBehaviour(){
        return EnderDragonDamageBlockBehaviour;
    }
    public boolean getDenyAllExplosions() {
        return config_deny_all_explosions;
    }

    public ClaimBehaviourData getDispenserArrowBehaviour() {
        return DispenserArrowBehaviour;
    }

    public ClaimBehaviourData getDispenserEggBehaviour() {
        return this.DispenserEggBehaviour;
    }

    public ClaimBehaviourData getDispenserFireChargeBehaviour() {
        return DispenserFireChargeBehaviour;
    }

    public ClaimBehaviourData getDispenserFireworkBehaviour() {
        return this.DispenserFireworkBehaviour;
    }

    public ClaimBehaviourData getDispenserFlintandSteelBehaviour() {
        return DispenserFlintandSteelBehaviour;
    }

    public ClaimBehaviourData getDispenserLavaBehaviour() {
        return DispenserLavaBehaviour;
    }

    public ClaimBehaviourData getDispenserMiscBehaviour() {
        return this.DispenserMiscBehaviour;
    }

    public ClaimBehaviourData getDispenserNegativePotionBehaviour() {
        return this.DispenserPositivePotionBehaviour;
    }

    public ClaimBehaviourData getDispenserPositivePotionBehaviour() {
        return this.DispenserPositivePotionBehaviour;
    }

    public ClaimBehaviourData getDispenserSnowballBehaviour() {
        return this.DispenserSnowballBehaviour;
    }

    public ClaimBehaviourData getDispenserSpawnEggBehaviour() {
        return this.DispenserSpawnEggBehaviour;
    }

    public ClaimBehaviourData getDispenserWaterBehaviour() {
        return DispenserWaterBehaviour;
    }

    public boolean getEavesDrop() {
        return config_eavesdrop;
    }

    public boolean getEavesDropBookDrop() {
        return config_eavesdrop_bookdrop;
    }

    public ClaimBehaviourData getEndermanPickupRules() {
        return EndermanPickupRules;
    }

    public ClaimBehaviourData getEndermanPlacementRules() {
        return EndermanPlacementRules;
    }

    public ClaimBehaviourData getEnderPearlOrigins() {
        return EnderPearlOrigins;
    }

    public ClaimBehaviourData getEnderPearlTargets() {
        return EnderPearlTargets;
    }

	/*
	 * private Material config_claims_giveAccessTrustTool; public Material
	 * getClaimsAccessTrustTool(){ return config_claims_giveAccessTrustTool;}
	 * 
	 * private Material config_claims_giveContainerTrustTool; public Material
	 * getClaimsContainerTrustTool(){ return
	 * config_claims_giveContainerTrustTool;}
	 * 
	 * private Material config_claims_giveTrustTool; public Material
	 * getClaimsGiveTrustTool(){ return config_claims_giveTrustTool;}
	 */

    public int getEntityClaimLimit() {
        return config_entityClaimLimit;
    }

    public boolean getEntityCleanupEnabled() {
        return config_entitycleanup_enabled;
    }

    public ClaimBehaviourData getVehicleDamage() {
        return VehicleDamage;
    }

    public ClaimBehaviourData getEnvironmentalVehicleDamage() {
        return this.EnvironmentalVehicleDamage;
    }

    public ClaimBehaviourData getEquineInventoryRules() {
        return EquineInventoryRules;
    }

    public ClaimBehaviourData getFeedingRules() {
        return FeedingRules;
    }

    public ClaimBehaviourData getFenceGates() {
        return FenceGates;
    }


    public ClaimBehaviourData getFireExtinguishing() {
        return FireExtinguishing;
    }

    public ClaimBehaviourData getFireSetting() {
        return FireSetting;
    }


    // than this many more claim blocks, the first corner will be reset. This is
    // a "hack" for
    // modded servers where some events don't fire properly.
    public int getInsufficientSneakResetBound() {
        return InsufficientSneakResetBound;
    }

    public ClaimBehaviourData getIronGolemSpawnBehaviour() {
        return IronGolemSpawnBehaviour;
    }

    public List<ItemUsageRules> getItemRules() {
        return ItemRules;
    }

    public ClaimBehaviourData getLavaBucketEmptyBehaviour() {
        return LavaBucketEmptyBehaviour;
    }

    public ClaimBehaviourData getLavaBucketFillBehaviour() {
        return LavaBucketFillBehaviour;
    }

    public ClaimBehaviourData getLeadUsageRules() {
        return LeadUsageRules;
    }

    public ClaimBehaviourData getLevers() {
        return Levers;
    }

    public int getMessageCooldownClaims() {
        return config_message_cooldown_claims;
    }

    public int getMessageCooldownStuck() {
        return config_message_cooldown_stuck;
    }

    public int getMessageCooldownIgnore() {
        return config_message_cooldown_ignore;
    }

    public int getMinClaimSize() {
        return config_claims_minSize;
    }

    public int getMinClaimSizeBlocks() {
        return config_claims_minBlocks;
    }

    public ClaimBehaviourData getModInteractables() {
        return ModInteractables;
    }

    public MaterialCollection getModsAccessTrustIds() {
        return config_mods_accessTrustIds;
    }

    public MaterialCollection getModsContainerTrustIds() {
        return config_mods_containerTrustIds;
    }

    public List<String> getModsIgnoreClaimsAccounts() {
        return config_mods_ignoreClaimsAccounts;
    }

    public ClaimBehaviourData getNameTagUsageRules() {
        return NameTagUsageRules;
    }

    public boolean getNoPvPCombatinAdminClaims() {
        return config_pvp_noCombatInAdminLandClaims;
    }

    public ClaimBehaviourData getOtherExplosionBehaviour() {
        return OtherExplosionBehaviour;
    }

    public ClaimBehaviourData getOtherExplosionBlockDamageBehaviour() {
        return OtherExplosionBlockDamageBehaviour;
    }

    public ClaimBehaviourData getPlayerTrampleRules() {
        return PlayerTrampleRules;
    }

    public boolean getSpawnProtectEnabled() {
        return SpawnProtectEnabled;
    }

    public boolean getPvPBlockContainers() {
        return config_pvp_blockContainers;
    }

    public List<String> getPvPBlockedCommands() {
        return config_pvp_blockedCommands;
    }

    public int getPvPCombatTimeoutSeconds() {
        return config_pvp_combatTimeoutSeconds;
    }

    public boolean getPvPEnabled() {
        return config_pvp_enabled || GriefPrevention.instance.Configuration.getGlobalPVPEnabled();
    }

    public boolean getPvPNoCombatinPlayerClaims() {
        return config_pvp_noCombatInPlayerLandClaims;
    }

    public boolean getPvPPunishLogout() {
        return config_pvp_punishLogout;
    }

    public boolean getRegrowGriefedTrees() {
        return config_trees_regrowGriefedTrees;
    }

    public boolean getRemoveFloatingTreetops() {
        return config_trees_removeFloatingTreetops;
    }
    public int getTreeCleanupDelay(){
        return config_trees_cleanupDelay;
    }
    private int config_trees_cleanupDelay = (20*60)*10; //ten minutes.
    // private HashMap<String, Integer> config_seaLevelOverride; //override for
    // sea level, because bukkit doesn't report the right value for all
    // situations
    public Integer getSeaLevelOverride() {
        if (config_seaLevelOverride == -1)                                     {
            World w = Bukkit.createWorld(new WorldCreator(this.getWorldName()));
            if(w==null) {
                GriefPrevention.AddLogEntry("World:" + this.getWorldName() + " failed to load.");
            }
            return (config_seaLevelOverride = w.getSeaLevel());
        }
        else
            return config_seaLevelOverride;
    }

    /**
     * Returns whether Siege is Enabled for this world.
     *
     * @return
     */
    public boolean getSiegeEnabled() {
        return claims_Siege_Enabled || GriefPrevention.instance.Configuration.getGlobalSiegeEnabled();
    }

    public int getSiegeLootChests() {
        return config_pvp_Siege_Loot_Chests;
    }

    public ClaimBehaviourData getShearingRules() {
        return SheepShearingRules;
    }

    public ClaimBehaviourData getSheepDyeingRules() {
        return SheepDyeing;
    }

    public boolean getSiegeAutoTransfer() {
        return SiegeAutoTransfer;
    }

    public boolean getSiegeBlockRevert() {
        return SiegeBlockRevert;
    }

    public List<SiegeableData> getSiegeBlocks() {
        return config_siege_blocks;
    }

    public boolean isSiegeMaterial(Material b) {
        return SiegeableData.CheckList(config_siege_blocks, b);
    }

    public int getSiegeTamedAnimalDistance() {
        return Siege_TamedAnimalDistance;
    }

    public boolean getSignEavesdrop() {
        return config_sign_Eavesdrop;
    }

    public ClaimBehaviourData getSilverfishBreakRules() {
        return SilverfishBreakRules;
    }

    public boolean getSmartBan() {
        return config_smartBan && GriefPrevention.instance.Configuration.getGlobalSmartBan();
    }

    public ClaimBehaviourData getSnowGolemSpawnBehaviour() {
        return SnowGolemSpawnBehaviour;
    }

    public String getSpamAllowedIpAddresses() {
        return config_spam_allowedIpAddresses;
    }

    public int getSpamASCIIArtMinLength() {
        return config_SpamASCIIArtMinLength;
    }

    public String getSpamBanCommand() {
        return config_spam_bancommand;
    }

    public String getSpamBanMessage() {
        return config_spam_banMessage;
    }

    public int getSpamBanThreshold() {
        return config_SpamBanThreshold;
    }

    public int getSpamCapsMinLength() {
        return config_SpamCapsMinLength;
    }

    public int getSpamDeathMessageCooldownSeconds() {
        return config_spam_deathMessageCooldownSeconds;
    }

    public int getSpamDelayThreshold() {
        return config_SpamDelayThreshold;
    }

    public String getSpamKickCommand() {
        return config_spam_kickcommand;
    }

    public String getSpamKickMessage() {
        return config_spam_kickMessage;
    }

    public int getSpamKickThreshold() {
        return config_SpamKickThreshold;
    }

    public int getSpamLoginCooldownSeconds() {
        return config_spam_loginCooldownSeconds;
    }

    public List<String> getSpamMonitorSlashCommands() {
        return config_spam_monitorSlashCommands;
    }

    public int getSpamMuteThreshold() {
        return config_SpamMuteThreshold;
    }

    public int getSpamNonAlphaNumMinLength() {
        return config_SpamNonAlphaNumMinLength;
    }

    public boolean getSpamProtectionEnabled() {
        return GriefPrevention.instance.Configuration.getGlobalSpamEnabled() &&  config_spam_enabled;
    }

    public int getSpamShortMessageMaxLength() {
        return config_SpamShortMessageMaxLength;
    }

    public int getSpamShortMessageTimeout() {
        return config_SpamShortMessageTimeout;
    }

    public String getSpamWarningMessage() {
        return config_spam_warningMessage;
    }

    public ClaimBehaviourData getStoneButton() {
        return StoneButton;
    }

    public ClaimBehaviourData getStonePressurePlates() {
        return StonePressurePlates;
    }

    public ClaimBehaviourData getTamingRules() {
        return TamingRules;
    }

    public ClaimBehaviourData getTNTExplosionBehaviour() {
        return TNTExplosionsBehaviour;
    }

    public ClaimBehaviourData getTNTExplosionBlockDamageBehaviour() {
        return TNTExplosionBlockDamageBehaviour;
    }

    public ClaimBehaviourData getTrapDoors() {
        return TrapDoors;
    }

    /**
     * Rules that determine how TrashBlocks can be placed.
     *
     * @return
     */
    public ClaimBehaviourData getTrashBlockPlacementBehaviour() {
        return TrashBlockPlacementBehaviour;
    }

    /**
     * returns the List of Trash block materials for this world. These are
     * Materials that can be -placed in survival
     *
     * @return
     */
    public MaterialCollection getTrashBlocks() {
        return config_trash_blocks;
    }

    public boolean getTrashBlocksWithoutPermission() {
        return config_claims_TrashBlocksWithoutPermission;
    }

    public boolean getTreecleanupEnabled() {
        return config_claimcleanup_enabled;
    }

    public int getUnusedClaimExpirationDays() {
        return config_claims_unusedClaimExpirationDays;
    }

    public ClaimBehaviourData getVillagerTrades() {
        return VillagerTrades;
    }

    public ClaimBehaviourData getWaterBucketEmptyBehaviour() {
        return WaterBucketEmptyBehaviour;
    }

    public ClaimBehaviourData getWaterBucketFillBehaviour() {
        return WaterBucketFillBehaviour;
    }

    public ClaimBehaviourData getWitherEatBehaviour() {
        return WitherEatBehaviour;
    }

    public ClaimBehaviourData getWitherExplosionBehaviour() {
        return WitherExplosionBehaviour;
    }

    public ClaimBehaviourData getWitherExplosionBlockDamageBehaviour() {
        return WitherExplosionBlockDamageBehaviour;
    }

    public ClaimBehaviourData getWitherSpawnBehaviour() {
        return WitherSpawnBehaviour;
    }

    public ClaimBehaviourData getWoodenButton() {
        return WoodenButton;
    }

    public ClaimBehaviourData getWoodenDoors() {
        return WoodenDoors;
    }

    public ClaimBehaviourData getWoodPressurePlates() {
        return WoodPressurePlates;
    }

    public String getWorldName() {
        return WorldName;
    }

    public ClaimBehaviourData getZombieDoorBreaking() {
        return ZombieDoorBreaking;
    }

    /**
     * Creates a FileConfiguration based on a Parent set of Configuration Options and a child set of
     * Configuration Options.
     *
     * @param Parent Parent FileConfiguration.
     * @param Child  Child FileConfiguration.
     * @return Upmarked FileConfiguration that uses inherited values from the Parent where specified in the Child configuration,
     *         as well as defaulting to the Parent Configuration where the Child configuration doesn't specify.
     */
    public static FileConfiguration InheritTraits(FileConfiguration Parent, FileConfiguration Child) {
        return null;


    }

    private static void InheritSection(ConfigurationSection Parent, ConfigurationSection Child, ConfigurationSection Target) {

        //dupe values.


        Set<String> childsections = Child.getKeys(false);


    }


}
