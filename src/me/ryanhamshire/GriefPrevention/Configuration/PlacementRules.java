package me.ryanhamshire.GriefPrevention.Configuration;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData.ClaimAllowanceConstants;

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
	
	public enum BasicPermissionConstants{
		Deny,
		Allow;
		
		public boolean Allowed(){ return this==Allow;}
		public boolean Denied(){ return  !Allowed();}
		public static BasicPermissionConstants fromBoolean(boolean value){
			return value?Allow:Deny;
		}
		public static BasicPermissionConstants fromString(String Source){
			if(Source.equalsIgnoreCase(Boolean.TRUE.toString())) return Allow;
			if(Source.equalsIgnoreCase(Boolean.FALSE.toString())) return Deny;
			for(BasicPermissionConstants iterate:values()){
				if(iterate.name().equalsIgnoreCase(Source.trim())){
					return iterate;
				}
			}
			return null;
		}
	}
	
	private BasicPermissionConstants AboveSeaLevel;
	private BasicPermissionConstants BelowSeaLevel;
	/**
	 * returns whether this placement rule allows Action above sea level.
	 * @return
	 */
	public BasicPermissionConstants getAboveSeaLevel(){ return AboveSeaLevel;}
	/**
	 * returns whether this placement rule allows Action below sea level.
	 * @return
	 */
	public BasicPermissionConstants getBelowSeaLevel(){ return BelowSeaLevel;}
	public static PlacementRules AboveOnly = new PlacementRules(true,false);
	public static PlacementRules BelowOnly = new PlacementRules(false,true);
	public static PlacementRules Both = new PlacementRules(true,true);
	public static PlacementRules Neither = new PlacementRules(false,false);
	@Override
	public String toString(){
		if(AboveSeaLevel.Allowed() && BelowSeaLevel.Denied()) return "Only Above Sea Level";
		if(AboveSeaLevel.Denied() && BelowSeaLevel.Allowed()) return "Only Below Sea Level";
		if(AboveSeaLevel.Allowed() && BelowSeaLevel.Allowed()) return "Anywhere";
		return "Nowhere";
	}
	public PlacementRules(boolean Above,boolean Below){
		AboveSeaLevel = BasicPermissionConstants.fromBoolean(Above);
		BelowSeaLevel = BasicPermissionConstants.fromBoolean(Below);
	}
	public PlacementRules(BasicPermissionConstants Above,BasicPermissionConstants Below){
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
		
		String sAboveSeaLevel = Source.getString(NodePath + ".AboveSeaLevel",Defaults.AboveSeaLevel.name());
		String sBelowSeaLevel = Source.getString(NodePath + ".BelowSeaLevel",Defaults.BelowSeaLevel.name());
		AboveSeaLevel = BasicPermissionConstants.fromString(sAboveSeaLevel);
		BelowSeaLevel = BasicPermissionConstants.fromString(sBelowSeaLevel);
		if(AboveSeaLevel==null) AboveSeaLevel = Defaults.AboveSeaLevel;
		if(BelowSeaLevel==null) BelowSeaLevel = Defaults.BelowSeaLevel;
		Target.set(NodePath + ".AboveSeaLevel", AboveSeaLevel.name());
		Target.set(NodePath + ".BelowSeaLevel", BelowSeaLevel.name());
		
		
		
	}
	/**
	 * determines if this Placementrule allows for the given location.
	 * @param Target
	 * @return
	 */
	public boolean Allow(Location Target){
		int SeaLevelofWorld = GriefPrevention.instance.getSeaLevel(Target.getWorld());
		boolean result =  (AboveSeaLevel.Allowed() && (Target.getBlockY() >= SeaLevelofWorld)) ||
				(BelowSeaLevel.Allowed() && (Target.getBlockY() < SeaLevelofWorld));
		//System.out.println("Block:" + Target.getBlockY() + " SeaLevel:" + SeaLevelofWorld + " Allow:" + result);
		return result;
		
	}
	
}
