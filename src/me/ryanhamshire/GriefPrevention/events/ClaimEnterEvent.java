package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * hey look the event that was never going to get added was added. What do ya
 * know. Fired when the movementwatcher is enabled when players enter and claims
 * 
 * 
 */
public class ClaimEnterEvent extends PlayerClaimEvent {

	// Custom Event Requirements
	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public ClaimEnterEvent(Claim c, Player p) {
		super(c, p);
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
