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
 
 package me.ryanhamshire.GriefPrevention.tasks;

import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;

import org.bukkit.Location;
import org.bukkit.entity.Player;

//FEATURE: give players claim blocks for playing, as long as they're not away from their computer

//runs every 5 minutes in the main thread, grants blocks per hour / 12 to each online player who appears to be actively playing
public class DeliverClaimBlocksTask implements Runnable 
{	
	public void run()
	{
		Player [] players = GriefPrevention.instance.getServer().getOnlinePlayers();
		
		//ensure players get at least 1 block (if accrual is totally disabled, this task won't even be scheduled)
		//BC: refactored, now it calculates the blocks that have been accrued on a per-Player basis.
		
		
		//for each online player
		for(Player player:players)
		{
			
			WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
			
			int accruedBlocks = Math.max(1, (int)(wc.getClaimBlocksAccruedPerHour() / 12));
			
			
			
			DataStore dataStore = GriefPrevention.instance.dataStore;
			PlayerData playerData = dataStore.getPlayerData(player.getName());
			
			Location lastLocation = playerData.lastAfkCheckLocation;
			try  //distance squared will throw an exception if the player has changed worlds
			{
				//if he's not in a vehicle and has moved at least three blocks since the last check
				//and he's not being pushed around by fluids
				if(!player.isInsideVehicle() && 
				   (lastLocation == null || lastLocation.distanceSquared(player.getLocation()) >= wc.getafkDistanceCheck()) &&
				   !player.getLocation().getBlock().isLiquid())
				{					
					//if player is over accrued limit, accrued limit was probably reduced in config file AFTER he accrued
					//in that case, leave his blocks where they are
					if(playerData.accruedClaimBlocks > GriefPrevention.instance.config_claims_maxAccruedBlocks) continue;
					
					//add blocks
					playerData.accruedClaimBlocks += accruedBlocks;
					
					//respect limits
					if(playerData.accruedClaimBlocks > GriefPrevention.instance.config_claims_maxAccruedBlocks)
					{
						playerData.accruedClaimBlocks = GriefPrevention.instance.config_claims_maxAccruedBlocks; 
					}
					
					//intentionally NOT saving data here to reduce overall secondary storage access frequency
					//many other operations will cause this players data to save, including his eventual logout
					//dataStore.savePlayerData(player.getName(), playerData);
				}
			}
			catch(Exception e) { }
			
			//remember current location for next time
			playerData.lastAfkCheckLocation = player.getLocation();
		}
	}
}
