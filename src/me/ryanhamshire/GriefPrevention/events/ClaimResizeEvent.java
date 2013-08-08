package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Whenever a claim is resized this event is called.
 * 
 * @author Tux2
 * 
 */
public class ClaimResizeEvent extends PlayerClaimEvent implements Cancellable {

	// Custom Event Requirements
	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;
	}

	boolean canceled = false;

	private Claim claim;
	private Location newGreaterBoundaryCorner;
	private Location newLesserBoundaryCorner;

	public ClaimResizeEvent(Claim oldclaim, Location newLesserBoundary, Location newGreaterBoundary, Player Resizer) {
		super(oldclaim, Resizer);
		newLesserBoundaryCorner = newLesserBoundary;
		newGreaterBoundaryCorner = newGreaterBoundary;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public Location getNewGreaterBoundaryCorner() {
		return newGreaterBoundaryCorner;
	}

	public Location getNewLesserBoundaryCorner() {
		return newLesserBoundaryCorner;
	}

	/**
	 * retrieves the Player performing this resize. This is the same result as
	 * the inherited getPlayer() event.
	 * 
	 * @return
	 */
	public Player getResizer() {
		return p;
	}

	public boolean isCancelled() {
		return canceled;
	}

	public void setCancelled(boolean iscancelled) {
		canceled = iscancelled;
	}

}
