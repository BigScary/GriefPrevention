package me.ryanhamshire.GriefPrevention.Configuration;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.Debugger;

import me.ryanhamshire.GriefPrevention.Debugger.DebugLevel;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.events.PermissionCheckEvent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
//this enum is used for some of the configuration options.
import org.bukkit.entity.Tameable;

import java.util.EnumSet;
import java.util.List;

//holds data pertaining to an option and where it works. 
//used primarily for information on explosions.
public class ClaimBehaviourData {

	public enum ClaimAllowanceConstants {
		None, Allow, Allow_Forced, Deny, Deny_Forced  ;
		public boolean Allowed() {

			return this == Allow || this == Allow_Forced;
		}

		public boolean Denied() {

			return this == Deny || this == Deny_Forced;
		}

	}

    /**
     * special rules enum indicates special requirements for a given ClaimBehaviourData.
     * For example, a special rule can be set to require that players have no claims for this Behaviour to ever pass.
     */
    public enum SpecialRules{
        ClaimRule_RequireNoClaims, //the player being tested MUST have no claims for this behaviour to pass.
        ClaimRule_RequireClaims,  //The player being tested MUST have claims for this behaviour to pass.
        ClaimRule_Claim, //allows on a claim. Will still test set permission value.
        ClaimRule_Wilderness  //allows in wilderness.
    }
	//enum for "overriding" the default permissions during PvP or during a siege.
	
		public enum SiegePVPOverrideConstants{
			/**
			 * Default Constant. no special provisions or action occurs during PvP or Siege.
			 */
		     None,
		     /**
		      * Allows this Permission during a siege or PvP.
		      */
		     Allow,
		     /**
		      * Denies this permission during a siege or PvP.
		      */
		     Deny,
		     /**
		      * Similar to Allow, but will only prevent Siege and PVP related logic. For example
		      * during a siege of PVP, chests are normally inaccessible. By adding
		      * AllowPermission to SiegeDefenderOverride, the Siege or PVP Logic will not be taken into account.
		      */
		     AllowRequireAccess,
		     
		     AllowRequireBuild,
		     AllowRequireOwner

		}
	public enum ClaimBehaviourMode {
		Disabled, RequireAccess, RequireBuild, RequireContainer, RequireManager, RequireNone, RequireOwner;
        public boolean hasAccess(Player p,Claim toClaim){
            if(toClaim==null) return false;
            if(this==Disabled) return false; //Disabled so no Access anywhere no matter what.
            if(this==RequireNone) return true;
            if(this==RequireAccess) return toClaim.allowAccess(p)==null;
            if(this==RequireBuild) return toClaim.allowBuild(p) == null;
            if(this==RequireContainer) return toClaim.allowContainers(p)==null;
            if(this==RequireManager) return toClaim.isManager(p);
            if(this==RequireOwner) return toClaim.getOwnerName()!=null && toClaim.getOwnerName().equals(p.getName());


            return false;

        }

        public static ClaimBehaviourMode parseMode(String name) {
			// System.out.println("Looking for " + name);
			for (ClaimBehaviourMode cb : ClaimBehaviourMode.values()) {
				// System.out.println("Comparing " + cb.name() + " to " + name);
				if (cb.name().equalsIgnoreCase(name))
					return cb;
			}
			return ClaimBehaviourMode.RequireNone;

		}

