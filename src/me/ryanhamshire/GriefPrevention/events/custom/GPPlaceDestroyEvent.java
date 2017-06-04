package me.ryanhamshire.GriefPrevention.events.custom;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.metadata.Metadatable;

import javax.annotation.Nullable;

/**
 * Fired when a block or block-like sourceEntity is placed or destroyed
 * Block-like entities include item frames, armor stands, paintings, etc.
 *
 * Created on 2/23/2017.
 *
 * @author RoboMWM
 */
public class GPPlaceDestroyEvent extends GPBaseEvent
{
    public GPPlaceDestroyEvent(Event baseEvent, @Nullable Entity sourceEntity, Location location, Metadatable target)
    {
        super(baseEvent, sourceEntity, location, target);
    }
    public GPPlaceDestroyEvent(Event baseEvent, @Nullable Metadatable source, Location location, Metadatable target)
    {
        super(baseEvent, source, location, target);
    }
}
