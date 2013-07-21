package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;

public abstract class ClaimEvent extends Event {
	// Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    
    public HandlerList getHandlers() {
        return handlers;
    }
     
    public static HandlerList getHandlerList() {
        return handlers;
        
        
    }
    
    protected Claim claim;
    /**
     * the claim being affected
     * @return
     */
    public Claim getClaim() {
    	return claim;
    	
    }
    protected ClaimEvent(Claim c){
    	claim = c;
    }
}
