package me.ryanhamshire.GriefPrevention.Configuration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

/**
 * When it comes to handling extra information on a per-claim basis,
 * the current method appears to rest on simply leaving it to the plugins.
 * My intention here is to provide a useful framework that makes saving and loading
 * extra data by dependent plugins on a per-claim basis much easier.
 * The envisions idea:
 * GriefPrevention.instance.getClaimMeta(Claim,plugin) would retrieve the ClaimMeta 
 * for the given claim, of the given plugin.
 * 
 * @author BC_Programming
 * 
 *
 */
public class ClaimMetaHandler {

	//handles claim metadata setting and retrieval.
	//Claim meta is retrieved and in the form of a FileConfiguration.
	//Metadata is found in dataLayerFolderPath + "/ClaimMeta/PluginName/ClaimID-SubID.yml"
	
	//outer hashmap indexes by the Plugin Name; the value of that is another HashMap that indexes by a string consisting
	//if the Claim, a hyphen, and a subclaim id. if this is the top-level claim we are dealing with, there will
	//be no hyphen.
	private HashMap<String,HashMap<String,FileConfiguration>> MetaData =
			new HashMap<String,HashMap<String,FileConfiguration>>();
	private String MetaFolder ="";
	/**
	 * constructs default MetaHandler instance.
	 */
	public ClaimMetaHandler(){
		this(GriefPrevention.instance.dataStore.dataLayerFolderPath + "/ClaimMeta/");
	}
	/**
	 * constructs a MetaHandler for the given source path.
	 * @param SourcePath
	 */
	public ClaimMetaHandler(String SourcePath){
		MetaFolder = SourcePath;
	}
	public String getClaimTag(Claim c){
		if(c.parent!=null)
			return c.parent.getID() + "-" + String.valueOf(c.getSubClaimID());
		else 
		    return String.valueOf(c.getID());
	}
	public Claim getClaimFromTag(String tag){
		String[] splitval = tag.split("-");
		if(splitval.length==1)
			return GriefPrevention.instance.dataStore.getClaim(Long.parseLong(splitval[0]));
		else {
			
			long parentID = Long.parseLong(splitval[0]);
			long subid = Long.parseLong(splitval[1]);
			Claim parentclaim = GriefPrevention.instance.dataStore.getClaim(parentID);
			if(parentclaim==null) return null;
			
			return parentclaim.getSubClaim(subid);
			
		}
		
	}
	/**
	 * retrieves the metadata for the given claim for the given Plugin Key.
	 * @param PluginKey Unique key for your Plugin. The Plugin Name is usually sufficient.
	 * @param c Claim to get meta for.
	 * @return a FileConfiguration of metadata for that Claim. This will be empty if it is not set. When you make the needed changes,
	 * pass the resulting changed FileConfiguration to setClaimMeta() to save it back.
	 */
	public FileConfiguration getClaimMeta(String PluginKey,Claim c){
		String useclaimkey=null;
		if(c.parent==null) useclaimkey = String.valueOf(c.getID());
		if(c.parent!=null) useclaimkey = String.valueOf(getClaimTag(c));
		return getClaimMeta(PluginKey,useclaimkey);
	}
	/**
	 * retrieves a list of All Meta Keys currently
	 * registered.
	 * @return
	 */
	public List<String> getMetaPluginKeys(){
		String LookFolder = MetaFolder +"/";
		//retrieve all Directories in this folder.
		File di = new File(LookFolder);
		if(!di.exists()){
			return new ArrayList<String>(); //return empty list.
		}
		else {
			ArrayList<String> resultkeys = new ArrayList<String>();
			//directory does exist. Each plugin meta is a folder.
			for(File iterate:di.listFiles()){
				if(iterate.isDirectory()){
					String pluginname = iterate.getName();
					//it's a plugin key.
					resultkeys.add(pluginname);
					
				}
			}
			
			return resultkeys;	
		}
	}
	public List<Claim> getClaimsForKey(String PluginKey){
		
		File PluginFolder = new File(MetaFolder + "/" + PluginKey + "/");
		if(!PluginFolder.exists()) return new ArrayList<Claim>();
		else {
			ArrayList<Claim> resultvalue = new ArrayList<Claim>();
			//if the folder does exist, read each one. return it as a list.
			if(PluginFolder.isDirectory()){
				for(File iteratefile:PluginFolder.listFiles()){
					if(iteratefile.getName().toUpperCase().endsWith(".YML")){
						String basename = iteratefile.getName().substring(0, iteratefile.getName().lastIndexOf(".")+1);
						Claim gotfromtag = getClaimFromTag(basename);
						if(gotfromtag!=null){resultvalue.add(gotfromtag);}
					}
				}
				
				
			}
			return resultvalue;
		}
		
		
		
		
	}
	//retrieves the name of the appropriate claim file, making sure that the path exists.
	private String getClaimMetaFile(String PluginKey,String ClaimKey){
		String PluginFolder = MetaFolder + "/" + PluginKey;
		//make sure that directory exists...
		File pfolder = new File(PluginFolder);
		if(!pfolder.exists()){
			pfolder.mkdirs();
		}
		
		//now add the Claim Key to the path.
		String ClaimMeta = PluginFolder + "/" + ClaimKey + ".yml";
		return ClaimMeta;
	}

	/**
	 * sets the ClaimMeta data of a given claim for the given PluginKey to a given FileConfiguration.
	 * @param PluginKey  Unique Key of your Plugin. The Plugin Name is usually sufficient.
	 * @param c Claim to get meta for.
	 * 
	 */
	public void setClaimMeta(String PluginKey,Claim c,FileConfiguration result){
		String useclaimkey=null;
		if(c.parent==null) useclaimkey = String.valueOf(c.getID());
		if(c.parent!=null) useclaimkey = String.valueOf(c.parent.getID()) + "-" + String.valueOf(c.getSubClaimID());
		setClaimMeta(PluginKey,useclaimkey,result);
	}
	public void setClaimMeta(String PluginKey,String ClaimKey,FileConfiguration result){
		String ClaimMeta = getClaimMetaFile(PluginKey,ClaimKey);
		try {result.save(ClaimMeta);
		}
		catch(IOException iox){
		GriefPrevention.instance.getLogger().log(Level.SEVERE, "Failed to save Claim Meta to file," + ClaimMeta);
		iox.printStackTrace();
		}
		
	}
	private FileConfiguration getClaimMeta(String PluginKey,String ClaimKey){
		//find the Plugin key folder.
		String ClaimMeta = getClaimMetaFile(PluginKey,ClaimKey);
		//if the file exists...
		if(new File(ClaimMeta).exists()){
			return YamlConfiguration.loadConfiguration(new File(ClaimMeta));
		}
		else {
			//no file. SAD FACE.
			//return a new configuration.
			return new YamlConfiguration();
		}
		
	}
	
	
	
	
}
