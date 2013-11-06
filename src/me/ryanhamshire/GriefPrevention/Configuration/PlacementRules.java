package me.ryanhamshire.GriefPrevention.Configuration;

import java.util.ArrayList;
import java.util.List;

import me.ryanhamshire.GriefPrevention.Debugger;
import me.ryanhamshire.GriefPrevention.Debugger.DebugLevel;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * represents the placement rules for a particular Claim Behaviour 'packet'.
 * This is designed to allow for unneeded flexibility later, or something.
 * 
 * @author BC_Programming
 * 
 */
public class PlacementRules {
	// above and below placement rules.

	public enum SeaLevelOverrideTypes
	{
		None,
		Offset,
		Absolute
	}
	
	public enum BasicPermissionConstants {
		Allow,Deny,Force_Allow, Force_Deny;

		public static BasicPermissionConstants fromBoolean(boolean value) {
			return value ? Allow : Deny;
		}

		public static BasicPermissionConstants fromString(String Source) {
			if (Source.equalsIgnoreCase(Boolean.TRUE.toString()))
				return Allow;
			if (Source.equalsIgnoreCase(Boolean.FALSE.toString()))
				return Deny;
			for (BasicPermissionConstants iterate : values()) {
				if (iterate.name().equalsIgnoreCase(Source.trim())) {
					return iterate;
				}
			}
			return null;
		}

		public boolean Allowed() {
			return this == Allow || this==Force_Allow;
		}

		public boolean Denied() {
			return !Allowed();
		}
	}

	
	
	public static PlacementRules AboveOnly = new PlacementRules(true, false);
	public static PlacementRules BelowOnly = new PlacementRules(false, true);

	public static PlacementRules Both = new PlacementRules(true, true);

	public static PlacementRules Neither = new PlacementRules(false, false);
    
	
	
	
	private BasicPermissionConstants AboveSeaLevel;
	private BasicPermissionConstants BelowSeaLevel;
	private List<String> RequiredPermissions = new ArrayList<String>();
	private int SeaLevelOffset = 0;
	/**
	 * whether players that would otherwise be denied the action can
	 * 
	 */

	public PlacementRules setSeaLevelOffset(SeaLevelOverrideTypes useType,int value){
		SeaLevelType = useType;
		SeaLevelOffset = value;
		return this;
	}
	public int getSeaLevelOffset() { return SeaLevelOffset;}
	private SeaLevelOverrideTypes SeaLevelType;
	public SeaLevelOverrideTypes getSeaLevelType(){ return SeaLevelType;}
	public String getSeaLevelOffsetString(){
		
		if(SeaLevelType==SeaLevelOverrideTypes.Offset){
			return ((SeaLevelOffset>0)?"+":"-") + String.valueOf(Math.abs(SeaLevelOffset)).trim();
		}
		else if(SeaLevelType==SeaLevelOverrideTypes.Absolute){
			return String.valueOf(SeaLevelOffset).trim();
		}
		else {return "0";}
	}
	public PlacementRules(BasicPermissionConstants Above, BasicPermissionConstants Below) {
		AboveSeaLevel = Above;
		BelowSeaLevel = Below;
	}

	public PlacementRules(boolean Above, boolean Below) {
		AboveSeaLevel = BasicPermissionConstants.fromBoolean(Above);
		BelowSeaLevel = BasicPermissionConstants.fromBoolean(Below);
	}


	/**
	 * constructs a new PlacementRules based on the settings in the given
	 * configuration file at the given Node, using specific defaults and a
	 * target Configuration to save the elements too.
	 * 
	 * @param Source
	 *            Source Configuration.
	 * @param Target
	 *            Target Configuration to save to.
	 * @param NodePath
	 *            Path to the Configuration Node to read from.
	 * @param Defaults
	 *            instance containing Default settings to default to.
	 */
	public PlacementRules(FileConfiguration Source, FileConfiguration Target, String NodePath, PlacementRules Defaults) {

		String sAboveSeaLevel = Source.getString(NodePath + ".AboveSeaLevel", Defaults.AboveSeaLevel.name());
		String sBelowSeaLevel = Source.getString(NodePath + ".BelowSeaLevel", Defaults.BelowSeaLevel.name());
		AboveSeaLevel = BasicPermissionConstants.fromString(sAboveSeaLevel);
		BelowSeaLevel = BasicPermissionConstants.fromString(sBelowSeaLevel);
		if (AboveSeaLevel == null)
			AboveSeaLevel = Defaults.AboveSeaLevel;
		if (BelowSeaLevel == null)
			BelowSeaLevel = Defaults.BelowSeaLevel;
		Target.set(NodePath + ".AboveSeaLevel", AboveSeaLevel.name());
		Target.set(NodePath + ".BelowSeaLevel", BelowSeaLevel.name());
		if (Source.contains(NodePath + ".RequiredPermissions")) {
			this.RequiredPermissions = Source.getStringList(NodePath + ".RequiredPermissions");
		} else {
			// System.out.println("RequiredPerms setting to item with " +
			// Defaults.RequiredPermissions.size() + " Elements.");
			RequiredPermissions = Defaults.RequiredPermissions;
		}
		// if it's null, no worries; just initialize to an empty list.
		if (RequiredPermissions == null)
			RequiredPermissions = new ArrayList<String>();
		// if it contains elements, save the list.
		if (RequiredPermissions.size() > 0) {
			Target.set(NodePath + ".RequiredPermissions", RequiredPermissions);
		}
		//attempt to read SeaLevel override value.
		//parse rules: if it starts with a + or -, it's an offset. if it's a plain value, it's an absolute sea level,
		String sealevelvalue = Source.getString(NodePath + ".SeaLevel",Defaults.getSeaLevelOffsetString());
		//System.out.println("Sealevel value:" + sealevelvalue);
		//if it starts with a + or a -, it's an offset.
		
		if(sealevelvalue.startsWith("+") || sealevelvalue.startsWith("-")){
			//offset.
			
			try {
				this.SeaLevelOffset = Integer.parseInt(sealevelvalue);
				this.SeaLevelType = SeaLevelOverrideTypes.Offset;
			}
			catch(NumberFormatException nfe){
				GriefPrevention.AddLogEntry("Parse Error reading SeaLevelOverride Type:");
				nfe.printStackTrace();
				SeaLevelOffset=0;
				
			}
		}
		else{
			
			try {
				SeaLevelOffset = Integer.parseInt(sealevelvalue);
				SeaLevelType = SeaLevelOverrideTypes.Absolute;
			}
			catch(NumberFormatException nfe){
				GriefPrevention.AddLogEntry("Parse Error reading SeaLevelOverride Type:");
				nfe.printStackTrace();
				SeaLevelOffset=0;
			}
			
			
		}
		if(SeaLevelOffset!=0){
			Debugger.Write("SeaLevelOffset read in." + this.getSeaLevelType().name() + " " + this.getSeaLevelOffsetString(), DebugLevel.Verbose);
			Target.set(NodePath + ".SeaLevel",this.getSeaLevelOffsetString());
			
		}
		
	
		
		//tada.
		
		
		

	}

