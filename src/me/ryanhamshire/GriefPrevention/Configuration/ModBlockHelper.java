package me.ryanhamshire.GriefPrevention.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import java.util.List;
import java.util.regex.Pattern;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.MaterialInfo;

/**
 * 
 * @author BC_Programming
 * Helper class designed to try to scour for certain Mod config files on modded installations.
 * 
 */
public class ModBlockHelper {
	//search path for mod installation config folder.
	
	public final static String moddedConfigPath = "plugins" + File.separator + "../config/";
	private static List<String> CachedConfigs = null;
	private static HashMap<Integer,String> ModdedNameLookup = new HashMap<Integer,String>(); //lookup of Modded Item IDs to Modded Item Names where applicable. usually for blocks.
	public static ModdedBlocksSearchResults ScanCfgs(){
		//this class is used by WorldConfig, If the core setting
		//'AutoScanModCfg' is set to true.
		ModdedBlocksSearchResults resultsFound = new ModdedBlocksSearchResults();
		//go through all the cfgs. 
		if(CachedConfigs==null){
			CachedConfigs = new ArrayList<String>();
			CachedConfigs = FindConfigFiles(moddedConfigPath);
			
		}
		for(String lookcfg:CachedConfigs){
			AddSearchResults(lookcfg,resultsFound);
		}
		return resultsFound;
	}
	private static boolean containsWord(String test,String[] testwords){
		String[] words = test.split(" ");
		for(String lookword:words){
			for(String compareagainst:testwords){
				if(compareagainst.equalsIgnoreCase(lookword)) return true;
			}
		}
		return false;
		
		
	}
	private static String SeparateCamelCase(String strSource){
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<strSource.length();i++){
			char currchar = strSource.charAt(i);
			if(Character.isUpperCase(currchar)){
				sb.append(" ");
				
			}
			sb.append(currchar);
			
		}
		return sb.toString();
		
		
		
	}

	private static void AddSearchResults(String CfgFile,
			ModdedBlocksSearchResults ResultStore) {

		boolean inblocksection = false;

		//GriefPrevention.AddLogEntry("Searching Mod Config:" + CfgFile);
	
		// open this file. Well, if it doesn't exist, give up.
		File grabfile = new File(CfgFile);
		if (!grabfile.exists())
			return;
		Scanner sc=null;
		try {
			sc = new Scanner(grabfile);
			while (sc.hasNext()) {
				// read in a line.
				String lineread = sc.nextLine();
				if (lineread == null)
					continue;
				lineread = lineread.trim();
				// parse...
				// does it start with block, and end with a {?
				if (lineread.startsWith("block") && lineread.endsWith("{")) {
					// yep! activate inblocksection.
					inblocksection = true;
					continue;
				}
				else if (inblocksection && lineread.startsWith("}"))
				{
					//end of the block section.
					inblocksection=false;
					break;
				} else if (inblocksection) {
					// I:idBlockTieredTreasureChest=3381
					// I:BlockChestHungry=2410
					// if there is a colon, strip off everything up to the char
					// after it.
					if(lineread.contains("I:")) lineread = lineread.substring(lineread.indexOf("I:")+2);
					if(lineread.contains("B:")) lineread = lineread.substring(lineread.indexOf("B:")+2);
					if (lineread.contains(":")) {
						lineread.substring(lineread.indexOf(":") + 1);
					}
					// split at = sign. if there isn't one, just continue.
					if (!lineread.contains("="))
						continue;
					String[] splittered = lineread.split("=");
					String ItemName = splittered[0];
					
					int useID=0;
					String ItemID=null;
					try {
					ItemID = splittered[1];
					useID = Integer.parseInt(ItemID);
					}
					catch(Exception numformat){
						continue;
					}
					
					ItemName = SeparateCamelCase(ItemName.replace(".", " ")) + " ";
					//GriefPrevention.AddLogEntry("ItemName:" + ItemName + " ID:" + useID);
					ModdedNameLookup.put(useID, ItemName);
					// we want to tag Items that contain secret code words.
					// specifically.
					if(GriefPrevention.instance.ModdedBlockRegexHelper.match(ItemName)) {
						// we want to create a new MaterialInfo.
						MaterialInfo mi = new MaterialInfo(useID, (byte) 0,
								ItemName);
						mi.allDataValues = true;
						ResultStore.FoundContainers.add(mi);
						GriefPrevention.AddLogEntry("Found Container Match:"
								+ ItemName + " With ID:" + useID);

					} 
					else if (GriefPrevention.instance.OreBlockRegexHelper.match(ItemName)) {
						MaterialInfo mi = new MaterialInfo(useID, (byte) 0,
								ItemName);
						mi.allDataValues = true;

						ResultStore.FoundOres.add(mi);
						GriefPrevention.AddLogEntry("Found Ore Match:"
								+ ItemName + " With ID:" + useID);
					}
					else if(GriefPrevention.instance.AccessRegexPattern.match(ItemName)){
						MaterialInfo mi = new MaterialInfo(useID, (byte) 0,
								ItemName);
						mi.allDataValues = true;

						ResultStore.FoundAccess.add(mi);
						GriefPrevention.AddLogEntry("Found Access Match:"
								+ ItemName + " With ID:" + useID);
					}
				}
			

			}
			
		} catch (IOException exx) {
			
		}
		finally{
			if(sc!=null) sc.close();
		}
		

	}
	
	private static List<String> FindConfigFiles(String SourcePath){
		//try to find all .cfg files.
		ArrayList<String> results = new ArrayList<String>();
		File f = new File(SourcePath);
		if(f.exists()){
			for(File iterate:f.listFiles()){
				if(iterate.isDirectory()){
					for(String addresult:FindConfigFiles(iterate.getAbsolutePath())){
						results.add(addresult);
					}
					
				}
				else if(iterate.getName().toLowerCase().endsWith(".cfg") || iterate.getName().toLowerCase().endsWith(".conf")){
					GriefPrevention.AddLogEntry("Found cfg File:" + iterate.getAbsolutePath());
					results.add(iterate.getAbsolutePath());
				}
				
				
				
			}
			
			GriefPrevention.AddLogEntry("Config Search in " + SourcePath + " found " + results.size() + " Files.");
			return results;
		}
		return new ArrayList<String>();
	}
	
	
	
	
}
