package me.ryanhamshire.GriefPrevention.Configuration;

import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
/**
 * represents the placement rules for a particular Claim Behaviour 'packet'. This is designed to allow for unneeded
 * flexibility later, or something.
 * @author BC_Programming
 *
 */
public class PlacementRules {
//above and below placement rules.
	private boolean AboveSeaLevel;
	private boolean BelowSeaLevel;
	/**
	 * returns whether this placement rule allows Action above sea level.
	 * @return
	 */
	public boolean getAboveSeaLevel(){ return AboveSeaLevel;}
	/**
	 * returns whether this placement rule allows Action below sea level.
	 * @return
	 */
	public boolean getBelowSeaLevel(){ return BelowSeaLevel;}
	public static PlacementRules AboveOnly = new PlacementRules(true,false);
	public static PlacementRules BelowOnly = new PlacementRules(false,true);
	public static PlacementRules Both = new PlacementRules(true,true);
	public static PlacementRules Neither = new PlacementRules(false,false);
	public PlacementRules(boolean Above,boolean Below){
		AboveSeaLevel = Above;
		BelowSeaLevel = Below;
	}
	/**
	 * constructs a new PlacementRules based on the settings in the given configuration file at the given Node, using specific defaults and
	 * a target Configuration to save the elements too.
	 * @param Source Source Configuration.
	 * @param Target Target Configuration to save to.
	 * @param NodePath Path to the Configuration Node to read from.
	 * @param Defaults instance containing Default settings to default to.
	 */
	public PlacementRules(FileConfiguration Source,FileConfiguration Target,String NodePath,PlacementRules Defaults){
		
		AboveSeaLevel = Source.getBoolean(NodePath + ".AboveSeaLevel",Defaults.AboveSeaLevel);
		BelowSeaLevel = Source.getBoolean(NodePath + ".BelowSeaLevel",Defaults.BelowSeaLevel);
		
		Target.set(NodePath + ".AboveSeaLevel", AboveSeaLevel);
		Target.set(NodePath + ".BelowSeaLevel", BelowSeaLevel);
		
		
		
	}
	/**
	 * determines if this Placementrule allows for the given location.
	 * @param Target
	 * @return
	 */
	public boolean Allow(Location Target){
		int SeaLevelofWorld = GriefPrevention.instance.getSeaLevel(Target.getWorld());
		boolean result =  (AboveSeaLevel && (Target.getBlockY() >= SeaLevelofWorld)) ||
				(BelowSeaLevel && (Target.getBlockY() < SeaLevelofWorld));
		//System.out.println("Block:" + Target.getBlockY() + " SeaLevel:" + SeaLevelofWorld + " Allow:" + result);
		return result;
		
	}
	
}
