package me.ryanhamshire.GriefPrevention.events;


import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GPLoadEvent extends Event{
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
    public GPLoadEvent(GriefPrevention newInstance){
    	_Instance = newInstance;
    }
}
