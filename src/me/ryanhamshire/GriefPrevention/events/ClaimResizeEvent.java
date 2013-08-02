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

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	private Claim claim;
	private Location newLesserBoundaryCorner;
	private Location newGreaterBoundaryCorner;

	public Location getNewLesserBoundaryCorner() {
		return newLesserBoundaryCorner;
	}

	public Location getNewGreaterBoundaryCorner() {
		return newGreaterBoundaryCorner;
	}

	public ClaimResizeEvent(Claim oldclaim, Location newLesserBoundary,
			Location newGreaterBoundary, Player Resizer) {
		super(oldclaim, Resizer);
		newLesserBoundaryCorner = newLesserBoundary;
		newGreaterBoundaryCorner = newGreaterBoundary;
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

	boolean canceled = false;

	public boolean isCancelled() {
		return canceled;
	}

	public void setCancelled(boolean iscancelled) {
		canceled = iscancelled;
	}

}