		public boolean PerformTest(Location testLocation, Player testPlayer, boolean ShowMessages) {
			boolean testresult = false;
			try {
				WorldConfig wc = GriefPrevention.instance.getWorldCfg(testLocation.getWorld());
				PlayerData pd = null;
				// System.out.println("PerformTest:" + this.name());
				if (testPlayer == null)
					return testresult = true;
				if (testPlayer != null)
					pd = GriefPrevention.instance.dataStore.getPlayerData(testPlayer.getName());
				if ((pd != null) && pd.ignoreClaims || this == RequireNone)
					return true;




				String result = null;
				Claim atposition = GriefPrevention.instance.dataStore.getClaimAt(testLocation, false);
				if (atposition == null)
					return testresult = true; // unexpected...
				switch (this) {
				case Disabled:
					if (testPlayer != null && ShowMessages)
						GriefPrevention.sendMessage(testPlayer, TextMode.Err, Messages.ConfigDisabled);
					return testresult = false;
				case RequireNone:
					return testresult = true;
				case RequireOwner:
					if (atposition.getOwnerName().equalsIgnoreCase(testPlayer.getName())) {
						return testresult = true;

					} else {
						if (ShowMessages)
							GriefPrevention.sendMessage(testPlayer, TextMode.Err, "You need to Own the claim to do that.");
						return testresult = false;
					}
				case RequireManager:

					if (atposition.isManager(testPlayer.getName())) {
						return testresult = true; // success
					} else {
						// failed! if showmessages is on, show that message.
						if (ShowMessages)
							GriefPrevention.sendMessage(testPlayer, TextMode.Err, "You need to have Manager trust to do that.");
						return testresult = false;
					}
				case RequireBuild:

					if (null == (result = atposition.allowBuild(testPlayer))) {
						return true; // success
					} else {
						// failed! if showmessages is on, show that message.
						if (ShowMessages)
							GriefPrevention.sendMessage(testPlayer, TextMode.Err, result);
						return testresult = false;
					}
				case RequireAccess:

					if (null == (result = atposition.allowAccess(testPlayer))) {
						return testresult = true; // success
					} else {
						// failed! if showmessages is on, show that message.
						if (ShowMessages)
							GriefPrevention.sendMessage(testPlayer, TextMode.Err, result);
						return testresult = false;
					}
				case RequireContainer:

					if (null == (result = atposition.allowContainers(testPlayer))) {
						return testresult = true; // success
					} else {
						// failed! if showmessages is on, show that message.
						if (ShowMessages)
							GriefPrevention.sendMessage(testPlayer, TextMode.Err, result);
						return testresult = false;
					}
				default:
					// System.out.println("defaulting on " + name());
					return testresult = false;
				}
			} finally {
				// System.out.println("ClaimBehaviour returning " + testresult);
			}

		}
	}

	public static ClaimBehaviourData getAboveSeaLevel(String pName) {
		return new ClaimBehaviourData(pName, PlacementRules.AboveOnly, PlacementRules.AboveOnly, ClaimBehaviourMode.RequireNone);
	}

	public static ClaimBehaviourData getAll(String pName) {
		return new ClaimBehaviourData(pName, PlacementRules.Both, PlacementRules.Both, ClaimBehaviourMode.RequireNone);
	}

	public static ClaimBehaviourData getBelowSeaLevel(String pName) {
		return new ClaimBehaviourData(pName, PlacementRules.BelowOnly, PlacementRules.BelowOnly, ClaimBehaviourMode.RequireNone);
	}

	public static ClaimBehaviourData getInsideClaims(String pName) {
		return new ClaimBehaviourData(pName, PlacementRules.Neither, PlacementRules.Both, ClaimBehaviourMode.RequireNone);
	}

	public static ClaimBehaviourData getNone(String pName) {
		return new ClaimBehaviourData(pName, PlacementRules.Neither, PlacementRules.Neither, ClaimBehaviourMode.RequireNone);
	}

	public static ClaimBehaviourData getOutsideClaims(String pName) {
		return new ClaimBehaviourData(pName, PlacementRules.Both, PlacementRules.Neither, ClaimBehaviourMode.RequireNone);
	}

	private String BehaviourName;

    private String DenialMessage;

	private ClaimBehaviourMode ClaimBehaviour;

	private PlacementRules Claims;

	private PlacementRules Wilderness;

	private boolean TameableAllowOwner = false;

    private EnumSet<SpecialRules> SpecialRuleFlags = EnumSet.noneOf(SpecialRules.class);
	private SiegePVPOverrideConstants SiegeAttackerOverride = SiegePVPOverrideConstants.None;
	private SiegePVPOverrideConstants SiegeDefenderOverride = SiegePVPOverrideConstants.None;
	private SiegePVPOverrideConstants SiegeBystanderOverride = SiegePVPOverrideConstants.None;
	private SiegePVPOverrideConstants SiegeNonPlayerOverride = SiegePVPOverrideConstants.None;
	
