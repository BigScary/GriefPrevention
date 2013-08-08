package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.SiegeData;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event notification for when Sieges finish. This event cannot be cancelled.
 * 
 * @author BC_Programming
 * 
 */
public class SiegeEndEvent extends Event {

	// Custom Event Requirements
	private static final HandlerList handlers = new HandlerList();

	public static HandlerList getHandlerList() {
		return handlers;
	}

	SiegeData SiegeInfo;

	public SiegeEndEvent(SiegeData sd) {
		SiegeInfo = sd;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public SiegeData getSiegeData() {
		return SiegeInfo;
	}

}
