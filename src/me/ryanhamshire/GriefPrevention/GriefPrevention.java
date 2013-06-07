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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.ryanhamshire.GriefPrevention.Configuration.ClaimMetaHandler;
import me.ryanhamshire.GriefPrevention.Configuration.ConfigData;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.tasks.CleanupUnusedClaimsTask;
import me.ryanhamshire.GriefPrevention.tasks.DeliverClaimBlocksTask;
import me.ryanhamshire.GriefPrevention.tasks.EntityCleanupTask;
import me.ryanhamshire.GriefPrevention.tasks.PlayerRescueTask;
import me.ryanhamshire.GriefPrevention.tasks.RestoreNatureProcessingTask;
import me.ryanhamshire.GriefPrevention.tasks.SendPlayerMessageTask;
import me.ryanhamshire.GriefPrevention.tasks.TreeCleanupTask;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
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
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

//import com.gmail.nossr50.mcMMO;

public class GriefPrevention extends JavaPlugin
{
	//for convenience, a reference to the instance of this plugin
	public static GriefPrevention instance;
	
	//for logging to the console and log file
	private static Logger log = Logger.getLogger("Minecraft");
	public ConfigData Configuration = null;
	//this handles data storage, like player and region data
	public DataStore dataStore;
	public PlayerGroups config_player_groups = null;
	//configuration variables, loaded/saved from a config.yml
	//public ArrayList<String> config_claims_enabledWorlds;			//list of worlds where players can create GriefPrevention claims
	//public ArrayList<String> config_claims_enabledCreativeWorlds;	//list of worlds where additional creative mode anti-grief rules apply
	public int config_claims_initialBlocks;							//the number of claim blocks a new player starts with
	public int config_claims_maxAccruedBlocks;						//the limit on accrued blocks (over time).  doesn't limit purchased or admin-gifted blocks
	//start removal....
	//reference to the economy plugin, if economy integration is enabled
	public static Economy economy = null;	
	//private ArrayList<World> config_siege_enabledWorlds;				//whether or not /siege is enabled on this server
	//public static mcMMO MinecraftMMO = null; 
	private double config_economy_claimBlocksPurchaseCost;			//cost to purchase a claim block.  set to zero to disable purchase.
	
	private double config_economy_claimBlocksSellValue;				//return on a sold claim block.  set to zero to disable sale.
	
	//how far away to search from a tree trunk for its branch blocks
	public static final int TREE_RADIUS = 5;
	
	//how long to wait before deciding a player is staying online or staying offline, for notication messages
	public static final int NOTIFICATION_SECONDS = 20;
	
	//adds a server log entry
	public static void AddLogEntry(String entry)
	{
		log.info("GriefPrevention: " + entry);
	}
	/**
	 * Retrieves a World Configuration given the World. if the World Configuration is not loaded,
	 * it will be loaded from the plugins/GriefPreventionData/WorldConfigs folder. If a file is not present for the world,
	 * the template file will be used. The template file is configured in config.yml, and defaults to _template.cfg in the given folder.
	 * if no template is found, a default, empty configuration is created and returned.
	 * @param world World to retrieve configuration for.
	 * @return WorldConfig representing the configuration of the given world.
	 * @see getWorldCfg
	 */
	public WorldConfig getWorldCfg(World world){
		return Configuration.getWorldConfig(world);
	}
	/**
	 * Retrieves a World Configuration given the World Name. If the World Configuration is not loaded, it will be loaded
	 * from the plugins/GriefPreventionData/WorldConfigs folder. If a file is not present, the template will be used and a new file will be created for
	 * the given name.
	 * @param worldname Name of world to get configuration for.
	 * @return WorldConfig representing the configuration of the given world.
	 */
	public WorldConfig getWorldCfg(String worldname){
		return Configuration.getWorldConfig(worldname);
	}
	private ClaimMetaHandler MetaHandler = null;
	/**
	 * Retrieves the Claim Metadata handler. Unused by GP itself, this is useful for 
	 * Plugins that which to create Claim-based data. A prime example is a plugin like GriefPreventionFlags, which
	 * adds Claim-based flag information to claims. Many plugins use their own special methods of indexing per-claim,
	 * so I thought it made sense to add a sort of "official" API to it, so that they are all consistent.
	 * @return ClaimMetaHandler object.
	 */
	public ClaimMetaHandler getMetaHandler(){
		return MetaHandler;
	}
	private static boolean eventsRegistered = false;
	//initializes well...   everything
	public void onEnable()
	{ 		
		AddLogEntry("Grief Prevention enabled.");
		
		instance = this;
		//MinecraftMMO = (mcMMO) Bukkit.getPluginManager().getPlugin("mcMMO");
		GriefPrevention.AddLogEntry(new File(DataStore.configFilePath).getAbsolutePath());
		GriefPrevention.AddLogEntry("File Exists:" + new File(DataStore.configFilePath).exists());
		//load the config if it exists
		FileConfiguration config = YamlConfiguration.loadConfiguration(new File(DataStore.configFilePath));
		FileConfiguration outConfig = new YamlConfiguration();
		Configuration = new ConfigData(config,outConfig);
		//read configuration settings (note defaults)
		
		
		
		//load player groups.
		//System.out.println("reading player groups...");
		this.config_player_groups = new PlayerGroups(config,"GriefPrevention.Groups");
		this.config_player_groups.Save(outConfig, "GriefPrevention.Groups");
		//optional database settings
		String databaseUrl = config.getString("GriefPrevention.Database.URL", "");
		String databaseUserName = config.getString("GriefPrevention.Database.UserName", "");
		String databasePassword = config.getString("GriefPrevention.Database.Password", "");
		//sea level
		
		
		outConfig.set("GriefPrevention.Database.URL", databaseUrl);
		outConfig.set("GriefPrevention.Database.UserName",databaseUserName);
		outConfig.set("GriefPrevention.Database.Password",databasePassword);
		
		this.config_economy_claimBlocksPurchaseCost = config.getDouble("GriefPrevention.Economy.ClaimBlocksPurchaseCost", 0);
		this.config_economy_claimBlocksSellValue = config.getDouble("GriefPrevention.Economy.ClaimBlocksSellValue", 0);
		this.config_claims_maxAccruedBlocks = config.getInt("GriefPrevention.Claims.MaxAccruedBlocks",5000);
		outConfig.set("GriefPrevention.Claims.MaxAccruedBlocks", config_claims_maxAccruedBlocks);
		
		this.config_claims_initialBlocks = config.getInt("GriefPrevention.Claims.InitialBlocks",100);
		
		outConfig.set("GriefPrevention.Economy.ClaimBlocksPurchaseCost", this.config_economy_claimBlocksPurchaseCost);
		outConfig.set("GriefPrevention.Economy.ClaimBlocksSellValue", this.config_economy_claimBlocksSellValue);
		outConfig.set("GriefPrevention.Claims.InitialBlocks",config_claims_initialBlocks);
		
		

		
		//when datastore initializes, it loads player and claim data, and posts some stats to the log
		if(databaseUrl.length() > 0)
		{
			try
			{
				DatabaseDataStore databaseStore = new DatabaseDataStore(databaseUrl, databaseUserName, databasePassword);
			
				if(FlatFileDataStore.hasData())
				{
					GriefPrevention.AddLogEntry("There appears to be some data on the hard drive.  Migrating those data to the database...");
					FlatFileDataStore flatFileStore = new FlatFileDataStore();
					flatFileStore.migrateData(databaseStore);
					GriefPrevention.AddLogEntry("Data migration process complete.  Reloading data from the database...");
					databaseStore.close();
					databaseStore = new DatabaseDataStore(databaseUrl, databaseUserName, databasePassword);
				}
				
				this.dataStore = databaseStore;
			}
			catch(Exception e)
			{
				GriefPrevention.AddLogEntry("Because there was a problem with the database, GriefPrevention will not function properly.  Either update the database config settings resolve the issue, or delete those lines from your config.yml so that GriefPrevention can use the file system to store data.");
				return;
			}			
		}
		
		//if not using the database because it's not configured or because there was a problem, use the file system to store data
		//this is the preferred method, as it's simpler than the database scenario
		if(this.dataStore == null)
		{
			try
			{
				this.dataStore = new FlatFileDataStore();
			}
			catch(Exception e)
			{
				GriefPrevention.AddLogEntry("Unable to initialize the file system data store.  Details:");
				GriefPrevention.AddLogEntry(e.getMessage());
			}
		}
		boolean claimblockaccrual = false;
		for(WorldConfig wconfig:this.Configuration.getWorldConfigs().values()){
			if(wconfig.getClaimBlocksAccruedPerHour()>0){
				claimblockaccrual=true;
				break;
			}
		}
		//unless claim block accrual is disabled, start the recurring per 5 minute event to give claim blocks to online players
		//20L ~ 1 second
		if(claimblockaccrual)
		{
			DeliverClaimBlocksTask task = new DeliverClaimBlocksTask();
			this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task, 20L * 60 * 5, 20L * 60 * 5);
		}
		
		//start the recurring cleanup event for entities in creative worlds, if enabled.
		
		//start recurring cleanup scan for unused claims belonging to inactive players
		//if the option is enabled.
		//look through all world configurations.
		boolean claimcleanupOn=false;
		boolean entitycleanupEnabled=false;
		for(WorldConfig wconfig:Configuration.getWorldConfigs().values()){
			if(wconfig.getClaimCleanupEnabled())
				claimcleanupOn=true;
			if(wconfig.getEntityCleanupEnabled())
				entitycleanupEnabled=true;
		}
		
