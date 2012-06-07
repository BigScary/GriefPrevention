/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2011 Ryan Hamshire

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.PoweredMinecart;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

class PlayerEventHandler implements Listener 
{
	private DataStore dataStore;
	
	//list of temporarily banned ip's
	private ArrayList<IpBanInfo> tempBannedIps = new ArrayList<IpBanInfo>();
	
	//number of milliseconds in a day
	private final long MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;
	
	//typical constructor, yawn
	PlayerEventHandler(DataStore dataStore, GriefPrevention plugin)
	{
		this.dataStore = dataStore;
	}
	
	//when a player chats, monitor for spam
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	void onPlayerChat (PlayerChatEvent event)
	{		
		Player player = event.getPlayer();
		String message = event.getMessage();
		
		//FEATURE: automatically educate players about the /trapped command
		
		//check for "trapped" or "stuck" to educate players about the /trapped command
		if(message.contains("trapped") || message.contains("stuck"))
		{
			GriefPrevention.sendMessage(player, TextMode.Info, "Are you trapped in someone's claim?  Consider the /trapped command.");
		}
		
		//FEATURE: monitor for chat and command spam
		
		if(!GriefPrevention.instance.config_spam_enabled) return;
		
		//if the player has permission to spam, don't bother even examining the message
		if(player.hasPermission("griefprevention.spam")) return;
		
		//remedy any CAPS SPAM without bothering to fault the player for it
		if(message.length() > 4 && message.toUpperCase().equals(message))
		{
			event.setMessage(message.toLowerCase());
		}
		
		//where spam is concerned, casing isn't significant
		message = message.toLowerCase();
		
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		boolean spam = false;
		boolean muted = false;
		
		//filter IP addresses
		if(!(event instanceof PlayerCommandPreprocessEvent))
		{
			Pattern ipAddressPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
			Matcher matcher = ipAddressPattern.matcher(event.getMessage());
			
			//if it looks like an IP address
			while(matcher.find())
			{
				//and it's not in the list of allowed IP addresses
				if(!GriefPrevention.instance.config_spam_allowedIpAddresses.contains(matcher.group()))
				{
					//log entry
					GriefPrevention.AddLogEntry("Muted IP address from " + player.getName() + ": " + event.getMessage());
					
					//spam notation
					playerData.spamCount++;
					spam = true;
					
					//block message
					muted = true;
				}
			}
		}
		
		//check message content and timing		
		long millisecondsSinceLastMessage = (new Date()).getTime() - playerData.lastMessageTimestamp.getTime();
		
		//if the message came too close to the last one
		if(millisecondsSinceLastMessage < 3000)
		{
			//increment the spam counter
			playerData.spamCount++;
			spam = true;
		}
		
		//if it's very similar to the last message
		if(this.stringsAreSimilar(message, playerData.lastMessage))
		{
			playerData.spamCount++;
			spam = true;
			muted = true;
		}
		
		//if the message was mostly non-alpha-numerics or doesn't include much whitespace, consider it a spam (probably ansi art or random text gibberish) 
		if(message.length() > 5)
		{
			int symbolsCount = 0;
			int whitespaceCount = 0;
			for(int i = 0; i < message.length(); i++)
			{
				char character = message.charAt(i);
				if(!(Character.isLetterOrDigit(character)))
				{
					symbolsCount++;
				}
				
				if(Character.isWhitespace(character))
				{
					whitespaceCount++;
				}
			}
			
			if(symbolsCount > message.length() / 2 || (message.length() > 15 && whitespaceCount < message.length() / 10))
			{
				spam = true;
				playerData.spamCount++;
			}
		}
		
		//if the message was determined to be a spam, consider taking action		
		if(!player.hasPermission("griefprevention.spam") && spam)
		{		
			//anything above level 4 for a player which has received a warning...  kick or if enabled, ban 
			if(playerData.spamCount > 4 && playerData.spamWarned)
			{
				if(GriefPrevention.instance.config_spam_banOffenders)
				{
					//log entry
					GriefPrevention.AddLogEntry("Banning " + player.getName() + " for spam.");
					
					//ban
					GriefPrevention.instance.getServer().getOfflinePlayer(player.getName()).setBanned(true);
					
					//kick
					player.kickPlayer(GriefPrevention.instance.config_spam_banMessage);
				}	
				else
				{
					player.kickPlayer("");
				}
			}
			
			//cancel any messages while at or above the third spam level and issue warnings
			//anything above level 2, mute and warn
			if(playerData.spamCount >= 3)
			{
				muted = true;
				if(!playerData.spamWarned)
				{
					GriefPrevention.sendMessage(player, TextMode.Warn, GriefPrevention.instance.config_spam_warningMessage);
					GriefPrevention.AddLogEntry("Warned " + player.getName() + " about spam penalties.");
					playerData.spamWarned = true;
				}
			}
			
			if(muted)
			{
				//cancel the event and make a log entry
				//cancelling the event guarantees players don't receive the message
				event.setCancelled(true);
				GriefPrevention.AddLogEntry("Muted spam from " + player.getName() + ": " + message);
				
				//send a fake message so the player doesn't realize he's muted
				//less information for spammers = less effective spam filter dodging
				player.sendMessage("<" + player.getName() + "> " + event.getMessage());
			}		
		}
		
		//otherwise if not a spam, reset the spam counter for this player
		else
		{
			playerData.spamCount = 0;
			playerData.spamWarned = false;
		}
		
		//in any case, record the timestamp of this message and also its content for next time
		playerData.lastMessageTimestamp = new Date();
		playerData.lastMessage = message;	
	}
	
	//if two strings are 75% identical, they're too close to follow each other in the chat
	private boolean stringsAreSimilar(String message, String lastMessage)
	{
		//determine which is shorter
		String shorterString, longerString;
		if(lastMessage.length() < message.length())
		{
			shorterString = lastMessage;
			longerString = message;
		}
		else
		{
			shorterString = message;
			longerString = lastMessage;
		}
		
		if(shorterString.length() <= 5) return shorterString.equals(longerString);
		
		//set similarity tolerance
		int maxIdenticalCharacters = longerString.length() - longerString.length() / 4;
		
		//trivial check on length
		if(shorterString.length() < maxIdenticalCharacters) return false;
		
		//compare forward
		int identicalCount = 0;
		for(int i = 0; i < shorterString.length(); i++)
		{
			if(shorterString.charAt(i) == longerString.charAt(i)) identicalCount++;
			if(identicalCount > maxIdenticalCharacters) return true;
		}
		
		//compare backward
		for(int i = 0; i < shorterString.length(); i++)
		{
			if(shorterString.charAt(shorterString.length() - i - 1) == longerString.charAt(longerString.length() - i - 1)) identicalCount++;
			if(identicalCount > maxIdenticalCharacters) return true;
		}
		
		return false;
	}

