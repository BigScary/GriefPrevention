package me.ryanhamshire.GriefPrevention.events.custom;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;

import java.util.ArrayList;
import java.util.List;

import static org.bukkit.event.EventPriority.LOWEST;

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

    private boolean callWithoutCancelingEvent(GPBaseEvent event)
    {
        instance.getServer().getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    @EventHandler(priority = LOWEST)
    void onBlockPlace(BlockPlaceEvent event)
    {
        //getBlock vs. getBlockPlaced - from Javadoc:
        //public Block getBlockPlaced()
        //Clarity method for getting the placed block. Not really needed except for reasons of clarity.

        callEvent(new GPPlaceDestroyEvent(event, event.getPlayer(), event.getBlock().getLocation(), event.getBlock()));
    }
    @EventHandler(priority = LOWEST)
    void onBlockBreak(BlockBreakEvent event)
    {
        callEvent(new GPPlaceDestroyEvent(event, event.getPlayer(), event.getBlock().getLocation(), event.getBlock()));
    }

    @EventHandler(priority = LOWEST)
    void onPaintingPlace(HangingPlaceEvent event)
    {
        //TODO: block location, or entity location? Big_Scary used entity location
        callEvent(new GPPlaceDestroyEvent(event, event.getPlayer(), event.getEntity().getLocation(), event.getEntity()));
    }
    @EventHandler(priority = LOWEST)
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

    @EventHandler(priority = LOWEST)
    void onVehicleDamage(VehicleDamageEvent event)
    {
        callEvent(new GPPlaceDestroyEvent(event, event.getAttacker(), event.getVehicle().getLocation(), event.getAttacker()));
    }

    @EventHandler(priority = LOWEST)
    void onEntityExplode(EntityExplodeEvent event)
    {
        //Call an event for each block that's to-be-destroyed
        //Thus, the base event won't be canceled unless it's explicitly canceled
        List<Block> blocksToRemove = new ArrayList<Block>();
        for (Block block : event.blockList())
        {
            if (callWithoutCancelingEvent(new GPPlaceDestroyEvent(event, event.getEntity(), block.getLocation(), block)))
                blocksToRemove.add(block);
        }
        event.blockList().removeAll(blocksToRemove);
    }
    @EventHandler(priority = LOWEST)
    void onBlockExplode(BlockExplodeEvent event) //largely same as above, but block as source
    {
        List<Block> blocksToRemove = new ArrayList<Block>();
        for (Block block : event.blockList())
        {
            if (callWithoutCancelingEvent(new GPPlaceDestroyEvent(event, event.getBlock(), block.getLocation(), block)))
                blocksToRemove.add(block);
        }
        event.blockList().removeAll(blocksToRemove);
    }

    @EventHandler(priority = LOWEST)
    void onEntityFormBlock(EntityBlockFormEvent event) //Frost walker
    {
        callEvent(new GPPlaceDestroyEvent(event, event.getEntity(), event.getBlock().getLocation(), event.getBlock()));
    }

    @EventHandler(priority = LOWEST)
    void onEntityChangeBlock(EntityChangeBlockEvent event)
    {
        callEvent(new GPPlaceDestroyEvent(event, event.getEntity(), event.getBlock().getLocation(), event.getBlock()));
    }

    /////////////

    @EventHandler(priority = LOWEST)
    public void onEntityPickup(EntityChangeBlockEvent event) //Endermen
    {
        callEvent(new GPPlaceDestroyEvent(event, event.getEntity(), event.getBlock().getLocation(), event.getBlock()));
    }
}
