package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Whenever a claim is created this event is called, after it has been added to
 * the datastore.
 */
public class ClaimAfterCreateEvent extends PlayerClaimEvent {

	// Custom Event Requirements
	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;

	}

	public ClaimAfterCreateEvent(Claim claim, Player p) {
		super(claim, p);

	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

}