	//when a player uses a slash command, monitor for spam
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	void onPlayerCommandPreprocess (PlayerCommandPreprocessEvent event)
	{
		if(!GriefPrevention.instance.config_spam_enabled) return;
		
		//if the slash command used is in the list of monitored commands, treat it like a chat message (see above)
		String [] args = event.getMessage().split(" ");
		if(GriefPrevention.instance.config_spam_monitorSlashCommands.contains(args[0])) this.onPlayerChat(event);
		
		if(GriefPrevention.instance.config_eavesdrop && args[0].equalsIgnoreCase("/tell") && !event.getPlayer().hasPermission("griefprevention.eavesdrop") && args.length > 2)
		{			
			StringBuilder logMessageBuilder = new StringBuilder();
			logMessageBuilder.append("[").append(event.getPlayer().getName()).append(" > ").append(args[1]).append("] ");			
			
			for(int i = 2; i < args.length; i++)
			{
				logMessageBuilder.append(args[i]).append(" ");
			}
			
			String logMessage = logMessageBuilder.toString();
			
			GriefPrevention.AddLogEntry(logMessage.toString());
			
			Player [] players = GriefPrevention.instance.getServer().getOnlinePlayers();
			for(int i = 0; i < players.length; i++)
			{
				Player player = players[i];
				if(player.hasPermission("griefprevention.eavesdrop") && !player.getName().equalsIgnoreCase(args[1]))
				{
					player.sendMessage(ChatColor.GRAY + logMessage);
				}
			}
		}
	}
	
	//when a player attempts to join the server...
	@EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerLogin (PlayerLoginEvent event)
	{
		Player player = event.getPlayer();
		
		//all this is anti-spam code
		if(GriefPrevention.instance.config_spam_enabled)
		{
			//FEATURE: login cooldown to prevent login/logout spam with custom clients
			
			//if allowed to join and login cooldown enabled
			if(GriefPrevention.instance.config_spam_loginCooldownMinutes > 0 && event.getResult() == Result.ALLOWED)
			{
				//determine how long since last login and cooldown remaining
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());
				long millisecondsSinceLastLogin = (new Date()).getTime() - playerData.lastLogin.getTime();
				long minutesSinceLastLogin = millisecondsSinceLastLogin / 1000 / 60;
				long cooldownRemaining = GriefPrevention.instance.config_spam_loginCooldownMinutes - minutesSinceLastLogin;
				
				//if cooldown remaining and player doesn't have permission to spam
				if(cooldownRemaining > 0 && !player.hasPermission("griefprevention.spam"))
				{
					//DAS BOOT!
					event.setResult(Result.KICK_OTHER);				
					event.setKickMessage("You must wait " + cooldownRemaining + " more minutes before logging-in again.");
					event.disallow(event.getResult(), event.getKickMessage());
					return;
				}
			}
		}
		
		//remember the player's ip address
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		playerData.ipAddress = event.getAddress();
		