	public SiegePVPOverrideConstants getSiegeAttackerOverride(){ return SiegeAttackerOverride;}
	public SiegePVPOverrideConstants getSiegeDefenderOverride(){ return SiegeDefenderOverride;}
	public SiegePVPOverrideConstants getSiegeBystanderOverride(){ return SiegeBystanderOverride;}
    public SiegePVPOverrideConstants getSiegeNonPlayerOverride(){return SiegeNonPlayerOverride;}
	//PvP Overrides can only apply to Claims currently.
	//Attacker and Defender refers to the Attacker and Defender- the Attacker is the person with higher Permissions
	//on a claim.
	
	private SiegePVPOverrideConstants PvPOverride = SiegePVPOverrideConstants.None;
	public SiegePVPOverrideConstants getPvPOverride(){ return PvPOverride;}
	public boolean getTameableAllowOwner(){ return TameableAllowOwner;}
	public ClaimBehaviourData setTameableAllowOwner(boolean value){ TameableAllowOwner = value; return this;}
	public ClaimBehaviourData setSiegeOverrides(SiegePVPOverrideConstants Attacker,SiegePVPOverrideConstants Defender){
		return setSiegeOverrides(Attacker,Defender,SiegeBystanderOverride);
	}
    public ClaimBehaviourData setSiegeOverrides(SiegePVPOverrideConstants Attacker,SiegePVPOverrideConstants Defender,SiegePVPOverrideConstants ByStander){
        return setSiegeOverrides(Attacker,Defender,ByStander,SiegeNonPlayerOverride);
    }
	public ClaimBehaviourData setSiegeOverrides(SiegePVPOverrideConstants Attacker,SiegePVPOverrideConstants Defender,SiegePVPOverrideConstants ByStander,SiegePVPOverrideConstants NonPlayer){
		
		SiegeAttackerOverride = Attacker;
		SiegeDefenderOverride = Defender;
		SiegeBystanderOverride = ByStander;
        SiegeNonPlayerOverride = NonPlayer;
		return this;
	}
    public ClaimBehaviourData setSiegeAttackerOverride(SiegePVPOverrideConstants Attacker){
        SiegeAttackerOverride = Attacker;
        return this;
    }
    public ClaimBehaviourData setSiegeDefenderOverride(SiegePVPOverrideConstants Defender){
        SiegeDefenderOverride = Defender;
        return this;
    }
    public ClaimBehaviourData setSiegeBystanderOverride(SiegePVPOverrideConstants Bystander){
        SiegeBystanderOverride = Bystander;
        return this;
    }
    public ClaimBehaviourData setSiegeNonPlayerOverride(SiegePVPOverrideConstants nonPlayer){
        SiegeNonPlayerOverride = nonPlayer;
        return this;
    }
	public ClaimBehaviourData setPVPOverride(SiegePVPOverrideConstants newPVP){
		this.PvPOverride = newPVP;
		return this;
	}
	
	
	public ClaimBehaviourData setSeaLevelOffsets(PlacementRules.SeaLevelOverrideTypes useType,int Offset){
		Claims.setSeaLevelOffset(useType, Offset);
		Wilderness.setSeaLevelOffset(useType, Offset);
		return this;
	}
	
	public ClaimBehaviourData(ClaimBehaviourData Source) {

		this.BehaviourName = Source.BehaviourName;
        this.SpecialRuleFlags = Source.SpecialRuleFlags;
		this.Claims = (PlacementRules) Source.Claims.clone();
		this.Wilderness = (PlacementRules) Source.Wilderness.clone();
		this.ClaimBehaviour = Source.ClaimBehaviour;
		this.PvPOverride = Source.PvPOverride;
		this.SiegeAttackerOverride = Source.SiegeAttackerOverride;
		this.SiegeDefenderOverride = Source.SiegeDefenderOverride;
        this.SiegeBystanderOverride = Source.SiegeBystanderOverride;
        this.SiegeNonPlayerOverride = Source.SiegeNonPlayerOverride;
		this.TameableAllowOwner = Source.getTameableAllowOwner();
	
	}

