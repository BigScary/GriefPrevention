package me.ryanhamshire.GriefPrevention.events.custom;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;

import static org.bukkit.event.EventPriority.LOW;

/**
 * Fires the respective custom GP events, and handles cancellation
 * TODO: listen at lowest priority?
 * TODO: call events even if canceled?
 * Created on 2/23/2017.
 *
 * @author RoboMWM
 */
public class BukkitToGPEventListener implements Listener
{
    GriefPrevention instance;
    public BukkitToGPEventListener(GriefPrevention griefPrevention)
    {
        this.instance = griefPrevention;
    }

    private boolean callEvent(GPBaseEvent event)
    {
        Cancellable baseEvent = null;

        //GP event.isCancelled() = base event.isCancelled()
        if (event.getBaseEvent() instanceof Cancellable)
        {
            baseEvent = (Cancellable)event.getBaseEvent();
            event.setCancelled(baseEvent.isCancelled());
        }

        instance.getServer().getPluginManager().callEvent(event);

        //base event.isCancelled() = GP event.isCancelled()
        if (baseEvent != null)
        {
            baseEvent.setCancelled(event.isCancelled());
        }

        return event.isCancelled();
    }

    @EventHandler(priority = LOW)
    void onBlockPlace(BlockPlaceEvent event)
    {
        //getBlock vs. getBlockPlaced - from Javadoc:
        //public Block getBlockPlaced()
        //Clarity method for getting the placed block. Not really needed except for reasons of clarity.

        callEvent(new GPPlaceDestroyEvent(event, event.getPlayer(), event.getBlock().getLocation(), event.getBlock()));
    }
    @EventHandler(priority = LOW)
    void onBlockBreak(BlockBreakEvent event)
    {
        callEvent(new GPPlaceDestroyEvent(event, event.getPlayer(), event.getBlock().getLocation(), event.getBlock()));
    }

    @EventHandler(priority = LOW)
    void onPaintingPlace(HangingPlaceEvent event)
    {
        //TODO: block location, or entity location?
        callEvent(new GPPlaceDestroyEvent(event, event.getPlayer(), event.getEntity().getLocation(), event.getEntity()));
    }

    @EventHandler(priority = LOW)
    void onPaintingBreak(HangingBreakEvent event)
    {
        Entity destroyerEntity = null;
        if (event instanceof HangingBreakByEntityEvent)
        {
            HangingBreakByEntityEvent entityEvent = (HangingBreakByEntityEvent)event;
            destroyerEntity = entityEvent.getRemover();
        }

        callEvent(new GPPlaceDestroyEvent(event, destroyerEntity, event.getEntity().getLocation(), event.getEntity()));
    }


}
