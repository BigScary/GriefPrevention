package me.ryanhamshire.GriefPrevention.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when GP is about to deliver claim blocks to a player (~every 10 minutes)
 *
 * @author RoboMWM
 * 11/15/2016.
 */
public class AccrueClaimBlocksEvent extends Event
{
    // Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() {
        return handlers;
    }
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    private Player player;
    private int blocksToAccrue;
    private boolean cancelled = false;

    public AccrueClaimBlocksEvent(Player player, int blocksToAccrue)
    {
        this.player = player;
        this.blocksToAccrue = blocksToAccrue;
    }

    public Player getPlayer()
    {
        return this.player;
    }

    public int getBlocksToAccrue()
    {
        return this.blocksToAccrue;
    }

    public boolean isCancelled()
    {
        return this.cancelled;
    }

    /**
     * Modify the amount of claim blocks to deliver to the player
     * @param blocksToAccrue blocks to deliver
     */
    public void setBlocksToAccrue(int blocksToAccrue)
    {
        this.blocksToAccrue = blocksToAccrue;
    }

    public void setCancelled(boolean cancel)
    {
        this.cancelled = cancel;
    }
}
