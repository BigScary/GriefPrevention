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

import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

//sends a message to a player
//used to send delayed messages, for example help text triggered by a player's chat
public class SendPlayerMessageTask implements Runnable {
	private ChatColor color;
	private String message;
	private Player player;

	public SendPlayerMessageTask(Player player, ChatColor color, String message) {
		this.player = player;
		this.color = color;
		this.message = message;
	}

	public void run() {

		GriefPrevention.sendMessage(this.player, this.color, this.message);
	}
}
