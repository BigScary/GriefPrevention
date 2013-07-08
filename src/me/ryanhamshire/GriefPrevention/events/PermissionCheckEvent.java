package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData;
import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData.ClaimAllowanceConstants;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a Permission is checked. Can be used to change the Permission result that will
 * be returned. the Result value defaults to Null, if an event changes it that change will be returned
 * without performing further checks.
 */
public class PermissionCheckEvent extends Event {

	// Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    
    public HandlerList getHandlers() {
        return handlers;
    }
     
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    private ClaimBehaviourData PermissionCheck;
    private Player CheckPlayer;
    private ClaimAllowanceConstants Result=null;
    
    public ClaimBehaviourData getPermissionCheck(){ return PermissionCheck;}
    public Player getPlayer(){ return CheckPlayer;}
    public ClaimAllowanceConstants getResult(){ return Result;}
    public void setResult(ClaimAllowanceConstants value){ Result = value;}
    
    public PermissionCheckEvent(ClaimBehaviourData Permission,Player p){
    	PermissionCheck = Permission;
    	CheckPlayer=p;
    	
    	
    }
    
   
  

}
