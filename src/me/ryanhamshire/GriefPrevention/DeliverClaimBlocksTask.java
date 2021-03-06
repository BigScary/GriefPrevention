/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 
 package me.ryanhamshire.GriefPrevention;

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.entity.Player;

//FEATURE: give players claim blocks for playing, as long as they're not away from their computer

//runs every 5 minutes in the main thread, grants blocks per hour / 12 to each online player who appears to be actively playing
class DeliverClaimBlocksTask implements Runnable 
{	
    private Player player;
    
    public DeliverClaimBlocksTask(Player player)
    {
        this.player = player;
    }
    
	@Override
	public void run()
	{
	    //if no player specified, this task will create a player-specific task for each online player, scheduled one tick apart
	    if(this.player == null)
		{
	        @SuppressWarnings("unchecked")
            Collection<Player> players = (Collection<Player>)GriefPrevention.instance.getServer().getOnlinePlayers();
	        
	        long i = 0;
	        for(Player onlinePlayer : players)
	        {
	            DeliverClaimBlocksTask newTask = new DeliverClaimBlocksTask(onlinePlayer);
	            GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, newTask, i++);
	        }
		}
	    
	    //otherwise, deliver claim blocks to the specified player
	    else
	    {
	        if(!this.player.isOnline()) return;
	        
	        DataStore dataStore = GriefPrevention.instance.dataStore;
            PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
            
            Location lastLocation = playerData.lastAfkCheckLocation;
            try
            {
                //if he's not in a vehicle and has moved at least three blocks since the last check
                //and he's not being pushed around by fluids
                if(!player.isInsideVehicle() && 
                   (lastLocation == null || lastLocation.distanceSquared(player.getLocation()) >= 0) &&
                   !player.getLocation().getBlock().isLiquid())
                {                   
                    //determine how fast blocks accrue for this player
                    int accrualRate = GriefPrevention.instance.config_claims_blocksAccruedPerHour_default;
                    if(player.hasPermission("griefprevention.fastestaccrual")) accrualRate = GriefPrevention.instance.config_claims_blocksAccruedPerHour_fastest;
                    else if(player.hasPermission("griefprevention.fasteraccrual")) accrualRate = GriefPrevention.instance.config_claims_blocksAccruedPerHour_faster;
                    
                    //add blocks
                    int accruedBlocks = accrualRate / 6;
                    if(accruedBlocks < 0) accruedBlocks = 1;
                    playerData.accrueBlocks(accruedBlocks);
                    GriefPrevention.AddLogEntry("Delivering " + accruedBlocks + " blocks to " + player.getName(), CustomLogEntryTypes.Debug, true);
                    
                    //intentionally NOT saving data here to reduce overall secondary storage access frequency
                    //many other operations will cause this player's data to save, including his eventual logout
                    //dataStore.savePlayerData(player.getUniqueIdentifier(), playerData);
                }
                else
                {
                    GriefPrevention.AddLogEntry(player.getName() + " wasn't active enough to accrue claim blocks this round.", CustomLogEntryTypes.Debug, true);
                }
            }
            catch(IllegalArgumentException e)  //can't measure distance when to/from are different worlds
            {
                
            }
            catch(Exception e)
            {
                GriefPrevention.AddLogEntry("Problem delivering claim blocks to player " + player.getName() + ":");
                e.printStackTrace();
            }
            
            //remember current location for next time
            playerData.lastAfkCheckLocation = player.getLocation();
	    }
	}
}
