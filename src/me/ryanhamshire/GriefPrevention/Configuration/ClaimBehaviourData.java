package me.ryanhamshire.GriefPrevention.Configuration;

import java.util.ArrayList;
import java.util.List;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.Debugger;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.Debugger.DebugLevel;
import me.ryanhamshire.GriefPrevention.events.PermissionCheckEvent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
//this enum is used for some of the configuration options.
import org.bukkit.entity.Player;

//holds data pertaining to an option and where it works. 
//used primarily for information on explosions.
public class ClaimBehaviourData {
	public enum ClaimAllowanceConstants {
		Allow_Forced,
		Allow,
		Deny,
		Deny_Forced;
		public boolean Allowed(){ return this==Allow || this==Allow_Forced;}
		public boolean Denied(){ return this==Deny || this==Deny_Forced;}
		
		
		
	}
	public enum ClaimBehaviourMode{
		RequireNone,
		RequireOwner,
		RequireManager,
		RequireAccess,
		RequireContainer,
		RequireBuild,
		Disabled;
		
		
		public static ClaimBehaviourMode parseMode(String name){
			//System.out.println("Looking for " + name);
			for(ClaimBehaviourMode cb:ClaimBehaviourMode.values()){
				//System.out.println("Comparing " + cb.name() + " to " + name);
				if(cb.name().equalsIgnoreCase(name))
					return cb;
			}
			return ClaimBehaviourMode.RequireNone;
			
		}
		public boolean PerformTest(Location testLocation,Player testPlayer,boolean ShowMessages){
			boolean testresult = false;
			try {
			WorldConfig wc = GriefPrevention.instance.getWorldCfg(testLocation.getWorld());
			PlayerData pd = null;
			//System.out.println("PerformTest:" + this.name());
			if(testPlayer==null) return testresult=true;
			if(testPlayer!=null) pd = GriefPrevention.instance.dataStore.getPlayerData(testPlayer.getName());
			if((pd!=null)&&pd.ignoreClaims || this==RequireNone) return true;
			String result = null;
			Claim atposition  = GriefPrevention.instance.dataStore.getClaimAt(testLocation, false);
			if(atposition==null) return testresult=true; //unexpected...
			switch(this){
			case Disabled:
				if(testPlayer!=null && ShowMessages)
					GriefPrevention.sendMessage(testPlayer, TextMode.Err, Messages.ConfigDisabled);
				return testresult=false;
			case RequireNone:
				return testresult=true;	
			case RequireOwner:
				if(atposition.ownerName.equalsIgnoreCase(testPlayer.getName())){
					return testresult=true;
					
				}else {
					if(ShowMessages)
						GriefPrevention.sendMessage(testPlayer, 
								TextMode.Err, "You need to Own the claim to do that.");
					return testresult=false;
				}
			case RequireManager:
				
				if(atposition.isManager(testPlayer.getName())){
					return testresult=true; //success
				}
				else {
					//failed! if showmessages is on, show that message.
					if(ShowMessages)
						GriefPrevention.sendMessage(testPlayer, 
								TextMode.Err, "You need to have Manager trust to do that.");
					return testresult=false;
				}
			case RequireBuild:
				
				if(null==(result=atposition.allowBuild(testPlayer))){
					return true; //success
				}
				else {
					//failed! if showmessages is on, show that message.
					if(ShowMessages)
						GriefPrevention.sendMessage(testPlayer, 
								TextMode.Err, result);
					return testresult=false;
				}
			case RequireAccess:
				
				if(null==(result=atposition.allowAccess(testPlayer))){
					return testresult=true; //success
				}
				else {
					//failed! if showmessages is on, show that message.
					if(ShowMessages)
						GriefPrevention.sendMessage(testPlayer, 
								TextMode.Err, result);
					return testresult=false;
				}
			case RequireContainer:
				
				if(null==(result=atposition.allowContainers(testPlayer))){
					return testresult=true; //success
				}
				else {
					//failed! if showmessages is on, show that message.
					if(ShowMessages)
						GriefPrevention.sendMessage(testPlayer, 
								TextMode.Err,result);
					return testresult=false;
				}
			default:
				//System.out.println("defaulting on " + name());
				return testresult=false;
			}
			}
			finally
			{
				//System.out.println("ClaimBehaviour returning " + testresult);
			}
			
			
			
			
			
		}
	}
	private PlacementRules Wilderness;
	private PlacementRules Claims;
	
	private ClaimBehaviourMode ClaimBehaviour;
	public ClaimBehaviourMode getBehaviourMode(){ return ClaimBehaviour;}
	public ClaimBehaviourData setBehaviourMode(ClaimBehaviourMode b){
		if(b==null) b = ClaimBehaviourMode.RequireNone;
		ClaimBehaviourData cdc = new ClaimBehaviourData(this);
		cdc.ClaimBehaviour=b;
		return cdc;
	}
	
