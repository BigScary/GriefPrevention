package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Whenever a claim is created this event is called.
 */
public class ClaimCreatedEvent extends PlayerClaimEvent implements Cancellable {

	// Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    
    public HandlerList getHandlers() {
        return handlers;
    }
     
    public static HandlerList getHandlerList() {
        return handlers;
        
        
    }
    
  
    public ClaimCreatedEvent(Claim claim,Player p) {
    	super(claim,p);
    	
    }
    
    
    boolean canceled = false;

	
	public boolean isCancelled() {
		return canceled;
	}

	
	public void setCancelled(boolean iscancelled) {
		canceled = iscancelled;
	}

}
