package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Whenever a claim is resized this event is called.
 * @author Tux2
 *
 */
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
    
    /**
     * Gets the existing claim.
     * @return
     */
    public Claim getOldClaim() {
    	return oldclaim;
    }
    
    /**
     * How the claim will look after the resize.
     * @return
     */
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