		//FEATURE: auto-ban accounts who use an IP address which was very recently used by another banned account
		if(GriefPrevention.instance.config_smartBan)
		{		
			//if logging-in account is banned, remember IP address for later
			long now = Calendar.getInstance().getTimeInMillis();
			if(event.getResult() == Result.KICK_BANNED)
			{
				this.tempBannedIps.add(new IpBanInfo(event.getAddress(), now + this.MILLISECONDS_IN_DAY, player.getName()));
			}
			
			//otherwise if not banned
			else
			{
				//search temporarily banned IP addresses for this one
				for(int i = 0; i < this.tempBannedIps.size(); i++)
				{
					IpBanInfo info = this.tempBannedIps.get(i);
					String address = info.address.toString();
					
					//eliminate any expired entries
					if(now > info.expirationTimestamp)
					{
						this.tempBannedIps.remove(i--);
					}
					
					//if we find a match				
					else if(address.equals(playerData.ipAddress.toString()))
					{
						//if the account associated with the IP ban has been pardoned, remove all ip bans for that ip and we're done
						OfflinePlayer bannedPlayer = GriefPrevention.instance.getServer().getOfflinePlayer(info.bannedAccountName);
						if(!bannedPlayer.isBanned())
						{
							for(int j = 0; j < this.tempBannedIps.size(); j++)
							{
								IpBanInfo info2 = this.tempBannedIps.get(j);
								if(info2.address.toString().equals(address))
								{
									OfflinePlayer bannedAccount = GriefPrevention.instance.getServer().getOfflinePlayer(info2.bannedAccountName);
									bannedAccount.setBanned(false);
									this.tempBannedIps.remove(j--);
								}
							}
							
							break;
						}
						
						//otherwise if that account is still banned, ban this account, too
						else
						{
							player.setBanned(true);
							event.setResult(Result.KICK_BANNED);				
							event.disallow(event.getResult(), "");
							GriefPrevention.AddLogEntry("Auto-banned " + player.getName() + " because that account is using an IP address very recently used by banned player " + info.bannedAccountName + " (" + info.address.toString() + ").");
						}
					}
				}
			}
		}
	}
	
	//when a player spawns, conditionally apply temporary pvp protection 
	@EventHandler(ignoreCancelled = true)
	void onPlayerRespawn (PlayerRespawnEvent event)
	{
		PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(event.getPlayer().getName());
		playerData.lastSpawn = Calendar.getInstance().getTimeInMillis();
		GriefPrevention.instance.checkPvpProtectionNeeded(event.getPlayer());
	}
	
	//when a player successfully joins the server...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	void onPlayerJoin(PlayerJoinEvent event)
	{
		String playerName = event.getPlayer().getName();
		
		//note login time
		PlayerData playerData = this.dataStore.getPlayerData(playerName);
		playerData.lastSpawn = Calendar.getInstance().getTimeInMillis();
		playerData.lastLogin = new Date();
		this.dataStore.savePlayerData(playerName, playerData);
		
		//check inventory, may need pvp protection
		GriefPrevention.instance.checkPvpProtectionNeeded(event.getPlayer());
		
		//how long since the last logout?
		long elapsed = Calendar.getInstance().getTimeInMillis() - playerData.lastLogout;
		
		//remember message, then silence it.  may broadcast it later
		
		//only do this if notification_seconds is bigger than 0
		if(GriefPrevention.NOTIFICATION_SECONDS > 0) {
			String message = event.getJoinMessage();
			event.setJoinMessage(null);
			
			if(message != null && elapsed >= GriefPrevention.NOTIFICATION_SECONDS * 1000)
			{
				//start a timer for a delayed join notification message (will only show if player is still online in 30 seconds)
				JoinLeaveAnnouncementTask task = new JoinLeaveAnnouncementTask(event.getPlayer(), message, true);		
				GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 20L * GriefPrevention.NOTIFICATION_SECONDS);
			}
		}
	}
	
	//when a player quits...
	@EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerKicked(PlayerKickEvent event)
	{
		Player player = event.getPlayer();
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		if(player.isBanned())
		{
			long now = Calendar.getInstance().getTimeInMillis(); 
			this.tempBannedIps.add(new IpBanInfo(playerData.ipAddress, now + this.MILLISECONDS_IN_DAY, player.getName()));
		}	
	}
	
	//when a player quits...
	@EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerQuit(PlayerQuitEvent event)
	{
		this.onPlayerDisconnect(event.getPlayer(), event.getQuitMessage());
		
		//silence the leave message (may be broadcast later, if the player stays offline)
		
		//only do this if notification_seconds is bigger than 0
		if(GriefPrevention.NOTIFICATION_SECONDS > 0) {
			event.setQuitMessage(null);			
		}
	}
	
	//helper for above
	private void onPlayerDisconnect(Player player, String notificationMessage)
	{
		String playerName = player.getName();
		PlayerData playerData = this.dataStore.getPlayerData(playerName);
		
		//FEATURE: players in pvp combat when they log out will die
		if(GriefPrevention.instance.config_pvp_punishLogout && playerData.inPvpCombat())
		{
			player.setHealth(0);
		}
		
		//FEATURE: during a siege, any player who logs out dies and forfeits the siege
		
		//if player was involved in a siege, he forfeits
		if(playerData.siegeData != null)
		{
			if(player.getHealth() > 0) player.setHealth(0);  //might already be zero from above, this avoids a double death message
		}
		
		//how long was the player online?
		long now = Calendar.getInstance().getTimeInMillis();
		long elapsed = now - playerData.lastLogin.getTime();
		
		//remember logout time
		playerData.lastLogout = Calendar.getInstance().getTimeInMillis();
		
		//if notification message isn't null and the player has been online for at least 30 seconds...
		
		//only do this if notification_seconds is bigger than 0
		if(GriefPrevention.NOTIFICATION_SECONDS > 0) {
			if(notificationMessage != null && elapsed >= 1000 * GriefPrevention.NOTIFICATION_SECONDS)
			{
				//start a timer for a delayed leave notification message (will only show if player is still offline in 30 seconds)
				JoinLeaveAnnouncementTask task = new JoinLeaveAnnouncementTask(player, notificationMessage, false);		
				GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 20L * GriefPrevention.NOTIFICATION_SECONDS);		
			}
		}
	}

	//when a player drops an item
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerDropItem(PlayerDropItemEvent event)
	{
		Player player = event.getPlayer();
		
		//in creative worlds, dropping items is blocked
		if(GriefPrevention.instance.creativeRulesApply(player.getLocation()))
		{
			event.setCancelled(true);
			return;
		}
		
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		//FEATURE: players under siege or in PvP combat, can't throw items on the ground to hide 
		//them or give them away to other players before they are defeated
		
		//if in combat, don't let him drop it
		if(!GriefPrevention.instance.config_pvp_allowCombatItemDrop && playerData.inPvpCombat())
		{
			GriefPrevention.sendMessage(player, TextMode.Err, "You can't drop items while in PvP combat.");
			event.setCancelled(true);			
		}
		
		//if he's under siege, don't let him drop it
		else if(playerData.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, "You can't drop items while involved in a siege.");
			event.setCancelled(true);
		}
	}
	
	//when a player teleports
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		//FEATURE: prevent teleport abuse to win sieges
		
		//these rules only apply to non-ender-pearl teleportation
		if(event.getCause() == TeleportCause.ENDER_PEARL) return;
		
		Player player = event.getPlayer();
		
		Location source = event.getFrom();
		Claim sourceClaim = this.dataStore.getClaimAt(source, false, null);
		if(sourceClaim != null && sourceClaim.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, "You can't teleport out of a besieged area.");
			event.setCancelled(true);
			return;
		}
		
		Location destination = event.getTo();
		Claim destinationClaim = this.dataStore.getClaimAt(destination, false, null);
		if(destinationClaim != null && destinationClaim.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, "You can't teleport into a besieged area.");
			event.setCancelled(true);
			return;
		}
	}
	
	//when a player interacts with an entity...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
	{
		Player player = event.getPlayer();
		Entity entity = event.getRightClicked();
		
		//don't allow container access during pvp combat
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		if((entity instanceof StorageMinecart || entity instanceof PoweredMinecart))
		{
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You can't access containers while under siege.");
				event.setCancelled(true);
				return;
			}
			
			if(playerData.inPvpCombat())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You can't access containers during PvP combat.");
				event.setCancelled(true);
				return;
			}			
		}
		
		//if the entity is a vehicle and we're preventing theft in claims		
		if(GriefPrevention.instance.config_claims_preventTheft && entity instanceof Vehicle)
		{
			//if the entity is in a claim
			Claim claim = this.dataStore.getClaimAt(entity.getLocation(), false, null);
			if(claim != null)
			{
				//for storage and powered minecarts, apply container rules (this is a potential theft)
				if(entity instanceof StorageMinecart || entity instanceof PoweredMinecart)
				{					
					String noContainersReason = claim.allowContainers(player);
					if(noContainersReason != null)
					{
						GriefPrevention.sendMessage(player, TextMode.Err, noContainersReason);
						event.setCancelled(true);
					}
				}
				
				//for boats, apply access rules
				else if(entity instanceof Boat)
				{
					String noAccessReason = claim.allowAccess(player);
					if(noAccessReason != null)
					{
						player.sendMessage(noAccessReason);
						event.setCancelled(true);
					}
				}
				
				//if the entity is an animal, apply container rules
				else if(entity instanceof Animals)
				{
					if(claim.allowContainers(player) != null)
					{
						GriefPrevention.sendMessage(player, TextMode.Err, "That animal belongs to " + claim.getOwnerName() + ".");
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	//when a player picks up an item...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerPickupItem(PlayerPickupItemEvent event)
	{
		Player player = event.getPlayer();
		
		if(!event.getPlayer().getWorld().getPVP()) return;
		
		//if we're preventing spawn camping and the player was previously empty handed...
		if(GriefPrevention.instance.config_pvp_protectFreshSpawns && (player.getItemInHand().getType() == Material.AIR))
		{
			//if that player is currently immune to pvp
			PlayerData playerData = this.dataStore.getPlayerData(event.getPlayer().getName());
			if(playerData.pvpImmune)
			{
				//if it's been less than 10 seconds since the last time he spawned, don't pick up the item
				long now = Calendar.getInstance().getTimeInMillis();
				long elapsedSinceLastSpawn = now - playerData.lastSpawn;
				if(elapsedSinceLastSpawn < 10000)
				{
					event.setCancelled(true);
					return;
				}
				
				//otherwise take away his immunity. he may be armed now.  at least, he's worth killing for some loot
				playerData.pvpImmune = false;
				GriefPrevention.sendMessage(player, TextMode.Warn, "Now you can fight with other players.");
			}			
		}
	}
	
	//when a player switches in-hand items
	@EventHandler(ignoreCancelled = true)
	public void onItemHeldChange(PlayerItemHeldEvent event)
	{
		Player player = event.getPlayer();
		
		//if he's switching to the golden shovel
		ItemStack newItemStack = player.getInventory().getItem(event.getNewSlot());
		if(newItemStack != null && newItemStack.getType() == Material.GOLD_SPADE)
		{
			EquipShovelProcessingTask task = new EquipShovelProcessingTask(player);
			GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 15L);  //15L is approx. 3/4 of a second
		}
	}
	
	//block players from entering beds they don't have permission for
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerBedEnter (PlayerBedEnterEvent bedEvent)
	{
		if(!GriefPrevention.instance.config_claims_preventButtonsSwitches) return;
		
		Player player = bedEvent.getPlayer();
		Block block = bedEvent.getBed();
		
		//if the bed is in a claim 
		Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, null);
		if(claim != null)
		{
			//if the player doesn't have access in that claim, tell him so and prevent him from sleeping in the bed
			if(claim.allowAccess(player) != null)
			{
				bedEvent.setCancelled(true);
				GriefPrevention.sendMessage(player, TextMode.Err, claim.getOwnerName() + " hasn't given you permission to sleep here.");
			}
		}		
	}
	
	//block use of buckets within other players' claims
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerBucketEmpty (PlayerBucketEmptyEvent bucketEvent)
	{
		Player player = bucketEvent.getPlayer();
		Block block = bucketEvent.getBlockClicked().getRelative(bucketEvent.getBlockFace());
		int minLavaDistance = 10;
		
		//make sure the player is allowed to build at the location
		String noBuildReason = GriefPrevention.instance.allowBuild(player, block.getLocation());
		if(noBuildReason != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
			bucketEvent.setCancelled(true);
			return;
		}
		
		//if the bucket is being used in a claim, allow for dumping lava closer to other players
		Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, null);
		if(claim != null)
		{
			minLavaDistance = 3;			
		}
		
		//otherwise no dumping anything unless underground
		else
		{
			if(block.getY() >= block.getWorld().getSeaLevel() - 5 && !player.hasPermission("griefprevention.lava"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You may only dump buckets inside your claim(s) or underground.");
				bucketEvent.setCancelled(true);
				return;
			}			
		}
		
		//lava buckets can't be dumped near other players unless pvp is on
		if(!block.getWorld().getPVP() && !player.hasPermission("griefprevention.lava"))
		{
			if(bucketEvent.getBucket() == Material.LAVA_BUCKET)
			{
				List<Player> players = block.getWorld().getPlayers();
				for(int i = 0; i < players.size(); i++)
				{
					Player otherPlayer = players.get(i);
					Location location = otherPlayer.getLocation();
					if(!otherPlayer.equals(player) && block.getY() >= location.getBlockY() - 1 && location.distanceSquared(block.getLocation()) < minLavaDistance * minLavaDistance)
					{
						GriefPrevention.sendMessage(player, TextMode.Err, "You can't place lava this close to " + otherPlayer.getName() + ".");
						bucketEvent.setCancelled(true);
						return;
					}					
				}
			}
		}
	}
	
	//see above
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerBucketFill (PlayerBucketFillEvent bucketEvent)
	{
		Player player = bucketEvent.getPlayer();
		Block block = bucketEvent.getBlockClicked();
		
		//make sure the player is allowed to build at the location
		String noBuildReason = GriefPrevention.instance.allowBuild(player, block.getLocation());
		if(noBuildReason != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
			bucketEvent.setCancelled(true);
			return;
		}
	}
	
	//when a player interacts with the world
	@EventHandler(priority = EventPriority.LOWEST)
	void onPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		
		//determine target block.  FEATURE: shovel and string can be used from a distance away
		Block clickedBlock = null;
		
		try
		{
			clickedBlock = event.getClickedBlock();  //null returned here means interacting with air			
			if(clickedBlock == null)
			{
				//try to find a far away non-air block along line of sight
				clickedBlock = player.getTargetBlock(null, 250);
			}			
		}
		catch(Exception e)  //an exception intermittently comes from getTargetBlock().  when it does, just ignore the event
		{
			return;
		}
		
		//if no block, stop here
		if(clickedBlock == null)
		{
			return;
		}
		
		Material clickedBlockType = clickedBlock.getType();
		
		//apply rules for buttons and switches
		if(GriefPrevention.instance.config_claims_preventButtonsSwitches && (clickedBlockType == null || clickedBlockType == Material.STONE_BUTTON || clickedBlockType == Material.LEVER))
		{
			Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, null);
			if(claim != null)
			{
				String noAccessReason = claim.allowAccess(player);
				if(noAccessReason != null)
				{
					event.setCancelled(true);
					GriefPrevention.sendMessage(player, TextMode.Err, noAccessReason);
				}
			}			
		}
		
		//otherwise apply rules for containers and crafting blocks
		else if(	GriefPrevention.instance.config_claims_preventTheft && (
						event.getAction() == Action.RIGHT_CLICK_BLOCK && (
						clickedBlock.getState() instanceof InventoryHolder || 
						clickedBlockType == Material.BREWING_STAND || 
						clickedBlockType == Material.WORKBENCH || 
						clickedBlockType == Material.JUKEBOX || 
						clickedBlockType == Material.ENCHANTMENT_TABLE)))
		{			
			//block container use while under siege, so players can't hide items from attackers
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You can't access containers while involved in a siege.");
				event.setCancelled(true);
				return;
			}
			
			//block container use during pvp combat, same reason
			if(playerData.inPvpCombat())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You can't access containers during PvP combat.");
				event.setCancelled(true);
				return;
			}
			
			//otherwise check permissions for the claim the player is in
			Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, null);
			if(claim != null)
			{
				String noContainersReason = claim.allowContainers(player);
				if(noContainersReason != null)
				{
					event.setCancelled(true);
					GriefPrevention.sendMessage(player, TextMode.Err, noContainersReason);
				}
			}
			
			//if the event hasn't been cancelled, then the player is allowed to use the container
			//so drop any pvp protection
			if(playerData.pvpImmune)
			{
				playerData.pvpImmune = false;
				GriefPrevention.sendMessage(player, TextMode.Warn, "Now you can fight with other players.");
			}
		}
		
		//apply rule for players trampling tilled soil back to dirt (never allow it)
		//NOTE: that this event applies only to players.  monsters and animals can still trample.
		else if(event.getAction() == Action.PHYSICAL && clickedBlockType == Material.SOIL)
		{
			event.setCancelled(true);
		}
		
		//otherwise handle right click (shovel, string, bonemeal)
		else
		{
			//ignore all actions except right-click on a block or in the air
			Action action = event.getAction();
			if(action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;
			
			//what's the player holding?
			Material materialInHand = player.getItemInHand().getType();		
			
			//if it's bonemeal, check for build permission (ink sac == bone meal, must be a Bukkit bug?)
			if(materialInHand == Material.INK_SACK)
			{
				String noBuildReason = GriefPrevention.instance.allowBuild(player, clickedBlock.getLocation());
				if(noBuildReason != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
					event.setCancelled(true);
				}
				
				return;
			}
			
			//if it's a spawn egg or minecart and this is a creative world, apply special rules
			else if((materialInHand == Material.MONSTER_EGG || materialInHand == Material.MINECART || materialInHand == Material.POWERED_MINECART || materialInHand == Material.STORAGE_MINECART) && GriefPrevention.instance.creativeRulesApply(clickedBlock.getLocation()))
			{
				//player needs build permission at this location
				String noBuildReason = GriefPrevention.instance.allowBuild(player, clickedBlock.getLocation());
				if(noBuildReason != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
					event.setCancelled(true);
					return;
				}
			
				//enforce limit on total number of entities in this claim
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());
				Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
				if(claim == null) return;
				
				String noEntitiesReason = claim.allowMoreEntities();
				if(noEntitiesReason != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, noEntitiesReason);
					event.setCancelled(true);
					return;
				}
				
				return;
			}
			
			//if it's a string, he's investigating a claim			
			else if(materialInHand == Material.STRING)
			{
				//air indicates too far away
				if(clickedBlockType == Material.AIR)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "That's too far away.");
					return;
				}
				
				Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false /*ignore height*/, null);
				
				//no claim case
				if(claim == null)
				{
					GriefPrevention.sendMessage(player, TextMode.Info, "No one has claimed this block.");
					Visualization.Revert(player);
				}
				
				//claim case
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Info, "This block has been claimed by " + claim.getOwnerName() + ".");
					
					//visualize boundary
					Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.Claim);
					Visualization.Apply(player, visualization);
				}
				
				return;
			}
			
			//if it's a golden shovel
			else if(materialInHand != Material.GOLD_SPADE) return;
			
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			
			//disable golden shovel while under siege
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You can't use your shovel tool while involved in a siege.");
				event.setCancelled(true);
				return;
			}
			
			//can't use the shovel from too far away
			if(clickedBlockType == Material.AIR)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "That's too far away!");
				return;
			}
			
			//if the player is in restore nature mode, do only that
			String playerName = player.getName();
			playerData = this.dataStore.getPlayerData(player.getName());
			if(playerData.shovelMode == ShovelMode.RestoreNature || playerData.shovelMode == ShovelMode.RestoreNatureAggressive)
			{
				//if the clicked block is in a claim, visualize that claim and deliver an error message
				Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
				if(claim != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, claim.getOwnerName() + " claimed that block.");
					Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.ErrorClaim);
					Visualization.Apply(player, visualization);
					
					return;
				}
				
				//figure out which chunk to repair
				Chunk chunk = player.getWorld().getChunkAt(clickedBlock.getLocation());
				
				//check it for players, and cancel if there are any
				Entity [] entities = chunk.getEntities();
				for(int i = 0; i < entities.length; i++)
				{
					if(entities[i] instanceof Player)
					{
						Player otherPlayer = (Player)entities[i];
						GriefPrevention.sendMessage(player, TextMode.Err, "Unable to restore.  " + otherPlayer.getName() + " is in that chunk.");
						return;
					}
				}
				
				//build a snapshot of this chunk, including 1 block boundary outside of the chunk all the way around
				int maxHeight = chunk.getWorld().getMaxHeight();
				BlockSnapshot[][][] snapshots = new BlockSnapshot[18][maxHeight][18];
				Block startBlock = chunk.getBlock(0, 0, 0);
				Location startLocation = new Location(chunk.getWorld(), startBlock.getX() - 1, 0, startBlock.getZ() - 1);
				for(int x = 0; x < snapshots.length; x++)
				{
					for(int z = 0; z < snapshots[0][0].length; z++)
					{
						for(int y = 0; y < snapshots[0].length; y++)
						{
							Block block = chunk.getWorld().getBlockAt(startLocation.getBlockX() + x, startLocation.getBlockY() + y, startLocation.getBlockZ() + z);
							snapshots[x][y][z] = new BlockSnapshot(block.getLocation(), block.getTypeId(), block.getData());
						}
					}
				}
				
				//create task to process those data in another thread
				
				//set boundaries for processing
				int miny = clickedBlock.getY();
				
				//if not in aggressive mode, extend the selection down to a little below sea level
				if(!(playerData.shovelMode == ShovelMode.RestoreNatureAggressive))
				{
					if(miny > chunk.getWorld().getSeaLevel() - 10)
					{
						miny = chunk.getWorld().getSeaLevel() - 10;
					}
				}
				
				Location lesserBoundaryCorner = chunk.getBlock(0,  0, 0).getLocation();
				Location greaterBoundaryCorner = chunk.getBlock(15, 0, 15).getLocation();
				
				//create task
				//when done processing, this task will create a main thread task to actually update the world with processing results
				RestoreNatureProcessingTask task = new RestoreNatureProcessingTask(snapshots, miny, chunk.getWorld().getEnvironment(), chunk.getWorld().getBiome(lesserBoundaryCorner.getBlockX(), lesserBoundaryCorner.getBlockZ()), lesserBoundaryCorner, greaterBoundaryCorner, chunk.getWorld().getSeaLevel(), playerData.shovelMode == ShovelMode.RestoreNatureAggressive, player);
				GriefPrevention.instance.getServer().getScheduler().scheduleAsyncDelayedTask(GriefPrevention.instance, task);
				
				return;
			}
			
			//if in restore nature fill mode
			if(playerData.shovelMode == ShovelMode.RestoreNatureFill)
			{
				ArrayList<Material> allowedFillBlocks = new ArrayList<Material>();				
				Environment environment = clickedBlock.getWorld().getEnvironment();
				if(environment == Environment.NETHER)
				{
					allowedFillBlocks.add(Material.NETHERRACK);
				}
				else if(environment == Environment.THE_END)
				{
					allowedFillBlocks.add(Material.ENDER_STONE);
				}			
				else
				{
					allowedFillBlocks.add(Material.STONE);
					allowedFillBlocks.add(Material.SAND);
					allowedFillBlocks.add(Material.SANDSTONE);
					allowedFillBlocks.add(Material.DIRT);
					allowedFillBlocks.add(Material.GRASS);
				}
				
				Block centerBlock = clickedBlock;
				int maxHeight = centerBlock.getY();
				int minx = centerBlock.getX() - playerData.fillRadius;
				int maxx = centerBlock.getX() + playerData.fillRadius;
				int minz = centerBlock.getZ() - playerData.fillRadius;
				int maxz = centerBlock.getZ() + playerData.fillRadius;				
				int minHeight = maxHeight - 10;
				if(minHeight < 0) minHeight = 0;
				
				Claim cachedClaim = null;
				for(int x = minx; x <= maxx; x++)
				{
					for(int z = minz; z <= maxz; z++)
					{
						//circular brush
						Location location = new Location(centerBlock.getWorld(), x, centerBlock.getY(), z);
						if(location.distance(centerBlock.getLocation()) > playerData.fillRadius) continue;
						
						//fill bottom to top
						for(int y = minHeight; y <= maxHeight; y++)
						{
							Block block = centerBlock.getWorld().getBlockAt(x, y, z);
							
							//respect claims
							Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, cachedClaim);
							if(claim != null)
							{
								cachedClaim = claim;
								break;
							}
							
							//only replace air and spilling water
							if(block.getType() == Material.AIR || block.getType() == Material.STATIONARY_WATER && block.getData() != 0)
							{							
								//look to neighbors for an appropriate fill block
								Block eastBlock = block.getRelative(BlockFace.EAST);
								Block westBlock = block.getRelative(BlockFace.WEST);
								Block northBlock = block.getRelative(BlockFace.NORTH);
								Block southBlock = block.getRelative(BlockFace.SOUTH);
								Block underBlock = block.getRelative(BlockFace.DOWN);
								
								//first, check lateral neighbors (ideally, want to keep natural layers)
								if(allowedFillBlocks.contains(eastBlock.getType()))
								{
									block.setType(eastBlock.getType());
								}
								else if(allowedFillBlocks.contains(westBlock.getType()))
								{
									block.setType(westBlock.getType());
								}
								else if(allowedFillBlocks.contains(northBlock.getType()))
								{
									block.setType(northBlock.getType());
								}
								else if(allowedFillBlocks.contains(southBlock.getType()))
								{
									block.setType(southBlock.getType());
								}
								
								//then check underneath
								else if(allowedFillBlocks.contains(underBlock.getType()))
								{
									block.setType(underBlock.getType());
								}
								
								//if all else fails, use the first material listed in the acceptable fill blocks above
								else
								{
									block.setType(allowedFillBlocks.get(0));
								}
							}
						}
					}
				}
				
				return;
			}
			
			//if the player doesn't have claims permission, don't do anything
			if(GriefPrevention.instance.config_claims_creationRequiresPermission && !player.hasPermission("griefprevention.createclaims"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You don't have permission to claim land.");
				return;
			}
			
			//if he's resizing a claim and that claim hasn't been deleted since he started resizing it
			if(playerData.claimResizing != null && playerData.claimResizing.inDataStore)
			{
				if(clickedBlock.getLocation().equals(playerData.lastShovelLocation)) return;

				//figure out what the coords of his new claim would be
				int newx1, newx2, newz1, newz2, newy1, newy2;
				if(playerData.lastShovelLocation.getBlockX() == playerData.claimResizing.getLesserBoundaryCorner().getBlockX())
				{
					newx1 = clickedBlock.getX();
				}
				else
				{
					newx1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockX();
				}
				
				if(playerData.lastShovelLocation.getBlockX() == playerData.claimResizing.getGreaterBoundaryCorner().getBlockX())
				{
					newx2 = clickedBlock.getX();
				}
				else
				{
					newx2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockX();
				}
				
				if(playerData.lastShovelLocation.getBlockZ() == playerData.claimResizing.getLesserBoundaryCorner().getBlockZ())
				{
					newz1 = clickedBlock.getZ();
				}
				else
				{
					newz1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockZ();
				}
				
				if(playerData.lastShovelLocation.getBlockZ() == playerData.claimResizing.getGreaterBoundaryCorner().getBlockZ())
				{
					newz2 = clickedBlock.getZ();
				}
				else
				{
					newz2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockZ();
				}
				
				newy1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockY();
				newy2 = clickedBlock.getY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance;
				
				//for top level claims, apply size rules and claim blocks requirement
				if(playerData.claimResizing.parent == null)
				{				
					//measure new claim, apply size rules
					int newWidth = (Math.abs(newx1 - newx2) + 1);
					int newHeight = (Math.abs(newz1 - newz2) + 1);
							
					if(!playerData.claimResizing.isAdminClaim() && (newWidth < GriefPrevention.instance.config_claims_minSize || newHeight < GriefPrevention.instance.config_claims_minSize))
					{
						GriefPrevention.sendMessage(player, TextMode.Err, "This new size would be too small.  Claims must be at least " + GriefPrevention.instance.config_claims_minSize + " x " + GriefPrevention.instance.config_claims_minSize + ".");
						return;
					}
					
					//make sure player has enough blocks to make up the difference
					if(!playerData.claimResizing.isAdminClaim())
					{
						int newArea =  newWidth * newHeight;
						int blocksRemainingAfter = playerData.getRemainingClaimBlocks() + playerData.claimResizing.getArea() - newArea;
						
						if(blocksRemainingAfter < 0)
						{
							GriefPrevention.sendMessage(player, TextMode.Err, "You don't have enough blocks for this size.  You need " + Math.abs(blocksRemainingAfter) + " more.");
							return;
						}
					}
				}
				
				//special rules for making a top-level claim smaller.  to check this, verifying the old claim's corners are inside the new claim's boundaries.
				//rule1: in creative mode, top-level claims can't be moved or resized smaller.
				//rule2: in any mode, shrinking a claim removes any surface fluids
				Claim oldClaim = playerData.claimResizing;
				if(oldClaim.parent == null)
				{				
					//temporary claim instance, just for checking contains()
					Claim newClaim = new Claim(
							new Location(oldClaim.getLesserBoundaryCorner().getWorld(), newx1, newy1, newz1), 
							new Location(oldClaim.getLesserBoundaryCorner().getWorld(), newx2, newy2, newz2),
							"", new String[]{}, new String[]{}, new String[]{}, new String[]{});
					
					//if the new claim is smaller
					if(!newClaim.contains(oldClaim.getLesserBoundaryCorner(), true, false) || !newClaim.contains(oldClaim.getGreaterBoundaryCorner(), true, false))
					{
						//enforce creative mode rule
						if(!player.hasPermission("griefprevention.deleteclaims") && GriefPrevention.instance.creativeRulesApply(player.getLocation()))
						{
							GriefPrevention.sendMessage(player, TextMode.Err, "You can't un-claim creative mode land.  You can only make this claim larger or create additional claims.");
							return;
						}
						
						//remove surface fluids about to be unclaimed
						oldClaim.removeSurfaceFluids(newClaim);
					}
				}
				
				//ask the datastore to try and resize the claim, this checks for conflicts with other claims
				CreateClaimResult result = GriefPrevention.instance.dataStore.resizeClaim(playerData.claimResizing, newx1, newx2, newy1, newy2, newz1, newz2);
				
				if(result.succeeded)
				{
					//inform and show the player
					GriefPrevention.sendMessage(player, TextMode.Success, "Claim resized.  You now have " + playerData.getRemainingClaimBlocks() + " available claim blocks.");
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim);
					Visualization.Apply(player, visualization);
					
					//if resizing someone else's claim, make a log entry
					if(!playerData.claimResizing.ownerName.equals(playerName))
					{
						GriefPrevention.AddLogEntry(playerName + " resized " + playerData.claimResizing.getOwnerName() + "'s claim at " + GriefPrevention.getfriendlyLocationString(playerData.claimResizing.lesserBoundaryCorner) + ".");
					}
					
					//clean up
					playerData.claimResizing = null;
					playerData.lastShovelLocation = null;
				}
				else
				{
					//inform player
					GriefPrevention.sendMessage(player, TextMode.Err, "Can't resize here because it would overlap another nearby claim.");
					
					//show the player the conflicting claim
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim);
					Visualization.Apply(player, visualization);
				}
				
				return;
			}
			
			//otherwise, since not currently resizing a claim, must be starting a resize, creating a new claim, or creating a subdivision
			Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), true /*ignore height*/, playerData.lastClaim);			
			
			//if within an existing claim, he's not creating a new one
			if(claim != null)
			{
				//if the player has permission to edit the claim or subdivision
				String noEditReason = claim.allowEdit(player);
				if(noEditReason == null)
				{
					//if he clicked on a corner, start resizing it
					if((clickedBlock.getX() == claim.getLesserBoundaryCorner().getBlockX() || clickedBlock.getX() == claim.getGreaterBoundaryCorner().getBlockX()) && (clickedBlock.getZ() == claim.getLesserBoundaryCorner().getBlockZ() || clickedBlock.getZ() == claim.getGreaterBoundaryCorner().getBlockZ()))
					{
						playerData.claimResizing = claim;
						playerData.lastShovelLocation = clickedBlock.getLocation();
						player.sendMessage("Resizing claim.  Use your shovel again at the new location for this corner.");
					}
					
					//if he didn't click on a corner and is in subdivision mode, he's creating a new subdivision
					else if(playerData.shovelMode == ShovelMode.Subdivide)
					{
						//if it's the first click, he's trying to start a new subdivision
						if(playerData.lastShovelLocation == null)
						{						
							//if the clicked claim was a subdivision, tell him he can't start a new subdivision here
							if(claim.parent != null)
							{
								GriefPrevention.sendMessage(player, TextMode.Err, "You can't create a subdivision here because it would overlap another subdivision.  Consider /abandonclaim to delete it, or use your shovel at a corner to resize it.");							
							}
						
							//otherwise start a new subdivision
							else
							{
								GriefPrevention.sendMessage(player, TextMode.Instr, "Subdivision corner set!  Use your shovel at the location for the opposite corner of this new subdivision.");
								playerData.lastShovelLocation = clickedBlock.getLocation();
								playerData.claimSubdividing = claim;
							}
						}
						
						//otherwise, he's trying to finish creating a subdivision by setting the other boundary corner
						else
						{
							//if last shovel location was in a different world, assume the player is starting the create-claim workflow over
							if(!playerData.lastShovelLocation.getWorld().equals(clickedBlock.getWorld()))
							{
								playerData.lastShovelLocation = null;
								this.onPlayerInteract(event);
								return;
							}
							
							//try to create a new claim (will return null if this subdivision overlaps another)
							CreateClaimResult result = this.dataStore.createClaim(
									player.getWorld(), 
									playerData.lastShovelLocation.getBlockX(), clickedBlock.getX(), 
									playerData.lastShovelLocation.getBlockY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance, clickedBlock.getY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance, 
									playerData.lastShovelLocation.getBlockZ(), clickedBlock.getZ(), 
									"--subdivision--",  //owner name is not used for subdivisions
									playerData.claimSubdividing);
							
							//if it didn't succeed, tell the player why
							if(!result.succeeded)
							{
								GriefPrevention.sendMessage(player, TextMode.Err, "Your selected area overlaps another subdivision.");
																				
								Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim);
								Visualization.Apply(player, visualization);
								
								return;
							}
							
							//otherwise, advise him on the /trust command and show him his new subdivision
							else
							{					
								GriefPrevention.sendMessage(player, TextMode.Success, "Subdivision created!  Use /trust to share it with friends.");
								Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim);
								Visualization.Apply(player, visualization);
								playerData.lastShovelLocation = null;
								playerData.claimSubdividing = null;
							}
						}
					}
					
					//otherwise tell him he can't create a claim here, and show him the existing claim
					//also advise him to consider /abandonclaim or resizing the existing claim
					else
					{						
						GriefPrevention.sendMessage(player, TextMode.Err, "You can't create a claim here because it would overlap your other claim.  Use /abandonclaim to delete it, or use your shovel at a corner to resize it.");
						Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.Claim);
						Visualization.Apply(player, visualization);
					}
				}
				
				//otherwise tell the player he can't claim here because it's someone else's claim, and show him the claim
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "You can't create a claim here because it would overlap " + claim.getOwnerName() + "'s claim.");
					Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.ErrorClaim);
					Visualization.Apply(player, visualization);						
				}
				
				return;
			}
			
			//otherwise, the player isn't in an existing claim!
			
			//if he hasn't already start a claim with a previous shovel action
			Location lastShovelLocation = playerData.lastShovelLocation;
			if(lastShovelLocation == null)
			{
				//if claims are not enabled in this world and it's not an administrative claim, display an error message and stop
				if(!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld()) && playerData.shovelMode != ShovelMode.Admin)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "Land claims are disabled in this world.");
					return;
				}
				
				//remember it, and start him on the new claim
				playerData.lastShovelLocation = clickedBlock.getLocation();
				GriefPrevention.sendMessage(player, TextMode.Instr, "Claim corner set!  Use the shovel again at the opposite corner to claim a rectangle of land.  To cancel, put your shovel away.");
			}
			
			//otherwise, he's trying to finish creating a claim by setting the other boundary corner
			else
			{
				//if last shovel location was in a different world, assume the player is starting the create-claim workflow over
				if(!lastShovelLocation.getWorld().equals(clickedBlock.getWorld()))
				{
					playerData.lastShovelLocation = null;
					this.onPlayerInteract(event);
					return;
				}
				
				//apply minimum claim dimensions rule
				int newClaimWidth = Math.abs(playerData.lastShovelLocation.getBlockX() - clickedBlock.getX()) + 1;
				int newClaimHeight = Math.abs(playerData.lastShovelLocation.getBlockZ() - clickedBlock.getZ()) + 1;
				
				if(playerData.shovelMode != ShovelMode.Admin && (newClaimWidth < GriefPrevention.instance.config_claims_minSize || newClaimHeight < GriefPrevention.instance.config_claims_minSize))
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "This claim would be too small.  Any claim must be at least " + GriefPrevention.instance.config_claims_minSize + " x " + GriefPrevention.instance.config_claims_minSize + ".");
					return;
				}
				
				//if not an administrative claim, verify the player has enough claim blocks for this new claim
				if(playerData.shovelMode != ShovelMode.Admin)
				{					
					int newClaimArea = newClaimWidth * newClaimHeight; 
					int remainingBlocks = playerData.getRemainingClaimBlocks();
					if(newClaimArea > remainingBlocks)
					{
						GriefPrevention.sendMessage(player, TextMode.Err, "You don't have enough blocks to claim that entire area.  You need " + (newClaimArea - remainingBlocks) + " more blocks.");
						GriefPrevention.sendMessage(player, TextMode.Instr, "To delete another claim and free up some blocks, use /AbandonClaim.");
						return;
					}
				}					
				else
				{
					playerName = "";
				}
				
				//try to create a new claim (will return null if this claim overlaps another)
				CreateClaimResult result = this.dataStore.createClaim(
						player.getWorld(), 
						lastShovelLocation.getBlockX(), clickedBlock.getX(), 
						lastShovelLocation.getBlockY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance, clickedBlock.getY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance, 
						lastShovelLocation.getBlockZ(), clickedBlock.getZ(), 
						playerName,
						null);
				
				//if it didn't succeed, tell the player why
				if(!result.succeeded)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "Your selected area overlaps an existing claim.");
					
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim);
					Visualization.Apply(player, visualization);
					
					return;
				}
				
				//otherwise, advise him on the /trust command and show him his new claim
				else
				{					
					GriefPrevention.sendMessage(player, TextMode.Success, "Claim created!  Use /trust to share it with friends.");
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim);
					Visualization.Apply(player, visualization);
					playerData.lastShovelLocation = null;
				}
			}
		}
	}	
}
