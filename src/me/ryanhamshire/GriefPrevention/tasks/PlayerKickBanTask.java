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
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

//kicks or bans a player
//need a task for this because async threads (like the chat event handlers) can't kick or ban.
//but they CAN schedule a task to run in the main thread to do that job
public class PlayerKickBanTask implements Runnable {
	// ban message. if null, don't ban
	private String banReason;

	// player to kick or ban
	private Player player;

	public PlayerKickBanTask(Player player, String banReason) {
		this.player = player;
		this.banReason = banReason;
	}

	public void run() {
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		String kickcommands = wc.getSpamKickCommand();
		String bancommands = wc.getSpamBanCommand();

		if (this.banReason != null) {
			// ban
			// GriefPrevention.instance.getServer().getOfflinePlayer(this.player.getName()).setBanned(true);
			runCommands(bancommands, this.player.getName());
		} else if (this.player.isOnline()) {
			runCommands(kickcommands, this.player.getName());
		}
	}

	private void runCommands(String cmds, String... replacements) {

		String[] commandsrun = cmds.split(";");

		for (String cmd : commandsrun) {
			int i = 0;
			for (String replacement : replacements) {
				String substitution = "{" + i + "}";
				cmd = cmd.replace(substitution, replacement);
				i++;

			}
			if (cmd.startsWith("/"))
				cmd = cmd.substring(2);
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

		}

	}
}
