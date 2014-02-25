package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData;
import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData.ClaimAllowanceConstants;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a Permission is checked. Can be used to change the Permission
 * result that will be returned. the Result value defaults to Null, if an event
 * changes it that change will be returned without performing further checks.
 */
public class PermissionCheckEvent extends Event {

	// Custom Event Requirements
	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;
	}

	private Player CheckPlayer;

	private ClaimBehaviourData PermissionCheck;
	private ClaimAllowanceConstants Result = null;

	public PermissionCheckEvent(ClaimBehaviourData Permission, Player p) {
		PermissionCheck = Permission;
		CheckPlayer = p;

	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

    /**
     * retrieves the permission/ClaimBehaviourData instance being checked.
     * @return Permission being checked.
     */
	public ClaimBehaviourData getPermissionCheck() {
		return PermissionCheck;
	}

    /**
     * retrieves the Player applicable to this Permission check.
     * @return
     */
	public Player getPlayer() {
		return CheckPlayer;
	}

    /**
     * retrieves any value set
     * in a previous call to setResult().
     * @return
     */
	public ClaimAllowanceConstants getResult() {
		return Result;
	}

    /**
     * sets the ClaimAllowanceConstants enumeration value that should result from the Allowed() call that caused this event to occur.
     * @param value new value for Rule check Result.
     */
	public void setResult(ClaimAllowanceConstants value) {
		Result = value;
	}

}
