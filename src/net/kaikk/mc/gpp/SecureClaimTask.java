/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2015 Ryan Hamshire

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
 
package net.kaikk.mc.gpp;


import org.bukkit.entity.Player;

//secures a claim after a siege looting window has closed
class SecureClaimTask implements Runnable 
{
	private SiegeData siegeData;
	
	public SecureClaimTask(SiegeData siegeData)
	{
		this.siegeData = siegeData;
	}
	
	@Override
	public void run()
	{
		//for each claim involved in this siege
		for(int i = 0; i < this.siegeData.claims.size(); i++)
		{
			//lock the doors
			Claim claim = this.siegeData.claims.get(i);
			claim.doorsOpen = false;
			
			//eject bad guys
			Player [] onlinePlayers = GriefPreventionPlus.instance.getServer().getOnlinePlayers();
			for(int j = 0; j < onlinePlayers.length; j++)
			{
				Player player = onlinePlayers[j];
				if(claim.contains(player.getLocation(), false, false) && claim.allowAccess(player) != null)
				{
					GriefPreventionPlus.sendMessage(player, TextMode.Err, Messages.SiegeDoorsLockedEjection);
					GriefPreventionPlus.instance.ejectPlayer(player);
				}
			}
		}
	}	
}