		if(entitycleanupEnabled){
			EntityCleanupTask task = new EntityCleanupTask(0);
			this.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 20L);
			}
		
		if(claimcleanupOn){
		CleanupUnusedClaimsTask task2 = new CleanupUnusedClaimsTask();
		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task2, 20L * 60 * 2, 20L * 60 * 5);
		}
		
			//register for events
			if(!eventsRegistered){
				eventsRegistered=true;
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
			}
		
		//if economy is enabled
		if(this.config_economy_claimBlocksPurchaseCost > 0 || this.config_economy_claimBlocksSellValue > 0)
		{
			//try to load Vault
			GriefPrevention.AddLogEntry("GriefPrevention requires Vault for economy integration.");
			GriefPrevention.AddLogEntry("Attempting to load Vault...");
			RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
			GriefPrevention.AddLogEntry("Vault loaded successfully!");
			
			//ask Vault to hook into an economy plugin
			GriefPrevention.AddLogEntry("Looking for a Vault-compatible economy plugin...");
			if (economyProvider != null) 
	        {
	        	GriefPrevention.economy = economyProvider.getProvider();
	            
	            //on success, display success message
				if(GriefPrevention.economy != null)
		        {
	            	GriefPrevention.AddLogEntry("Hooked into economy: " + GriefPrevention.economy.getName() + ".");  
	            	GriefPrevention.AddLogEntry("Ready to buy/sell claim blocks!");
		        }
		        
				//otherwise error message
				else
		        {
		        	GriefPrevention.AddLogEntry("ERROR: Vault was unable to find a supported economy plugin.  Either install a Vault-compatible economy plugin, or set both of the economy config variables to zero.");
		        }	            
	        }
			
			//another error case
			else
			{
				GriefPrevention.AddLogEntry("ERROR: Vault was unable to find a supported economy plugin.  Either install a Vault-compatible economy plugin, or set both of the economy config variables to zero.");
			}
		}
		MetaHandler = new ClaimMetaHandler();
		try {
			new File(DataStore.configFilePath).delete();
			outConfig.save(new File(DataStore.configFilePath).getAbsolutePath());
		}
		catch(IOException exx){
			this.log.log(Level.SEVERE, "Failed to save primary configuration file:" + DataStore.configFilePath);
		}
	}
	private void HandleClaimClean(Claim c,MaterialInfo source,MaterialInfo target,Player player){
		Location lesser = c.getLesserBoundaryCorner();
		Location upper = c.getGreaterBoundaryCorner();
		System.out.println("HandleClaimClean:" + source.typeID + " to " + target.typeID);
		
		for(int x =lesser.getBlockX();x<=upper.getBlockX();x++){
			for(int y = 0;y<=255;y++){
				for(int z = lesser.getBlockZ();z<=upper.getBlockZ();z++){
					Location createloc =  new Location(lesser.getWorld(),x,y,z);
					Block acquired = lesser.getWorld().getBlockAt(createloc);
					if(acquired.getTypeId() == source.typeID && acquired.getData() == source.data){
						acquired.setTypeIdAndData(target.typeID, target.data, true);
						
					}
					
					
					
				}
			}
		}
		
		
		
	}
	private static final String[] HelpIndex = new String[] { 
			ChatColor.AQUA + "-----=GriefPrevention Help Index=------",
	                         "use /gphelp [topic] to view each topic." ,
	ChatColor.YELLOW +       "Topics: Claims,Trust"};
	
	
	private static final String[] ClaimsHelp = new String[] {
			ChatColor.AQUA + "-----=GriefPrevention Claims=------" ,
	      ChatColor.YELLOW + " GriefPrevention uses Claims to allow you to claim areas and prevent " ,
	      ChatColor.YELLOW + "other players from messing with your stuff without your permission.",
	      ChatColor.YELLOW + "Claims are created by either placing your first Chest or by using the",
	      ChatColor.YELLOW + "Claim creation tool, which is by default a Golden Shovel.",
	      ChatColor.YELLOW + "You can resize your claims by using a Golden Shovel on a corner, or",
	      ChatColor.YELLOW + "by defining a new claim that encompasses it. The original claim",
	      ChatColor.YELLOW + "Will be resized. you can use trust commands to give other players abilities",
	      ChatColor.YELLOW + "In your claim. See /gphelp trust for more information"};
	
	private static final String[] TrustHelp = new String[] {
		ChatColor.AQUA +     "------=GriefPrevention Trust=------",
		ChatColor.YELLOW +   "You can control who can do things in your claims by using the trust commands",
		ChatColor.YELLOW +   "/accesstrust can be used to allow a player to interact with items in your claim",
		ChatColor.YELLOW +   "/containertrust can be used to allow a player to interact with your chests.",
		ChatColor.YELLOW +   "/trust allows players to build on your claim.",
		ChatColor.YELLOW +   "Each trust builds upon the previous one in this list; containertrust includes accesstrust",
		ChatColor.YELLOW +   "and build trust includes container trust and access trust."};
		                     
		
		
	
	        
	private void handleHelp(Player p,String Topic){
		if(p==null) return;
		String[] uselines;
		if(Topic.equalsIgnoreCase("claims"))
			uselines = ClaimsHelp;
		else if(Topic.equalsIgnoreCase("trust"))
			uselines = TrustHelp;
		else
			uselines = HelpIndex;
			
			
		for(String iterate:uselines){
		    p.sendMessage(uselines);	
		}
			
			
			
		
		
		
		
		
	}
	//handles slash commands
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		
		Player player = null;
		WorldConfig wc = null;
		if (sender instanceof Player) 
		{
			player = (Player) sender;
			wc = getWorldCfg(player.getWorld());
			
		}
		
		//abandonclaim
		if(cmd.getName().equalsIgnoreCase("gphelp") && player != null){
		String topic="index";
		if(args.length>0) topic = args[0];
		handleHelp(player,topic);
		
			
		}
		else if(cmd.getName().equalsIgnoreCase("abandonclaim") && player != null)
		{
			return this.abandonClaimHandler(player, false);
		}		
		else if(cmd.getName().equalsIgnoreCase("claiminfo") && player !=null){
			//show information about a claim.
			Claim claimatpos = null;
			if(args.length ==0)
				claimatpos = dataStore.getClaimAt(player.getLocation(),true,null);
			else {
				int claimid = Integer.parseInt(args[0]);
				claimatpos = dataStore.getClaim(claimid);
				if(claimatpos==null){
				    GriefPrevention.sendMessage(player, TextMode.Err, "Invalid Claim ID:" + claimid);
				    return true;
				}
			}
			if(claimatpos==null){
				GriefPrevention.sendMessage(player,TextMode.Err, "There is no Claim here!");
				GriefPrevention.sendMessage(player,TextMode.Err, "Make sure you are inside a claim.");
				
				return true;
			}
			else {
				//there is a claim, show all sorts of pointless info about it.
				//we do not show trust, since that can be shown with /trustlist.
				//first, get the upper and lower boundary.
				//see that it has Children.
				if(claimatpos.children.size()>0){
					
				}
				
				
				String lowerboundary = GriefPrevention.getfriendlyLocationString(claimatpos.getLesserBoundaryCorner());
				String upperboundary = GriefPrevention.getfriendlyLocationString(claimatpos.getGreaterBoundaryCorner()) ;
				String SizeString = "(" +String.valueOf(claimatpos.getWidth()) + "," + String.valueOf(claimatpos.getHeight()) + ")";
				String ClaimOwner = claimatpos.getOwnerName();
				GriefPrevention.sendMessage(player,TextMode.Info, "ID:" + claimatpos.getID());
				GriefPrevention.sendMessage(player,TextMode.Info, "Position:" + lowerboundary + "-" + upperboundary);
				GriefPrevention.sendMessage(player,TextMode.Info,"Size:" + SizeString);
				GriefPrevention.sendMessage(player, TextMode.Info, "Owner:" + ClaimOwner);
				String parentid = claimatpos.parent==null?"(no parent)":String.valueOf(claimatpos.parent.getID());
				GriefPrevention.sendMessage(player,TextMode.Info, "Parent ID:" + parentid);
				String childinfo = "";
				//if no subclaims, set childinfo to indicate as such.
				if(claimatpos.children.size() ==0){
					childinfo = "No subclaims.";
				}
				else { //otherwise, we need to get the childclaim info by iterating through each child claim.
					childinfo = claimatpos.children.size() + " (";
					
					for(Claim childclaim:claimatpos.children){
					    childinfo+=String.valueOf(childclaim.getSubClaimID()) + ",";	
					}
					//remove the last character since it is a comma we do not want.
					childinfo = childinfo.substring(0,childinfo.length()-1);
					
					//tada
				}
				GriefPrevention.sendMessage(player,TextMode.Info,"Subclaims:" + childinfo);
				
				return true;
			}
			
			
			
		}
		else if(cmd.getName().equalsIgnoreCase("cleanclaim") && player !=null){
			//source is first arg; target is second arg.
			player.sendMessage("cleanclaim command..." + args.length);
			if(args.length==0){
				return true;
			}
			MaterialInfo source = MaterialInfo.fromString(args[0]);
		    if(source==null){
		    	Material attemptparse = Material.valueOf(args[0]); 
		    	if(attemptparse!=null){
		    		source = new MaterialInfo(attemptparse.getId(),(byte)0,args[0]);
		    	}
		    	else
		    	{
		    		player.sendMessage("Failed to parse Source Material," + args[0]);
		    		return true;
		    	}
		    	
		    }
		    MaterialInfo target = new MaterialInfo(Material.AIR.getId(),(byte)0,"Air");
		    if(args.length >1){
		    	target = MaterialInfo.fromString(args[1]);
		    	if(target==null){
		    		Material attemptparse = Material.valueOf(args[1]);
		    		if(attemptparse!=null){
		    			target = new MaterialInfo(attemptparse.getId(),(byte)0,args[1]);
		    		}
		    		else {
		    			player.sendMessage("Failed to parse Target Material," + args[1]);
		    		}
		    	}
		    
		    }
		    System.out.println(source.typeID + " " +target.typeID);
		    PlayerData pd = dataStore.getPlayerData(player.getName());
		    Claim retrieveclaim = dataStore.getClaimAt(player.getLocation(), true, null);
		    if(retrieveclaim!=null){
			    if(pd.ignoreClaims || retrieveclaim.ownerName.equalsIgnoreCase(player.getName())){
			    	HandleClaimClean(retrieveclaim,source,target,player);
			    	return true;
			    }
		    }
			
			
		}
		if(cmd.getName().equalsIgnoreCase("setclaimblocks") && player !=null){
			
			
			
		}
		if(cmd.getName().equalsIgnoreCase("clearmanagers") && player!=null){
			Claim claimatpos = dataStore.getClaimAt(player.getLocation(), true, null);
			PlayerData pdata = dataStore.getPlayerData(player.getName());
			if(claimatpos!=null){
				if(claimatpos.isAdminClaim()){
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearManagersNotAdmin);
					return true;
				}
				if(pdata.ignoreClaims ||  claimatpos.ownerName.equalsIgnoreCase(player.getName())){
					for(String currmanager :claimatpos.getManagerList()){
						claimatpos.removeManager(currmanager);
					}
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearManagersSuccess);
				} else {
					//nope
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearManagersNotOwned);
				}
				
			}
			else {
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearManagersNotFound);
			}
		}
		if(cmd.getName().equalsIgnoreCase("gpreload")){
			if(player==null || player.hasPermission("griefprevention.reload")){
			this.onDisable();
			this.onEnable();
			}
		}
		//abandontoplevelclaim
		if(cmd.getName().equalsIgnoreCase("abandontoplevelclaim") && player != null)
		{
			return this.abandonClaimHandler(player, true);
		}
		
		//ignoreclaims
		if(cmd.getName().equalsIgnoreCase("ignoreclaims") && player != null)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			
			playerData.ignoreClaims = !playerData.ignoreClaims;
			
			//toggle ignore claims mode on or off
			if(!playerData.ignoreClaims)
			{
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.RespectingClaims);
			}
			else
			{
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.IgnoringClaims);
			}
			
			return true;
		}
		else if(cmd.getName().equalsIgnoreCase("giveclaimblocks") && player!=null){
			if(args.length<2)
			{
				return false;
			}
			int desiredxfer=0;
			try {desiredxfer = Integer.parseInt(args[1]);}
			catch(NumberFormatException nfe){
				return false;
			}
			this.transferClaimBlocks(player.getName(),args[0],desiredxfer);
		}
		else if(cmd.getName().equalsIgnoreCase("transferclaimblocks") && player!=null){
				if(args.length<3){
					return false;
				}
				String sourcename = args[0];
				String targetname = args[1];
				int desiredxfer = 0;
				try {desiredxfer = Integer.parseInt(args[2]);}
				catch(NumberFormatException exx){
					return false;
				}
				this.transferClaimBlocks(sourcename, targetname, desiredxfer);
				
				
		}
		//abandonallclaims
		else if(cmd.getName().equalsIgnoreCase("abandonallclaims") && player != null)
		{
			if(args.length > 1) return false;
			boolean deletelocked = false;
			if(args.length > 0) {
				deletelocked = Boolean.parseBoolean(args[0]);
			}
			
			if(!wc.getAllowUnclaim())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoCreativeUnClaim);
				return true;
			}
			
			//count claims
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			int originalClaimCount = playerData.claims.size();
			
			//check count
			if(originalClaimCount == 0)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.YouHaveNoClaims);
				return true;
			}
			
			//delete them
			this.dataStore.deleteClaimsForPlayer(player.getName(), false, deletelocked);
			
			//inform the player
			int remainingBlocks = playerData.getRemainingClaimBlocks();
			if(deletelocked) {
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandonIncludingLocked, String.valueOf(remainingBlocks));
			}else {
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandonExcludingLocked, String.valueOf(remainingBlocks));
			}
			
			//revert any current visualization
			Visualization.Revert(player);
			
			return true;
		}
		
		//restore nature
		else if(cmd.getName().equalsIgnoreCase("restorenature") && player != null)
		{
			//change shovel mode
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.RestoreNature;
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.RestoreNatureActivate);
			return true;
		}
		
		//restore nature aggressive mode
		else if(cmd.getName().equalsIgnoreCase("restorenatureaggressive") && player != null)
		{
			//change shovel mode
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.RestoreNatureAggressive;
			GriefPrevention.sendMessage(player, TextMode.Warn, Messages.RestoreNatureAggressiveActivate);
			return true;
		}
		
		//restore nature fill mode
		else if(cmd.getName().equalsIgnoreCase("restorenaturefill") && player != null)
		{
			//change shovel mode
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.RestoreNatureFill;
			
			//set radius based on arguments
			playerData.fillRadius = 2;
			if(args.length > 0)
			{
				try
				{
					playerData.fillRadius = Integer.parseInt(args[0]);
				}
				catch(Exception exception){ }
			}
			
			if(playerData.fillRadius < 0) playerData.fillRadius = 2;
			
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.FillModeActive, String.valueOf(playerData.fillRadius));
			return true;
		}
		
		//trust <player>
		else if(cmd.getName().equalsIgnoreCase("trust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			//most trust commands use this helper method, it keeps them consistent
			this.handleTrustCommand(player, ClaimPermission.Build, args[0]);
			
			return true;
		}
		
		//lockclaim
		else if(cmd.getName().equalsIgnoreCase("lockclaim") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 0) return false;
			
			Claim claim = dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
			if((player.hasPermission("griefprevention.lock") && claim.ownerName.equalsIgnoreCase(player.getName())) || player.hasPermission("griefprevention.adminlock")) {
				claim.neverdelete = true;
				dataStore.saveClaim(claim);
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClaimLocked);
			}
			
			return true;
		}
		
		//unlockclaim
		else if(cmd.getName().equalsIgnoreCase("unlockclaim") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 0) return false;
			
			Claim claim = dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
			if((player.hasPermission("griefprevention.lock") && claim.ownerName.equalsIgnoreCase(player.getName())) || player.hasPermission("griefprevention.adminlock")) {
				claim.neverdelete = false;
				dataStore.saveClaim(claim);
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClaimUnlocked);
			}
			
			return true;
		}
		else if(cmd.getName().equalsIgnoreCase("giveclaim") && player!=null){
			//gives a claim to another player. get the source player first.
			if(args.length==0) return false;
			Player source = player;
			Player target = Bukkit.getPlayer(args[0]);
			if(target==null){
				GriefPrevention.sendMessage(source,TextMode.Err, Messages.PlayerNotFound,args[0]);
				return true;
			}
			//if it's not null, make sure they have either have giveclaim permission or adminclaims permission.
			
			if(!(source.hasPermission("griefprevention.giveclaims") || source.hasPermission("griefprevention.adminclaims"))){
			
				//find the claim at the players location.
				Claim claimtogive = dataStore.getClaimAt(source.getLocation(), true, null);
				//if the owner is not the source, they have to have adminclaims permission too.
				if(!claimtogive.getOwnerName().equalsIgnoreCase(source.getName())){
					//if they don't have adminclaims permission, deny it.
					if(!source.hasPermission("griefprevention.adminclaims")){
						GriefPrevention.sendMessage(source, TextMode.Err, Messages.NoAdminClaimsPermission);
						return true;
					}
				}
				//transfer ownership.
				claimtogive.ownerName = target.getName();

				String originalOwner = claimtogive.getOwnerName();
				try {dataStore.changeClaimOwner(claimtogive, target.getName());
				//message both players.
				GriefPrevention.sendMessage(source, TextMode.Success, Messages.GiveSuccessSender,originalOwner,target.getName());
				if(target!=null && target.isOnline()){
					GriefPrevention.sendMessage(target,TextMode.Success,Messages.GiveSuccessTarget,originalOwner);
				}
				}
				catch(Exception exx){
					GriefPrevention.sendMessage(source, TextMode.Err, "Failed to transfer Claim.");
				}
				
				
				
			}
			
			
			
		}
		//transferclaim <player>
		else if(cmd.getName().equalsIgnoreCase("transferclaim") && player != null)
		{
			//can take two parameters. Source Player and target player.
			if(args.length == 0) return false;
			//one arg requires "GriefPrevention.transferclaims" or "GriefPrevention.adminclaims" permission.
			//two args requires the latter.
			if(args.length >0)
			//check additional permission
			if(!player.hasPermission("griefprevention.adminclaims"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.TransferClaimPermission);
				return true;
			}
			
			//which claim is the user in?
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true, null);
			if(claim == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferClaimMissing);
				return true;
			}
			
			OfflinePlayer targetPlayer = this.resolvePlayer(args[0]);
			if(targetPlayer == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
				return true;
			}
			
			//change ownership
			try
			{
				this.dataStore.changeClaimOwner(claim, targetPlayer.getName());
			}
			catch(Exception e)
			{
				GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferTopLevel);
				return true;
			}
			
			//confirm
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.TransferSuccess);
			GriefPrevention.AddLogEntry(player.getName() + " transferred a claim at " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " to " + targetPlayer.getName() + ".");
			
			return true;
		}
		
		//trustlist
		else if(cmd.getName().equalsIgnoreCase("trustlist") && player != null)
		{
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true, null);
			
			//if no claim here, error message
			if(claim == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrustListNoClaim);
				return true;
			}
			
			//if no permission to manage permissions, error message
			String errorMessage = claim.allowGrantPermission(player);
			if(errorMessage != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, errorMessage);
				return true;
			}
			
			//otherwise build a list of explicit permissions by permission level
			//and send that to the player
			ArrayList<String> builders = new ArrayList<String>();
			ArrayList<String> containers = new ArrayList<String>();
			ArrayList<String> accessors = new ArrayList<String>();
			ArrayList<String> managers = new ArrayList<String>();
			claim.getPermissions(builders, containers, accessors, managers);
			
			player.sendMessage("Explicit permissions here:");
			
			StringBuilder permissions = new StringBuilder();
			permissions.append(ChatColor.GOLD + "M: ");
			
			if(managers.size() > 0)
			{
				for(int i = 0; i < managers.size(); i++)
					permissions.append(managers.get(i) + " ");
			}
			
			player.sendMessage(permissions.toString());
			permissions = new StringBuilder();
			permissions.append(ChatColor.YELLOW + "B: ");
			
			if(builders.size() > 0)
			{				
				for(int i = 0; i < builders.size(); i++)
					permissions.append(builders.get(i) + " ");		
			}
			
			player.sendMessage(permissions.toString());
			permissions = new StringBuilder();
			permissions.append(ChatColor.GREEN + "C: ");				
			
			if(containers.size() > 0)
			{
				for(int i = 0; i < containers.size(); i++)
					permissions.append(containers.get(i) + " ");		
			}
			
			player.sendMessage(permissions.toString());
			permissions = new StringBuilder();
			permissions.append(ChatColor.BLUE + "A :");
				
			if(accessors.size() > 0)
			{
				for(int i = 0; i < accessors.size(); i++)
					permissions.append(accessors.get(i) + " ");			
			}
			
			player.sendMessage(permissions.toString());
			
			player.sendMessage("(M-anager, B-builder, C-ontainers, A-ccess)");
			
			return true;
		}
		
		//untrust <player> or untrust [<group>]
		else if(cmd.getName().equalsIgnoreCase("untrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			//determine which claim the player is standing in
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
			
			//bracket any permissions
			if(args[0].contains("."))
			{
				args[0] = "[" + args[0] + "]";
			}
			
			//determine whether a single player or clearing permissions entirely
			boolean clearPermissions = false;
			OfflinePlayer otherPlayer = null;
			System.out.println("clearing perms for name:" + args[0]);
			if(args[0].equals("all"))				
			{
				if(claim == null || claim.allowEdit(player) == null)
				{
					clearPermissions = true;
				}
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearPermsOwnerOnly);
					return true;
				}
			}
			
			else if((!args[0].startsWith("[") || !args[0].endsWith("]"))
				&& !args[0].toUpperCase().startsWith("G:") && ! args[0].startsWith("!"))
				{
					otherPlayer = this.resolvePlayer(args[0]);
					if(!clearPermissions && otherPlayer == null && !args[0].equals("public"))
					{
						GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
						return true;
					}
					
					//correct to proper casing
					if(otherPlayer != null)
						args[0] = otherPlayer.getName();
				}
			else if(args[0].startsWith("G:")){
				//make sure the group exists, otherwise show the message.
				String groupname = args[0].substring(2);
				if(!config_player_groups.GroupExists(groupname)){
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.GroupNotFound);
					return true;
				}
			}
					
					
			
			//if no claim here, apply changes to all his claims
			if(claim == null)
			{
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());
				for(int i = 0; i < playerData.claims.size(); i++)
				{
					claim = playerData.claims.get(i);
					
					//if untrusting "all" drop all permissions
					if(clearPermissions)
					{	
						claim.clearPermissions();
					}
					
					//otherwise drop individual permissions
					else
					{
						claim.dropPermission(args[0]);
						claim.removeManager(args[0]);
						//claim.managers.remove(args[0]);
					}
					
					//save changes
					this.dataStore.saveClaim(claim);
				}
				
				//beautify for output
				if(args[0].equals("public"))
				{
					args[0] = "the public";
				}
				
				//confirmation message
				if(!clearPermissions)
				{
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualAllClaims, args[0]);
				}
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustEveryoneAllClaims);
				}
			}			
			
			//otherwise, apply changes to only this claim
			else if(claim.allowGrantPermission(player) != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
			}
			else
			{
				//if clearing all
				if(clearPermissions)
				{
					claim.clearPermissions();
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClearPermissionsOneClaim);
				}
				
				//otherwise individual permission drop
				else
				{
					claim.dropPermission(args[0]);
					if(claim.allowEdit(player) == null)
					{
						claim.removeManager(args[0]);
												
						//beautify for output
						if(args[0].equals("public"))
						{
							args[0] = "the public";
						}
						
						GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualSingleClaim, args[0]);
					}
					else
					{
						GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustOwnerOnly, claim.getOwnerName());
					}
				}
				
				//save changes
				this.dataStore.saveClaim(claim);										
			}
			
			return true;
		}
		
		//accesstrust <player>
		else if(cmd.getName().equalsIgnoreCase("accesstrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			this.handleTrustCommand(player, ClaimPermission.Access, args[0]);
			
			return true;
		}
		
		//containertrust <player>
		else if(cmd.getName().equalsIgnoreCase("containertrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			this.handleTrustCommand(player, ClaimPermission.Inventory, args[0]);
			
			return true;
		}
		
		//permissiontrust <player>
		else if(cmd.getName().equalsIgnoreCase("permissiontrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			this.handleTrustCommand(player, null, args[0]);  //null indicates permissiontrust to the helper method
			
			return true;
		}
		
		//buyclaimblocks
		else if(cmd.getName().equalsIgnoreCase("buyclaimblocks") && player != null)
		{
			//if economy is disabled, don't do anything
			if(GriefPrevention.economy == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
				return true;
			}
			
			if(!player.hasPermission("griefprevention.buysellclaimblocks"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionForCommand);
				return true;
			}
			
			//if purchase disabled, send error message
			if(GriefPrevention.instance.config_economy_claimBlocksPurchaseCost == 0)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlySellBlocks);
				return true;
			}
			
			//if no parameter, just tell player cost per block and balance
			if(args.length != 1)
			{
				GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockPurchaseCost, String.valueOf(GriefPrevention.instance.config_economy_claimBlocksPurchaseCost), String.valueOf(GriefPrevention.economy.getBalance(player.getName())));
				return false;
			}
			
			else
			{
				//determine max purchasable blocks
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());
				int maxPurchasable = GriefPrevention.instance.config_claims_maxAccruedBlocks - playerData.accruedClaimBlocks;
				
				//if the player is at his max, tell him so
				if(maxPurchasable <= 0)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimBlockLimit);
					return true;
				}
				
				//try to parse number of blocks
				int blockCount;
				try
				{
					blockCount = Integer.parseInt(args[0]);
				}
				catch(NumberFormatException numberFormatException)
				{
					return false;  //causes usage to be displayed
				}
				
				if(blockCount <= 0)
				{
					return false;
				}
				
				//correct block count to max allowed
				if(blockCount > maxPurchasable)
				{
					blockCount = maxPurchasable;
				}
				
				//if the player can't afford his purchase, send error message
				double balance = economy.getBalance(player.getName());				
				double totalCost = blockCount * GriefPrevention.instance.config_economy_claimBlocksPurchaseCost;				
				if(totalCost > balance)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.InsufficientFunds, String.valueOf(totalCost),  String.valueOf(balance));
				}
				
				//otherwise carry out transaction
				else
				{
					//withdraw cost
					economy.withdrawPlayer(player.getName(), totalCost);
					
					//add blocks
					playerData.accruedClaimBlocks += blockCount;
					this.dataStore.savePlayerData(player.getName(), playerData);
					
					//inform player
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.PurchaseConfirmation, String.valueOf(totalCost), String.valueOf(playerData.getRemainingClaimBlocks()));
				}
				
				return true;
			}
		}
		
		//sellclaimblocks <amount> 
		else if(cmd.getName().equalsIgnoreCase("sellclaimblocks") && player != null)
		{
			//if economy is disabled, don't do anything
			if(GriefPrevention.economy == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
				return true;
			}
			
			if(!player.hasPermission("griefprevention.buysellclaimblocks"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionForCommand);
				return true;
			}
			
			//if disabled, error message
			if(GriefPrevention.instance.config_economy_claimBlocksSellValue == 0)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlyPurchaseBlocks);
				return true;
			}
			
			//load player data
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			int availableBlocks = playerData.getRemainingClaimBlocks();
			
			//if no amount provided, just tell player value per block sold, and how many he can sell
			if(args.length != 1)
			{
				GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockSaleValue, String.valueOf(GriefPrevention.instance.config_economy_claimBlocksSellValue), String.valueOf(availableBlocks));
				return false;
			}
						
			//parse number of blocks
			int blockCount;
			try
			{
				blockCount = Integer.parseInt(args[0]);
			}
			catch(NumberFormatException numberFormatException)
			{
				return false;  //causes usage to be displayed
			}
			
			if(blockCount <= 0)
			{
				return false;
			}
			
			//if he doesn't have enough blocks, tell him so
			if(blockCount > availableBlocks)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotEnoughBlocksForSale);
			}
			
			//otherwise carry out the transaction
			else
			{					
				//compute value and deposit it
				double totalValue = blockCount * GriefPrevention.instance.config_economy_claimBlocksSellValue;					
				economy.depositPlayer(player.getName(), totalValue);
				
				//subtract blocks
				playerData.accruedClaimBlocks -= blockCount;
				this.dataStore.savePlayerData(player.getName(), playerData);
				
				//inform player
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.BlockSaleConfirmation, String.valueOf(totalValue), String.valueOf(playerData.getRemainingClaimBlocks()));
			}
			
			return true;
		}		
		
		//adminclaims
		else if(cmd.getName().equalsIgnoreCase("adminclaims") && player != null)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.Admin;
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdminClaimsMode);
			
			return true;
		}
		
		//basicclaims
		else if(cmd.getName().equalsIgnoreCase("basicclaims") && player != null)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.Basic;
			playerData.claimSubdividing = null;
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.BasicClaimsMode);
			
			return true;
		}
		
		//subdivideclaims
		else if(cmd.getName().equalsIgnoreCase("subdivideclaims") && player != null)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.Subdivide;
			playerData.claimSubdividing = null;
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionMode);
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionDemo);
			
			return true;
		}
		
		//deleteclaim
		else if(cmd.getName().equalsIgnoreCase("deleteclaim") && player != null)
		{
			//determine which claim the player is standing in
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
			
			if(claim == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
			}
			
			else 
			{
				//deleting an admin claim additionally requires the adminclaims permission
				if(!claim.isAdminClaim() || player.hasPermission("griefprevention.adminclaims"))
				{
					PlayerData playerData = this.dataStore.getPlayerData(player.getName());
					if(claim.children.size() > 0 && !playerData.warnedAboutMajorDeletion)
					{
						GriefPrevention.sendMessage(player, TextMode.Warn, Messages.DeletionSubdivisionWarning);
						playerData.warnedAboutMajorDeletion = true;
					}else if(claim.neverdelete && !playerData.warnedAboutMajorDeletion) {
						GriefPrevention.sendMessage(player, TextMode.Warn, Messages.DeleteLockedClaimWarning);
						playerData.warnedAboutMajorDeletion = true;
					}
					else
					{
						claim.removeSurfaceFluids(null);
						this.dataStore.deleteClaim(claim);
						
						//if in a creative mode world, /restorenature the claim
						if(wc.getAutoRestoreUnclaimed() && GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()))
						{
							GriefPrevention.instance.restoreClaim(claim, 0);
						}
						
						GriefPrevention.sendMessage(player, TextMode.Success, Messages.DeleteSuccess);
						GriefPrevention.AddLogEntry(player.getName() + " deleted " + claim.getOwnerName() + "'s claim at " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
						
						//revert any current visualization
						Visualization.Revert(player);
						
						playerData.warnedAboutMajorDeletion = false;
					}
				}
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantDeleteAdminClaim);
				}
			}

			return true;
		}
		
		else if(cmd.getName().equalsIgnoreCase("claimexplosions") && player != null)
		{
			//determine which claim the player is standing in
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
			
			if(claim == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
			}
			
			else
			{
				String noBuildReason = claim.allowBuild(player);
				if(noBuildReason != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
					return true;
				}
				
				if(claim.areExplosivesAllowed)
				{
					claim.areExplosivesAllowed = false;
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.ExplosivesDisabled);
				}
				else
				{
					claim.areExplosivesAllowed = true;
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.ExplosivesEnabled);
				}
			}

			return true;
		}
		
		//deleteallclaims <player>
		else if(cmd.getName().equalsIgnoreCase("deleteallclaims"))
		{
			//requires one or two parameters, the other player's name and whether to delete locked claims.
			if(args.length < 1 && args.length > 2) return false;
			
			//try to find that player
			OfflinePlayer otherPlayer = this.resolvePlayer(args[0]);
			if(otherPlayer == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
				return true;
			}
			
			boolean deletelocked = false;
			if(args.length == 2) {
				deletelocked = Boolean.parseBoolean(args[1]);
			}
			
			//delete all that player's claims
			this.dataStore.deleteClaimsForPlayer(otherPlayer.getName(), true, deletelocked);
			
			if(deletelocked) {
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.DeleteAllSuccessIncludingLocked, otherPlayer.getName());
			}else {
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.DeleteAllSuccessExcludingLocked, otherPlayer.getName());
			}
			if(player != null)
			{
				GriefPrevention.AddLogEntry(player.getName() + " deleted all claims belonging to " + otherPlayer.getName() + ".");
			
				//revert any current visualization
				Visualization.Revert(player);
			}
			
			return true;
		}
		
		//claimslist or claimslist <player>
		else if(cmd.getName().equalsIgnoreCase("claimslist"))
		{
			//at most one parameter
			if(args.length > 1) return false;
			
			//player whose claims will be listed
			OfflinePlayer otherPlayer;
			
			//if another player isn't specified, assume current player
			if(args.length < 1)
			{
				if(player != null)
					otherPlayer = player;
				else
					return false;
			}
			
			//otherwise if no permission to delve into another player's claims data
			else if(player != null && !player.hasPermission("griefprevention.deleteclaims"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimsListNoPermission);
				return true;
			}
						
			//otherwise try to find the specified player
			else
			{
				otherPlayer = this.resolvePlayer(args[0]);
				if(otherPlayer == null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
					return true;
				}
			}
			
			//load the target player's data
			PlayerData playerData = this.dataStore.getPlayerData(otherPlayer.getName());
			GriefPrevention.sendMessage(player, TextMode.Instr, " " + playerData.accruedClaimBlocks + "(+" + (playerData.bonusClaimBlocks + this.dataStore.getGroupBonusBlocks(otherPlayer.getName())) + ")=" + (playerData.accruedClaimBlocks + playerData.bonusClaimBlocks + this.dataStore.getGroupBonusBlocks(otherPlayer.getName())));
			for(int i = 0; i < playerData.claims.size(); i++)
			{
				Claim claim = playerData.claims.get(i);
				GriefPrevention.sendMessage(player, TextMode.Instr, "  (-" + claim.getArea() + ") " + getfriendlyLocationString(claim.getLesserBoundaryCorner()));
			}
			
			if(playerData.claims.size() > 0)
				GriefPrevention.sendMessage(player, TextMode.Instr, "   =" + playerData.getRemainingClaimBlocks());
			
			//drop the data we just loaded, if the player isn't online
			if(!otherPlayer.isOnline())
				this.dataStore.clearCachedPlayerData(otherPlayer.getName());
			
			return true;
		}
		
		//deathblow <player> [recipientPlayer]
		else if(cmd.getName().equalsIgnoreCase("deathblow"))
		{
			//requires at least one parameter, the target player's name
			if(args.length < 1) return false;
			
			//try to find that player
			Player targetPlayer = this.getServer().getPlayer(args[0]);
			if(targetPlayer == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
				return true;
			}
			
			//try to find the recipient player, if specified
			Player recipientPlayer = null;
			if(args.length > 1)
			{
				recipientPlayer = this.getServer().getPlayer(args[1]);
				if(recipientPlayer == null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
					return true;
				}
			}
			
			//if giving inventory to another player, teleport the target player to that receiving player
			if(recipientPlayer != null)
			{
				targetPlayer.teleport(recipientPlayer);
			}
			
			//otherwise, plan to "pop" the player in place
			else
			{
				//if in a normal world, shoot him up to the sky first, so his items will fall on the surface.
				if(targetPlayer.getWorld().getEnvironment() == Environment.NORMAL)
				{
					Location location = targetPlayer.getLocation();
					location.setY(location.getWorld().getMaxHeight());
					targetPlayer.teleport(location);
				}
			}
			 
			//kill target player
			targetPlayer.setHealth(0);
			
			//log entry
			if(player != null)
			{
				GriefPrevention.AddLogEntry(player.getName() + " used /DeathBlow to kill " + targetPlayer.getName() + ".");
			}
			else
			{
				GriefPrevention.AddLogEntry("Killed " + targetPlayer.getName() + ".");
			}
			
			return true;
		}
		
		//deletealladminclaims
		else if(cmd.getName().equalsIgnoreCase("deletealladminclaims"))
		{
			if(!player.hasPermission("griefprevention.deleteclaims"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoDeletePermission);
				return true;
			}
			
			//delete all admin claims
			this.dataStore.deleteClaimsForPlayer("", true, true);  //empty string for owner name indicates an administrative claim
			
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.AllAdminDeleted);
			if(player != null)
			{
				GriefPrevention.AddLogEntry(player.getName() + " deleted all administrative claims.");
			
				//revert any current visualization
				Visualization.Revert(player);
			}
			
			return true;
		}
		
		//adjustbonusclaimblocks <player> <amount> or [<permission>] amount
		else if(cmd.getName().equalsIgnoreCase("adjustbonusclaimblocks"))
		{
			//requires exactly two parameters, the other player or group's name and the adjustment
			if(args.length != 2) return false;
			
			//parse the adjustment amount
			int adjustment;			
			try
			{
				adjustment = Integer.parseInt(args[1]);
			}
			catch(NumberFormatException numberFormatException)
			{
				return false;  //causes usage to be displayed
			}
			
			//if granting blocks to all players with a specific permission
			if(args[0].startsWith("[") && args[0].endsWith("]"))
			{
				String permissionIdentifier = args[0].substring(1, args[0].length() - 1);
				int newTotal = this.dataStore.adjustGroupBonusBlocks(permissionIdentifier, adjustment);
				
				if(player!=null) GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustGroupBlocksSuccess, permissionIdentifier, String.valueOf(adjustment), String.valueOf(newTotal));
				if(player != null) GriefPrevention.AddLogEntry(player.getName() + " adjusted " + permissionIdentifier + "'s bonus claim blocks by " + adjustment + ".");
				
				return true;
			}
			
			//otherwise, find the specified player
			OfflinePlayer targetPlayer = this.resolvePlayer(args[0]);
			if(targetPlayer == null)
			{
				if(player!=null) GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
				return true;
			}
			
			//give blocks to player
			PlayerData playerData = this.dataStore.getPlayerData(targetPlayer.getName());
			playerData.bonusClaimBlocks += adjustment;
			this.dataStore.savePlayerData(targetPlayer.getName(), playerData);
			
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustBlocksSuccess, targetPlayer.getName(), String.valueOf(adjustment), String.valueOf(playerData.bonusClaimBlocks));
			if(player != null) GriefPrevention.AddLogEntry(player.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".");
			
			return true;			
		}
		
		//trapped
		else if(cmd.getName().equalsIgnoreCase("trapped") && player != null)
		{
			//FEATURE: empower players who get "stuck" in an area where they don't have permission to build to save themselves
			
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
			
			//if another /trapped is pending, ignore this slash command
			if(playerData.pendingTrapped)
			{
				return true;
			}
			
			//if the player isn't in a claim or has permission to build, tell him to man up
			if(claim == null || claim.allowBuild(player) == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotTrappedHere);				
				return true;
			}
			
			//if the player is in the nether or end, he's screwed (there's no way to programmatically find a safe place for him)
			if(player.getWorld().getEnvironment() != Environment.NORMAL)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);				
				return true;
			}
			
			//if the player is in an administrative claim, he should contact an admin
			if(claim.isAdminClaim())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);
				return true;
			}
			
			//check cooldown
			long lastTrappedUsage = playerData.lastTrappedUsage.getTime();
			long nextTrappedUsage = lastTrappedUsage + 1000 * 60 * 60 * wc.getClaimsTrappedCooldownHours(); 
			long now = Calendar.getInstance().getTimeInMillis();
			if(now < nextTrappedUsage)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedOnCooldown, String.valueOf(wc.getClaimsTrappedCooldownHours()), String.valueOf((nextTrappedUsage - now) / (1000 * 60) + 1));
				return true;
			}
			
			//send instructions
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.RescuePending);
			
			//create a task to rescue this player in a little while
			PlayerRescueTask task = new PlayerRescueTask(player, player.getLocation());
			this.getServer().getScheduler().scheduleSyncDelayedTask(this, task, 200L);  //20L ~ 1 second
			
			return true;
		}
		
		//siege
		else if(cmd.getName().equalsIgnoreCase("siege") && player != null)
		{
			//error message for when siege mode is disabled
			if(!this.siegeEnabledForWorld(player.getWorld()))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NonSiegeWorld);
				return true;
			}
			
			//requires one argument
			if(args.length > 1)
			{
				return false;
			}
			
			//can't start a siege when you're already involved in one
			Player attacker = player;
			PlayerData attackerData = this.dataStore.getPlayerData(attacker.getName());
			if(attackerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.AlreadySieging);
				return true;
			}
			
			//can't start a siege when you're protected from pvp combat
			if(attackerData.pvpImmune)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantFightWhileImmune);
				return true;
			}
			
			//if a player name was specified, use that
			Player defender = null;
			if(args.length >= 1)
			{
				defender = this.getServer().getPlayer(args[0]);
				if(defender == null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
					return true;
				}
			}
			
			//otherwise use the last player this player was in pvp combat with 
			else if(attackerData.lastPvpPlayer.length() > 0)
			{
				defender = this.getServer().getPlayer(attackerData.lastPvpPlayer);
				if(defender == null)
				{
					return false;
				}
			}
			
			else
			{
				return false;
			}
			
			//victim must not be under siege already
			PlayerData defenderData = this.dataStore.getPlayerData(defender.getName());
			if(defenderData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.AlreadyUnderSiegePlayer);
				return true;
			}
			
			//victim must not be pvp immune
			if(defenderData.pvpImmune)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoSiegeDefenseless);
				return true;
			}
			
			Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false, null);
			
			//defender must have some level of permission there to be protected
			if(defenderClaim == null || defenderClaim.allowAccess(defender) != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotSiegableThere);
				return true;
			}									
			
			//attacker must be close to the claim he wants to siege
			if(!defenderClaim.isNear(attacker.getLocation(), 25))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeTooFarAway);
				return true;
			}
			
			//claim can't be under siege already
			if(defenderClaim.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.AlreadyUnderSiegeArea);
				return true;
			}
			
			//can't siege admin claims
			if(defenderClaim.isAdminClaim())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoSiegeAdminClaim);
				return true;
			}
			
			//can't be on cooldown
			if(dataStore.onCooldown(attacker, defender, defenderClaim))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeOnCooldown);
				return true;
			}
			
			//start the siege
			if(dataStore.startSiege(attacker, defender, defenderClaim)){			

			   //confirmation message for attacker, warning message for defender
			   GriefPrevention.sendMessage(defender, TextMode.Warn, Messages.SiegeAlert, attacker.getName());
			   GriefPrevention.sendMessage(player, TextMode.Success, Messages.SiegeConfirmed, defender.getName());
			}
		}
		
		return false; 
	}
	/**
	 * transfers a number of claim blocks from a source player to a  target player.
	 * @param Source Source player name. 
	 * @param string Target Player name.
	 * @return number of claim blocks transferred.
	 */
	private synchronized int transferClaimBlocks(String Source, String Target,int DesiredAmount) {
		// TODO Auto-generated method stub
		
		//transfer claim blocks from source to target, return number of claim blocks transferred.
		PlayerData playerData = this.dataStore.getPlayerData(Source);
		PlayerData receiverData = this.dataStore.getPlayerData(Target);
		if(playerData!=null && receiverData!=null){
		    int xferamount = Math.min(playerData.accruedClaimBlocks,DesiredAmount);
		    playerData.accruedClaimBlocks-=xferamount;
		    receiverData.accruedClaimBlocks+=xferamount;
		    return xferamount;
		}
		return 0;
		
		
	}

	/**
	 * Creates a friendly Location string for the given Location.
	 * @param location Location to retrieve a string for.
	 * @return a formatted String to be shown to a user or for a log file depicting the approximate given location.
	 */
	public static String getfriendlyLocationString(Location location) 
	{
		return location.getWorld().getName() + "(" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ")";
	}

	private boolean abandonClaimHandler(Player player, boolean deleteTopLevelClaim) 
	{
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		WorldConfig wc = getWorldCfg(player.getWorld());
		
		//which claim is being abandoned?
		Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
		if(claim == null)
		{
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.AbandonClaimMissing);
			return true;
		}
		int claimarea = claim.getArea();
		//retrieve (1-abandonclaimration)*totalarea to get amount to subtract from the accrued claim blocks
		//after we delete the claim.
		int costoverhead =(int)Math.floor((double)claimarea*(1-wc.getClaimsAbandonReturnRatio()));
		System.out.println("costoverhead:" + costoverhead);
		
		
		//verify ownership
		if(claim.allowEdit(player) != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
		}
		
		//don't allow abandon of claims if not configured to allow.
		else if(!wc.getAllowUnclaim() )
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoCreativeUnClaim);
		}
		
		//warn if has children and we're not explicitly deleting a top level claim
		else if(claim.children.size() > 0 && !deleteTopLevelClaim)
		{
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.DeleteTopLevelClaim);
			return true;
		}
		
		//if the claim is locked, let's warn the player and give them a chance to back out
		else if(!playerData.warnedAboutMajorDeletion && claim.neverdelete)
		{			
			GriefPrevention.sendMessage(player, TextMode.Warn, Messages.ConfirmAbandonLockedClaim);
			playerData.warnedAboutMajorDeletion = true;
		}
		//if auto-restoration is enabled,
		else if(!playerData.warnedAboutMajorDeletion && wc.getClaimsAbandonNatureRestoration())
				{
			GriefPrevention.sendMessage(player,TextMode.Warn,Messages.AbandonClaimRestoreWarning);
			playerData.warnedAboutMajorDeletion=true;
		}
		else if(!playerData.warnedAboutMajorDeletion && costoverhead!=claimarea){
			playerData.warnedAboutMajorDeletion=true;
			GriefPrevention.sendMessage(player,TextMode.Warn,Messages.AbandonCostWarning,String.valueOf(costoverhead));
		}
		//if the claim has lots of surface water or some surface lava, warn the player it will be cleaned up
		else if(!playerData.warnedAboutMajorDeletion && claim.hasSurfaceFluids() && claim.parent == null)
		{			
			GriefPrevention.sendMessage(player, TextMode.Warn, Messages.ConfirmFluidRemoval);
			playerData.warnedAboutMajorDeletion = true;
		}
		
		else
		{
			//delete it
			//Only do water/lava cleanup when it's a top level claim.
			if(claim.parent == null) {
				claim.removeSurfaceFluids(null);
			}
			//retrieve area of this claim...
			
			
			if(!this.dataStore.deleteClaim(claim,player)){
				//cancelled!
				//assume the event called will show an appropriate message...
				return false;
			}
			
			//if in a creative mode world, restore the claim area
			//CHANGE: option is now determined by configuration options.
			//if we are in a creative world and the creative Abandon Nature restore option is enabled,
			//or if we are in a survival world and the creative Abandon Nature restore option is enabled,
			//then perform the restoration.
			if((wc.getClaimsAbandonNatureRestoration())){
			
				GriefPrevention.AddLogEntry(player.getName() + " abandoned a claim @ " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
				GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
				GriefPrevention.instance.restoreClaim(claim, 20L * 60 * 2);
			}
			//remove the interest cost, and message the player.
			if(costoverhead > 0){
			    playerData.accruedClaimBlocks-=costoverhead;
				//
			    GriefPrevention.sendMessage(player, TextMode.Warn, Messages.AbandonCost,0,String.valueOf(costoverhead));
			}
			int remainingBlocks = playerData.getRemainingClaimBlocks();
			//tell the player how many claim blocks he has left
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.AbandonSuccess, 0,String.valueOf(remainingBlocks));
			
			//revert any current visualization
			Visualization.Revert(player);
			
			playerData.warnedAboutMajorDeletion = false;
			}
		
		
		return true;
		
	}

	
	
	//helper method keeps the trust commands consistent and eliminates duplicate code
	private void handleTrustCommand(Player player, ClaimPermission permissionLevel, String recipientName) 
	{
		//determine which claim the player is standing in
		Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
		
		//validate player or group argument
		String permission = null;
		OfflinePlayer otherPlayer = null;
		boolean isforceddenial = false;
		//if it starts with "!", remove it and set the forced denial value.
		//we use this flag to indicate to add in a "!" again when we set the perm.
		//This will have the effect of causing the logic to explicitly deny permissions for players that do not match.
		if(recipientName.startsWith("!")){
			isforceddenial=true;
			recipientName = recipientName.substring(1); //remove the exclamation for the rest of the parsing.
		}
		
		if(recipientName.startsWith("[") && recipientName.endsWith("]"))
		{
			permission = recipientName.substring(1, recipientName.length() - 1);
			if(permission == null || permission.isEmpty())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.InvalidPermissionID);
				return;
			}
		}
		
		else if(recipientName.contains("."))
		{
			permission = recipientName;
		}
		
		else
		{		
			otherPlayer = this.resolvePlayer(recipientName);
			//addition: if it starts with G:, it indicates a group name, rather than a player name.
			
			if(otherPlayer == null && !recipientName.equals("public") && !recipientName.equals("all") &&
					!recipientName.toUpperCase().startsWith("G:"))
				
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
				return;
			}
			else if(recipientName.toUpperCase().startsWith("G:")){
				//keep it as is.
				//we will give trust to that group, that is...
				
			}
			
			else if(otherPlayer != null)
			{
				recipientName = otherPlayer.getName();
			}
			
			else
			{
				recipientName = "public";
			}
		}
		
		//determine which claims should be modified
		ArrayList<Claim> targetClaims = new ArrayList<Claim>();
		if(claim == null)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			for(int i = 0; i < playerData.claims.size(); i++)
			{
				targetClaims.add(playerData.claims.get(i));
			}
		}
		else
		{
			//check permission here
			if(claim.allowGrantPermission(player) != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
				return;
			}
			
			//see if the player has the level of permission he's trying to grant
			String errorMessage = null;
			
			//permission level null indicates granting permission trust
			if(permissionLevel == null)
			{
				errorMessage = claim.allowEdit(player);
				if(errorMessage != null)
				{
					errorMessage = "Only " + claim.getOwnerName() + " can grant /PermissionTrust here."; 
				}
			}
			
			//otherwise just use the ClaimPermission enum values
			else
			{
				switch(permissionLevel)
				{
					case Access:
						errorMessage = claim.allowAccess(player);
						break;
					case Inventory:
						errorMessage = claim.allowContainers(player);
						break;
					default:
						errorMessage = claim.allowBuild(player);					
				}
			}
			
			//error message for trying to grant a permission the player doesn't have
			if(errorMessage != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantGrantThatPermission);
				return;
			}
			
			targetClaims.add(claim);
		}
		
		//if we didn't determine which claims to modify, tell the player to be specific
		if(targetClaims.size() ==  0)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.GrantPermissionNoClaim);
			return;
		}
		//if forcedenial is true, we will add the exclamation back to the name for addition.
		if(isforceddenial) recipientName = "!" + recipientName;
		//apply changes
		for(int i = 0; i < targetClaims.size(); i++)
		{
			Claim currentClaim = targetClaims.get(i);
			if(permissionLevel == null)
			{
				if(!currentClaim.isManager(recipientName))
				{
					currentClaim.addManager(recipientName);
				}
			}
			else
			{				
				currentClaim.setPermission(recipientName, permissionLevel);
			}
			this.dataStore.saveClaim(currentClaim);
		}
		
		//notify player
		if(recipientName.equals("public")) recipientName = this.dataStore.getMessage(Messages.CollectivePublic);
		String permissionDescription;
		if(permissionLevel == null)
		{
			permissionDescription = this.dataStore.getMessage(Messages.PermissionsPermission);
		}
		else if(permissionLevel == ClaimPermission.Build)
		{
			permissionDescription = this.dataStore.getMessage(Messages.BuildPermission);
		}		
		else if(permissionLevel == ClaimPermission.Access)
		{
			permissionDescription = this.dataStore.getMessage(Messages.AccessPermission);
		}
		else //ClaimPermission.Inventory
		{
			permissionDescription = this.dataStore.getMessage(Messages.ContainersPermission);
		}
		
		String location;
		if(claim == null)
		{
			
			location = this.dataStore.getMessage(Messages.LocationAllClaims);
		}
		else
		{
			location = this.dataStore.getMessage(Messages.LocationCurrentClaim);
		}
		String userecipientName = recipientName;
		if(userecipientName.toUpperCase().startsWith("G:")){
			userecipientName="Group " + userecipientName.substring(2);
		}
		GriefPrevention.sendMessage(player, TextMode.Success, Messages.GrantPermissionConfirmation, recipientName, permissionDescription, location);
	}

	//helper method to resolve a player by name
	public OfflinePlayer resolvePlayer(String name) 
	{
		//try online players first
		Player player = this.getServer().getPlayer(name);
		if(player != null) return player;
		
		//then search offline players
		OfflinePlayer [] offlinePlayers = this.getServer().getOfflinePlayers();
		for(int i = 0; i < offlinePlayers.length; i++)
		{
			if(offlinePlayers[i].getName().equalsIgnoreCase(name))
			{
				return offlinePlayers[i];
			}
		}
		
		//if none found, return null
		return null;
	}

	public void onDisable()
	{ 
		//save data for any online players
		Player [] players = this.getServer().getOnlinePlayers();
		for(int i = 0; i < players.length; i++)
		{
			Player player = players[i];
			String playerName = player.getName();
			PlayerData playerData = this.dataStore.getPlayerData(playerName);
			this.dataStore.savePlayerData(playerName, playerData);
		}
		//cancel ALL pending tasks.
		Bukkit.getScheduler().cancelTasks(this);
		this.dataStore.close();
		
		AddLogEntry("GriefPrevention disabled.");
	}
	
	//called when a player spawns, applies protection for that player if necessary
	public void checkPvpProtectionNeeded(Player player)
	{
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		//if pvp is disabled, do nothing
		if(!player.getWorld().getPVP()) return;
		
		//if player is in creative mode, do nothing
		if(player.getGameMode() == GameMode.CREATIVE) return;
		
		//if anti spawn camping feature is not enabled, do nothing
		if(!wc.getProtectFreshSpawns()) return;
		
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
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		playerData.pvpImmune = true;
		
		//inform the player
		GriefPrevention.sendMessage(player, TextMode.Success, Messages.PvPImmunityStart);
	}
	
	//checks whether players can create claims in a world
	public boolean claimsEnabledForWorld(World world)
	{
		return this.getWorldCfg(world).getClaimsEnabled();
	}
	
	//checks whether players siege in a world
	public boolean siegeEnabledForWorld(World world)
	{
		return this.getWorldCfg(world).getSeigeEnabled();
	}

	//processes broken log blocks to automatically remove floating treetops
	void handleLogBroken(Block block) 
	{
		//find the lowest log in the tree trunk including this log
		Block rootBlock = this.getRootBlock(block); 
		
		//null indicates this block isn't part of a tree trunk
		if(rootBlock == null) return;
		
		//next step: scan for other log blocks and leaves in this tree
		
		//set boundaries for the scan 
		int min_x = rootBlock.getX() - GriefPrevention.TREE_RADIUS;
		int max_x = rootBlock.getX() + GriefPrevention.TREE_RADIUS;
		int min_z = rootBlock.getZ() - GriefPrevention.TREE_RADIUS;
		int max_z = rootBlock.getZ() + GriefPrevention.TREE_RADIUS;
		int max_y = rootBlock.getWorld().getMaxHeight() - 1;
		
		//keep track of all the examined blocks, and all the log blocks found
		ArrayList<Block> examinedBlocks = new ArrayList<Block>();
		ArrayList<Block> treeBlocks = new ArrayList<Block>();
		
		//queue the first block, which is the block immediately above the player-chopped block
		ConcurrentLinkedQueue<Block> blocksToExamine = new ConcurrentLinkedQueue<Block>();
		blocksToExamine.add(rootBlock);
		examinedBlocks.add(rootBlock);
		
		boolean hasLeaves = false;
		
		while(!blocksToExamine.isEmpty())
		{
			//pop a block from the queue
			Block currentBlock = blocksToExamine.remove();
			
			//if this is a log block, determine whether it should be chopped
			if(currentBlock.getType() == Material.LOG)
			{
				boolean partOfTree = false;
				
				//if it's stacked with the original chopped block, the answer is always yes
				if(currentBlock.getX() == block.getX() && currentBlock.getZ() == block.getZ())
				{
					partOfTree = true;
				}
				
				//otherwise find the block underneath this stack of logs
				else
				{
					Block downBlock = currentBlock.getRelative(BlockFace.DOWN);
					while(downBlock.getType() == Material.LOG)
					{
						downBlock = downBlock.getRelative(BlockFace.DOWN);
					}
					
					//if it's air or leaves, it's okay to chop this block
					//this avoids accidentally chopping neighboring trees which are close enough to touch their leaves to ours
					if(downBlock.getType() == Material.AIR || downBlock.getType() == Material.LEAVES)
					{
						partOfTree = true;
					}
					
					//otherwise this is a stack of logs which touches a solid surface
					//if it's close to the original block's stack, don't clean up this tree (just stop here)
					else
					{
						if(Math.abs(downBlock.getX() - block.getX()) <= 1 && Math.abs(downBlock.getZ() - block.getZ()) <= 1) return;
					}
				}
				
				if(partOfTree)
				{
					treeBlocks.add(currentBlock);
				}
			}
			
			//if this block is a log OR a leaf block, also check its neighbors
			if(currentBlock.getType() == Material.LOG || currentBlock.getType() == Material.LEAVES)
			{
				if(currentBlock.getType() == Material.LEAVES)
				{
					hasLeaves = true;
				}
				
				Block [] neighboringBlocks = new Block [] 
				{
					currentBlock.getRelative(BlockFace.EAST),
					currentBlock.getRelative(BlockFace.WEST),
					currentBlock.getRelative(BlockFace.NORTH),
					currentBlock.getRelative(BlockFace.SOUTH),
					currentBlock.getRelative(BlockFace.UP),						
					currentBlock.getRelative(BlockFace.DOWN)
				};
				
				for(int i = 0; i < neighboringBlocks.length; i++)
				{
					Block neighboringBlock = neighboringBlocks[i];
											
					//if the neighboringBlock is out of bounds, skip it
					if(neighboringBlock.getX() < min_x || neighboringBlock.getX() > max_x || neighboringBlock.getZ() < min_z || neighboringBlock.getZ() > max_z || neighboringBlock.getY() > max_y) continue;						
					
					//if we already saw this block, skip it
					if(examinedBlocks.contains(neighboringBlock)) continue;
					
					//mark the block as examined
					examinedBlocks.add(neighboringBlock);
					
					//if the neighboringBlock is a leaf or log, put it in the queue to be examined later
					if(neighboringBlock.getType() == Material.LOG || neighboringBlock.getType() == Material.LEAVES)
					{
						blocksToExamine.add(neighboringBlock);
					}
					
					//if we encounter any player-placed block type, bail out (don't automatically remove parts of this tree, it might support a treehouse!)
					else if(this.isPlayerBlock(neighboringBlock)) 
					{
						return;						
					}
				}					
			}				
		}
		
		//if it doesn't have leaves, it's not a tree, so don't clean it up
		if(hasLeaves)
		{		
			//schedule a cleanup task for later, in case the player leaves part of this tree hanging in the air		
			TreeCleanupTask cleanupTask = new TreeCleanupTask(block, rootBlock, treeBlocks, rootBlock.getData());

			//20L ~ 1 second, so 2 mins = 120 seconds ~ 2400L 
			GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, cleanupTask, 2400L);
		}
	}
	
	//helper for above, finds the "root" of a stack of logs
	//will return null if the stack is determined to not be a natural tree
	private Block getRootBlock(Block logBlock)
	{
		if(logBlock.getType() != Material.LOG) return null;
		
		//run down through log blocks until finding a non-log block
		Block underBlock = logBlock.getRelative(BlockFace.DOWN);
		while(underBlock.getType() == Material.LOG)
		{
			underBlock = underBlock.getRelative(BlockFace.DOWN);
		}
		
		//if this is a standard tree, that block MUST be dirt
		if(underBlock.getType() != Material.DIRT) return null;
		
		//run up through log blocks until finding a non-log block
		Block aboveBlock = logBlock.getRelative(BlockFace.UP);
		while(aboveBlock.getType() == Material.LOG)
		{
			aboveBlock = aboveBlock.getRelative(BlockFace.UP);
		}
		
		//if this is a standard tree, that block MUST be air or leaves
		if(aboveBlock.getType() != Material.AIR && aboveBlock.getType() != Material.LEAVES) return null;
		
		return underBlock.getRelative(BlockFace.UP);
	}
	
	//for sake of identifying trees ONLY, a cheap but not 100% reliable method for identifying player-placed blocks
	private boolean isPlayerBlock(Block block)
	{
		Material material = block.getType();
		
		//list of natural blocks which are OK to have next to a log block in a natural tree setting
		if(	material == Material.AIR || 
			material == Material.LEAVES || 
			material == Material.LOG || 
			material == Material.DIRT ||
			material == Material.GRASS ||			
			material == Material.STATIONARY_WATER ||
			material == Material.BROWN_MUSHROOM || 
			material == Material.RED_MUSHROOM ||
			material == Material.RED_ROSE ||
			material == Material.LONG_GRASS ||
			material == Material.SNOW ||
			material == Material.STONE ||
			material == Material.VINE ||
			material == Material.WATER_LILY ||
			material == Material.YELLOW_FLOWER ||
			material == Material.CLAY)
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	//moves a player from the claim he's in to a nearby wilderness location
	public Location ejectPlayer(Player player)
	{
		//look for a suitable location
		Location candidateLocation = player.getLocation();
		while(true)
		{
			Claim claim = null;
			claim = GriefPrevention.instance.dataStore.getClaimAt(candidateLocation, false, null);
			
			//if there's a claim here, keep looking
			if(claim != null)
			{
				candidateLocation = new Location(claim.lesserBoundaryCorner.getWorld(), claim.lesserBoundaryCorner.getBlockX() - 1, claim.lesserBoundaryCorner.getBlockY(), claim.lesserBoundaryCorner.getBlockZ() - 1);
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
	public static void sendMessage(Player player, ChatColor color, Messages messageID, String... args)
	{
		sendMessage(player, color, messageID, 0, args);
	}
	
	//sends a color-coded message to a player
	static void sendMessage(Player player, ChatColor color, Messages messageID, long delayInTicks, String... args)
	{
		
		String message = GriefPrevention.instance.dataStore.getMessage(messageID, args);
		if(message==null || message.equals("")) return;
		sendMessage(player, color, message, delayInTicks);
	}
	private static String removeColors(String source){
		
		for(ChatColor cc:ChatColor.values()){
			source=source.replace(cc.toString(), "");
		}
		return source;
		
		
	}
	//sends a color-coded message to a player
	public static void sendMessage(Player player, ChatColor color, String message)
	{
		if(player == null)
		{
			GriefPrevention.AddLogEntry(removeColors(message));
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
			GriefPrevention.instance.getServer().getScheduler().runTaskLater(GriefPrevention.instance, task, delayInTicks);
		}
		else
		{
			task.run();
		}
	}
	
	//determines whether creative anti-grief rules apply at a location
	public boolean creativeRulesApply(Location location)
	{
		//return this.config_claims_enabledCreativeWorlds.contains(location.getWorld().getName());
		return Configuration.getWorldConfig(location.getWorld()).getCreativeRules();
	}
	public String allowBuild(Player player, Location location)
	{
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		//exception: administrators in ignore claims mode and special player accounts created by server mods
		if(playerData.ignoreClaims || wc.getModsIgnoreClaimsAccounts().contains(player.getName())) return null;
		
		//wilderness rules
		if(claim == null)
		{
			//no building in the wilderness in creative mode
			if(this.creativeRulesApply(location))
			{
				String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims) + "  " + this.dataStore.getMessage(Messages.CreativeBasicsDemoAdvertisement);
				if(player.hasPermission("griefprevention.ignoreclaims"))
					reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
				return reason;
			}
			
			//no building in survival wilderness when that is configured
			else if(wc.getApplyTrashBlockRules() && wc.getClaimsEnabled())
			{
				if(wc.getTrashBlockPlacementBehaviour().Allowed(location,player).Denied())
					return this.dataStore.getMessage(Messages.NoBuildOutsideClaims) + "  " + this.dataStore.getMessage(Messages.SurvivalBasicsDemoAdvertisement);
				else
					return null;
			}
			
			else
			{
				//but it's fine in creative
				return null;
			}			
		}
		
		//if not in the wilderness, then apply claim rules (permissions, etc)
		else
		{
			//cache the claim for later reference
			playerData.lastClaim = claim;
			return claim.allowBuild(player);
		}
	}
	
	public String allowBreak(Player player, Location location)
	{
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		//exception: administrators in ignore claims mode, and special player accounts created by server mods
		if(playerData.ignoreClaims || wc.getModsIgnoreClaimsAccounts().contains(player.getName())) return null;
		
		//wilderness rules
		if(claim == null)
		{
			//no building in the wilderness in creative mode
			if(this.creativeRulesApply(location))
			{
				String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims) + "  " + this.dataStore.getMessage(Messages.CreativeBasicsDemoAdvertisement);
				if(player.hasPermission("griefprevention.ignoreclaims"))
					reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
				return reason;
			}
			
			else if(wc.getApplyTrashBlockRules() && wc.getClaimsEnabled())
			{
				return this.dataStore.getMessage(Messages.NoBuildOutsideClaims) + "  " + this.dataStore.getMessage(Messages.SurvivalBasicsDemoAdvertisement);
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
			return claim.allowBreak(player, location.getBlock());
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
		
		Chunk lesserChunk = claim.getLesserBoundaryCorner().getChunk();
		Chunk greaterChunk = claim.getGreaterBoundaryCorner().getChunk();
		
		for(int x = lesserChunk.getX(); x <= greaterChunk.getX(); x++)
			for(int z = lesserChunk.getZ(); z <= greaterChunk.getZ(); z++)
			{
				Chunk chunk = lesserChunk.getWorld().getChunkAt(x, z);
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
		RestoreNatureProcessingTask task = new RestoreNatureProcessingTask(snapshots, miny, chunk.getWorld().getEnvironment(), lesserBoundaryCorner.getBlock().getBiome(), lesserBoundaryCorner, greaterBoundaryCorner, this.getSeaLevel(chunk.getWorld()), aggressiveMode, GriefPrevention.instance.creativeRulesApply(lesserBoundaryCorner), playerReceivingVisualization);
		GriefPrevention.instance.getServer().getScheduler().runTaskLaterAsynchronously(GriefPrevention.instance, task, delayInTicks);
	}
	
	public void parseMaterialListFromConfig(List<String> stringsToParse, MaterialCollection materialCollection)
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
				GriefPrevention.AddLogEntry("ERROR: Unable to read a material entry from the config file.  Please update your config.yml.");
				
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
		int overrideValue = getWorldCfg(world).getSeaLevelOverride();
		
		
		/*if(overrideValue == null || overrideValue == -1)
		{
			return world.getSeaLevel();
		}
		else
		{
			return overrideValue;
		}*/
		return overrideValue;
	}
}
