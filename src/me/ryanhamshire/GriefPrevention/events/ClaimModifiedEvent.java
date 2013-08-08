package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Whenever a claim's data is modified, (besides being resized), this event will
 * be called.
 * 
 * @author Tux2
 * 
 */
public class ClaimModifiedEvent extends ClaimEvent implements Cancellable {

	public enum Type {
		AddedAccessTrust, AddedBuildTrust, AddedInventoryTrust, AddedManager, OwnerChanged, PermissionsCleared, RemovedAccessTrust, RemovedBuildTrust, RemovedInventoryTrust, RemovedManager;
	}

	// Custom Event Requirements
	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;
	}

	boolean canceled = false;

	private String PlayerChanged;

	Type type;

	public ClaimModifiedEvent(Claim claim, String pPlayerChanged, Type type) {
		super(claim);
		this.type = type;
		PlayerChanged = pPlayerChanged;
	}

	@Override
	public Claim getClaim() {
		return claim;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public String getPlayerChanged() {
		return PlayerChanged;
	}

	public Type getType() {
		return type;
	}

	public boolean isCancelled() {
		return canceled;
	}

	public void setCancelled(boolean iscancelled) {
		canceled = iscancelled;
	}

}
