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
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * holds configuration data global to GP as well as 
 * providing accessors to retrieve/create configurations for individual worlds.
 * @author BC_Programming
 *
 */
public class ConfigData {

	private String TemplateFile;
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
			if(wcon.creative_rules())
				buildList.add(wcon);
		}
		return buildList;
		
	}
	public WorldConfig getWorldConfig(World forWorld){
		return getWorldConfig(forWorld.getName());
	}
	public WorldConfig getWorldConfig(String worldName){
		World grabfor =null;
		if((grabfor = Bukkit.getWorld(worldName))==null){
			GriefPrevention.instance.getLogger().log(Level.SEVERE, "invalid World:" + worldName);
		}
		
		
		
		if(!WorldCfg.containsKey(worldName)){
			//special code: it's possible a configuration might already exist for this file, so we'll
			//check 
			String checkyamlfile = DataStore.dataLayerFolderPath + File.separator + "WorldConfigs/" + worldName + ".cfg";
			
			if(new File(checkyamlfile).exists()){
				YamlConfiguration existingcfg = YamlConfiguration.loadConfiguration(new File(checkyamlfile));
				YamlConfiguration outConfiguration = new YamlConfiguration();
				WorldCfg.put(worldName,new WorldConfig(worldName,existingcfg,outConfiguration));
				try {outConfiguration.save(new File(checkyamlfile));}
				catch(IOException iex){
					GriefPrevention.instance.getLogger().log(Level.SEVERE,"Failed to save World Config for world " + worldName);
					
				}
			}
			else {
				//set the input Yaml to default to the template.
				FileConfiguration useSource= (new File(TemplateFile).exists()?
						YamlConfiguration.loadConfiguration(new File(TemplateFile)):
							new YamlConfiguration());
				
				FileConfiguration Target = new YamlConfiguration();
				
				WorldCfg.put(worldName, new WorldConfig(grabfor.getName(),useSource,Target));
				try {
					Target.save(new File(checkyamlfile));
				}catch(IOException ioex){
					GriefPrevention.instance.getLogger().log(Level.SEVERE, "Failed to write world configuration to " + checkyamlfile);
				}
				
				}
				
			
			//save target
			
			
			
			}
		
		return WorldCfg.get(worldName);
		
	}
	public static FileConfiguration createTargetConfiguration(String sName){
		return YamlConfiguration.loadConfiguration(new File(sName));
	}
	public static String getWorldConfigLocation(String sName){
		
		return DataStore.dataLayerFolderPath + File.separator + "WorldConfigs/" + sName + ".cfg";
	}
	public ConfigData(FileConfiguration CoreConfig,FileConfiguration outConfig){
		//core configuration is configuration that is Global.
		//we try to avoid these now. Normally the primary interest is the 
		//GriefPrevention.WorldConfigFolder setting.
		String DefaultConfigFolder = DataStore.dataLayerFolderPath + File.separator + "WorldConfigs" + File.separator;
		String DefaultTemplateFile = DefaultConfigFolder + "_template.cfg";
		
		TemplateFile = CoreConfig.getString("GriefPrevention.WorldConfig.TemplateFile",DefaultTemplateFile);
		if(!(new File(TemplateFile).exists())){
			TemplateFile = DefaultTemplateFile;
			
		}
		outConfig.set("GriefPrevention.WorldConfig.TemplateFile", TemplateFile);
		//check for appropriate configuration in given FileConfiguration. Note we also save out this configuration information.
		String ConfigFolder = CoreConfig.getString("GriefPrevention.WorldConfigFolder");
		if(ConfigFolder==null || ConfigFolder.length()==0){
			ConfigFolder = DefaultConfigFolder;
		}
		File ConfigLocation = new File(ConfigFolder);
		if(!ConfigLocation.exists()){
			//if not found, create the directory.
			GriefPrevention.instance.getLogger().log(Level.INFO, "mkdirs() on " + ConfigLocation.getAbsolutePath());
			ConfigLocation.mkdirs();
			
			
		}
		GriefPrevention.instance.getLogger().log(Level.INFO, "Reading WorldConfigurations from " + ConfigLocation.getAbsolutePath());
		if(ConfigLocation.exists() && ConfigLocation.isDirectory()){
			for(File lookfile: ConfigLocation.listFiles()){
				
				String Extension = lookfile.getName().substring(lookfile.getName().indexOf('.')+1);
				String baseName = lookfile.getName().substring(0,lookfile.getName().length()-Extension.length()-1);
				if(baseName.startsWith("_")) continue; //configs starting with underscore are templates. Normally just _template.cfg.
				//if baseName is an existing world...
				if(Bukkit.getWorld(baseName)!=null){
					GriefPrevention.instance.getLogger().log(Level.INFO, "World " + baseName + " Configuration found.");
				}
				//read it in...
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
		else if(ConfigLocation.exists() && ConfigLocation.isFile()){
			GriefPrevention.instance.getLogger().log(Level.SEVERE, "World Configuration Folder found, but it's a File. Double-check your GriefPrevention configuration files, and try again.");
			
		}
		
		
		
		
	}
	
	
}
