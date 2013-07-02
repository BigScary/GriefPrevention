package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event gets called whenever a claim is going to be deleted.
 * This event is not called when a claim is resized.
 * @author Tux2
 *
 */
public class ClaimDeletedEvent extends Event implements Cancellable {

	// Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    
    public HandlerList getHandlers() {
        return handlers;
    }
     
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    private Claim claim;
    private Player player;
    public ClaimDeletedEvent(Claim claim,Player player) {
    	this.claim = claim;
    	this.player=player;
    }
    
    /**
     * Gets the claim to be deleted.
     * @return
     */
    public Claim getClaim() {
    	return claim;
    }
    
    /**
     * returns the player deleting the Claim.
     * @return
     */
    public Player getPlayer(){ return player;}
    
    boolean canceled = false;

	
	public boolean isCancelled() {
		return canceled;
	}

	
	public void setCancelled(boolean iscancelled) {
		canceled = iscancelled;
	}
	

}
