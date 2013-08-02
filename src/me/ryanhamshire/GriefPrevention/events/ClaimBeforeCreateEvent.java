package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Whenever a claim is created this event is called. This event is called before
 * the claim is added to the dataStore and initialized, as such it does not yet
 * have an ID. If you need to process events for new claims as they are made,
 * consider using the ClaimAfterCreateEvent.
 */
public class ClaimBeforeCreateEvent extends PlayerClaimEvent implements
		Cancellable {

	// Custom Event Requirements
	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;

	}

	public ClaimBeforeCreateEvent(Claim claim, Player p) {
		super(claim, p);

	}

	boolean canceled = false;

	public boolean isCancelled() {
		return canceled;
	}

	public void setCancelled(boolean iscancelled) {
		canceled = iscancelled;
	}

}
