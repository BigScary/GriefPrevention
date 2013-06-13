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
	public static Pattern ModBlockNamePattern = null;
	public static Pattern OreBlockPattern = null;
	public static Pattern AccessPattern = null;
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

		if (ModBlockNamePattern == null)
			ModBlockNamePattern = Pattern
					.compile(GriefPrevention.instance.ModdedBlockRegexPattern,Pattern.CASE_INSENSITIVE);
		if (OreBlockPattern == null)
			OreBlockPattern = Pattern
					.compile(GriefPrevention.instance.OreBlockRegexPattern,Pattern.CASE_INSENSITIVE);
		if(AccessPattern == null)
			AccessPattern = Pattern.compile(GriefPrevention.instance.AccessRegexPattern,Pattern.CASE_INSENSITIVE);
		// open this file. Well, if it doesn't exist, give up.
		File grabfile = new File(CfgFile);
		if (!grabfile.exists())
			return;
		try {
			Scanner sc = new Scanner(grabfile);
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
				} else if (inblocksection) {
					// I:idBlockTieredTreasureChest=3381
					// I:BlockChestHungry=2410
					// if there is a colon, strip off everything up to the char
					// after it.
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
					
					ItemName = SeparateCamelCase(ItemName);
					//GriefPrevention.AddLogEntry("ItemName:" + ItemName + " ID:" + useID);
					ModdedNameLookup.put(useID, ItemName);
					// we want to tag Items that contain secret code words.
					// specifically.
					if (ModBlockNamePattern.matcher(ItemName).find()) {
						// we want to create a new MaterialInfo.
						MaterialInfo mi = new MaterialInfo(useID, (byte) 0,
								ItemName);
						mi.allDataValues = true;
						ResultStore.FoundContainers.add(mi);
						GriefPrevention.AddLogEntry("Found Container Match:"
								+ ItemName + " With ID:" + useID);

					} else if (OreBlockPattern.matcher(ItemName).find()) {
						MaterialInfo mi = new MaterialInfo(useID, (byte) 0,
								ItemName);
						mi.allDataValues = true;

						ResultStore.FoundOres.add(mi);
						GriefPrevention.AddLogEntry("Found Ore Match:"
								+ ItemName + " With ID:" + useID);
					}
					else if(AccessPattern.matcher(ItemName).find()){
						MaterialInfo mi = new MaterialInfo(useID, (byte) 0,
								ItemName);
						mi.allDataValues = true;

						ResultStore.FoundAccess.add(mi);
						GriefPrevention.AddLogEntry("Found Access Match:"
								+ ItemName + " With ID:" + useID);
					}
				}

			}
			sc.close();
		} catch (IOException exx) {

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
				else if(iterate.getName().endsWith(".cfg") || iterate.getName().endsWith(".conf")){
					results.add(iterate.getAbsolutePath());
				}
				
				
				
			}
			
			GriefPrevention.AddLogEntry("Config Search in " + SourcePath + " found " + results.size() + " Files.");
			return results;
		}
		return new ArrayList<String>();
	}
	
	
	
	
}
