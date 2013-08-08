package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class ClaimTransferEvent extends ClaimEvent implements Cancellable {

	// Custom Event Requirements
	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;

	}

	private boolean Cancelled = false;

	private String TargetPlayer;

	public ClaimTransferEvent(Claim c, String targetPlayer) {
		super(c);
		TargetPlayer = targetPlayer;
		//
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * returns the target player for the pending transfer.
	 * 
	 * @return
	 */
	public String getTargetPlayer() {
		return TargetPlayer;
	}

	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return Cancelled;
	}

	public void setCancelled(boolean argument) {
		Cancelled = argument;

	}

}
