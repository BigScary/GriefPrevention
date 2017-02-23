package me.ryanhamshire.GriefPrevention.events.custom;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.metadata.Metadatable;

/**
 * Foundation event
 *
 * Created on 2/23/2017.
 *
 * @author RoboMWM
 */
public class GPBaseEvent extends Event implements Cancellable
{
    // Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList()
    {
        return handlers;
    }
    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }
    private boolean cancel = false;
    public boolean isCancelled()
    {
        return cancel;
    }
    public void setCancelled(boolean cancelled)
    {
        this.cancel = cancelled;
    }

    private Event baseEvent;

    public GPBaseEvent(Event baseEvent)
    {
        this.baseEvent = baseEvent;
    }

    public Event getBaseEvent()
    {
        return baseEvent;
    }
}
