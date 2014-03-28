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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.ryanhamshire.GriefPrevention.Configuration.*;
import me.ryanhamshire.GriefPrevention.Debugger.DebugLevel;
import me.ryanhamshire.GriefPrevention.CommandHandling.CommandHandler;
import me.ryanhamshire.GriefPrevention.events.GPLoadEvent;
import me.ryanhamshire.GriefPrevention.events.GPUnloadEvent;
import me.ryanhamshire.GriefPrevention.tasks.DeliverClaimBlocksTask;
import me.ryanhamshire.GriefPrevention.tasks.EntityCleanupTask;
import me.ryanhamshire.GriefPrevention.tasks.RestoreNatureProcessingTask;
import me.ryanhamshire.GriefPrevention.tasks.SendPlayerMessageTask;
import me.ryanhamshire.GriefPrevention.tasks.TreeCleanupTask;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Wolf;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

//import com.gmail.nossr50.mcMMO;

public class GriefPrevention extends JavaPlugin {
    public enum MinecraftVersions
    {
        MC125,
        MC13,
        MC14,
        MC15,
        MC16,
        MC17


    }
	private class RecursiveCopyResult {
		public int DirCount;
		public int FileCount;

		public RecursiveCopyResult(int pFileCount, int pDirCount) {
			FileCount = pFileCount;
			DirCount = pDirCount;
		}
	}

	private static final int ChunkSize = 4096;
	// start removal....
	// reference to the economy plugin, if economy integration is enabled
	public static Economy economy = null;
	private static boolean eventsRegistered = false;
	// for convenience, a reference to the instance of this plugin
	public static GriefPrevention instance;
	private static Logger log = Logger.getLogger("Minecraft");
	// how long to wait before deciding a player is staying online or staying
	// offline, for notication messages
	public static final int NOTIFICATION_SECONDS = 20;
	// to delete
	// the
	// PlayerData
	// if a
	// Players
	// last
	// claim is
	// cleaned
	// up.
	// how far away to search from a tree trunk for its branch blocks
	public static final int TREE_RADIUS = 5;

	// adds a server log entry
	public static void AddLogEntry(String entry) {
		if (instance == null)
			return;
		instance.getLogger().log(Level.INFO, entry);
	}

	/**
	 * Creates a friendly Location string for the given Location.
	 *
	 * @param location
	 *            Location to retrieve a string for.
	 * @return a formatted String to be shown to a user or for a log file
	 *         depicting the approximate given location.
	 */
	public static String getfriendlyLocationString(Location location) {
		if(location==null) return "null";
        return location.getWorld().getName() + "(" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ")";
	}

	// ensures a piece of the managed world is loaded into server memory
	// (generates the chunk if necessary)
	private static void GuaranteeChunkLoaded(Location location) {
		Chunk chunk = location.getChunk();
		while (!chunk.isLoaded() || !chunk.load(true))
			;
	}

	private static String removeColors(String source) {

		for (ChatColor cc : ChatColor.values()) {
			source = source.replace(cc.toString(), "");
		}
		return source;

	}

	public static void sendEavesDropMessage(Player Source, String logMessage) {
		Player[] players = GriefPrevention.instance.getServer().getOnlinePlayers();
		if (!Source.hasPermission(PermNodes.EavesDropPermission)) {
			for (int i = 0; i < players.length; i++) {
				Player player = players[i];
				if (player.hasPermission(PermNodes.EavesDropPermission) && !player.getName().equalsIgnoreCase(Source.getName())) {
					player.sendMessage(ChatColor.GRAY + logMessage);
				}
			}
		} else {
			for (int i = 0; i < players.length; i++) {
				Player player = players[i];
				if (player.hasPermission(PermNodes.AdminEavesDropPermission) && !player.getName().equalsIgnoreCase(Source.getName())) {
					player.sendMessage(ChatColor.GRAY + logMessage);
				}
			}
		}
	}

	// sends a color-coded message to a player
	public static void sendMessage(Player player, ChatColor color, Messages messageID, long delayInTicks, String... args) {

		String message = GriefPrevention.instance.dataStore.getMessage(messageID, args);
		if (message == null || message.equals(""))
			return;
		sendMessage(player, color, message, delayInTicks);
	}

	// sends a color-coded message to a player
	public static void sendMessage(Player player, ChatColor color, Messages messageID, String... args) {
		sendMessage(player, color, messageID, 0, args);
	}

	// sends a color-coded message to a player
	public static void sendMessage(Player player, ChatColor color, String message) {
		// if the message is zero-length, display nothing.
		if (message.length() == 0)
			return;
		if (player == null) {
			GriefPrevention.AddLogEntry(removeColors(message));
		} else {
			player.sendMessage(color + message);
		}
	}

	static void sendMessage(Player player, ChatColor color, String message, long delayInTicks) {
		SendPlayerMessageTask task = new SendPlayerMessageTask(player, color, message);
		if (delayInTicks > 0) {
			GriefPrevention.instance.getServer().getScheduler().runTaskLater(GriefPrevention.instance, task, delayInTicks);
		} else {
			task.run();
		}
	}

	public RegExTestHelper AccessRegexPattern;
	public DeliverClaimBlocksTask ClaimTask = null;
	public CommandHandler cmdHandler = null;
	// (over time). doesn't limit
	// purchased or admin-gifted
	// blocks
	public boolean config_autosubclaims; // auto subclaims, for right-clicking

	public double config_claimcleanup_deletePlayerDataWithNoClaims; // whether

	// with shovel in normal mode within
	// a claim. Kind of annoying so
	// defaults to false.
	public boolean config_claims_deleteclaimswithunrecognizedowners;
	// configuration variables, loaded/saved from a config.yml
	// public ArrayList<String> config_claims_enabledWorlds; //list of worlds
	// where players can create GriefPrevention claims
	// public ArrayList<String> config_claims_enabledCreativeWorlds; //list of
	// worlds where additional creative mode anti-grief rules apply
	public int config_claims_initialBlocks; // the number of claim blocks a new
											// player starts with
	public int config_claims_maxAccruedBlocks; // the limit on accrued blocks

