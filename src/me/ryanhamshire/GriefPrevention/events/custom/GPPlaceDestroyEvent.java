package me.ryanhamshire.GriefPrevention.events.custom;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.metadata.Metadatable;

import javax.annotation.Nullable;

/**
 * Fired when a block or block-like entity is placed or destroyed
 * Block-like entities include item frames, armor stands, paintings, etc.
 *
 * Created on 2/23/2017.
 *
 * @author RoboMWM
 */
public class GPPlaceDestroyEvent extends GPBaseEvent
{
    private Entity entity;
    private Location location;
    private Metadatable thing; //TODO: rename?

    public GPPlaceDestroyEvent(Event baseEvent, @Nullable Entity entity, Location location, Metadatable thing)
    {
        super(baseEvent);
        this.entity = entity;
        this.location = location;
        this.thing = thing;
    }

    public Entity getEntity()
    {
        return entity;
    }

    public boolean isPlayer()
    {
        return entity != null && entity.getType() == EntityType.PLAYER;
    }

    public Player getPlayer()
    {
        if (isPlayer())
            return (Player)entity;
        return null;
    }

    public Location getLocation()
    {
        return location;
    }

    public Metadatable getThing()
    {
        return thing;
    }
}
