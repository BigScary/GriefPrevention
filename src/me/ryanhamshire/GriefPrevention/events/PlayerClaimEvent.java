package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public abstract class PlayerClaimEvent extends ClaimEvent {
	// Custom Event Requirements
	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;

	}

	protected Player p;

	/**
	 * returns the relevant player for this event.
	 * 
	 * @return
	 */
	public Player getPlayer() {
		return p;
	}

	protected PlayerClaimEvent(Claim pClaim, Player pPlayer) {
		super(pClaim);
		p = pPlayer;

	}
}