	// private ArrayList<World> config_siege_enabledWorlds; //whether or not
	// /siege is enabled on this server
	// public static mcMMO MinecraftMMO = null;
	public double config_economy_claimBlocksPurchaseCost; // cost to purchase a
															// claim block. set
															// to zero to
															// disable purchase.
	public double config_economy_claimBlocksSellValue; // return on a sold claim
														// block. set to zero to
														// disable sale.

	public boolean config_mod_config_search;

	public boolean config_movementWatcher = false;

	public PlayerGroups config_player_groups = null;

	private String config_Storage_Kind; // currently supported:flat and mysql.

	
	
	public ConfigData Configuration = null;

	// this handles data storage, like player and region data
	public DataStore dataStore;

	// for logging to the console and log file
	public Debugger debug = null;

	public Debugger.DebugLevel DebuggingLevel;

	private ClaimMetaHandler MetaHandler = null;

	public RegExTestHelper ModdedBlockRegexHelper;

	public ModdedBlocksSearchResults ModdedBlocks = null;

	private MovementWatcher moveWatcher = null;

	public RegExTestHelper OreBlockRegexHelper;

	public WorldWatcher ww = new WorldWatcher();
    public String allowBreak(Player player,Location location){
        return allowBreak(player,location,true);
    }
	public String allowBreak(Player player, Location location,boolean ShowMessages) {
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		Claim claim = this.dataStore.getClaimAt(location, false);
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		// exception: administrators in ignore claims mode, and special player
		// accounts created by server mods
        ClaimBehaviourData cbd = null;
        if(null!=(cbd =wc.getBlockBreakOverrides().getBehaviourforBlock(location.getBlock()))){
            ClaimBehaviourData.ClaimAllowanceConstants cac;
            if((cac=cbd.Allowed(location,player,ShowMessages)).Allowed()){
                return null;
            }
            else if(cac== ClaimBehaviourData.ClaimAllowanceConstants.Deny_Forced){
                return "";
            }

        }

		if (playerData.ignoreClaims || wc.getModsIgnoreClaimsAccounts().contains(player.getName()))
			return null;

		// wilderness rules
		if (claim == null) {
			// no building in the wilderness in creative mode
			if (this.creativeRulesApply(location)) {
				String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims) + "  " + this.dataStore.getMessage(Messages.CreativeBasicsDemoAdvertisement);
				if (player.hasPermission(PermNodes.IgnoreClaimsPermission))
					reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
				return reason;
			}

		/*	else if (wc.getApplyTrashBlockRules() && wc.getClaimsEnabled()) {
				return this.dataStore.getMessage(Messages.NoBuildOutsideClaims) + "  " + this.dataStore.getMessage(Messages.SurvivalBasicsDemoAdvertisement);
			}*/

			// but it's fine in survival mode
			else {
				return null;
			}
		} else {



			// cache the claim for later reference
			playerData.lastClaim = claim;

			// if not in the wilderness, then apply claim rules (permissions,
			// etc)
			return claim.allowBreak(player, location.getBlock());
		}
	}

	public String allowBuild(Player player, Location location) {
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		Claim claim = this.dataStore.getClaimAt(location, false);
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		// exception: administrators in ignore claims mode and special player
		// accounts created by server mods
		if (playerData.ignoreClaims || wc.getModsIgnoreClaimsAccounts().contains(player.getName()))
			return null;
        ClaimBehaviourData cbd;
        if(null!=(cbd=wc.getBlockPlaceOverrides().getBehaviourforBlock(location.getBlock()))){
            if(cbd.Allowed(location,player,true).Denied()){
                return "";
            }
        }


		// wilderness rules
		if (claim == null) {
			// no building in the wilderness in creative mode
			if (this.creativeRulesApply(location)) {
				String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims) + "  " + this.dataStore.getMessage(Messages.CreativeBasicsDemoAdvertisement);
				if (player.hasPermission(PermNodes.IgnoreClaimsPermission))
					reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
				return reason;
			}

			// no building in survival wilderness when that is configured
			/*else if (wc.getApplyTrashBlockRules() && wc.getClaimsEnabled()) {
				if (wc.getTrashBlockPlacementBehaviour().Allowed(location, player).Denied())
					return this.dataStore.getMessage(Messages.NoBuildOutsideClaims) + "  " + this.dataStore.getMessage(Messages.SurvivalBasicsDemoAdvertisement);
				else
					return null;
			}*/

			else {
				// but it's fine in creative
				return null;
			}
		}

		// if not in the wilderness, then apply claim rules (permissions, etc)
		else {
			// cache the claim for later reference
			playerData.lastClaim = claim;
			return claim.allowBuild(player);
		}
	}

	public boolean checkPerm(Player p, World w, String Perm) {

		return p.hasPermission(Perm) || p.hasPermission(Perm + "." + w.getName());

	}

	// called when a player spawns, applies protection for that player if
	// necessary
	public void checkPvpProtectionNeeded(Player player) {
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		// if pvp is disabled, do nothing
		if (!player.getWorld().getPVP())
			return;

		// if player is in creative mode, do nothing
		if (player.getGameMode() == GameMode.CREATIVE)
			return;

		// if anti spawn camping feature is not enabled, do nothing
		if (!wc.getSpawnProtectEnabled())         {
            PlayerData playerData = this.dataStore.getPlayerData(player.getName());
            playerData.pvpImmune=false;
			return;
        }

		// if the player has the damage any player permission enabled, do
		// nothing
		if (player.hasPermission(PermNodes.NoPvPImmunityPermission))
			return;

		// check inventory for well, anything
		PlayerInventory inventory = player.getInventory();
		ItemStack[] armorStacks = inventory.getArmorContents();

		// check armor slots, stop if any items are found
		for (int i = 0; i < armorStacks.length; i++) {
			if (!(armorStacks[i] == null || armorStacks[i].getType() == Material.AIR))
				return;
		}

		// check other slots, stop if any items are found
		ItemStack[] generalStacks = inventory.getContents();
		for (int i = 0; i < generalStacks.length; i++) {
			if (!(generalStacks[i] == null || generalStacks[i].getType() == Material.AIR))
				return;
		}

		// otherwise, apply immunity
		final PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		playerData.pvpImmune = true;

		// inform the player
		GriefPrevention.sendMessage(player, TextMode.Success, Messages.PvPImmunityStart);
        final Player p = player;
        //set a timeout callback to reset PvP if a timeout is set.
        if(wc.getSpawnProtectTimeout()>0){
            Bukkit.getScheduler().runTaskLater(this,new Runnable(){
                public void run(){
                    playerData.pvpImmune=false;
                    GriefPrevention.sendMessage(p, TextMode.Success, Messages.PvPImmunityEnd);

                }
            },wc.getSpawnProtectTimeout());
        }

	}

	// checks whether players can create claims in a world
	public boolean claimsEnabledForWorld(World world) {
		return this.getWorldCfg(world).getClaimsEnabled();
	}

	// determines whether creative anti-grief rules apply at a location
	public boolean creativeRulesApply(Location location) {
		// return
		// this.config_claims_enabledCreativeWorlds.contains(location.getWorld().getName());
		return Configuration.getWorldConfig(location.getWorld()).getCreativeRules();
	}

	// helper method keeps the trust commands consistent and eliminates
	// duplicate code

	// moves a player from the claim he's in to a nearby wilderness location
	public Location ejectPlayer(Player player) {
		// look for a suitable location
		Location candidateLocation = player.getLocation();
		while (true) {
			Claim claim = null;
			claim = GriefPrevention.instance.dataStore.getClaimAt(candidateLocation, false);

			// if there's a claim here, keep looking
			if (claim != null) {
				candidateLocation = new Location(claim.lesserBoundaryCorner.getWorld(), claim.lesserBoundaryCorner.getBlockX() - 1, claim.lesserBoundaryCorner.getBlockY(), claim.lesserBoundaryCorner.getBlockZ() - 1);
				continue;
			}

			// otherwise find a safe place to teleport the player
			else {
				// find a safe height, a couple of blocks above the surface
				GuaranteeChunkLoaded(candidateLocation);
				Block highestBlock = candidateLocation.getWorld().getHighestBlockAt(candidateLocation.getBlockX(), candidateLocation.getBlockZ());
				Location destination = new Location(highestBlock.getWorld(), highestBlock.getX(), highestBlock.getY() + 2, highestBlock.getZ());
				player.teleport(destination);
				return destination;
			}
		}
	}

	/**
	 * helper method to get Breeding Item for a given entity.
	 *
	 * @param forEntity
	 *            Entity to retrieve.
	 * @return Appropriate material for specified entity, or null if none are
	 *         applicable.
	 */

	public Material[] getEntityBreedingItems(Entity forEntity) {

		if (forEntity instanceof Chicken) {
			return new Material[] { Material.SEEDS };
		} else if (forEntity instanceof Pig) {
			return new Material[] { Material.CARROT_ITEM };
		} else if (forEntity instanceof Sheep || forEntity instanceof Cow || forEntity instanceof MushroomCow) {
			return new Material[] { Material.WHEAT };
		} else if (forEntity instanceof Horse) {
			return new Material[] { Material.GOLDEN_APPLE };
		} else if (forEntity instanceof Wolf) {
			return new Material[] { Material.COOKED_BEEF, Material.RAW_BEEF, Material.RAW_CHICKEN, Material.COOKED_CHICKEN, Material.ROTTEN_FLESH };
		} else if (forEntity instanceof Ocelot) {
			return new Material[] { Material.RAW_FISH, Material.COOKED_FISH };
		}
		return null;
	}

	/**
	 * Retrieves the Claim Metadata handler. Unused by GP itself, this is useful
	 * for Plugins that which to create Claim-based data. A prime example is a
	 * plugin like GriefPreventionFlags, which adds Claim-based flag information
	 * to claims. Many plugins use their own special methods of indexing
	 * per-claim, so I thought it made sense to add a sort of "official" API to
	 * it, so that they are all consistent.
	 *
	 * @return ClaimMetaHandler object.
	 */
	public ClaimMetaHandler getMetaHandler() {
		return MetaHandler;
	}

	public Player getNearestPlayer(Location l) {

		Player currclosest = null;
		double currentmin = Double.MAX_VALUE;
		double grabdist = 0;
		for (Player p : l.getWorld().getPlayers()) {
			if ((grabdist = l.distanceSquared(p.getLocation())) < currentmin) {
				currentmin = grabdist;
				currclosest = p;
			}
		}

		return currclosest;

	}

	// helper for above, finds the "root" of a stack of logs
	// will return null if the stack is determined to not be a natural tree
	private Block getRootBlock(Block logBlock) {
		if (logBlock.getType() != Material.LOG)
			return null;

		// run down through log blocks until finding a non-log block
		Block underBlock = logBlock.getRelative(BlockFace.DOWN);
		while (underBlock.getType() == Material.LOG) {
			underBlock = underBlock.getRelative(BlockFace.DOWN);
		}

		// if this is a standard tree, that block MUST be dirt
		if (underBlock.getType() != Material.DIRT)
			return null;

		// run up through log blocks until finding a non-log block
		Block aboveBlock = logBlock.getRelative(BlockFace.UP);
		while (aboveBlock.getType() == Material.LOG) {
			aboveBlock = aboveBlock.getRelative(BlockFace.UP);
		}

		// if this is a standard tree, that block MUST be air or leaves
		if (aboveBlock.getType() != Material.AIR && aboveBlock.getType() != Material.LEAVES)
			return null;

		return underBlock.getRelative(BlockFace.UP);
	}

	public int getSeaLevel(World world) {
		int overrideValue = getWorldCfg(world).getSeaLevelOverride();

		/*
		 * if(overrideValue == null || overrideValue == -1) { return
		 * world.getSeaLevel(); } else { return overrideValue; }
		 */
		return overrideValue;
	}

	/**
	 * Retrieves a World Configuration given the World Name. If the World
	 * Configuration is not loaded, it will be loaded from the
	 * plugins/GriefPreventionData/WorldConfigs folder. If a file is not
	 * present, the template will be used and a new file will be created for the
	 * given name.
	 *
	 * @param worldname
	 *            Name of world to get configuration for.
	 * @return WorldConfig representing the configuration of the given world.
	 */
	public WorldConfig getWorldCfg(String worldname) {
		return Configuration.getWorldConfig(worldname);
	}

	/**
	 * Retrieves a World Configuration given the World. if the World
	 * Configuration is not loaded, it will be loaded from the
	 * plugins/GriefPreventionData/WorldConfigs folder. If a file is not present
	 * for the world, the template file will be used. The template file is
	 * configured in config.yml, and defaults to _template.cfg in the given
	 * folder. if no template is found, a default, empty configuration is
	 * created and returned.
	 *
	 * @param world
	 *            World to retrieve configuration for.
	 * @return WorldConfig representing the configuration of the given world.
	 *
	 */
	public WorldConfig getWorldCfg(World world) {
		return Configuration.getWorldConfig(world);
	}

	// processes broken log blocks to automatically remove floating treetops
	void handleLogBroken(Block block) {
		// find the lowest log in the tree trunk including this log
		Block rootBlock = this.getRootBlock(block);
        WorldConfig wc = GriefPrevention.instance.getWorldCfg(block.getWorld());
		// null indicates this block isn't part of a tree trunk
		if (rootBlock == null)
			return;

		// next step: scan for other log blocks and leaves in this tree

		// set boundaries for the scan
		int min_x = rootBlock.getX() - GriefPrevention.TREE_RADIUS;
		int max_x = rootBlock.getX() + GriefPrevention.TREE_RADIUS;
		int min_z = rootBlock.getZ() - GriefPrevention.TREE_RADIUS;
		int max_z = rootBlock.getZ() + GriefPrevention.TREE_RADIUS;
		int max_y = rootBlock.getWorld().getMaxHeight() - 1;

		// keep track of all the examined blocks, and all the log blocks found
		ArrayList<Block> examinedBlocks = new ArrayList<Block>();
		ArrayList<Block> treeBlocks = new ArrayList<Block>();

		// queue the first block, which is the block immediately above the
		// player-chopped block
		ConcurrentLinkedQueue<Block> blocksToExamine = new ConcurrentLinkedQueue<Block>();
		blocksToExamine.add(rootBlock);
		examinedBlocks.add(rootBlock);

		boolean hasLeaves = false;

		while (!blocksToExamine.isEmpty()) {
			// pop a block from the queue
			Block currentBlock = blocksToExamine.remove();

			// if this is a log block, determine whether it should be chopped
			if (currentBlock.getType() == Material.LOG) {
				boolean partOfTree = false;

				// if it's stacked with the original chopped block, the answer
				// is always yes
				if (currentBlock.getX() == block.getX() && currentBlock.getZ() == block.getZ()) {
					partOfTree = true;
				}

				// otherwise find the block underneath this stack of logs
				else {
					Block downBlock = currentBlock.getRelative(BlockFace.DOWN);
					while (downBlock.getType() == Material.LOG) {
						downBlock = downBlock.getRelative(BlockFace.DOWN);
					}

					// if it's air or leaves, it's okay to chop this block
					// this avoids accidentally chopping neighboring trees which
					// are close enough to touch their leaves to ours
					if (downBlock.getType() == Material.AIR || downBlock.getType() == Material.LEAVES) {
						partOfTree = true;
					}

					// otherwise this is a stack of logs which touches a solid
					// surface
					// if it's close to the original block's stack, don't clean
					// up this tree (just stop here)
					else {
						if (Math.abs(downBlock.getX() - block.getX()) <= 1 && Math.abs(downBlock.getZ() - block.getZ()) <= 1)
							return;
					}
				}

				if (partOfTree) {
					treeBlocks.add(currentBlock);
				}
			}

			// if this block is a log OR a leaf block, also check its neighbors
			if (currentBlock.getType() == Material.LOG || currentBlock.getType() == Material.LEAVES) {
				if (currentBlock.getType() == Material.LEAVES) {
					hasLeaves = true;
				}

				Block[] neighboringBlocks = new Block[] { currentBlock.getRelative(BlockFace.EAST), currentBlock.getRelative(BlockFace.WEST), currentBlock.getRelative(BlockFace.NORTH), currentBlock.getRelative(BlockFace.SOUTH), currentBlock.getRelative(BlockFace.UP), currentBlock.getRelative(BlockFace.DOWN) };

				for (int i = 0; i < neighboringBlocks.length; i++) {
					Block neighboringBlock = neighboringBlocks[i];

					// if the neighboringBlock is out of bounds, skip it
					if (neighboringBlock.getX() < min_x || neighboringBlock.getX() > max_x || neighboringBlock.getZ() < min_z || neighboringBlock.getZ() > max_z || neighboringBlock.getY() > max_y)
						continue;

					// if we already saw this block, skip it
					if (examinedBlocks.contains(neighboringBlock))
						continue;

					// mark the block as examined
					examinedBlocks.add(neighboringBlock);

					// if the neighboringBlock is a leaf or log, put it in the
					// queue to be examined later
					if (neighboringBlock.getType() == Material.LOG || neighboringBlock.getType() == Material.LEAVES) {
						blocksToExamine.add(neighboringBlock);
					}

					// if we encounter any player-placed block type, bail out
					// (don't automatically remove parts of this tree, it might
					// support a treehouse!)
					else if (this.isPlayerBlock(neighboringBlock)) {
						return;
					}
				}
			}
		}

		// if it doesn't have leaves, it's not a tree, so don't clean it up
		if (hasLeaves) {
			// schedule a cleanup task for later, in case the player leaves part
			// of this tree hanging in the air
			TreeCleanupTask cleanupTask = new TreeCleanupTask(block, rootBlock, treeBlocks, rootBlock.getData());


			GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, cleanupTask, wc.getTreeCleanupDelay());
		}
	}

	/**
	 * This method should be called by any Plugin that wishes to track Claim
	 * Entry and Exit Events. It enables the Movement Watcher Listener. This is
	 * disabled by default. It can also be force-enabled in the configuration
	 * file. However plugins should call this method to ensure that the
	 * MovementWatcher is active. Calling this method multiple times will not
	 * cause multiple movementwatchers to be created.
	 */
	public void InitializeMovementWatcher() {

		if (this.moveWatcher != null)
			return;
		moveWatcher = new MovementWatcher();
		Bukkit.getPluginManager().registerEvents(moveWatcher, this);

	}

	public boolean isHorse(Entity entitytest) {
		try{
			return entitytest instanceof Horse;
		}catch(NoClassDefFoundError err){
			return false;
		}

	}

	// for sake of identifying trees ONLY, a cheap but not 100% reliable method
	// for identifying player-placed blocks
	private boolean isPlayerBlock(Block block) {
		Material material = block.getType();

		// list of natural blocks which are OK to have next to a log block in a
		// natural tree setting
		if (material == Material.AIR || material == Material.LEAVES || material == Material.LOG || material == Material.DIRT || material == Material.GRASS || material == Material.STATIONARY_WATER || material == Material.BROWN_MUSHROOM || material == Material.RED_MUSHROOM || material == Material.RED_ROSE || material == Material.LONG_GRASS || material == Material.SNOW || material == Material.STONE || material == Material.VINE || material == Material.WATER_LILY || material == Material.YELLOW_FLOWER || material == Material.CLAY) {
			return false;
		} else {
			return true;
		}
	}

	private void migrateData() {

		// Migrates data from 7.7 GriefPreventionData folder to GriefPrevention
		// folder.

	}

	// handles slash commands
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		return cmdHandler.onCommand(sender, cmd, commandLabel, args);
	}

	@Override
	public void onDisable() {
		// emulate each world unloading.

		// cancel ALL pending tasks.
		Bukkit.getScheduler().cancelTasks(this);
        ClaimTask = null;
		GriefPrevention.AddLogEntry("GriefPrevention is being Disabled.");
		if(dataStore!=null) this.dataStore.saveClaimData();

		GPUnloadEvent uevent = new GPUnloadEvent(this);
		Bukkit.getPluginManager().callEvent(uevent);
		// save data for any online players
        if (this.dataStore != null)
        {
            Player[] players = this.getServer().getOnlinePlayers();
            for (int i = 0; i < players.length; i++) {
                Player player = players[i];
                String playerName = player.getName();
                PlayerData playerData = this.dataStore.getPlayerData(playerName);
                Debugger.Write("Saving Player Data for Player:" + playerName,DebugLevel.Verbose);
                this.dataStore.savePlayerData(playerName, playerData);
            }
            if(ww!=null){
              for (World iterate : Bukkit.getWorlds()) {
                  Debugger.Write("Unloading World:" + iterate.getName(),DebugLevel.Verbose);
                  ww.WorldUnload(new WorldUnloadEvent(iterate));
              }
            }

            this.dataStore.close();
            dataStore=null;
        }
        else
            GriefPrevention.AddLogEntry("ERROR: DataStore is not configured correctly, no data is being saved. Please fix your config!");




        ww.clear();
        dataStore = null;
        ClaimTask=null;
        this.cmdHandler = null;
        AddLogEntry("GriefPrevention disabled.");
	}

	// initializes well... everything
	@Override
	public void onEnable() {
		instance = this;
		AddLogEntry("Grief Prevention enabled.");
        AddLogEntry("Grief Prevention Running for " + getMinecraftVersionString());

		// if the old data folder exists and the new one doesn't...
		File oldData = new File(DataStore.oldDataLayerFolderPath);
		File newData = new File(DataStore.dataLayerFolderPath);
		if (oldData.exists() && !newData.exists()) {
			// migrateData();
			AddLogEntry("Found old GriefPrevention 7.7 or Earlier Data, but no 7.8 or later data. Attempting to copy to new folder.");
			AddLogEntry("This will Copy your GriefPrevention 7.7 Data to the new GriefPrevention 7.8 and Later location.");
			AddLogEntry("You will need to reconfigure your settings using the new World-based Configuration.");
			try {
				RecursiveCopyResult copied = recursiveCopy(oldData, newData, true);
				AddLogEntry("Migration complete. Copied " + copied.FileCount + " Files in " + copied.DirCount + " Directories.");
			} catch (IOException exx) {
				AddLogEntry("Exception occured attempting to copy config data.");
				exx.printStackTrace();
			}
		}

		// MinecraftMMO = (mcMMO) Bukkit.getPluginManager().getPlugin("mcMMO");
		Debugger.Write(new File(DataStore.configFilePath).getAbsolutePath(), DebugLevel.Verbose);
		Debugger.Write("File Exists:" + new File(DataStore.configFilePath).exists(), DebugLevel.Verbose);
		// load the config if it exists
		FileConfiguration config = YamlConfiguration.loadConfiguration(new File(DataStore.configFilePath));
		FileConfiguration outConfig = new YamlConfiguration();

		// read configuration settings (note defaults)

		// load player groups.
		// System.out.println("reading player groups...");
		this.config_player_groups = new PlayerGroups(config, "GriefPrevention.Groups");
		this.config_player_groups.Save(outConfig, "GriefPrevention.Groups");
		// optional database settings
		this.config_Storage_Kind = config.getString("GriefPrevention.DataStore", "flat");
		outConfig.set("GriefPrevention.DataStore", config_Storage_Kind);
		String usestoragedata = DataStore.dataLayerFolderPath + File.separator + "dataconfig.yml";

		GriefPrevention.AddLogEntry("Reading dataconfiguration from " + usestoragedata);

		File storagedatafile = new File(usestoragedata);

		// read it in for the init of the datastore later.
		YamlConfiguration DataStoreRead = null;
		if (storagedatafile.exists())
			DataStoreRead = YamlConfiguration.loadConfiguration(storagedatafile);
		else
			DataStoreRead = new YamlConfiguration();

		ConfigurationSection DataSettings = DataStoreRead.getConfigurationSection(config_Storage_Kind);

		if (DataSettings == null) {
			DataSettings = DataStoreRead.createSection(config_Storage_Kind);
		}
		YamlConfiguration DataStoreWrite = DataStoreRead;

		// sea level
		String AcquiredLevel = config.getString("GriefPrevention.DebugLevel", "None");
		this.DebuggingLevel = Debugger.DebugLevel.valueOf(AcquiredLevel);
		outConfig.set("GriefPrevention.DebugLevel", DebuggingLevel.name());
		this.debug = new Debugger(DebuggingLevel);

		this.config_economy_claimBlocksPurchaseCost = config.getDouble("GriefPrevention.Economy.ClaimBlocksPurchaseCost", 0);
		this.config_economy_claimBlocksSellValue = config.getDouble("GriefPrevention.Economy.ClaimBlocksSellValue", 0);
		this.config_claims_maxAccruedBlocks = config.getInt("GriefPrevention.Claims.MaxAccruedBlocks", 5000);
		outConfig.set("GriefPrevention.Claims.MaxAccruedBlocks", config_claims_maxAccruedBlocks);

		this.ModdedBlockRegexHelper = new RegExTestHelper(config, outConfig, "GriefPrevention.Mods.Containers", RegExTestHelper.DefaultContainers);
		this.AccessRegexPattern = new RegExTestHelper(config, outConfig, "GriefPrevention.Mods.Access", RegExTestHelper.DefaultAccess);
		this.OreBlockRegexHelper = new RegExTestHelper(config, outConfig, "GriefPrevention.Mods.Trash", RegExTestHelper.DefaultTrash);

		this.config_claims_initialBlocks = config.getInt("GriefPrevention.Claims.InitialBlocks", 100);
		this.config_mod_config_search = config.getBoolean("GriefPrevention.Mods.PerformConfigSearch", true);
		this.config_claims_deleteclaimswithunrecognizedowners = config.getBoolean("GriefPrevention.Claims.DeleteWithUnrecognizedOwner", false);
		this.config_autosubclaims = config.getBoolean("GriefPrevention.Claims.AutoSubClaimSwitch", false);
		outConfig.set("GriefPrevention.Claims.AutoSubClaimsSwitch", this.config_autosubclaims);
		outConfig.set("GriefPrevention.Claims.DeleteWithUnrecognizedOwner", this.config_claims_deleteclaimswithunrecognizedowners);
		outConfig.set("GriefPrevention.Economy.ClaimBlocksPurchaseCost", this.config_economy_claimBlocksPurchaseCost);
		outConfig.set("GriefPrevention.Economy.ClaimBlocksSellValue", this.config_economy_claimBlocksSellValue);
		outConfig.set("GriefPrevention.Claims.InitialBlocks", config_claims_initialBlocks);
		outConfig.set("GriefPrevention.Mods.PerformConfigSearch", false);
		this.ModdedBlocks = new ModdedBlocksSearchResults();
		if (config_mod_config_search) {
			// Show message indicating what will happen. With mod_config_search
			// enabled, we will search for configs and find
			// IDs according to a regular expression, but for this entire
			// session all World Configuration loads will
			// change the configured values for modded containers and access
			// blocks, so mention that.
			AddLogEntry("Performing Configuration Search.");
			AddLogEntry("World Configurations Loaded during this session will have their current Container and Access IDs Overwritten!");

			this.ModdedBlocks = ModBlockHelper.ScanCfgs();

			// save these to _template.

		}
		this.config_movementWatcher = config.getBoolean("GriefPrevention.EnableMoveWatcher", false);
		outConfig.set("GriefPrevention.EnableMoveWatcher", config_movementWatcher);
		Configuration = new ConfigData(config, outConfig);

		if (config_mod_config_search) {
		    // if specified, to search, save the results to the template file.
		    // WorldConfig's will save the ModdedBlock Contents when they are created,
			// therefore we will set the template in this manner. Otherwise,
			// this setting (ModdedBlock search results) will only be valid for this one server session.
			// we don't actually need to do anything with the templateFile variable, all the
			// work was done in fromFile() and the WorldConfig constructors.
            WorldConfig templatefile = WorldConfig.fromFile(Configuration.getTemplateFile());
		}

		// when datastore initializes, it loads player and claim data, and posts
		// some stats to the log

		if (this.config_Storage_Kind.equalsIgnoreCase("mysql")) {
			DatabaseDataStore databaseStore = null;
			try {
				try {
					databaseStore = new DatabaseDataStore(DataSettings, DataSettings);

				} catch (Exception exx) {
					exx.printStackTrace();
					try {
						DataStoreWrite.save(usestoragedata);
					} catch (Exception except) {
						exx.printStackTrace();
					}
				}
                boolean allowmigrate = Configuration.getAllowAutomaticMigration();
				if (FlatFileDataStore.hasData() && databaseStore != null && allowmigrate) {
					GriefPrevention.AddLogEntry("There appears to be some data on the hard drive.  Migrating that data to the database...");
					FlatFileDataStore flatFileStore = new FlatFileDataStore();
					flatFileStore.migrateData(databaseStore);
					GriefPrevention.AddLogEntry("Data migration process complete.  Reloading data from the database...");
					databaseStore.close();
					databaseStore = new DatabaseDataStore(DataSettings, DataSettings);
				}
                else if(!allowmigrate){
                    GriefPrevention.AddLogEntry("Flat File data detected. This data will NOT be migrated, because GriefPrevention.AllowAutomaticMigration is set to false.");
                }

				this.dataStore = databaseStore;
			} catch (Exception e) {
				GriefPrevention.AddLogEntry("Because there was a problem with the database, GriefPrevention will not function properly.  Either update the database config settings resolve the issue, or delete those lines from your config.yml so that GriefPrevention can use the file system to store data.");
				return;
			}
		}

		else if (this.config_Storage_Kind.equalsIgnoreCase("flat")) {
			try {
				this.dataStore = new FlatFileDataStore();
			} catch (Exception e) {
				GriefPrevention.AddLogEntry("Unable to initialize the file system data store.  Details:");
				GriefPrevention.AddLogEntry(e.getMessage());
			}
		}
        //start the command handler.
        cmdHandler = new CommandHandler();
		// start the recurring cleanup event for entities in creative worlds, if enabled.

		// start recurring cleanup scan for unused claims belonging to inactive players
		// if the option is enabled. look through all world configurations.
		boolean claimcleanupOn = false;
		boolean entitycleanupEnabled = false;
		try {
			DataStoreWrite.save(usestoragedata);
		} catch (Exception exx) {
			exx.printStackTrace();
		}
		if (config_movementWatcher) {
			this.moveWatcher = new MovementWatcher();
			Bukkit.getPluginManager().registerEvents(moveWatcher, this);
		}

		if (entitycleanupEnabled) {
			EntityCleanupTask task = new EntityCleanupTask(0);
			this.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 20L);
		}

		// register for events
		if (!eventsRegistered) {
			eventsRegistered = true;
			PluginManager pluginManager = this.getServer().getPluginManager();

			// player events
			PlayerEventHandler playerEventHandler = new PlayerEventHandler();
			pluginManager.registerEvents(playerEventHandler, this);

			// block events
			BlockEventHandler blockEventHandler = new BlockEventHandler();
			pluginManager.registerEvents(blockEventHandler, this);

			// entity events
			EntityEventHandler entityEventHandler = new EntityEventHandler();
			pluginManager.registerEvents(entityEventHandler, this);

            Bukkit.getPluginManager().registerEvents(ww, this);
		}

		// if economy is enabled
		if (this.config_economy_claimBlocksPurchaseCost > 0 || this.config_economy_claimBlocksSellValue > 0) {
			// try to load Vault
			GriefPrevention.AddLogEntry("GriefPrevention requires Vault for economy integration.");
			GriefPrevention.AddLogEntry("Attempting to load Vault...");
			RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
			GriefPrevention.AddLogEntry("Vault loaded successfully!");

			// ask Vault to hook into an economy plugin
			GriefPrevention.AddLogEntry("Looking for a Vault-compatible economy plugin...");
			if (economyProvider != null) {
				GriefPrevention.economy = economyProvider.getProvider();

				// on success, display success message
				if (GriefPrevention.economy != null) {
					GriefPrevention.AddLogEntry("Hooked into economy: " + GriefPrevention.economy.getName() + ".");
					GriefPrevention.AddLogEntry("Ready to buy/sell claim blocks!");
				}

				// otherwise error message
				else {
					GriefPrevention.AddLogEntry("ERROR: Vault was unable to find a supported economy plugin.  Either install a Vault-compatible economy plugin, or set both of the economy config variables to zero.");
				}
			}

			// another error case
			else {
				GriefPrevention.AddLogEntry("ERROR: Vault was unable to find a supported economy plugin.  Either install a Vault-compatible economy plugin, or set both of the economy config variables to zero.");
			}
		}
		MetaHandler = new ClaimMetaHandler();
		try {
			// new File(DataStore.configFilePath).delete();
			outConfig.save(new File(DataStore.configFilePath));
		} catch (IOException exx) {
			GriefPrevention.log.log(Level.SEVERE, "Failed to save primary configuration file:" + DataStore.configFilePath);
		}

		// go through all available worlds, and fire a "world load" event for
		// them.
        ww.Refresh();


		Bukkit.getPluginManager().callEvent(new GPLoadEvent(this));

	}

	public void parseMaterialListFromConfig(List<String> stringsToParse, MaterialCollection materialCollection) {
		materialCollection.clear();
        Debugger.Write("parseMaterialListFromConfig:" + String.valueOf(stringsToParse.size()) + " Items.",DebugLevel.Verbose);
        if(stringsToParse==null || stringsToParse.size()==0) return;


		// for each string in the list
		for (int i = 0; i < stringsToParse.size(); i++) {
			// try to parse the string value into a material info
			MaterialInfo materialInfo = MaterialInfo.fromString(stringsToParse.get(i));

			// null value returned indicates an error parsing the string from
			// the config file
			if (materialInfo == null) {
				// ignore and remove null entries
				if(stringsToParse.get(i) == null){
				    stringsToParse.remove(i);
				    i -= 1;
				    continue;
				}
				
				// show error in log
				GriefPrevention.AddLogEntry("ERROR: Unable to read a material entry from the config file.  Please update your config.yml.");

				// update string, which will go out to config file to help user
				// find the error entry
				if (!stringsToParse.get(i).contains("can't")) {
					stringsToParse.set(i, stringsToParse.get(i) + "     <-- can't understand this entry, see BukkitDev documentation");
				}
			}

			// otherwise store the valid entry in config data
			// but only if not currently in the list.
			else {
				materialCollection.add(materialInfo);
			}
		}
        Debugger.Write("parsed material collection contains " + materialCollection + " Elements.",DebugLevel.Verbose);
	}

	private RecursiveCopyResult recursiveCopy(File pSource, File pDest, boolean ShowMessages) throws IOException {

		int CountFiles = 0;
		int CountDirs = 0;
		if (pSource.isDirectory()) {
			// A simple validation, if the destination is not exist then create
			// it
			if (!pDest.exists()) {
				if (ShowMessages)
					System.out.println("Creating Target:" + pDest.getPath());
				pDest.mkdirs();
				CountDirs++;
			}

			// Create list of files and directories on the current source
			// Note: with the recursion 'fSource' changed accordingly
			String[] copyList = pSource.list();

			for (int index = 0; index < copyList.length; index++) {
				File dest = new File(pDest, copyList[index]);
				File source = new File(pSource, copyList[index]);

				// Recursion call take place here
				RecursiveCopyResult recursiveresult = recursiveCopy(source, dest, ShowMessages);
				CountDirs += recursiveresult.DirCount;
				CountFiles += recursiveresult.FileCount;
			}
		} else {
			// Copy the file into the already created destination.
			FileInputStream fInStream = null;
			FileOutputStream fOutStream = null;
			try {
				fInStream = new FileInputStream(pSource);
				fOutStream = new FileOutputStream(pDest);

				// Read 2K at a time from the file
				byte[] buffer = new byte[ChunkSize];
				int iBytesReads;

				// In each successful read, write back to the source
				while ((iBytesReads = fInStream.read(buffer)) >= 0) {
					fOutStream.write(buffer, 0, iBytesReads);
				}
				CountFiles++;
			} finally {
				// Safe exit
				if (fInStream != null) {
					fInStream.close();
				}

				if (fOutStream != null) {
					fOutStream.close();
				}
			}

		}

		return new RecursiveCopyResult(CountFiles, CountDirs);
	}

	public void reloadConfiguration() {
		// TODO Auto-generated method stub
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gpreload");
	}

	// helper method to resolve a player by name
	public OfflinePlayer resolvePlayer(String name) {
        name = name.toLowerCase();
		// try online players first
		Player player = this.getServer().getPlayer(name);
		if (player != null)
			return player;

		// then search offline players
		OfflinePlayer[] offlinePlayers = this.getServer().getOfflinePlayers();
		for (int i = 0; i < offlinePlayers.length; i++) {
			if (offlinePlayers[i].getName().equalsIgnoreCase(name)) {
				return offlinePlayers[i];
			}
		}

		// if none found, return null
		return null;
	}

	public void restoreChunk(Chunk chunk, int miny, boolean aggressiveMode, long delayInTicks, Player playerReceivingVisualization) {
		// build a snapshot of this chunk, including 1 block boundary outside of
		// the chunk all the way around
		int maxHeight = chunk.getWorld().getMaxHeight();
		BlockSnapshot[][][] snapshots = new BlockSnapshot[18][maxHeight][18];
		Block startBlock = chunk.getBlock(0, 0, 0);
		Location startLocation = new Location(chunk.getWorld(), startBlock.getX() - 1, 0, startBlock.getZ() - 1);
		for (int x = 0; x < snapshots.length; x++) {
			for (int z = 0; z < snapshots[0][0].length; z++) {
				for (int y = 0; y < snapshots[0].length; y++) {
					Block block = chunk.getWorld().getBlockAt(startLocation.getBlockX() + x, startLocation.getBlockY() + y, startLocation.getBlockZ() + z);
					snapshots[x][y][z] = new BlockSnapshot(block.getLocation(), block.getTypeId(), block.getData());
				}
			}
		}

		// create task to process those data in another thread
		Location lesserBoundaryCorner = chunk.getBlock(0, 0, 0).getLocation();
		Location greaterBoundaryCorner = chunk.getBlock(15, 0, 15).getLocation();

		// create task
		// when done processing, this task will create a main thread task to
		// actually update the world with processing results
		RestoreNatureProcessingTask task = new RestoreNatureProcessingTask(snapshots, miny, chunk.getWorld().getEnvironment(), lesserBoundaryCorner.getBlock().getBiome(), lesserBoundaryCorner, greaterBoundaryCorner, this.getSeaLevel(chunk.getWorld()), aggressiveMode, GriefPrevention.instance.creativeRulesApply(lesserBoundaryCorner), playerReceivingVisualization);
		GriefPrevention.instance.getServer().getScheduler().runTaskLaterAsynchronously(GriefPrevention.instance, task, delayInTicks);
	}

	// restores nature in multiple chunks, as described by a claim instance
	// this restores all chunks which have ANY number of claim blocks from this
	// claim in them
	// if the claim is still active (in the data store), then the claimed blocks
	// will not be changed (only the area bordering the claim)
	public void restoreClaim(Claim claim, long delayInTicks) {
		// admin claims aren't automatically cleaned up when deleted or
		// abandoned
		if (claim.isAdminClaim())
			return;

		// it's too expensive to do this for huge claims
		if (claim.getArea() > 10000)
			return;

		Chunk lesserChunk = claim.getLesserBoundaryCorner().getChunk();
		Chunk greaterChunk = claim.getGreaterBoundaryCorner().getChunk();

		for (int x = lesserChunk.getX(); x <= greaterChunk.getX(); x++)
			for (int z = lesserChunk.getZ(); z <= greaterChunk.getZ(); z++) {
				Chunk chunk = lesserChunk.getWorld().getChunkAt(x, z);
				this.restoreChunk(chunk, this.getSeaLevel(chunk.getWorld()) - 15, false, delayInTicks, null);
			}
	}

	// checks whether players siege in a world
	public boolean siegeEnabledForWorld(World world) {
		return this.getWorldCfg(world).getSiegeEnabled();
	}
    public static String getMinecraftVersionString(){
        switch (getMCVersion()) {
            case MC125:
                return "Minecraft 1.2.x";
            case MC13:
                return "Minecraft 1.3.x";
            case MC14:
                return "Minecraft 1.4.x";

            case MC15:
                return "Minecraft 1.5.x";

            case MC16:
                return "Minecraft 1.6.x";

            case MC17:
                return "Minecraft 1.7.x";

            default:
                return "Unknown Version";
        }

    }
    public static MinecraftVersions getMCVersion(){
        //go down the list.
        MinecraftVersions[] testversions = MinecraftVersions.values();
        //start from the last element and move towards the first...
        //the first one to not fail is the running version.
        for(int i=testversions.length-1;i>0;i--){
            if(isMCVersionorLater(testversions[i])) return testversions[i];
        }

        return null; //let's hope THIS doesn't happen...
    }
    //static version helpers.
    public static boolean isMCVersionorLater(MinecraftVersions VersionEnum){
        //each version adds material fields, so we can inspect the Material
        //enum using reflection.
        String findMaterial=null;
        switch (VersionEnum) {

            case MC125:
                //redstone lamp was added in 1.2.5.
                findMaterial="REDSTONE_LAMP_ON";
                break;
            case MC13: //command blocks are new in 1.3.
                findMaterial = "COMMAND";
                break;
            case MC14:
                findMaterial = "ENCHANTED_BOOK"; //enchanted books added in 1.4
                break;
            case MC15:
                findMaterial="REDSTONE_BLOCK";  //redstone blocks added in 1.5
                break;
            case MC16:
                findMaterial="STAINED_CLAY";  //stained clay added in 1.6.
                break;
            case MC17:
                findMaterial="STAINED_GLASS"; //stained glass is new in 1.7.
                break;
        }
        //if we catch a FieldNotFoundException, than we are NOT that version or later.
        try {

             Field acquired = Material.class.getField(findMaterial);
            return true; //no exception, so the field exists. we are that version or later.
        }
        catch(NoSuchFieldException fnf){
            //field not found- we are not that version or later.
            return false;
        }


    }
}