	public ClaimBehaviourData(String pName, FileConfiguration Source, FileConfiguration outConfig, String NodePath, ClaimBehaviourData Defaults) {
        Debugger.Write("Reading ClaimBehaviourData from Node:" + NodePath,Debugger.DebugLevel.Verbose);
        if(Defaults==null) Defaults = ClaimBehaviourData.getNone(pName);
		BehaviourName = pName;
		// we want to read NodePath.BelowSeaLevelWilderness and whatnot.
		// bases Defaults off another ClaimBehaviourData instance.

        //special rules.
        //read in the list of special rules.
        List<String> specialrules = Source.getStringList(NodePath + ".SpecialRules");

        if(specialrules!=null && specialrules.size() > 0){

            for(String iterate:specialrules){
            try {
            SpecialRules sr = Enum.valueOf(SpecialRules.class,iterate);
            this.SpecialRuleFlags.add(sr);
            }
            catch(Exception srexception){
                Debugger.Write("Failed to read SpecialRules item:" + iterate,DebugLevel.Verbose);
            }
            }

            outConfig.set(NodePath + ".SpecialRules",specialrules);
        }

        String _DenialMessage = Source.getString(NodePath + ".DenialMessage");
        if(_DenialMessage!=null && _DenialMessage.length()>0){
            this.DenialMessage = _DenialMessage;
        }

        Wilderness = new PlacementRules(Source, outConfig, NodePath + ".Wilderness", Defaults.getWildernessRules());
		Claims = new PlacementRules(Source, outConfig, NodePath + ".Claims", Defaults.getClaimsRules());
		String strmode = Source.getString(NodePath + ".Claims.ClaimControl", Defaults.getBehaviourMode().name());
		
		// check for a requiredpermissions entry. If there isn't one, though,
		// don't save it.
		TameableAllowOwner = Source.getBoolean(NodePath + ".Claims.TameableAllowOwner",Defaults.getTameableAllowOwner());
		
		
		if(TameableAllowOwner) outConfig.set(NodePath + ".Claims.TameableAllowOwner", TameableAllowOwner);
	
		//check for siege and PVP options.
		//NodePath + ".Claims.SiegeAttacker"
		//NodePath + ".Claims.SiegeDefender"
		//NodePath + ".PVP"
		//retrieve each. Set the value. Then if it's not the default, save them back.
		String SAttacker = Source.getString(NodePath + ".Claims.SiegeAttacker",Defaults.getSiegeAttackerOverride().name());
		String SDefender = Source.getString(NodePath + ".Claims.SiegeDefender",Defaults.getSiegeDefenderOverride().name());
		String SBystander = Source.getString(NodePath + ".Claims.SiegeBystander",Defaults.getSiegeBystanderOverride().name());
        Debugger.Write("NoPlayer==null:" + (Defaults.getSiegeNonPlayerOverride()==null),DebugLevel.Verbose);
        String SNoPlayer = Source.getString(NodePath + ".Claims.SiegeNonPlayer",Defaults.getSiegeNonPlayerOverride().name());
		String PVPProvision = Source.getString(NodePath + ".PVP",Defaults.getPvPOverride().name());
		
		
		//parse each.
		try {this.SiegeAttackerOverride = SiegePVPOverrideConstants.valueOf(SAttacker);}
		catch(Exception sAtt){this.SiegeAttackerOverride = SiegePVPOverrideConstants.None;}
		try {this.SiegeDefenderOverride = SiegePVPOverrideConstants.valueOf(SDefender);}
		catch(Exception sDef){this.SiegeDefenderOverride = SiegePVPOverrideConstants.None;}
		
		try {this.SiegeBystanderOverride = SiegePVPOverrideConstants.valueOf(SBystander);}
		catch(Exception sBy){ this.SiegeBystanderOverride = SiegePVPOverrideConstants.None;}
		try {this.SiegeNonPlayerOverride = SiegePVPOverrideConstants.valueOf(SNoPlayer);}
        catch(Exception snon){this.SiegeNonPlayerOverride = SiegePVPOverrideConstants.None;}

		try {this.PvPOverride = SiegePVPOverrideConstants.valueOf(PVPProvision);}
		catch(Exception sPVP){this.PvPOverride = SiegePVPOverrideConstants.None;}

		//save each back. If they are none, however, don't save.
		if(SiegeAttackerOverride != SiegePVPOverrideConstants.None){
			outConfig.set(NodePath + ".Claims.SiegeAttacker", SiegeAttackerOverride.name());
		}
		if(SiegeDefenderOverride != SiegePVPOverrideConstants.None){
			outConfig.set(NodePath + ".Claims.SiegeDefender",SiegeDefenderOverride.name());
		}
		if(SiegeBystanderOverride != SiegePVPOverrideConstants.None){
			outConfig.set(NodePath + ".Claims.SiegeBystander", SiegeBystanderOverride.name());
		}
        if(SiegeNonPlayerOverride != SiegePVPOverrideConstants.None){
            outConfig.set(NodePath + ".Claims.SiegeNonPlayer",SiegeNonPlayerOverride.name());
        }

		if(PvPOverride != SiegePVPOverrideConstants.None){
			outConfig.set(NodePath + ".PVP", PvPOverride.name());
		}
		
			
		ClaimBehaviour = ClaimBehaviourMode.parseMode(strmode);

		
		outConfig.set(NodePath + ".Claims.ClaimControl", ClaimBehaviour.name());
		Debugger.Write(this.toString(),DebugLevel.Verbose);

	}

