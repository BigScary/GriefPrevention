package me.ryanhamshire.GriefPrevention.Configuration;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;
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
		None,
		ForceAllow,
		RequireOwner,
		RequireManager,
		RequireAccess,
		RequireContainer;
		
		
		public static ClaimBehaviourMode parseMode(String name){
			//System.out.println("Looking for " + name);
			for(ClaimBehaviourMode cb:ClaimBehaviourMode.values()){
				System.out.println("Comparing " + cb.name() + " to " + name);
				if(cb.name().equalsIgnoreCase(name))
					return cb;
			}
			return ClaimBehaviourMode.None;
			
		}
		
	}
	private PlacementRules Wilderness;
	private PlacementRules Claims;
	private ClaimBehaviourMode ClaimBehaviour = ClaimBehaviourMode.None;
	
	public ClaimBehaviourMode getClaimBehaviour(){ return ClaimBehaviour;}
	
	public ClaimAllowanceConstants Allowed(Location position,Player RelevantPlayer){
		return Allowed(position,RelevantPlayer,true);	
	}
	public ClaimAllowanceConstants Allowed(Location position,Player RelevantPlayer,boolean ShowMessages){
		
		System.out.println("ClaimBehaviour");
		//System.out.println("Testing Allowed," + BehaviourName);
		String result=null;
		Claim testclaim = GriefPrevention.instance.dataStore.getClaimAt(position, true, null);
		if(ClaimBehaviour!=ClaimBehaviourMode.None && testclaim!=null){
			
			//if forcibly allowed, allow.
			if(ClaimBehaviour == ClaimBehaviourMode.ForceAllow){
				return ClaimAllowanceConstants.Allow;
			}
			else if(ClaimBehaviour == ClaimBehaviourMode.RequireOwner){
				//RequireOwner means it only applies if the player passed is the owner.
				//if the passed player is null, then we assume the operation has no relevant player, so allow it in this case.
				if(RelevantPlayer!=null){
					if(!testclaim.getOwnerName().equalsIgnoreCase(RelevantPlayer.getName())){
						if(ShowMessages)
							GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err, Messages.NoOwnerTrust,testclaim.getOwnerName());
						//they aren't the owner, so fail the test.
						return ClaimAllowanceConstants.Deny;
					}
					
				}
			}
			else if(ClaimBehaviour == ClaimBehaviourMode.RequireAccess){
				if(RelevantPlayer!=null){
					if(null!=(result =testclaim.allowAccess(RelevantPlayer))){
						if(ShowMessages)
							GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err, result);
						return ClaimAllowanceConstants.Deny;
					}
				}
			}
			else if(ClaimBehaviour == ClaimBehaviourMode.RequireContainer){
				if(RelevantPlayer!=null){
					if(null!=(result = testclaim.allowContainers(RelevantPlayer))){
						if(ShowMessages)
							GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err, result);
						return ClaimAllowanceConstants.Deny;
					}
				}
			}
			else if(ClaimBehaviour == ClaimBehaviourMode.RequireManager){
				if(RelevantPlayer!=null){
					if(!testclaim.isManager(RelevantPlayer.getName())){
						if(ShowMessages)
							GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err, "You need Manager Permissions to do that here.");
						return ClaimAllowanceConstants.Deny;
					}
				}
			}
		}

		
		//retrieve the appropriate Sea Level for this world.
		/*int sealevel = GriefPrevention.instance.getWorldCfg(position.getWorld()).seaLevelOverride();
		int yposition = position.getBlockY();
		boolean abovesealevel = yposition > sealevel;*/
		if(testclaim==null){
			//we aren't inside a claim.
			//System.out.println(BehaviourName + "Wilderness test...");
			ClaimAllowanceConstants wildernessresult = Wilderness.Allow(position)?ClaimAllowanceConstants.Allow:ClaimAllowanceConstants.Deny;
			if(!wildernessresult.Denied() && ShowMessages){
				GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err, Messages.ConfigDisabled,this.BehaviourName);
			}
			return wildernessresult;
			
		}
		else{
			//we are inside a claim.
			//System.out.println(BehaviourName + "Claim test...");
			ClaimAllowanceConstants claimresult = Claims.Allow(position)?ClaimAllowanceConstants.Allow:ClaimAllowanceConstants.Deny;
			if(claimresult.Denied() && ShowMessages)
			{
				GriefPrevention.sendMessage(RelevantPlayer, TextMode.Err, Messages.ConfigDisabled,this.BehaviourName);
				return claimresult;
			}
		}
		
		return ClaimAllowanceConstants.Allow;
		
	}
	public PlacementRules getWildernessRules() { return Wilderness;}
	public PlacementRules getClaimsRules(){ return Claims;}
	private String BehaviourName;
	public String getBehaviourName(){ return BehaviourName;}
	@Override
	public String toString(){
		return BehaviourName + " in the wilderness " + getWildernessRules().toString() + " and in claims " + getClaimsRules().toString();
		
	}
	
	public ClaimBehaviourData(String pName,FileConfiguration Source,FileConfiguration outConfig,String NodePath,ClaimBehaviourData Defaults){
		
		BehaviourName = pName;
		//we want to read NodePath.BelowSeaLevelWilderness and whatnot.
		//bases Defaults off another ClaimBehaviourData instance.
		Wilderness = new PlacementRules(Source,outConfig,NodePath + ".Wilderness",Defaults.getWildernessRules());
		Claims = new PlacementRules (Source,outConfig,NodePath + ".Claims",Defaults.getClaimsRules());
		
		
		String claimbehave = Source.getString(NodePath + ".Claims.Behaviour","None");
		//System.out.println(NodePath + ".Claims.Behaviour:" + claimbehave);
		ClaimBehaviour = ClaimBehaviourMode.parseMode(claimbehave);
		
		outConfig.set(NodePath + ".Claims.Behaviour",ClaimBehaviour.name());
		
	}
	public ClaimBehaviourData(String pName,PlacementRules pWilderness,PlacementRules pClaims,
			ClaimBehaviourMode cb){
		Wilderness = pWilderness;
		Claims = pClaims;
		ClaimBehaviour = cb;
		BehaviourName=pName;
		
	}
	
	
	public static ClaimBehaviourData getOutsideClaims(String pName) { return new ClaimBehaviourData(pName,PlacementRules.Both,PlacementRules.Neither,ClaimBehaviourMode.None);}
	public static ClaimBehaviourData getInsideClaims(String pName) {return new ClaimBehaviourData(pName,PlacementRules.Neither,PlacementRules.Neither,ClaimBehaviourMode.None);}
	public static ClaimBehaviourData getAboveSeaLevel(String pName){return new ClaimBehaviourData(pName,PlacementRules.AboveOnly,PlacementRules.AboveOnly,ClaimBehaviourMode.None);}
	public static ClaimBehaviourData getBelowSeaLevel(String pName){return new ClaimBehaviourData(pName,PlacementRules.BelowOnly,PlacementRules.BelowOnly,ClaimBehaviourMode.None);}
	public static ClaimBehaviourData getNone(String pName){ return new ClaimBehaviourData(pName,PlacementRules.Neither,PlacementRules.Neither,ClaimBehaviourMode.None);}
	public static ClaimBehaviourData getAll(String pName){ return new ClaimBehaviourData(pName,PlacementRules.Both,PlacementRules.Both,ClaimBehaviourMode.None);}
	
}