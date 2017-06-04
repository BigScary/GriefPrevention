package me.ryanhamshire.GriefPrevention.events.custom;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.metadata.Metadatable;

import javax.annotation.Nullable;

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
    private Metadatable causer;
    //Most of these events contain an entity as a causer.
    //Thus, we'll store it as an entity to avoid needless instanceof calls
    private Entity sourceEntity;
    private Location location;
    private Metadatable target;

    public GPBaseEvent(Event baseEvent, @Nullable Metadatable causer, Location location, Metadatable target)
    {
        this.baseEvent = baseEvent;
        this.location = location;
        this.target = target;
        this.causer = causer;
    }

    public GPBaseEvent(Event baseEvent, @Nullable Entity sourceEntity, Location location, Metadatable target)
    {
        this.baseEvent = baseEvent;
        this.sourceEntity = sourceEntity;
        this.causer = sourceEntity;
        this.location = location;
        this.target = target;
    }

    public Event getBaseEvent()
    {
        return baseEvent;
    }

    public Metadatable getCauser()
    {
        return causer;
    }

    /**
     * Gets the causer entity.
     * @return null if no causer, or if causer is not an entity
     */
    public Entity getSourceEntity()
    {
        return sourceEntity;
    }

    public boolean isPlayer()
    {
        return sourceEntity != null && sourceEntity.getType() == EntityType.PLAYER;
    }

    public Player getPlayer()
    {
        if (isPlayer())
            return (Player) sourceEntity;
        return null;
    }

    /**
     * Gets location of target block/entity
     * @return
     */
    public Location getLocation()
    {
        return location;
    }

    public Metadatable getTarget()
    {
        return target;
    }
}
