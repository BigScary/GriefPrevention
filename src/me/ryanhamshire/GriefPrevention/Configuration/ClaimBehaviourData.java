package me.ryanhamshire.GriefPrevention.Configuration;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
//this enum is used for some of the configuration options.

//holds data pertaining to an option and where it works. 
//used primarily for information on explosions.
public class ClaimBehaviourData {
	private PlacementRules Wilderness;
	private PlacementRules Claims;
	
	private boolean OwnedClaim;
	/**
	 * returns whether, for applicable Claim allowances and for behaviours applicable for a player, that action
	 * can only be performed by the player that owns the claim.
	 * @return
	 */
	public boolean getOwnedClaim(){ return OwnedClaim;}
	
	public boolean Allowed(Location position){
		
		Claim testclaim = GriefPrevention.instance.dataStore.getClaimAt(position, true, null);
		//retrieve the appropriate Sea Level for this world.
		int sealevel = GriefPrevention.instance.getWorldCfg(position.getWorld()).seaLevelOverride();
		int yposition = position.getBlockY();
		boolean abovesealevel = yposition > sealevel;
		if(testclaim==null){
			//we aren't inside a claim.
			return Wilderness.Allow(position);
			
			
		}
		else{
			//we are inside a claim.
			return Claims.Allow(position);
		}
		
		
		
	}
	public PlacementRules getWildernessRules() { return Wilderness;}
	public PlacementRules getClaimsRules(){ return Claims;}
	
	
	public ClaimBehaviourData(FileConfiguration Source,FileConfiguration outConfig,String NodePath,ClaimBehaviourData Defaults){
		
		
		//we want to read NodePath.BelowSeaLevelWilderness and whatnot.
		//bases Defaults off another ClaimBehaviourData instance.
		Wilderness = new PlacementRules(Source,outConfig,NodePath + ".Wilderness",Defaults.getWildernessRules());
		Claims = new PlacementRules (Source,outConfig,NodePath + ".Claims",Defaults.getClaimsRules());
		
		this.OwnedClaim = Source.getBoolean(NodePath + ".OwnedClaim",Defaults.getOwnedClaim());
		//now save it to the given outConfig.
		
		outConfig.set(NodePath + ".OwnedClaim", OwnedClaim);
		
	}
	public ClaimBehaviourData(PlacementRules pWilderness,PlacementRules pClaims,
			boolean pOwnedClaim){
	Wilderness = pWilderness;
	Claims = pClaims;
	OwnedClaim = pOwnedClaim;
		
		
	}
	
	
	public static final ClaimBehaviourData OutsideClaims = new ClaimBehaviourData(PlacementRules.Both,PlacementRules.Neither,false);
	public static final ClaimBehaviourData InsideClaims = new ClaimBehaviourData(PlacementRules.Neither,PlacementRules.Neither,false);
	public static final ClaimBehaviourData AboveSeaLevel = new ClaimBehaviourData(PlacementRules.AboveOnly,PlacementRules.AboveOnly,false);
	public static final ClaimBehaviourData BelowSeaLevel = new ClaimBehaviourData(PlacementRules.BelowOnly,PlacementRules.BelowOnly,false);
	
	
}