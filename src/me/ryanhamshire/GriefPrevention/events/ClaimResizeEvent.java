package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.Location;
import org.bukkit.entity.Player;
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
    
    private Claim claim;
    private Location newLesserBoundaryCorner;
    private Location newGreaterBoundaryCorner;
    
    public Location getNewLesserBoundaryCorner(){ return newLesserBoundaryCorner;}
    public Location getNewGreaterBoundaryCorner(){ return newGreaterBoundaryCorner;}
    private Player resizer;
  
    public ClaimResizeEvent(Claim oldclaim, Location newLesserBoundary,Location newGreaterBoundary,Player Resizer) {
    	this.claim = oldclaim;
    	newLesserBoundaryCorner = newLesserBoundary;
    	newGreaterBoundaryCorner=newGreaterBoundary;
    	this.resizer = Resizer;
    }
    
    /**
     * Gets the claim being resized.
     * @return
     */
    public Claim getClaim(){ return claim;}
    
    
    
    
    /**
     * retrieves the Player performing this resize.
     * @return
     */
    public Player getResizer(){ return resizer;}
    boolean canceled = false;

	public boolean isCancelled() {
		return canceled;
	}

	
	public void setCancelled(boolean iscancelled) {
		canceled = iscancelled;
	}

}