	public PlacementRules(PlacementRules CopySource) {
		this.AboveSeaLevel = CopySource.AboveSeaLevel;
		this.BelowSeaLevel = CopySource.BelowSeaLevel;
		this.SeaLevelOffset = CopySource.SeaLevelOffset;
		this.SeaLevelType = CopySource.SeaLevelType;
	}

	private int getSeaLevel(World w){
		int currentresult = GriefPrevention.instance.getSeaLevel(w);
		if(this.SeaLevelOffset!=0){
			if(this.SeaLevelType == SeaLevelOverrideTypes.Absolute){
				return this.SeaLevelOffset;
			}
			else {
				return currentresult +this.SeaLevelOffset;
			}
		}
		return currentresult;
	}
	/**
	 * determines if this Placementrule allows for the given location.
	 * 
	 * @param Target
	 * @return
	 */
	public boolean Allow(Location Target, Player p, boolean ShowMessages) {
		int SeaLevelofWorld = getSeaLevel(Target.getWorld());
		boolean result = (AboveSeaLevel.Allowed() && (Target.getBlockY() >= SeaLevelofWorld)) || (BelowSeaLevel.Allowed() && (Target.getBlockY() < SeaLevelofWorld));
		Player RelevantPlayer = p;

		
		
		if (RelevantPlayer != null && RequiredPermissions.size() > 0) {
			// the player has to have at least one of the permissions, or we
			// will be forced to deny it.
			boolean permsucceed = false;
			for (String checkperm : RequiredPermissions) {
				if (RelevantPlayer.hasPermission(checkperm)) {
					permsucceed = true;
					break;
				}
			}
			if (!permsucceed) {

				StringBuffer buildpermnames = new StringBuffer();
				for (String perm : RequiredPermissions) {
					buildpermnames.append(perm);
					buildpermnames.append(",");
				}

				String uselisting = buildpermnames.toString().substring(0, buildpermnames.length() - 1);
				if (ShowMessages) {
					if (RequiredPermissions.size() == 1) {
						GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err, "You need " + uselisting + " Permission for that Action.");
					} else {
						GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err, "You need " + uselisting.replace(",", " or ") + " Permission for that Action");
					}
				}
				result = false;
			}
		}

		// System.out.println("Block:" + Target.getBlockY() + " SeaLevel:" +
		// SeaLevelofWorld + " Allow:" + result);
		return result;

	}

	@Override
	public Object clone() {
		return new PlacementRules(this);
	}

	/**
	 * returns whether this placement rule allows Action above sea level.
	 * 
	 * @return
	 */
	public BasicPermissionConstants getAboveSeaLevel() {
		return AboveSeaLevel;
	}

	/**
	 * returns whether this placement rule allows Action below sea level.
	 * 
	 * @return
	 */
	public BasicPermissionConstants getBelowSeaLevel() {
		return BelowSeaLevel;
	}

	public List<String> getRequiredPermissions() {
		return RequiredPermissions;
	}

	public PlacementRules setRequiredPermissions(List<String> requiredperms) {
		PlacementRules cdc = new PlacementRules(this);
		List<String> madeperms = new ArrayList<String>();
		for (String iterate : requiredperms) {
			madeperms.add(iterate);
		}
		cdc.RequiredPermissions = madeperms;
		return cdc;
	}

	public PlacementRules setRequiredPermissions(String... requiredperms) {
		List<String> makelist = new ArrayList<String>();
		PlacementRules cdc = new PlacementRules(this);
		for (String perm : requiredperms) {
			if (perm.contains(";")) {
				for (String addperm : perm.split(";")) {
					makelist.add(addperm);

				}
			} else
				makelist.add(perm);
		}
		cdc.RequiredPermissions = makelist;
		// System.out.println("Set to have " + cdc.RequiredPermissions.size() +
		// " perms");
		return cdc;
	}

	@Override
	public String toString() {
		if (AboveSeaLevel.Allowed() && BelowSeaLevel.Denied())
			return "Only Above Sea Level";
		if (AboveSeaLevel.Denied() && BelowSeaLevel.Allowed())
			return "Only Below Sea Level";
		if (AboveSeaLevel.Allowed() && BelowSeaLevel.Allowed())
			return "Anywhere";
		return "Nowhere";
	}

}