	/**
	 * returns whether this Behaviour is allowed at the given location. if the passed player currently has
	 * ignoreclaims on, this will return true no matter what. This delegates to the overload that displays messages
	 * and passes true for the omitted argument.
	 * @param position Position to test.
	 * @param RelevantPlayer Player to test. Can be null for actions or behaviours that do not involve a player.
	 * @return whether this behaviour is Allowed or Denied in this claim.
	 */
	
		
	public ClaimAllowanceConstants Allowed(Location position,Player RelevantPlayer){
		return Allowed(position,RelevantPlayer,true);	
	}
	/**
	 * returns whether this Behaviour is allowed at the given location. if the passed player currently has
	 * ignoreclaims on, this will return true no matter what.
	 * @param position Position to test.
	 * @param RelevantPlayer Player to test. Can be null for actions or behaviours that do not involve a player.
	 * @param ShowMessages Whether a Denied result will display an appropriate message.
	 * @return whether this behaviour is Allowed or Denied in this claim.
	 */
	public ClaimAllowanceConstants Allowed(Location position,Player RelevantPlayer,boolean ShowMessages){
		return Allowed(position,RelevantPlayer,ShowMessages,true);
	}
	
	
	/**
	 * returns whether this Behaviour is allowed at the given location. if the passed player currently has
	 * ignoreclaims on, this will return true no matter what.
	 * @param position Position to test.
	 * @param RelevantPlayer Player to test. Can be null for actions or behaviours that do not involve a player.
	 * @param ShowMessages Whether a Denied result will display an appropriate message.
	 * @param fireEvent Whether this call will fire the PermissionCheckEvent. This can be passed as false by plugins handling this event to get
	 * the value that would be retrieved without it's interference.
	 * @return whether this behaviour is Allowed or Denied in this claim.
	 */
	@SuppressWarnings("unused")
	public ClaimAllowanceConstants Allowed(Location position,Player RelevantPlayer,boolean ShowMessages,boolean fireEvent){
		ClaimAllowanceConstants returned = ClaimAllowanceConstants.Allow;
		try {
		//System.out.println("ClaimBehaviour:" + this.getBehaviourName());
		//System.out.println("Testing Allowed," + BehaviourName + " Messages:" + ShowMessages);
		//String result=null;
		PlayerData pd = null;
		boolean ignoringclaims = false;
		if(RelevantPlayer!=null) {
			pd = GriefPrevention.instance.dataStore.getPlayerData(RelevantPlayer.getName());
			if(pd!=null) ignoringclaims = pd.ignoreClaims;
			
		}
		if(fireEvent){
			PermissionCheckEvent permcheck = new PermissionCheckEvent(this,RelevantPlayer);
			Bukkit.getPluginManager().callEvent(permcheck);
			if(permcheck.getResult()!=null){
				return returned=permcheck.getResult();
		}
		}
		//check permissions if there is a player involved and we have them.
		
		
		Claim testclaim = GriefPrevention.instance.dataStore.getClaimAt(position, true);
		if(testclaim!=null){
			if(ignoringclaims) return ClaimAllowanceConstants.Allow;
			if(!this.ClaimBehaviour.PerformTest(position, RelevantPlayer, ShowMessages))
				return returned=ClaimAllowanceConstants.Deny;
			
			
			boolean varresult =  this.Claims.Allow(position, RelevantPlayer, ShowMessages);
			
			
			
			return returned = (varresult?ClaimAllowanceConstants.Allow:ClaimAllowanceConstants.Deny);
		}

		
		//retrieve the appropriate Sea Level for this world.
		/*int sealevel = GriefPrevention.instance.getWorldCfg(position.getWorld()).seaLevelOverride();
		int yposition = position.getBlockY();
		boolean abovesealevel = yposition > sealevel;*/
		else if(testclaim==null){
			//we aren't inside a claim.
			//System.out.println(BehaviourName + "Wilderness test...");
			ClaimAllowanceConstants wildernessresult = Wilderness.Allow(position,RelevantPlayer,ShowMessages && RelevantPlayer!=null)?ClaimAllowanceConstants.Allow:ClaimAllowanceConstants.Deny;
			//if(wildernessresult.Denied() && ShowMessages && RelevantPlayer!=null){
			//	GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err, Messages.ConfigDisabled,this.BehaviourName);
			//}
			return (returned=wildernessresult);
			
		}
		
		
		return (returned=ClaimAllowanceConstants.Allow);
		}
		finally {
			//System.out.println("ClaimBehaviourData returning:\"" + returned.name()  + "\"" + " For " + BehaviourName);
			Debugger.Write("ClaimBehaviourData returning:\"" + returned.name()  + "\"" + " For " + BehaviourName, DebugLevel.Verbose);
		}
	}
	/**
	 * retrieves the placement rules for this Behaviour outside claims (in the 'wilderness')
	 * @return PlacementRules instance encapsulating applicable placement rules.
	 */
	public PlacementRules getWildernessRules() { return Wilderness;}
	/**
	 * retrieves the placement rules for this Behaviour inside claims.
	 * @return PlacementRules instance encapsulating applicable placement rules.
	 */
	public PlacementRules getClaimsRules(){ return Claims;}
	
