package me.ryanhamshire.GriefPrevention.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.naming.ConfigurationException;

import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.Debugger;
import me.ryanhamshire.GriefPrevention.Debugger.DebugLevel;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.MaterialCollection;
import me.ryanhamshire.GriefPrevention.MaterialInfo;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * holds configuration data global to GP as well as 
 * providing accessors to retrieve/create configurations for individual worlds.
 * @author BC_Programming
 *
 */
public class ConfigData {

	private String TemplateFile;
	private String WorldConfigLocation = null;
	public String getTemplateFile(){ return TemplateFile;}
	private HashMap<String,WorldConfig> WorldCfg = new HashMap<String,WorldConfig>();
	//private WorldConfig DefaultSurvival = null;
	//private WorldConfig DefaultCreative = null;
	//private List<String> PvPEnabledWorlds = new ArrayList<String>();
	//private List<String> CreativeWorldRules = new ArrayList<String>();
	public Map<String,WorldConfig> getWorldConfigs(){
		return Collections.unmodifiableMap(WorldCfg);
		
		
		
	}
	public List<WorldConfig> getCreativeRulesConfigs(){
		
		ArrayList<WorldConfig> buildList = new ArrayList<WorldConfig>();
		for(WorldConfig wcon:WorldCfg.values()){
			if(wcon.getCreativeRules())
				buildList.add(wcon);
		}
		return buildList;
		
	}
	public WorldConfig getWorldConfig(String forWorld){
		
		World Grabfor = Bukkit.getWorld(forWorld);
		if(Grabfor==null) return new WorldConfig(forWorld);
		
		return getWorldConfig(Grabfor);
	}
	public List<String> GetWorldConfigurationFiles(){
		List<String> ActFiles = new ArrayList<String>();
		File SourceFolder = new File(WorldConfigLocation);
		
		ActFiles.add(getTemplateFile());
		
		//iterate through each file.
		for(File iterate:SourceFolder.listFiles()){
			if(iterate.getName().startsWith("_")) continue;
			if(iterate.getName().toUpperCase().endsWith(".yml")){
				ActFiles.add(iterate.getPath());
			}
			
			
			
		}
		return ActFiles;
		
	}
	public void AddContainerID(Player Invoker,World TargetWorld,MaterialInfo mi){
		AddMaterialListItem(Invoker,TargetWorld,"GriefPrevention.Mods.BlockIdsRequiringContainerTrust",mi);
	}
	public void RemoveContainerID(Player Invoker,World TargetWorld,MaterialInfo mi){
		RemoveMaterialListItem(Invoker,TargetWorld,"GriefPrevention.Mods.BlockIdsRequiringContainerTrust",mi);
	}
	//part of possible future feature to allow admins to add Container, Access, and other elements to the 
	//plugin configuration "on the fly"; for example a admin could use a certain tool, and left click would add it as a container and shift+left click would remove it
	//(or something, eg. Left click add, Right click remove, shift on each would do the same for all worlds, that sort of thing).
	
	
	public void AddMaterialListItem(Player Invoker,World TargetWorld,String NodePath,MaterialInfo mi){
		//special method, loads in this world configuration, adds the given Container ID to the container ID listing,
		//saves, and returns.
		//first, we need to get our file.
	
		
		String WorldTarget;
		List<String> ActFiles = new ArrayList<String>();
		if(TargetWorld!=null){
			ActFiles.add(WorldConfig.getWorldConfig(TargetWorld.getName()));
			WorldTarget = " World:" + TargetWorld.getName();
			
		}
		else {
			//if TargetWorld is null, we want to do this to <all configuration files>
			ActFiles = GetWorldConfigurationFiles();
			//System.out.println("Files:" + ActFiles.size());
			WorldTarget = "All Worlds";
		}
		
		for(String iterate:ActFiles){
			//open this configuration...
			
			
			YamlConfiguration yml = YamlConfiguration.loadConfiguration(new File(iterate));
			//GriefPrevention.Mods.BlockIdsRequiringContainerTrust
			List<String> StringResult = yml.getStringList(NodePath);
			//turn the list into a MaterialCollection.
			MaterialCollection mc = new MaterialCollection();
			GriefPrevention.instance.parseMaterialListFromConfig(StringResult,mc);
			if(mc.contains(mi)){
				
				continue;
			}
			mc.add(mi);
			yml.set("GriefPrevention.Mods.BlockIdsRequiringContainerTrust", mc.GetList());
			//add to the end of this list.
			//save the changed config.
			try {
			yml.save(iterate);
			}
			catch(IOException exx){
				
			}
			if(Invoker!=null){
				Invoker.sendMessage(ChatColor.ITALIC + TextMode.Instr.toString() + "Block ID:" + mi.getTypeID() + " added to " + NodePath + " in " + WorldTarget  );
			}
			
			
		}
		
	}
	public void RemoveMaterialListItem(Player Invoker,World TargetWorld,String NodePath,MaterialInfo mi){
		//special method, loads in this world configuration, adds the given Container ID to the container ID listing,
		//saves, and returns.
		//first, we need to get our file.
	
		String WorldTarget;
		List<String> ActFiles = new ArrayList<String>();
		if(TargetWorld!=null){
			ActFiles.add(WorldConfig.getWorldConfig(TargetWorld.getName()));
			WorldTarget = " World:" + TargetWorld.getName();
			
		}
		else {
			//if TargetWorld is null, we want to do this to <all configuration files>
			ActFiles = GetWorldConfigurationFiles();
			WorldTarget = "All Worlds";
		}
		
		for(String iterate:ActFiles){
			//open this configuration...
			YamlConfiguration yml = YamlConfiguration.loadConfiguration(new File(iterate));
			//GriefPrevention.Mods.BlockIdsRequiringContainerTrust
			List<String> StringResult = yml.getStringList(NodePath);
			//turn the list into a MaterialCollection.
			MaterialCollection mc = new MaterialCollection();
			GriefPrevention.instance.parseMaterialListFromConfig(StringResult,mc);
			if(!mc.contains(mi)){
				
				continue;
			}
			mc.remove(mi);
			yml.set(NodePath, mc.GetList());
			//add to the end of this list.
			if(Invoker!=null){
				Invoker.sendMessage(ChatColor.ITALIC + TextMode.Instr.toString() + "Block ID:" + mi.getTypeID() + " removed from " + NodePath + " in " + iterate);
			}
		}
		
	}
	/**
	 * retrieves the WorldConfiguration for the given world name.
	 * If the world is not valid, a log entry will be posted, but the config should still be loaded and returned.
	 * @param worldName Name of world to get configuration of.
	 * @return WorldConfig instance representing the configuration for the given world.
	 */
	public WorldConfig getWorldConfig(World grabfor){
		
		String worldName = grabfor.getName();
		
		//if it's not in the hashmap...
		if(!WorldCfg.containsKey(worldName)){
			//special code: it's possible a configuration might already exist for this file, so we'll
			//check 
			String checkyamlfile = WorldConfig.getWorldConfig(worldName);
			//if it exists...
			if(new File(checkyamlfile).exists()){
				//attempt to load the configuration from the given file.
				YamlConfiguration existingcfg = YamlConfiguration.loadConfiguration(new File(checkyamlfile));
				YamlConfiguration outConfiguration = new YamlConfiguration();
				//place it in the hashmap.
				WorldCfg.put(worldName,new WorldConfig(worldName,existingcfg,outConfiguration));
				//try to save it. this can error out for who knows what reason. If it does, we'll
				//squirt the issue to the log.
				try {outConfiguration.save(new File(checkyamlfile));}
				catch(IOException iex){
					GriefPrevention.instance.getLogger().log(Level.SEVERE,"Failed to save World Config for world " + worldName);
					
				}
			}
			else {
				//if the file doesn't exist, then we will go ahead and create a new configuration.
				//set the input Yaml to default to the template.
				//if the template file exists, load it's configuration and use the result as useSource. 
				//Otherwise, we create a blank configuration.
				Debugger.Write("Failed to find world configuration for World " + worldName, DebugLevel.Errors);
				File TemplFile = new File(TemplateFile);
				FileConfiguration useSource=null;
				if(TemplFile.exists()){
					useSource = YamlConfiguration.loadConfiguration(TemplFile);
				}
				else {
					Debugger.Write("Template file \"" + TemplateFile + " \"Not Found.", DebugLevel.Errors);
					useSource = new YamlConfiguration();
				}
				
				//The target save location.
				FileConfiguration Target = new YamlConfiguration();
				//place it in the hashmap.
				WorldCfg.put(worldName, new WorldConfig(grabfor.getName(),useSource,Target));
				try {
					Target.save(new File(checkyamlfile));
				}catch(IOException ioex){
					GriefPrevention.instance.getLogger().log(Level.SEVERE, "Failed to write world configuration to " + checkyamlfile);
				}
				
				}
			//save target
			}
		//after the above logic, we know it's in the hashmap, so return that.
		return WorldCfg.get(worldName);
		
	}
	public static FileConfiguration createTargetConfiguration(String sName){
		return YamlConfiguration.loadConfiguration(new File(sName));
	}
	/**
	 * returns the Configuration file location for the given world. Note that this file may or may not exist.
	 * @param sName Name of the world.
	 * @return the path name at which this configuration file will be found if it exists.
	 */
	public static String getWorldConfigLocation(String sName){
		
		return DataStore.dataLayerFolderPath + File.separator + "WorldConfigs/" + sName + ".cfg";
	}
	/**
	 * Constructs a new ConfigData instance from the given core configuration location
	 * and the passed in target out configuration.
	 * @param CoreConfig Configuration (config.yml) source file that contains core configuration information.
	 * @param outConfig Target file to save back to.
	 */
	