	public ClaimBehaviourData(String pName, PlacementRules pWilderness, PlacementRules pClaims, ClaimBehaviourMode behaviourmode) {
		Wilderness = pWilderness;
		Claims = pClaims;
		this.ClaimBehaviour = behaviourmode;
		BehaviourName = pName;
		
		
	}
	
	public ClaimAllowanceConstants Allowed(Entity Target,Player RelevantPlayer){
		return Allowed(Target,RelevantPlayer,false);
	}
	/**
	 * Entity overload for Allowed method.
	 * @param Target Target Entity. 
	 * @param RelevantPlayer Player performing this action.
	 * @return Whether this action is allowed, returning a number of ClaimAllowance values.
	 */
	public ClaimAllowanceConstants Allowed(Entity Target,Player RelevantPlayer,boolean ShowMessages){

        Debugger.Write("ClaimBehaviourData::Allowed-" + this.getBehaviourName(), DebugLevel.Verbose);



		if(!this.TameableAllowOwner || RelevantPlayer==null || !(Target instanceof Tameable)){
			return Allowed(Target.getLocation(),RelevantPlayer,ShowMessages);
			
		}
		else if(!(((Tameable)Target).getOwner()==null)){
			Tameable testTamed = (Tameable)Target;
			if(testTamed.getOwner().getName().equalsIgnoreCase(RelevantPlayer.getName())){
                Debugger.Write("ClaimBehaviourData::Allowed- Forcing allowance for Tameable owned by " + RelevantPlayer.getName(),DebugLevel.Verbose);
				return ClaimAllowanceConstants.Allow_Forced;
            }
			else
				return Allowed(Target.getLocation(),RelevantPlayer,ShowMessages);
			
		}
		
		return Allowed(Target.getLocation(),RelevantPlayer,ShowMessages);
		
	}
	/**
	 * returns whether this Behaviour is allowed at the given location. if the
	 * passed player currently has ignoreclaims on, this will return true no
	 * matter what. This delegates to the overload that displays messages and
	 * passes true for the omitted argument.
	 * 
	 * @param position
	 *            Position to test.
	 * @param RelevantPlayer
	 *            Player to test. Can be null for actions or behaviours that do
	 *            not involve a player.
	 * @return whether this behaviour is Allowed or Denied in this claim.
	 */

	public ClaimAllowanceConstants Allowed(Location position, Player RelevantPlayer) {
		return Allowed(position, RelevantPlayer, true);
	}

