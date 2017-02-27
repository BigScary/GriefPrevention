package me.ryanhamshire.GriefPrevention.listeners;

import me.ryanhamshire.GriefPrevention.events.custom.GPPlaceDestroyEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Created on 2/26/2017.
 *
 * @author RoboMWM
 */
public class WildernessRules implements Listener
{
    @EventHandler(ignoreCancelled = true)
    void wildernessRules(GPPlaceDestroyEvent event)
    {
        if (event.isPlayer()) return;
    }
}
