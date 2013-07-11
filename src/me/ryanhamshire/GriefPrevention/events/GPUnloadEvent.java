package me.ryanhamshire.GriefPrevention.events;


import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
/**
 * Event fired when GP is about to unload itself. This is done immediately before it invalidates it's own
 * Data structures. This is the ideal time for dependent plugins
 * to Save data related to GP, particularly those that use the Metadata features the plugin
 * provides.
 */
public class GPUnloadEvent extends Event{
	// Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    
    public HandlerList getHandlers() {
        return handlers;
    }
     
    public static HandlerList getHandlerList() {
        return handlers;
    }
    private GriefPrevention _Instance;
    public GriefPrevention getInstance(){return _Instance;}
    public GPUnloadEvent(GriefPrevention newInstance){
    	_Instance = newInstance;
    }
}
