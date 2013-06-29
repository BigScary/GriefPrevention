package me.ryanhamshire.GriefPrevention.Configuration;

import java.util.ArrayList;
import java.util.List;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData.ClaimAllowanceConstants;

import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import me.ryanhamshire.GriefPrevention.TextMode;
public class ItemUsageRules {

	private String Description;
	public String getDescription(){ return Description;}
	private List<Integer> ItemIDs;
	public List<Integer> getItemIDs(){ return ItemIDs;}
	private BlockPlacementRules PlayerLocationRules;
	private BlockPlacementRules TargetLocationRules;
	public BlockPlacementRules getPlayerLocationRules(){ return PlayerLocationRules;}
	public BlockPlacementRules getTargetLocationRules(){ return TargetLocationRules;}
	
	public ItemUsageRules(String pDescription,int[] pItemIDs,
			BlockPlacementRules pPlayerLocation,BlockPlacementRules pTargetLocation){
		Description = pDescription;
		PlayerLocationRules = pPlayerLocation;
		TargetLocationRules = pTargetLocation;
		ItemIDs = new ArrayList<Integer>();
		for(int iterate:pItemIDs){ItemIDs.add(iterate);}
	}
	public boolean Applicable(ItemStack testItem){
		return ItemIDs.contains(testItem.getTypeId());
	}
	public ClaimAllowanceConstants TestPlayer(Player p,Block Target){
		ClaimAllowanceConstants PlayerLoc = PlayerLocationRules.checkPlacement(p.getLocation(), p);
		ClaimAllowanceConstants TargetLoc = TargetLocationRules.checkPlacement(Target.getLocation(),p);
		if(PlayerLoc.Denied()){
			GriefPrevention.sendMessage(p, TextMode.Err, "You may not use that Item Here.");
			return ClaimAllowanceConstants.Deny;
		}
		else if(TargetLoc.Denied()){
			GriefPrevention.sendMessage(p, TextMode.Err, "You may not use that Item There.");
			return ClaimAllowanceConstants.Deny;
		}
		
		return ClaimAllowanceConstants.Allow;
		
	}
	public List<ItemUsageRules> ReadRules(String BaseNode,FileConfiguration Source,FileConfiguration Target){
		ArrayList<ItemUsageRules> BuildList = new ArrayList<ItemUsageRules>();
		ConfigurationSection grabsection = Source.getConfigurationSection(BaseNode);
		
		for(String iterate:grabsection.getKeys(false)){
			ItemUsageRules newelement = new ItemUsageRules(BaseNode + "." + iterate,Source,Target);
			BuildList.add(newelement);
		}
		return BuildList;
		
	}
	public ItemUsageRules(String NodePath,FileConfiguration Source,FileConfiguration Target){
		
		List<String> Items = Source.getStringList(NodePath + ".IDs");
		
		ItemIDs = new ArrayList<Integer>();
		for(String iterate:Items){
			ItemIDs.add(Integer.parseInt(iterate));
			
		}
		Target.set(NodePath + ".IDs", Items);
		PlayerLocationRules = new BlockPlacementRules("Item Use at Player Location",Source,Target,NodePath + ".PlayerLocation");
		TargetLocationRules = new BlockPlacementRules("Item Use Target Location",Source,Target,NodePath + ".TargetLocation");
		Description = Source.getString(NodePath + ".Description","");
		
		
		
		
	}
	
	
}
