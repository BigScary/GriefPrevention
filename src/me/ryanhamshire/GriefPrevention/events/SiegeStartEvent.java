package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.SiegeData;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
/**
 * Cancellable Event called when a Siege is about to start. 
 * @author BC_Programming
 *
 */
public class SiegeStartEvent extends Event implements Cancellable{

	// Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    
    public HandlerList getHandlers() {
        return handlers;
    }
     
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    SiegeData SiegeInfo;
    /**
     * returns the Information of the upcoming Siege.
     * @return
     */
    public SiegeData getSiegeData(){ return SiegeInfo;}
    public SiegeStartEvent(SiegeData sd) {
    	SiegeInfo = sd;
    }
    
  
    boolean canceled = false;

	public boolean isCancelled() {
		return canceled;
	}

	
	public void setCancelled(boolean iscancelled) {
		canceled = iscancelled;
	}
	
}
