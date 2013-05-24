package me.ryanhamshire.GriefPrevention.Configuration;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
//this enum is used for some of the configuration options.

//holds data pertaining to an option and where it works. 
//used primarily for information on explosions.
public class ClaimBehaviourData {
	private boolean BelowSeaLevelWilderness;
	private boolean BelowSeaLevelClaims;
	private boolean AboveSeaLevelWilderness;
	private boolean AboveSeaLevelClaims;
	
	public boolean Allowed(Location position){
		
		Claim testclaim = GriefPrevention.instance.dataStore.getClaimAt(position, true, null);
		//retrieve the appropriate Sea Level for this world.
		int sealevel = GriefPrevention.instance.getWorldCfg(position.getWorld()).seaLevelOverride();
		int yposition = position.getBlockY();
		boolean abovesealevel = yposition > sealevel;
		if(testclaim==null){
			//we aren't inside a claim.
			if(abovesealevel) return AboveSeaLevelWilderness; else return BelowSeaLevelWilderness;
			
			
		}
		else{
			//we are inside a claim.
			if(abovesealevel) return AboveSeaLevelClaims; else return BelowSeaLevelClaims;
		}
		
		
		
	}
	/**
	 * returns whether the specified element can act below sea level in the wilderness. (unclaimed areas).
	 * @return
	 */
	public boolean getBelowSeaLevelWilderness(){
		return BelowSeaLevelWilderness;
	}
	/**
	 * returns whether the specified element can act below sea level in claims.
	 * @return
	 */
	public boolean getBelowSeaLevelClaims(){
		return BelowSeaLevelClaims;
	}
	/**
	 * returns whether this element can act above SeaLevel in the wilderness. (unclaimed land)
	 * @return
	 */
	public boolean getAboveSeaLevelWilderness(){
		return BelowSeaLevelWilderness;
	}
	/**
	 * returns whether this element can act above sea level in claims.
	 * @return
	 */
	public boolean getAboveSeaLevelClaims(){
		return AboveSeaLevelClaims;
	}
	public ClaimBehaviourData(FileConfiguration Source,FileConfiguration outConfig,String NodePath,ClaimBehaviourData Defaults){
		
		
		//we want to read NodePath.BelowSeaLevelWilderness and whatnot.
		//bases Defaults off another ClaimBehaviourData instance.
		BelowSeaLevelWilderness = Source.getBoolean(NodePath + ".BelowSeaLevelWilderness",Defaults.BelowSeaLevelWilderness);
		BelowSeaLevelClaims = Source.getBoolean(NodePath +".BelowSeaLevelClaims",Defaults.BelowSeaLevelClaims);
		AboveSeaLevelWilderness = Source.getBoolean(NodePath + ".AboveSeaLevelWilderness",Defaults.AboveSeaLevelWilderness);
		AboveSeaLevelClaims = Source.getBoolean(NodePath + ".AboveSeaLevelClaims",Defaults.AboveSeaLevelClaims);
		//now save it to the given outConfig.
		outConfig.set(NodePath + ".BelowSeaLevelWilderness", BelowSeaLevelWilderness);
		outConfig.set(NodePath + ".BelowSeaLevelClaims", BelowSeaLevelClaims);
		outConfig.set(NodePath + ".AboveSeaLevelWilderness", AboveSeaLevelWilderness);
		outConfig.set(NodePath + ".AboveSeaLevelClaims",AboveSeaLevelClaims);
		
	}
	public ClaimBehaviourData(boolean pBelowSeaLevelWilderness,
			boolean pBelowSeaLevelClaims,
			boolean pAboveSeaLevelWilderness,
			boolean pAboveSeaLevelClaims){
		BelowSeaLevelWilderness = pBelowSeaLevelWilderness;
		BelowSeaLevelClaims = pBelowSeaLevelClaims;
		AboveSeaLevelWilderness = pAboveSeaLevelWilderness;
		AboveSeaLevelClaims = pAboveSeaLevelClaims;
	}
	
	public static final ClaimBehaviourData OutsideClaims = new ClaimBehaviourData(true,false,true,false);
	public static final ClaimBehaviourData InsideClaims = new ClaimBehaviourData(false,true,false,true);
	public static final ClaimBehaviourData AboveSeaLevel = new ClaimBehaviourData(false,false,true,true);
	public static final ClaimBehaviourData BelowSeaLevel = new ClaimBehaviourData(true,true,false,false);
	
	
}