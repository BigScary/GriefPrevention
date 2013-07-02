package me.ryanhamshire.GriefPrevention.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimDistanceResult;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.MaterialCollection;
import me.ryanhamshire.GriefPrevention.MaterialInfo;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;
//class that encapsulates the information from the BlockPlacementClaimDistance listing in the configuration.
//BlockPlacementClaimDistance:
//- 715:*:15
//Format: ID:Data,Distance
//prevents placing blocks with that ID or Data within the given distance.
public class BlockPlacementRules {

	private int MinDistance;
	private ClaimBehaviourData ClaimBehaviour;
	/**
	 * 
	 * @return ClaimBehaviourData associated with this BlockPlacementRules instance.
	 */
	public ClaimBehaviourData getClaimBehaviour(){ return ClaimBehaviour;}
	private MaterialCollection Materials = null;
	public MaterialCollection getMaterials(){ return Materials;}
	public int getMinDistance(){ return MinDistance;}
	
	public static List<BlockPlacementRules> ParseRules(FileConfiguration Source, FileConfiguration Target,String NodePath){
		
		ArrayList<BlockPlacementRules> Result = new ArrayList<BlockPlacementRules>();
		ConfigurationSection gotsection = Source.getConfigurationSection(NodePath);
		if(gotsection==null) return null;
		if(gotsection.getKeys(false).size()==0) return null;
		//retrieve all the section names.
		Set<String> sectionnames = gotsection.getKeys(false);
		for(String loopname:sectionnames){
			Result.add(new BlockPlacementRules(loopname,Source,Target,NodePath + "." + loopname));
		}
		
		return Result;
		
	}
	//returns whether this BlockPlacementRule effects the passed material.
	public boolean inEffect(Material m){
		//check our MaterialCollection.
		return Materials.contains(m);
		
		
	}
	public ClaimBehaviourData.ClaimAllowanceConstants checkPlacement(Location position,Player p){
		
		//
		//if .MinClaimDistance is true, determine distance to nearest claim.
		//if the player has Build trust in that claim, however, allow it.
		if(this.MinDistance>0){
			ClaimDistanceResult cdr = GriefPrevention.instance.dataStore.getNearestClaim(position, this.MinDistance);
			if(p!=null){
				GriefPrevention.sendMessage(p, TextMode.Err, Messages.BlockPlacementTooClose,String.valueOf(this.MinDistance));
				return ClaimBehaviourData.ClaimAllowanceConstants.Deny;
			}
		}
		
		return ClaimBehaviour.Allowed(position,p);
	}
	
	public BlockPlacementRules(String RuleName,FileConfiguration Source,FileConfiguration Target,String NodePath){
	
		/*
		 * GriefPrevention:
		 *     BlockPlacementRules:
		 *         Redstone:
		 *             IDs: [55,76,93,150,331,356,404]
		 *             MinClaimDistance : 16
		 *             Rules:
		 *                 Wilderness:
			                   AboveSeaLevel: Deny
			                   BelowSeaLevel: Deny
			               Claims:
                               AboveSeaLevel: Deny
                               BelowSeaLevel: Deny
                               ClaimControl: Disabled
		 */
		
		List<String> getlist = Source.getStringList(NodePath + ".IDs");
		List<String> buildlist = new ArrayList<String>();
		Materials = new MaterialCollection();
		for(String iterate:getlist){
			MaterialInfo acquired = MaterialInfo.fromString(iterate);
			if(acquired!=null){
				Materials.add(acquired);
				buildlist.add(acquired.toString());
			}
		}
		
		Target.set(NodePath + ".IDs", buildlist);
		
		
		
		MinDistance = Source.getInt(NodePath + ".MinClaimDistance",0);
		Target.set(NodePath + ".MinClaimDistance", MinDistance);
		this.ClaimBehaviour = new ClaimBehaviourData(NodePath + ".Rules",Source,Target, NodePath + ".Rules",ClaimBehaviourData.getAll(NodePath + ".Rules"));
		
		
		
		
		
	}
	
	
}
