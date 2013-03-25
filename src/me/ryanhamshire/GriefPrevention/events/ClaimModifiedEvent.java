package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ClaimModifiedEvent extends Event {

	// Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    
    public HandlerList getHandlers() {
        return handlers;
    }
     
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    Claim claim;
    
    public ClaimModifiedEvent(Claim claim) {
    	this.claim = claim;
    }
    
    public Claim getClaim() {
    	return claim;
    }

}