	/**
	 * returns whether this Behaviour is allowed at the given location. if the
	 * passed player currently has ignoreclaims on, this will return true no
	 * matter what.
	 * 
	 * @param position
	 *            Position to test.
	 * @param RelevantPlayer
	 *            Player to test. Can be null for actions or behaviours that do
	 *            not involve a player.
	 * @param ShowMessages
	 *            Whether a Denied result will display an appropriate message.
	 * @return whether this behaviour is Allowed or Denied in this claim.
	 */
	public ClaimAllowanceConstants Allowed(Location position, Player RelevantPlayer, boolean ShowMessages) {
		return Allowed(position, RelevantPlayer, ShowMessages, true);
	}

	/**
	 * returns whether this Behaviour is allowed at the given location. if the
	 * passed player currently has ignoreclaims on, this will return true no
	 * matter what. Otherwise, the Value will be determined based on the set fields and values of this instance.
	 * 
	 * @param position
	 *            Position to test.
	 * @param RelevantPlayer
	 *            Player to test. Can be null for actions or behaviours that do
	 *            not involve a player.
	 * @param ShowMessages
	 *            Whether a Denied result will display an appropriate message.
	 * @param fireEvent
	 *            Whether this call will fire the PermissionCheckEvent. This can
	 *            be passed as false by plugins handling this event to get the
	 *            value that would be retrieved without it's interference.
	 * @return whether this behaviour is Allowed or Denied at the given position and for the given player.
     *
     *
	 */