	public ConfigData(FileConfiguration CoreConfig,FileConfiguration outConfig){
		//core configuration is configuration that is Global.
		//we try to avoid these now. Normally the primary interest is the 
		//GriefPrevention.WorldConfigFolder setting.
		String DefaultConfigFolder = DataStore.dataLayerFolderPath + File.separator + "WorldConfigs" + File.separator;
		String DefaultTemplateFile = DefaultConfigFolder + "_template.cfg";
		//Configurable template file.
		TemplateFile = CoreConfig.getString("GriefPrevention.WorldConfig.TemplateFile",DefaultTemplateFile);
		if(!(new File(TemplateFile).exists())){
			TemplateFile = DefaultTemplateFile;
			
		}
		outConfig.set("GriefPrevention.WorldConfig.TemplateFile", TemplateFile);
		//check for appropriate configuration in given FileConfiguration. Note we also save out this configuration information.
		//configurable World Configuration folder.
		//save the configuration.
		
			
			

		
		
		WorldConfigLocation = CoreConfig.getString("GriefPrevention.WorldConfigFolder");
		if(WorldConfigLocation==null || WorldConfigLocation.length()==0){
			WorldConfigLocation = DefaultConfigFolder;
		}
		File ConfigLocation = new File(WorldConfigLocation);
		if(!ConfigLocation.exists()){
			//if not found, create the directory.
			GriefPrevention.instance.getLogger().log(Level.INFO, "mkdirs() on " + ConfigLocation.getAbsolutePath());
			ConfigLocation.mkdirs();
			
			
		}
		/*
		GriefPrevention.instance.getLogger().log(Level.INFO, "Reading WorldConfigurations from " + ConfigLocation.getAbsolutePath());
		if(ConfigLocation.exists() && ConfigLocation.isDirectory()){
			for(File lookfile: ConfigLocation.listFiles()){
				//System.out.println(lookfile);
				if(lookfile.isFile()){
					String Extension = lookfile.getName().substring(lookfile.getName().indexOf('.')+1);
					String baseName = Extension.length()==0?
							lookfile.getName():
							lookfile.getName().substring(0,lookfile.getName().length()-Extension.length()-1);
					if(baseName.startsWith("_")) continue; //configs starting with underscore are templates. Normally just _template.cfg.
					//if baseName is an existing world...
					if(Bukkit.getWorld(baseName)!=null){
						GriefPrevention.instance.getLogger().log(Level.INFO, "World " + baseName + " Configuration found.");
					}
					//read it in...
					GriefPrevention.AddLogEntry(lookfile.getAbsolutePath());
					FileConfiguration Source = YamlConfiguration.loadConfiguration(new File(lookfile.getAbsolutePath()));
					FileConfiguration Target = new YamlConfiguration();
					//load in the WorldConfig...
					WorldConfig wc = new WorldConfig(baseName,Source,Target);
					try {
						Target.save(lookfile);
						
					}catch(IOException iex){
						GriefPrevention.instance.getLogger().log(Level.SEVERE, "Failed to save to " + lookfile.getAbsolutePath());
					}
				
				
				}
			}
			
			
			
			
			
			
			
		}
		else if(ConfigLocation.exists() && ConfigLocation.isFile()){
			GriefPrevention.instance.getLogger().log(Level.SEVERE, "World Configuration Folder found, but it's a File. Double-check your GriefPrevention configuration files, and try again.");
			
		}
		
		*/
		
		
	}
	
	
}
