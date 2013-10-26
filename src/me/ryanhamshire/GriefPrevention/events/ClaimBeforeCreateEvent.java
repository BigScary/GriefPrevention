package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Whenever a claim is created this event is called. This event is called before
 * the claim is added to the dataStore and initialized, as such it does not yet
 * have an ID. The ID Value of a Claim does not exist before it is created and
 * any attempt to "fake" an ID by using the nextClaimID value will end in tears.
 * If you need to process events for new claims as they are made,
 * consider using the ClaimAfterCreateEvent.
 */
public class ClaimBeforeCreateEvent extends PlayerClaimEvent implements Cancellable {

	// Custom Event Requirements
	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;

	}

	boolean canceled = false;

	public ClaimBeforeCreateEvent(Claim claim, Player p) {
		super(claim, p);

	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public boolean isCancelled() {
		return canceled;
	}

	public void setCancelled(boolean iscancelled) {
		canceled = iscancelled;
	}

}