	public ClaimAllowanceConstants Allowed(Location position, Player RelevantPlayer, boolean ShowMessages, boolean fireEvent) {
		ClaimAllowanceConstants returned = ClaimAllowanceConstants.Allow;
		try {
			Debugger.Write("Behaviour: " +this.getBehaviourName(), DebugLevel.Verbose);

            //if there are special rules...
            if(this.SpecialRuleFlags.size()>0){
                Debugger.Write("SpecialRuleFlags found on element named " + this.BehaviourName,DebugLevel.Verbose);
                if(RelevantPlayer!=null){

                    Player p = RelevantPlayer;
                    Debugger.Write("SpecialRuleFlags testing with Player " + p.getName(),DebugLevel.Verbose);
                    PlayerData pd = GriefPrevention.instance.dataStore.getPlayerData(p.getName());
                    //check them all. Right now it doesn't really make sense to have more than one,
                    //and some are sorta mutually exclusive, but more might be added. Exclusivity will
                    //(hopefully) get documented, if it is ever added.
                    ClaimAllowanceConstants cac = ClaimAllowanceConstants.None;
                    Claim GrabClaim = GriefPrevention.instance.dataStore.getClaimAt(position,true);
                    boolean InClaim=false;
                    if(GrabClaim!=null && GrabClaim.allowBuild(p)==null) InClaim=true;
                    for(SpecialRules sr:this.SpecialRuleFlags){

                        if(sr==SpecialRules.ClaimRule_RequireClaims){
                            //if they have no claims, return deny.
                            Debugger.Write("RequireClaims: Player has " + String.valueOf(pd.claims.size()) + " claims.",DebugLevel.Verbose);
                            if(pd.claims.size()==0) cac = ClaimAllowanceConstants.Deny;
                        }
                        if(sr==SpecialRules.ClaimRule_RequireNoClaims){
                            //if they have claims, deny.
                            Debugger.Write("RequireNoClaims: Player has " + String.valueOf(pd.claims.size()) + " claims.",DebugLevel.Verbose);
                            if(pd.claims.size()>0) cac = ClaimAllowanceConstants.Deny;
                        }
                        if(sr==SpecialRules.ClaimRule_Claim){
                            Debugger.Write("ClaimRule_Claim: Player In claim:" + String.valueOf(InClaim),DebugLevel.Verbose);
                            if(InClaim) return ClaimAllowanceConstants.Allow;
                        }
                        if(sr==SpecialRules.ClaimRule_Wilderness){
                            Debugger.Write("ClaimRule_Wilderness: Player In claim:" + String.valueOf(InClaim),DebugLevel.Verbose);
                            if(!InClaim) return ClaimAllowanceConstants.Allow;
                        }




                    }
                    return cac;


                }
            }


			PlayerData pd = null;
			boolean ignoringclaims = false;
			if (RelevantPlayer != null) {
				pd = GriefPrevention.instance.dataStore.getPlayerData(RelevantPlayer.getName());
				if (pd != null)
					ignoringclaims = pd.ignoreClaims;

			}
			if (fireEvent) {
				PermissionCheckEvent permcheck = new PermissionCheckEvent(this, RelevantPlayer);

				Bukkit.getPluginManager().callEvent(permcheck);
				if (permcheck.getResult() != null) {
					return returned = permcheck.getResult();
				}
			}
			// check permissions if there is a player involved and we have them.

			
			//check pvp...
			if(pd!=null && pd.inPvpCombat()){
				
				if(this.PvPOverride!=SiegePVPOverrideConstants.None){
					
					if(PvPOverride == SiegePVPOverrideConstants.Allow){
						return ClaimAllowanceConstants.Allow;
					}
					else if(PvPOverride == SiegePVPOverrideConstants.Deny){
						return ClaimAllowanceConstants.Deny;
					}
						
				}
				
				
				
			}
			
			
			Claim testclaim = GriefPrevention.instance.dataStore.getClaimAt(position, true);
			if (testclaim != null) {
				if (ignoringclaims)
					return ClaimAllowanceConstants.Allow;


				//if the claim is under siege, apply the siege overrides, if available.
				
				if(testclaim.siegeData != null){

					SiegePVPOverrideConstants useval = SiegePVPOverrideConstants.None;
					//siege overrides apply to players being seiged or attacking, but also
					//another set applies to bystanders who happen to be in the claim (and are not the attacker or defender).
				    if(RelevantPlayer!=null){
                        if(testclaim.siegeData.attacker!=null && testclaim.siegeData.attacker.getName().equals(RelevantPlayer.getName())){
                            useval = this.SiegeAttackerOverride;

                        }
                        else if(testclaim.siegeData.defender!=null && testclaim.siegeData.defender.getName().equals(RelevantPlayer.getName())){

                            useval = this.SiegeDefenderOverride;
                        }
                        else {
                            useval = this.SiegeBystanderOverride;
                        }
                    }
                    else
                    {
                        useval = this.SiegeNonPlayerOverride;
                    }
					//if not set to none...
					if(useval!=SiegePVPOverrideConstants.None){
						
						if(useval == SiegePVPOverrideConstants.Allow){
							return ClaimAllowanceConstants.Allow;
							
						}
						else if(useval == SiegePVPOverrideConstants.AllowRequireAccess){
							//return allow, if the player has Access trust.
							String AccessResult = testclaim.allowAccess(RelevantPlayer);
							if(null==AccessResult){
								return ClaimAllowanceConstants.Allow;
							}  else {
								if(ShowMessages && hasDenialMessage()) GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err, DenialMessage);
                                else if (ShowMessages)  GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err, AccessResult);
								return ClaimAllowanceConstants.Deny;
							}
						}
						else if(useval == SiegePVPOverrideConstants.AllowRequireBuild){
							//return allow if player has build trust.
							String BuildResult = testclaim.allowBuild(RelevantPlayer);
							if(null == BuildResult){
								return ClaimAllowanceConstants.Allow;
							} else {
                                if(ShowMessages && hasDenialMessage()) GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err, DenialMessage);
                                else if (ShowMessages)  GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err, BuildResult);
								return ClaimAllowanceConstants.Deny;
							}
						}
						else if(useval == SiegePVPOverrideConstants.AllowRequireOwner){
							if(RelevantPlayer!=null && testclaim.getOwnerName().equals(RelevantPlayer.getName())){
								return ClaimAllowanceConstants.Allow;
							}
							else {
								if(ShowMessages) GriefPrevention.sendMessage(RelevantPlayer,TextMode.Err,Messages.NotYourClaim);
								return ClaimAllowanceConstants.Deny;
							}
						}
						else if(useval == SiegePVPOverrideConstants.Deny){
							return ClaimAllowanceConstants.Deny;
						}
						
						
					}
					
				}
				else if (!this.ClaimBehaviour.PerformTest(position, RelevantPlayer, ShowMessages))
                        return returned = ClaimAllowanceConstants.Deny;

				
				
				boolean varresult = this.Claims.Allow(position, RelevantPlayer, ShowMessages);

				return returned = (varresult ? ClaimAllowanceConstants.Allow : ClaimAllowanceConstants.Deny);

            }
			// retrieve the appropriate Sea Level for this world.
			/*
			 * int sealevel =
			 * GriefPrevention.instance.getWorldCfg(position.getWorld
			 * ()).seaLevelOverride(); int yposition = position.getBlockY();
			 * boolean abovesealevel = yposition > sealevel;
			 */
			else if (testclaim == null) {
				// we aren't inside a claim.
				// System.out.println(BehaviourName + "Wilderness test...");
				ClaimAllowanceConstants wildernessresult = Wilderness.Allow(position, RelevantPlayer, ShowMessages && RelevantPlayer != null) ? ClaimAllowanceConstants.Allow : ClaimAllowanceConstants.Deny;
				// if(wildernessresult.Denied() && ShowMessages &&
				// RelevantPlayer!=null){
				// GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err,
				// Messages.ConfigDisabled,this.BehaviourName);
				// }
				return (returned = wildernessresult);

			}

			return (returned = ClaimAllowanceConstants.Allow);
		} finally {
			 
			 
			Debugger.Write("ClaimBehaviourData returning:\"" + returned.name() + "\"" + " For " + BehaviourName, DebugLevel.Verbose);
            if(ShowMessages) if(hasDenialMessage()) GriefPrevention.sendMessage(RelevantPlayer,TextMode.Err,DenialMessage);
            try {
               // throw new Exception("stack trace");
            }
            catch(Exception exx){
            Debugger.Write(org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(exx),DebugLevel.Verbose);
            }
		}
	}
    private boolean hasDenialMessage(){
        return this.DenialMessage!=null && DenialMessage.length()>0;
    }
	@Override
	public Object clone() {
		return new ClaimBehaviourData(this);
	}

	public ClaimBehaviourMode getBehaviourMode() {
		return ClaimBehaviour;
	}

	/**
	 * retrieves the name for this Behaviour. This will be used in any
	 * applicable messages.
	 * 
	 * @return Name for this behaviour.
	 */
	public String getBehaviourName() {
		return BehaviourName;
	}

	/**
	 * retrieves the placement rules for this Behaviour inside claims.
	 * 
	 * @return PlacementRules instance encapsulating applicable placement rules.
	 */
	public PlacementRules getClaimsRules() {
		return Claims;
	};

	/**
	 * retrieves the placement rules for this Behaviour outside claims (in the
	 * 'wilderness')
	 * 
	 * @return PlacementRules instance encapsulating applicable placement rules.
	 */
	public PlacementRules getWildernessRules() {
		return Wilderness;
	};

	public ClaimBehaviourData setBehaviourMode(ClaimBehaviourMode b) {
		if (b == null)
			b = ClaimBehaviourMode.RequireNone;
		ClaimBehaviourData cdc = new ClaimBehaviourData(this);
		cdc.ClaimBehaviour = b;
		return cdc;
	}

	public ClaimBehaviourData setClaimRequiredPermission(String... params) {
		PlacementRules copyClaims = Claims.setRequiredPermissions(params);
		ClaimBehaviourData copydata = new ClaimBehaviourData(this);
		copydata.Claims = copyClaims;
		return copydata;

	}

	public ClaimBehaviourData setRequiredPermissions(String... params) {
		PlacementRules copyClaims = Claims.setRequiredPermissions(params);
		PlacementRules copyWilderness = Wilderness.setRequiredPermissions(params);
		ClaimBehaviourData copydata = new ClaimBehaviourData(this);
		copydata.Claims = copyClaims;
		copydata.Wilderness = copyWilderness;
		return copydata;
	}

	public ClaimBehaviourData setWildernessRequiredPermission(String... params) {
		PlacementRules copyWilds = Wilderness.setRequiredPermissions(params);
		ClaimBehaviourData copydata = new ClaimBehaviourData(this);
		copydata.Wilderness = copyWilds;
		return copydata;
	}

	@Override
	public String toString() {
		return BehaviourName + " in the wilderness " + getWildernessRules().toString() + " and in claims " + getClaimsRules().toString() + " Required Trust Level:" + this.getBehaviourMode().name();

	}
}