package me.ryanhamshire.GriefPrevention.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import me.ryanhamshire.GriefPrevention.Claim;

public class ClaimTransferEvent extends ClaimEvent implements Cancellable {

	// Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    
    public HandlerList getHandlers() {
        return handlers;
    }
     
    public static HandlerList getHandlerList() {
        return handlers;
        
        
    }
    private boolean Cancelled=false;
    public boolean isCancelled() {
		// TODO Auto-generated method stub
		return Cancelled;
	}

	public void setCancelled(boolean argument) {
		Cancelled=argument;
		
	}

	private String TargetPlayer;
	/**
	 * returns the target player for the pending transfer.
	 * @return
	 */
	public String getTargetPlayer(){ return TargetPlayer;}
	
	
	public ClaimTransferEvent(Claim c,String targetPlayer) {
		super(c);
		TargetPlayer=targetPlayer;
		// 
	}

	
}
