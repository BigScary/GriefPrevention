package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Whenever a claim's data is modified, (besides being resized), this event will be called.
 * @author Tux2
 *
 */
public class ClaimModifiedEvent extends Event implements Cancellable {

	public enum Type {
		OwnerChanged,
		AddedAccessTrust,
		RemovedAccessTrust,
		AddedBuildTrust,
		RemovedBuildTrust,
		AddedInventoryTrust,
		RemovedInventoryTrust,
		AddedManager,
		RemovedManager,
		PermissionsCleared;
	}

	// Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    
    public HandlerList getHandlers() {
        return handlers;
    }
     
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    Claim claim;
    String player;
    Type type;
    
    public ClaimModifiedEvent(Claim claim, String player, Type type) {
    	this.claim = claim;
    	this.player = player;
    }
    
    public Claim getClaim() {
    	return claim;
    }
    
    public String getPlayer() {
    	return player;
    }
    
    public Type getType() {
    	return type;
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
