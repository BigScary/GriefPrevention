package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ClaimResizeEvent extends Event implements Cancellable {

	// Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    
    public HandlerList getHandlers() {
        return handlers;
    }
     
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    Claim oldclaim;
    Claim newclaim;
    
    public ClaimResizeEvent(Claim oldclaim, Claim newclaim) {
    	this.oldclaim = oldclaim;
    	this.newclaim = newclaim;
    }
    
    public Claim getOldClaim() {
    	return oldclaim;
    }
    
    public Claim getNewClaim() {
    	return newclaim;
    }
    
    boolean canceled = false;

	@Override
	public boolean isCancelled() {
		return canceled;
	}

	@Override
	public void setCancelled(boolean iscancelled) {
		canceled = iscancelled;
	}

}