	private String BehaviourName;
	/**
	 * retrieves the name for this Behaviour. This will be used in any applicable messages.
	 * @return Name for this behaviour.
	 */
	public String getBehaviourName(){ return BehaviourName;}
	@Override
	public String toString(){
		return BehaviourName + " in the wilderness " + getWildernessRules().toString() + " and in claims " + getClaimsRules().toString();
		
	}
	public Object clone(){
		return new ClaimBehaviourData(this);
	}
	public ClaimBehaviourData(ClaimBehaviourData Source){
		
		this.BehaviourName = Source.BehaviourName;
		this.Claims= (PlacementRules) Source.Claims.clone();
		this.Wilderness = (PlacementRules)Source.Wilderness.clone();
		this.ClaimBehaviour = Source.ClaimBehaviour;
		
		
		
	}
	public ClaimBehaviourData(String pName,FileConfiguration Source,FileConfiguration outConfig,String NodePath,ClaimBehaviourData Defaults){
		
		BehaviourName = pName;
		//we want to read NodePath.BelowSeaLevelWilderness and whatnot.
		//bases Defaults off another ClaimBehaviourData instance.
		Wilderness = new PlacementRules(Source,outConfig,NodePath + ".Wilderness",Defaults.getWildernessRules());
		Claims = new PlacementRules (Source,outConfig,NodePath + ".Claims",Defaults.getClaimsRules());
		String strmode = Source.getString(NodePath + ".Claims.ClaimControl",Defaults.getBehaviourMode().name());
		//check for a requiredpermissions entry. If there isn't one, though, don't save it.
		
		
		ClaimBehaviour = ClaimBehaviourMode.parseMode(strmode);
		
		outConfig.set(NodePath +".Claims.ClaimControl",ClaimBehaviour.name());
		
		
	}
	public ClaimBehaviourData(String pName,PlacementRules pWilderness,PlacementRules pClaims,ClaimBehaviourMode behaviourmode){
		Wilderness = pWilderness;
		Claims = pClaims;
		this.ClaimBehaviour=behaviourmode;
		BehaviourName=pName;
		
	}
	
	
	public static ClaimBehaviourData getOutsideClaims(String pName) { return new ClaimBehaviourData(pName,PlacementRules.Both,PlacementRules.Neither,ClaimBehaviourMode.RequireNone);}
	public static ClaimBehaviourData getInsideClaims(String pName) {return new ClaimBehaviourData(pName,PlacementRules.Neither,PlacementRules.Both,ClaimBehaviourMode.RequireNone);}
	public static ClaimBehaviourData getAboveSeaLevel(String pName){return new ClaimBehaviourData(pName,PlacementRules.AboveOnly,PlacementRules.AboveOnly,ClaimBehaviourMode.RequireNone);};
	public static ClaimBehaviourData getBelowSeaLevel(String pName){return new ClaimBehaviourData(pName,PlacementRules.BelowOnly,PlacementRules.BelowOnly,ClaimBehaviourMode.RequireNone);};
	
	public static ClaimBehaviourData getNone(String pName){ return new ClaimBehaviourData(pName,PlacementRules.Neither,PlacementRules.Neither,ClaimBehaviourMode.RequireNone);}
	public static ClaimBehaviourData getAll(String pName){ return new ClaimBehaviourData(pName,PlacementRules.Both,PlacementRules.Both,ClaimBehaviourMode.RequireNone);}
	
	public ClaimBehaviourData setRequiredPermissions(String ... params){
		PlacementRules copyClaims = Claims.setRequiredPermissions(params);
		PlacementRules copyWilderness = Wilderness.setRequiredPermissions(params);
		ClaimBehaviourData copydata = new ClaimBehaviourData(this);
		copydata.Claims = copyClaims;
		copydata.Wilderness=copyWilderness;
		return copydata;
	}
	public ClaimBehaviourData setClaimRequiredPermission(String ... params){
		PlacementRules copyClaims = Claims.setRequiredPermissions(params);
		ClaimBehaviourData copydata = new ClaimBehaviourData(this);
		copydata.Claims = copyClaims;
		return copydata;
		
	}
	public ClaimBehaviourData setWildernessRequiredPermission(String ... params){
		PlacementRules copyWilds = Wilderness.setRequiredPermissions(params);
		ClaimBehaviourData copydata = new ClaimBehaviourData(this);
		copydata.Wilderness = copyWilds;
		return copydata;
	}
}