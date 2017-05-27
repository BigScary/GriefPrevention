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

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import me.ryanhamshire.GriefPrevention.claim.Claim;

import me.ryanhamshire.GriefPrevention.events.DeniedMessageEvent;
import me.ryanhamshire.GriefPrevention.player.PlayerData;
import org.bukkit.*;
import org.bukkit.entity.Player;

//singleton class which manages all GriefPrevention data (except for config options)
public interface DataStore
{
	//TODO: where is this _needed_
	//pattern for unique user identifiers (UUIDs)
	//protected final static Pattern uuidpattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
	
	void writeClaimToStorage(Claim claim);
	
	//increments the claim ID and updates secondary storage to be sure it's saved
	//As far as I know, 2 billion claim IDs _should_ be enough
	int nextClaimID();
	
	PlayerData getPlayerDataFromStorage(UUID playerID);
	
	void deleteClaimFromSecondaryStorage(Claim claim);

    //saves changes to player data to storage.
    void savePlayerData(UUID playerID, PlayerData playerData);

	//Generally used when the server shuts down.
    void savePlayerDataSync(UUID playerID, PlayerData playerData);
}
