package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Fired when the movementwatcher is enabled when players enter claims
 * Any plugins handling this event will need to enable the MovementWatcher, using
 * the static GriefPrevention.InitializeMovementWatcher method.
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
