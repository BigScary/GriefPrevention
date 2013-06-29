package me.ryanhamshire.GriefPrevention.Configuration;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.MaterialCollection;
import me.ryanhamshire.GriefPrevention.MaterialInfo;

public class ModdedBlocksSearchResults {
    //stores the search result from uh... searching- for container block keywords.
	public MaterialCollection FoundContainers = new MaterialCollection();
	public MaterialCollection FoundOres = new MaterialCollection();
	public MaterialCollection FoundAccess = new MaterialCollection();
	public static void saveMaterialList(List<MaterialInfo> source,String NodePath,FileConfiguration Target){
		
		List<String> buildlist = new ArrayList<String>();
		for(MaterialInfo iterate:source){
			buildlist.add(iterate.toString());
			
		}
		
		Target.set(NodePath, buildlist);
		
	}
	public MaterialCollection readMaterialList(List<MaterialInfo> source,String NodePath,FileConfiguration Source){
		MaterialCollection readList = new MaterialCollection();
		List<String> AcquiredStrings = Source.getStringList(NodePath);
		
		if(AcquiredStrings==null) return null;
		
		GriefPrevention.instance.parseMaterialListFromConfig(AcquiredStrings, readList);
		return readList;
	}
	
}
