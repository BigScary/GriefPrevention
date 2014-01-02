package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/***
 * abstract base class for all Claim-related events. includes a Claim field and accessors and extends from the
 * Bukkit event class.
 */

public abstract class ClaimEvent extends Event {
	// Custom Event Requirements
	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;

	}

	protected Claim claim;

	protected ClaimEvent(Claim c) {
		claim = c;
	}

	/**
	 * the claim being affected
	 * 
	 * @return
	 */
	public Claim getClaim() {
		return claim;

	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
