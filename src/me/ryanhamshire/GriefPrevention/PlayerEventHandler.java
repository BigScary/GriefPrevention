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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData.ClaimAllowanceConstants;
import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData;
import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData.ClaimBehaviourMode;
import me.ryanhamshire.GriefPrevention.Configuration.ItemUsageRules;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.Debugger.DebugLevel;
import me.ryanhamshire.GriefPrevention.tasks.EquipShovelProcessingTask;
import me.ryanhamshire.GriefPrevention.tasks.PlayerKickBanTask;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;
import me.ryanhamshire.GriefPrevention.visualization.VisualizationType;

import org.bukkit.Bukkit;
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
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Horse;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import com.sun.org.apache.xerces.internal.dom.EntityImpl;

class PlayerEventHandler implements Listener 
{
	private DataStore dataStore;
	
	//list of temporarily banned ip's
	private ArrayList<IpBanInfo> tempBannedIps = new ArrayList<IpBanInfo>();
	
	//number of milliseconds in a day
	private final long MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;
	
	//timestamps of login and logout notifications in the last minute
	private ArrayList<Long> recentLoginLogoutNotifications = new ArrayList<Long>();
	
	//regex pattern for the "how do i claim land?" scanner
	private Pattern howToClaimPattern = null;
	
	//typical constructor, yawn
	PlayerEventHandler(DataStore dataStore, GriefPrevention plugin)
	{
		this.dataStore = dataStore;
	}
	
	//when a player chats, monitor for spam
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	synchronized void onPlayerChat (AsyncPlayerChatEvent event)
	{		
	
		Player player = event.getPlayer();
		if(!player.isOnline())
		{
			event.setCancelled(true);
			return;
		}
		
		String message = event.getMessage();
		
		event.setCancelled(this.handlePlayerChat(player, message, event));
	}
	
	//returns true if the message should be sent, false if it should be muted 
	private boolean handlePlayerChat(Player player, String message, PlayerEvent event)
	{
		//FEATURE: automatically educate players about claiming land
		//watching for message format how*claim*, and will send a link to the basics video
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		
		
		if(this.howToClaimPattern == null)
		{
			this.howToClaimPattern = Pattern.compile(this.dataStore.getMessage(Messages.HowToClaimRegex), Pattern.CASE_INSENSITIVE);
		}
		Messages showclaimmessage =null;
		if(this.howToClaimPattern.matcher(message).matches())
		{
			if(GriefPrevention.instance.creativeRulesApply(player.getLocation()))
			{
				showclaimmessage=Messages.CreativeBasicsDemoAdvertisement;
				
			}
			else
			{
				showclaimmessage = Messages.SurvivalBasicsDemoAdvertisement;
			}
			//retrieve the data on this player...
			final PlayerData pdata = GriefPrevention.instance.dataStore.getPlayerData(player.getName());
			//if they are currently set to ignore, do not send anything.
			if(!pdata.IgnoreClaimMessage){
			
				//otherwise, set IgnoreClaimMessage and use a anonymous runnable to reset it after the timeout.
				//of note is that if the value is zero this is pretty much run right away, which means the end result is there
				//is no actual timeout.
				pdata.IgnoreClaimMessage=true;
				
				Bukkit.getScheduler()
						.runTaskLater(
								GriefPrevention.instance,
								new Runnable() {
									public void run() {
										pdata.IgnoreClaimMessage = false;
									}
								},wc.getMessageCooldownClaims()*20);
				
				
				
				
				
				//send off the message.
				GriefPrevention.sendMessage(player, TextMode.Info, showclaimmessage, 10L);
			
			}
		}
		
		//FEATURE: automatically educate players about the /trapped command
		//check for "trapped" or "stuck" to educate players about the /trapped command
		if(!message.contains("/trapped") && (message.contains("trapped") || message.contains("stuck") || message.contains(this.dataStore.getMessage(Messages.TrappedChatKeyword))))
		{
			final PlayerData pdata = 
					GriefPrevention.instance.dataStore.getPlayerData(player.getName());
			//if not set to ignore the stuck message, show it, set the ignore flag, and set an anonymous runnable to reset it after the
			//configured delay.
			if(!pdata.IgnoreStuckMessage){
			
				pdata.IgnoreStuckMessage=true;
				
				Bukkit.getScheduler()
				.runTaskLater(GriefPrevention.instance,new Runnable() {
								public void run() {
								pdata.IgnoreStuckMessage=true;
							}
						}
			,wc.getMessageCooldownStuck()*20);
				
				GriefPrevention.sendMessage(player, TextMode.Info, Messages.TrappedInstructions, 10L);
			}
		}
		
		//FEATURE: monitor for chat and command spam
		
		if(!wc.getSpamProtectionEnabled()) return false;
		
		//if the player has permission to spam, don't bother even examining the message
		if(player.hasPermission("griefprevention.spam")) return false;
		
		boolean spam = false;
		boolean muted = false;
		
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		//remedy any CAPS SPAM, exception for very short messages which could be emoticons like =D or XD
		if(message.length() > 4 && this.stringsAreSimilar(message.toUpperCase(), message))
		{
			//exception for strings containing forward slash to avoid changing a case-sensitive URL
			if(event instanceof AsyncPlayerChatEvent && !message.contains("/"))
			{
				((AsyncPlayerChatEvent)event).setMessage(message.toLowerCase());
			}
		}
		
		//where other types of spam are concerned, casing isn't significant
		message = message.toLowerCase();
		
		//check message content and timing		
		long millisecondsSinceLastMessage = (new Date()).getTime() - playerData.lastMessageTimestamp.getTime();
		
		//if the message came too close to the last one
		if(millisecondsSinceLastMessage < wc.getSpamDelayThreshold())
		{
			//increment the spam counter
			playerData.spamCount++;
			spam = true;
		}
		
		//if it's very similar to the last message
		if(!muted && this.stringsAreSimilar(message, playerData.lastMessage))
		{
			playerData.spamCount++;
			spam = true;
			muted = true;
		}
		
		//filter IP addresses
		if(!muted)
		{
			Pattern ipAddressPattern = Pattern.compile("\\d{1,4}\\D{1,3}\\d{1,4}\\D{1,3}\\d{1,4}\\D{1,3}\\d{1,4}");
			Matcher matcher = ipAddressPattern.matcher(message);
			
			//if it looks like an IP address
			if(matcher.find())
			{
				//and it's not in the list of allowed IP addresses
				if(!wc.getSpamAllowedIpAddresses().contains(matcher.group()))
				{
					//log entry
					GriefPrevention.AddLogEntry("Muted IP address from " + player.getName() + ": " + message);
					
					//spam notation
					playerData.spamCount++;
					spam = true;
					
					//block message
					muted = true;
				}
			}
		}
		if(!muted && message.length() > wc.getSpamCapsMinLength()){
		
			//check for caps in lengths greater than the specified limit.
			StringBuilder sbuffer = new StringBuilder();
			try {
			for(int i=0;i<message.length()-wc.getSpamCapsMinLength();i++){
				String teststr = ""; 
				try {
				teststr = message.substring(i,i+wc.getSpamCapsMinLength());
				}
				catch(StringIndexOutOfBoundsException exx){
					//testing code for ticket 212
					//print out diagnostic info.
					exx.printStackTrace();
					System.out.println("Unexpected Error Processing Chat Command");
					System.out.println("Current Message:\"" + message + "\"");
					System.out.println("SpamCapsMinLength:" + wc.getSpamCapsMinLength());
					System.out.println("i:" + i);
					
					
				}
				if(teststr.equals(teststr.toUpperCase())){
					//gotcha!
					sbuffer.append(teststr.toLowerCase());
					i+=teststr.length();
				}
				else
					sbuffer.append(message.charAt(i));
				
			}
			}
			finally{}
			
		}
		
		
		//if the message was mostly non-alpha-numerics or doesn't include much whitespace, consider it a spam (probably ansi art or random text gibberish) 
		if(!muted && message.length() > wc.getSpamNonAlphaNumMinLength())
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
			
			if(symbolsCount > message.length() / 2 || (message.length() > wc.getSpamASCIIArtMinLength() && whitespaceCount < message.length() / 10))
			{
				spam = true;
				if(playerData.spamCount > 0) muted = true;
				playerData.spamCount++;
			}
		}
		
		//very short messages close together are spam
		if(!muted && message.length() < wc.getSpamShortMessageMaxLength() && millisecondsSinceLastMessage < wc.getSpamShortMessageTimeout())
		{
			spam = true;
			playerData.spamCount++;
		}
		
		//if the message was determined to be a spam, consider taking action		
		if(spam)
		{		
			if(playerData.spamCount > wc.getSpamKickThreshold() && playerData.spamWarned){
				//log entry
				GriefPrevention.AddLogEntry("Banning " + player.getName() + " for spam.");
				
				//kick
				PlayerKickBanTask task = new PlayerKickBanTask(player, wc.getSpamKickMessage());
				GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 1L);					
			
			}
			
			//check for ban threshold. 
			if(playerData.spamCount > wc.getSpamBanThreshold() && wc.getSpamBanThreshold() >0 &&  playerData.spamWarned)
			{
					//log entry
					GriefPrevention.AddLogEntry("Acting on " + player.getName() + " for spam.");
					
					//kick and ban
					PlayerKickBanTask task = new PlayerKickBanTask(player, wc.getSpamBanMessage());
					GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 1L);
				
				
				
				return true;
			}
			
			//cancel any messages while at or above the third spam level and issue warnings
			//anything above level 2, mute and warn
			if(playerData.spamCount >= wc.getSpamMuteThreshold())
			{
				muted = true;
				if(!playerData.spamWarned)
				{
					GriefPrevention.sendMessage(player, TextMode.Warn, wc.getSpamWarningMessage(), 10L);
					GriefPrevention.AddLogEntry("Warned " + player.getName() + " about spam penalties.");
					playerData.spamWarned = true;
				}
			}
			
			if(muted)
			{
				//make a log entry
				GriefPrevention.AddLogEntry("Muted spam (spamcount:" + playerData.spamCount + ") from " + player.getName() + ": " + message);
				
				//send a fake message so the player doesn't realize he's muted
				//less information for spammers = less effective spam filter dodging
				player.sendMessage("<" + player.getName() + "> " + message);
				
				//cancelling the event guarantees other players don't receive the message
				return true;
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
		
		return false;
	}
	
	//if two strings are 75% identical, they're too close to follow each other in the chat
	private boolean stringsAreSimilar(String message, String lastMessage)
	{
		if(message==null||lastMessage==null) return false;
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

	//when a player uses a slash command...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	synchronized void onPlayerCommandPreprocess (PlayerCommandPreprocessEvent event)
	{
		String [] args = event.getMessage().split(" ");
		if(event.getPlayer()==null) return;
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getPlayer().getWorld());
		if(wc==null) return;
		//if eavesdrop enabled, eavesdrop
		List<String> WhisperCommands = wc.eavesdrop_whisperCommands();
		String command = args[0].toLowerCase();
		if(wc.getEavesDrop() && WhisperCommands.contains(command) && args.length > 1)
		{			
			StringBuilder logMessageBuilder = new StringBuilder();
			logMessageBuilder.append("[[").append(event.getPlayer().getName()).append("]] ");
			
			for(int i = 1; i < args.length; i++)
			{
				logMessageBuilder.append(args[i]).append(" ");
			}
			
			String logMessage = logMessageBuilder.toString();
			
			GriefPrevention.sendEavesDropMessage(event.getPlayer(), logMessage);
		}
		
		//if in pvp, block any pvp-banned slash commands
		PlayerData playerData = this.dataStore.getPlayerData(event.getPlayer().getName());
		if((playerData.inPvpCombat() || playerData.siegeData != null) && wc.getPvPBlockedCommands().contains(command))
		{
			event.setCancelled(true);
			GriefPrevention.sendMessage(event.getPlayer(), TextMode.Err, Messages.CommandBannedInPvP);
			return;
			
		}
		
		//if anti spam enabled, check for spam
		if(!wc.getSpamProtectionEnabled()) return;
		
		//if the slash command used is in the list of monitored commands, treat it like a chat message (see above)
		boolean isMonitoredCommand = false;
		for(String monitoredCommand : wc.getSpamMonitorSlashCommands())
		{
			if(args[0].equalsIgnoreCase(monitoredCommand))
			{
				isMonitoredCommand = true;
				break;
			}
		}
		
		if(isMonitoredCommand)
		{
			event.setCancelled(this.handlePlayerChat(event.getPlayer(), event.getMessage(), event));		
		}
	}

	
	
	//when a player attempts to join the server...
	@EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerLogin (PlayerLoginEvent event)
	{
		Player player = event.getPlayer();
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		//all this is anti-spam code
		if(wc.getSpamProtectionEnabled())
		{
			//FEATURE: login cooldown to prevent login/logout spam with custom clients
			
			//if allowed to join and login cooldown enabled
			if(wc.getSpamLoginCooldownSeconds() > 0 && event.getResult() == Result.ALLOWED)
			{
				//determine how long since last login and cooldown remaining
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());
				long millisecondsSinceLastLogin = (new Date()).getTime() - playerData.lastLogin.getTime();
				long secondsSinceLastLogin = millisecondsSinceLastLogin / 1000;
				long cooldownRemaining = wc.getSpamLoginCooldownSeconds() - secondsSinceLastLogin;
				
				//if cooldown remaining and player doesn't have permission to spam
				if(cooldownRemaining > 0 && !player.hasPermission("griefprevention.loginspam"))
				{
					//DAS BOOT!
					event.setResult(Result.KICK_OTHER);
					String cooldown = GriefPrevention.instance.dataStore.getMessage(Messages.LoginSpamWaitSeconds, Long.toString(cooldownRemaining));
					event.setKickMessage(cooldown);
					event.disallow(event.getResult(), event.getKickMessage());
					return;
				}
			}
			
			//if logging-in account is banned, remember IP address for later
			long now = Calendar.getInstance().getTimeInMillis();
			if(wc.getSmartBan() && event.getResult() == Result.KICK_BANNED)
			{
				this.tempBannedIps.add(new IpBanInfo(event.getAddress(), now + this.MILLISECONDS_IN_DAY, player.getName()));
			}
		}
		
		//remember the player's ip address 
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		playerData.ipAddress = event.getAddress();
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
		
		Player player = event.getPlayer();
		String playerName = player.getName();
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		//note login time
		long now = new Date().getTime();
		final PlayerData playerData = this.dataStore.getPlayerData(playerName);
		playerData.lastSpawn = now;
		playerData.lastLogin = new Date();
		this.dataStore.savePlayerData(playerName, playerData);
		
		//if player has never played on the server before, may need pvp protection
		if(!player.hasPlayedBefore())
		{
			GriefPrevention.instance.checkPvpProtectionNeeded(player);
			//also flip their spam
			playerData.spamCount=4; //start them off at 4.
			playerData.spamWarned=true; //don't give them a warning if they want to misbehave, either.
			//start a delayed Runnable to reset this after 30 seconds.
			Bukkit.getScheduler().runTaskLater(GriefPrevention.instance, 
					new Runnable(){
				public void run(){
					playerData.spamCount=0;
				}
			}
					
					,20*30);
			
			
			
		}
		
		//silence notifications when they're coming too fast
		if(event.getJoinMessage() != null && this.shouldSilenceNotification())
		{
			event.setJoinMessage(null);
		}
		
		//FEATURE: auto-ban accounts who use an IP address which was very recently used by another banned account
		if(wc.getSmartBan() && !player.hasPlayedBefore())
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
						GriefPrevention.AddLogEntry("Auto-banned " + player.getName() + " because that account is using an IP address very recently used by banned player " + info.bannedAccountName + " (" + info.address.toString() + ").");
						
						//notify any online ops
						Player [] players = GriefPrevention.instance.getServer().getOnlinePlayers();
						for(int k = 0; k < players.length; k++)
						{
							if(players[k].isOp())
							{
								GriefPrevention.sendMessage(players[k], TextMode.Success, Messages.AutoBanNotify, player.getName(), info.bannedAccountName);
							}
						}
						
						//ban player
						PlayerKickBanTask task = new PlayerKickBanTask(player, "");
						GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 10L);
						
						//silence join message
						event.setJoinMessage("");
						
						break;
					}
				}
			}
		}
	}
	
	//when a player dies...
	@EventHandler(priority = EventPriority.LOWEST)
	void onPlayerDeath(PlayerDeathEvent event)
	{
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		//FEATURE: prevent death message spam by implementing a "cooldown period" for death messages
		PlayerData playerData = this.dataStore.getPlayerData(event.getEntity().getName());
		long now = Calendar.getInstance().getTimeInMillis(); 
		if(now - playerData.lastDeathTimeStamp < wc.getSpamDeathMessageCooldownSeconds() * 1000)
		{
			event.setDeathMessage("");
		}
		
		playerData.lastDeathTimeStamp = now;
	}
	
	//when a player quits...
	@EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerQuit(PlayerQuitEvent event)
	{
		
		Player player = event.getPlayer();
		//WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		//if banned, add IP to the temporary IP ban list
		if(player.isBanned() && playerData.ipAddress != null)
		{
			long now = Calendar.getInstance().getTimeInMillis(); 
			this.tempBannedIps.add(new IpBanInfo(playerData.ipAddress, now + this.MILLISECONDS_IN_DAY, player.getName()));
		}
		
		//silence notifications when they're coming too fast
		if(event.getQuitMessage() != null && this.shouldSilenceNotification())
		{
			event.setQuitMessage(null);
		}
		
		//make sure his data is all saved - he might have accrued some claim blocks while playing that were not saved immediately
		this.dataStore.savePlayerData(player.getName(), playerData);
		
		this.onPlayerDisconnect(event.getPlayer(), event.getQuitMessage());
	}
	
	//helper for above
	private void onPlayerDisconnect(Player player, String notificationMessage)
	{
		String playerName = player.getName();
		PlayerData playerData = this.dataStore.getPlayerData(playerName);
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		//FEATURE: claims where players have allowed explosions will revert back to not allowing them when the owner logs out
		for(Claim claim : playerData.claims)
		{
			claim.areExplosivesAllowed = false;
		}
		
		//FEATURE: players in pvp combat when they log out will die
		if(wc.getPvPPunishLogout() && playerData.inPvpCombat())
		{
			player.setHealth(0);
		}
		
		//FEATURE: during a siege, any player who logs out dies and forfeits the siege
		
		//if player was involved in a siege, he forfeits
		if(playerData.siegeData != null)
		{
			if(player.getHealth() > 0) player.setHealth(0);  //might already be zero from above, this avoids a double death message
		}
		
		//drop data about this player
		this.dataStore.clearCachedPlayerData(player.getName());
	}
	
	//determines whether or not a login or logout notification should be silenced, depending on how many there have been in the last minute
	private boolean shouldSilenceNotification()
	{
		final long ONE_MINUTE = 60000;
		final int MAX_ALLOWED = 20;
		Long now = Calendar.getInstance().getTimeInMillis();
		
		//eliminate any expired entries (longer than a minute ago)
		for(int i = 0; i < this.recentLoginLogoutNotifications.size(); i++)
		{
			Long notificationTimestamp = this.recentLoginLogoutNotifications.get(i);
			if(now - notificationTimestamp > ONE_MINUTE)
			{
				this.recentLoginLogoutNotifications.remove(i--);
			}
			else
			{
				break;
			}
		}
		
		//add the new entry
		this.recentLoginLogoutNotifications.add(now);
		
		return this.recentLoginLogoutNotifications.size() > MAX_ALLOWED;
	}

	//when a player drops an item
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerDropItem(PlayerDropItemEvent event)
	{
		Player player = event.getPlayer();
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		//in creative worlds, dropping items is blocked
		if(wc.getCreativeRules())
		{
			event.setCancelled(true);
			return;
		}
		
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		//FEATURE: players under siege or in PvP combat, can't throw items on the ground to hide 
		//them or give them away to other players before they are defeated
		
		//if in combat, don't let him drop it
		if(!wc.getAllowCombatItemDrop() && playerData.inPvpCombat())
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.PvPNoDrop);
			event.setCancelled(true);			
		}
		
		//if he's under siege, don't let him drop it
		else if(playerData.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoDrop);
			event.setCancelled(true);
		}
	}
	
	//when a player teleports
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		Player player = event.getPlayer();
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		
		//FEATURE: prevent players from using ender pearls to gain access to secured claims
		if(event.getCause() == TeleportCause.ENDER_PEARL)
		{
			if(wc.getEnderPearlOrigins().Allowed(event.getFrom(), player).Denied()){
				player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL,1));
				event.setCancelled(true);
				return;
			}
			
			
			else if(wc.getEnderPearlTargets().Allowed(event.getTo(), player).Denied()){
				player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL,1));
				event.setCancelled(true);
				return;
			}
		}
		
		//FEATURE: prevent teleport abuse to win sieges
		
		//these rules only apply to non-ender-pearl teleportation
		if(event.getCause() == TeleportCause.ENDER_PEARL) return;
		
		Location source = event.getFrom();
		Claim sourceClaim = this.dataStore.getClaimAt(source, false, playerData.lastClaim);
		if(sourceClaim != null && sourceClaim.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoTeleport);
			event.setCancelled(true);
			return;
		}
		
		Location destination = event.getTo();
		Claim destinationClaim = this.dataStore.getClaimAt(destination, false, null);
		if(destinationClaim != null && destinationClaim.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.BesiegedNoTeleport);
			event.setCancelled(true);
			return;
		}
	}
	@EventHandler(priority=EventPriority.LOWEST)
	public void onPlayerShearEntity(PlayerShearEntityEvent event){
		
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		Player player = event.getPlayer();
		Entity entity = event.getEntity();
		if(wc.getShearingRules().Allowed(entity.getLocation(), player).Denied())
		{
			event.setCancelled(true);
		}
		
	}
	private boolean isHorse(Entity entityfor){
		return GriefPrevention.instance.isHorse(entityfor);
	}
	
	//when a player interacts with an entity...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
	{
		try {
		
		Player player = event.getPlayer();
		Entity entity = event.getRightClicked();
		
		ItemStack handItem = player.getItemInHand();
		//Note: if the player is currently riding that entity, then we allow with abandon the right-click operation.
		//I can imagine there might need to be additional logic to determine ownership of certain things such as horses, donkeys, and mules
		//once 1.6 rolls around, too.
		
		if(player.isInsideVehicle()){
			if(entity.getPassenger()==player){
				//allow.
				return;
			}
		}
		
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		
		
		//don't allow interaction with item frames in claimed areas without build permission
		if(entity instanceof Hanging)
		{
			String noBuildReason = GriefPrevention.instance.allowBuild(player, entity.getLocation()); 
			if(noBuildReason != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
				event.setCancelled(true);
				return;
			}			
		}
		
		if(entity instanceof Creature){
			//check for usage of Leads.
			//This is for with creatures, the BlockEvent interaction handles interaction with Fence and Netherbrick fence
			//blocks.
			if(handItem.getType()==Material.LEASH){
				if(wc.getLeadUsageRules().Allowed(entity.getLocation(), player).Denied()){
					event.setCancelled(true);
					return;
				}
			}
			else if(handItem.getType().getId()==421){
				if(wc.getNameTagUsageRules().Allowed(entity.getLocation(), player).Denied()){
					event.setCancelled(true);
					return;
				}
			}
		}
		if (isHorse(entity)){
			
			Horse h = (Horse)entity;
			
			if(h.isTamed() && handItem.getType()==Material.GOLDEN_APPLE){
				//if horse is tamed, apply breeding rules.
				if( wc.getBreedingRules().Allowed(h.getLocation(), player).Denied()){
					event.setCancelled(true);
					return;
				}
				
			}
			else if(handItem.getType()==Material.WHEAT || handItem.getType()==Material.HAY_BLOCK ||
					handItem.getType()==Material.APPLE || handItem.getType()==Material.GOLDEN_APPLE){
				//apply feeding rules.
				if(wc.getFeedingRules().Allowed(h.getLocation(), player).Denied()){
					event.setCancelled(true);
					return;
				}
				
			}
			else{
				
				if(h.isTamed()){
					//if the player is the owner of the horse,
					//they can do what they want no matter where they are.
				{
					if(h.getOwner()==null || h.getOwner().getName().equals(player.getName())) return;
					}
						//otherwise, Apply Containers rules if they aren't sneaking, otherwise,
						//apply the Inventory rules.
						//what this means is that if you ride a horse onto somebody elses claim, they will have access to the horse.
						//if the EquineInventoryRules/ContainerRules are set to allow.
						if(player.isSneaking()){
							if(wc.getEquineInventoryRules().Allowed(h.getLocation(), player).Denied())
								event.setCancelled(true);
								return;
							
						}
				}
				
				
				
			}
		}
		
		//check for breeding animals. We don't check horse breeding here because  that is covered above.
		//we cover breeding other tamables in another section of logic below.
		if(entity instanceof Pig || entity instanceof Sheep || entity instanceof Cow ||
				entity instanceof MushroomCow || entity instanceof Chicken){
			//is the player holding the breeding item for this entity?
			List<Material> breeder = new ArrayList<Material>();
			for(Material iterate:GriefPrevention.instance.getEntityBreedingItems(entity)){
			breeder.add(iterate);
			}
			if(breeder.contains(handItem.getType())){
				
				//can we breed?
				if(wc.getBreedingRules().Allowed(entity.getLocation(), player).Denied()){
					event.setCancelled(true);
					return; //nope, disallow.
				}
				
			}
		}
		
		
		
		//check for taming, feeding and breeding of wolves and ocelots.
		if(entity instanceof Wolf || entity instanceof Ocelot){
			Tameable tamed = (Tameable)entity;
			if(tamed.isTamed()){
				//tamed animals can be fed and bred.
				//if they are at full health, check for breeding.
				Creature cr = (Creature)entity;
				if(cr.getMaxHealth() == cr.getHealth()){
					//max health, so they cannot be fed, but they can be bred. so check for breeding.
					List<Material> breeder = new ArrayList<Material>();
					for(Material iterate:GriefPrevention.instance.getEntityBreedingItems(entity)){
					breeder.add(iterate);
					}
					if(breeder.contains(handItem)){
						//player is holding a breeding item and trying to use it, so check the rules.
						if(wc.getBreedingRules().Allowed(entity.getLocation(),player).Denied()){
							event.setCancelled(true);
							return;
						}
					}
					
				}
				else {
					//not max health, so they can be fed. Feeding items overlap with breeding items
					//so you cannot breed Tameables that aren't fully fed in most cases.
					List<Material> feeder = new ArrayList<Material>();
					for(Material iterate:GriefPrevention.instance.getEntityBreedingItems(entity)){
					feeder.add(iterate);
					}
					if(feeder.contains(handItem)){
						if(wc.getFeedingRules().Allowed(entity.getLocation(), player).Denied()){
							event.setCancelled(true);
							return;
						}
					}
					
					
				}
				
				
				
			}
			
			
		}
		if(entity instanceof Villager && wc.getVillagerTrades().Allowed(entity.getLocation(),player).Denied()){
			
			    	
			    	event.setCancelled(true);
			    	return;
		}
		if((entity instanceof Sheep && event.getPlayer().getItemInHand().getType()==Material.INK_SACK)){
			//apply dyeing rules.
			if(wc.getSheepDyeingRules().Allowed(entity.getLocation(), event.getPlayer()).Denied()){
				event.setCancelled(true);
				return;
			}
			
			
			
		}
	
			
		
		
		//don't allow container access during pvp combat
		
		if((entity instanceof StorageMinecart || entity instanceof PoweredMinecart || entity instanceof HopperMinecart))
		{
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoContainers);
				event.setCancelled(true);
				return;
			}
			
			if(playerData.inPvpCombat())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PvPNoContainers);
				event.setCancelled(true);
				return;
			}			
		}
		
		//if the entity is a vehicle and we're preventing theft in claims		
		if(wc.getContainersRules().Allowed(entity.getLocation(),player,false).Denied() && entity instanceof Vehicle)
		{
			//if the entity is in a claim
			Claim claim = this.dataStore.getClaimAt(entity.getLocation(), false, null);
			if(claim != null)
			{
				//for storage, hopper, and powered minecarts, apply container rules (this is a potential theft)
				if(entity instanceof StorageMinecart || entity instanceof PoweredMinecart || entity instanceof HopperMinecart)
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
						GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoDamageClaimedEntity);
						event.setCancelled(true);
					}
				}
			}
		}
		}
		finally {
			Debugger.Write("PlayerInteractEntity: Cancelled:" + event.isCancelled(),DebugLevel.Verbose);
		}
	}
	
	//when a player picks up an item...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerPickupItem(PlayerPickupItemEvent event)
	{
		//we allow plugins to give players items regardless.
		//eg: this only prevents "true" pickups.
		if(event.getItem().getTicksLived()<=10) return; 
		//presumption: items given by Plugins will be nearly instant.
		//so if a item is triggering this event and younger than half a second, we'll assume some plugin
		//has bestowed it.
		Player player = event.getPlayer();
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		if(!event.getPlayer().getWorld().getPVP()) return;
		
		//if we're preventing spawn camping and the player was previously empty handed...
		if(wc.getProtectFreshSpawns() && (player.getItemInHand().getType() == Material.AIR))
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
				GriefPrevention.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
			}			
		}
	}
	
	//when a player switches in-hand items
	@EventHandler(ignoreCancelled = true)
	public void onItemHeldChange(PlayerItemHeldEvent event)
	{
		Player player = event.getPlayer();
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		//if he's switching to the golden shovel
		ItemStack newItemStack = player.getInventory().getItem(event.getNewSlot());
		if(newItemStack == null) return;
		if(newItemStack.getType() == wc.getClaimsModificationTool())
		{
			PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getName());
		
			//always reset to basic claims mode
			if(playerData.shovelMode != ShovelMode.Basic)
			{			
				playerData.shovelMode = ShovelMode.Basic;
				GriefPrevention.sendMessage(player, TextMode.Info, Messages.ShovelBasicClaimMode);
			}
		
			//reset any work he might have been doing
			playerData.lastShovelLocation = null;
			playerData.claimResizing = null;
			
			//give the player his available claim blocks count and claiming instructions, but only if he keeps the shovel equipped for a minimum time, to avoid mouse wheel spam
			if(GriefPrevention.instance.claimsEnabledForWorld(player.getWorld()))
			{
				EquipShovelProcessingTask task = new EquipShovelProcessingTask(player);
				GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 15L);  //15L is approx. 3/4 of a second
			}
		}
		else if(newItemStack.getType()== wc.getAdministrationTool()){
			//make sure they have permission.
			if(player.hasPermission("GriefPrevention.Administration")){
			    GriefPrevention.sendMessage(player, TextMode.Info, "GriefPrevention Admin tool selected. Left-Click to add to container list. Shift Left-click to add to only the current world.");
			}
			
			
			
		}
//		else if(newItemStack.getType() == wc.getClaimsAccessTrustTool()){
//			//access trust
//		}
//		else if(newItemStack.getType() == wc.getClaimsContainerTrustTool()){
//			
//		}
	}
	
	//block players from entering beds they don't have permission for
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerBedEnter (PlayerBedEnterEvent bedEvent)
	{
		
		
		
		Player player = bedEvent.getPlayer();
		Block block = bedEvent.getBed();
		if(player==null || block==null) return;
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(block.getWorld());
		if(wc==null){
			bedEvent.setCancelled(true);
		}
		ClaimAllowanceConstants resultdata = wc.getBeds().Allowed(block.getLocation(),bedEvent.getPlayer(),false);
		
		if(resultdata== ClaimAllowanceConstants.Allow_Forced) return;
		//if the bed is in a claim
		else if(resultdata.Denied()){
			
		        Claim grabclaim = GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(), true, null);
				bedEvent.setCancelled(true);
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoBedPermission, grabclaim.getOwnerName());
			
		}		
	}
	
	//block use of buckets within other players' claims
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerBucketEmpty (PlayerBucketEmptyEvent bucketEvent)
	{
		Player player = bucketEvent.getPlayer();
		Block block = bucketEvent.getBlockClicked().getRelative(bucketEvent.getBlockFace());
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(block.getWorld());
		int minLavaDistance = 10;
		
		if(bucketEvent.getBucket()==Material.LAVA_BUCKET){
			switch(wc.getLavaBucketEmptyBehaviour().Allowed(block.getLocation(), player)) 
			 {
			 case Allow_Forced:
			     return; //force allow.
			 case Deny_Forced:
			     bucketEvent.setCancelled(true);
			     return;
			 default: 
				 //nothin.
			}
		}
		else if(bucketEvent.getBucket()==Material.WATER_BUCKET){
			 switch(wc.getWaterBucketEmptyBehaviour().Allowed(block.getLocation(), player,false)) 
			 {
			 case Allow_Forced:
			     return; //force allow.
			 case Deny_Forced:
			     bucketEvent.setCancelled(true);
			     return;
			 default: 
				 //nothin.
			}
		}
		
		//make sure the player is allowed to build at the location
		String noBuildReason = GriefPrevention.instance.allowBuild(player, block.getLocation());
		if(noBuildReason != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
			bucketEvent.setCancelled(true);
			return;
		}
		
		//if the bucket is being used in a claim, allow for dumping lava closer to other players
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, playerData.lastClaim);

		//checks for Behaviour perms.
		if(bucketEvent.getBucket() == Material.LAVA_BUCKET){
			if(wc.getLavaBucketEmptyBehaviour().Allowed(block.getLocation(),player).Denied()){
				//GriefPrevention.sendMessage(player, TextMode.Err, Messages.ConfigDisabled,"Lava placement ");
				bucketEvent.setCancelled(true);
				return;
			}
		}
		else if(bucketEvent.getBucket() == Material.WATER_BUCKET){
			if(wc.getWaterBucketEmptyBehaviour().Allowed(block.getLocation(),player).Denied()){
				//GriefPrevention.sendMessage(player, TextMode.Err, Messages.ConfigDisabled,"Water placement ");
				bucketEvent.setCancelled(true);
				return;
			}
			
		}
		
		
		
		if(claim != null)
		{
			
			minLavaDistance = 3;
		}
		//otherwise no wilderness dumping (unless underground) in worlds where claims are enabled
		else if(wc.getClaimsEnabled()) //outside claims logic...
		{
			
				if(bucketEvent.getBucket() == Material.LAVA_BUCKET)
				{
					if(wc.getLavaBucketEmptyBehaviour().Allowed(block.getLocation(), player).Denied()){
						
						bucketEvent.setCancelled(true);
						return;
					}
				}
			 if(bucketEvent.getBucket() == Material.WATER_BUCKET){
				
				if(wc.getWaterBucketEmptyBehaviour().Allowed(block.getLocation(), player).Denied()){
					
					bucketEvent.setCancelled(true);
					return;
				}
			
			
			 }
		}
		
		//lava buckets can't be dumped near other players unless pvp is on
		if(!block.getWorld().getPVP() && !player.hasPermission("griefprevention.lava"))
		{
			if(bucketEvent.getBucket() == Material.LAVA_BUCKET)
			{
				
				if(wc.getLavaBucketEmptyBehaviour().Allowed(block.getLocation(), player).Denied()){
					//GriefPrevention.sendMessage(player,TextMode.Err,Messages.ConfigDisabled,"Lava Placement");
					bucketEvent.setCancelled(true);
					return;
				}
				
				List<Player> players = block.getWorld().getPlayers();
				for(int i = 0; i < players.size(); i++)
				{
					Player otherPlayer = players.get(i);
					Location location = otherPlayer.getLocation();
					if(!otherPlayer.equals(player) && block.getY() >= location.getBlockY() - 1 && location.distanceSquared(block.getLocation()) < minLavaDistance * minLavaDistance)
					{
						GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoLavaNearOtherPlayer, otherPlayer.getName());
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
	{try {
		Player player = bucketEvent.getPlayer();
		Block block = bucketEvent.getBlockClicked();
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(block.getWorld());
		//make sure the player is allowed to build at the location
		//String noBuildReason = GriefPrevention.instance.allowBuild(player, block.getLocation());
		
		ClaimBehaviourData cbm = wc.getWaterBucketFillBehaviour();
		if(block.getType()==Material.LAVA){
			cbm = wc.getLavaBucketFillBehaviour();
		}
		
		
		if(cbm.Allowed(block.getLocation(), player).Denied())
		{
			//GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
			bucketEvent.setCancelled(true);
			return;
		}
	}
	finally {
		if(bucketEvent.isCancelled()){
			final PlayerBucketFillEvent cloned = bucketEvent; 
			Bukkit.getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance,new Runnable() {
			
			public void run(){
			
			cloned.getPlayer().sendBlockChange(cloned.getBlockClicked().getLocation(),
					cloned.getBlockClicked().getType(), cloned.getBlockClicked().getData());
			}}
			,0);
		}
	}
	}
	private static HashSet<Byte> transparentMaterials = null;
	
	private static HashSet<Material> GPTools = null;
	private static HashSet<Material> ContainerMaterials = null;
	private void getTransparentMaterials(){
		if(transparentMaterials==null){
			transparentMaterials = new HashSet<Byte>();
			transparentMaterials.add(Byte.valueOf((byte)Material.AIR.getId()));
			transparentMaterials.add(Byte.valueOf((byte)Material.SNOW.getId()));
			transparentMaterials.add(Byte.valueOf((byte)Material.LONG_GRASS.getId()));
			transparentMaterials.add(Byte.valueOf((byte)Material.WOOD_BUTTON.getId()));
			transparentMaterials.add(Byte.valueOf((byte)Material.STONE_BUTTON.getId()));
			transparentMaterials.add(Byte.valueOf((byte)Material.STONE_PLATE.getId()));
			transparentMaterials.add(Byte.valueOf((byte)Material.WOOD_PLATE.getId()));
			transparentMaterials.add(Byte.valueOf((byte)Material.IRON_PLATE.getId()));
			transparentMaterials.add(Byte.valueOf((byte)Material.GOLD_PLATE.getId()));
			transparentMaterials.add(Byte.valueOf((byte)Material.LEVER.getId()));
		}
	}
	//when a player interacts with the world
	@EventHandler(priority = EventPriority.LOWEST)
	void onPlayerInteract(PlayerInteractEvent event)
	{
		
		if(event==null) return;
		if(event.getPlayer()==null) return; //MCPC seems to sometimes fire events with a null player...
		Player player = event.getPlayer();
		String ItemName = event.getItem()==null?"null":"ID:" + String.valueOf(event.getItem().getTypeId());
		Debugger.Write("onPlayerInteract: Item:" + ItemName,DebugLevel.Verbose);
		
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		//determine target block.  FEATURE: shovel and stick can be used from a distance away
		Block clickedBlock = event.getClickedBlock();
		
		if(wc.getItemRules()!=null){
			getTransparentMaterials();
			 try {
			clickedBlock = player.getTargetBlock(transparentMaterials, 2500);
			 }
			 catch(Exception exx){
				 clickedBlock=null;
			 }
			
			 for(ItemUsageRules iur:wc.getItemRules()){
				 if(iur.Applicable(event.getItem())){
					 if(iur.TestPlayer(player, clickedBlock).Denied()){
						 event.setCancelled(true);
						 return;
						 
					 }
					 
					 
				 }
			 }
			 
			
		}
		
		
		//this block shows some debug info, but only when DebuggingLevel is set to verbose.
		if(GriefPrevention.instance.DebuggingLevel==DebugLevel.Verbose){
			StringBuffer sb = new StringBuffer();
			sb.append("Clicked:");
			if(clickedBlock!=null)
			{
				sb.append(clickedBlock.getType().name());
				sb.append("State:" + clickedBlock.getState()==null);
				if(clickedBlock.getState()!=null)
					sb.append(" " + clickedBlock.getState().getClass().getName());
			}
			sb.append(",");
			if(event!=null){
				if(event.getItem()!=null){
					sb.append("Item:" + event.getItem().getType().name() + " ts:" + event.getItem().toString());
					
				}
			}
			Debugger.Write(sb.toString(), DebugLevel.Verbose);
		}
		
		//if null, initialize.
		if(GPTools==null){
			GPTools = new HashSet<Material>();
			GPTools.add(wc.getClaimsInvestigationTool());
			GPTools.add(wc.getClaimsModificationTool());
		}
		//get material of the item that was used...
		Material inhand = event.getItem()==null?null:event.getItem().getType();
		if(inhand!=null && GPTools.contains(inhand)){
			//if the Tools HashSet contains the item used, apply 'selection extension' logic of interacting with air.
			try
			{
				clickedBlock = event.getClickedBlock();  //null returned here means interacting with air			
				if(clickedBlock == null || clickedBlock.getType() == Material.SNOW)
				{
					//try to find a far away non-air block along line of sight
					getTransparentMaterials();
					clickedBlock = player.getTargetBlock(transparentMaterials, 250);
				}			
			}
			catch(Exception e)  //an exception intermittently comes from getTargetBlock().  when it does, just ignore the event
			{
				System.out.println("getTarget Exception");
				return;
			}
		}
		//if no block, stop here
		if(clickedBlock == null)
		{
			
			return;
		}
		
		Material clickedBlockType = clickedBlock.getType();
		
		
		
		
		//
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		//Apply rules for the leash. Leashes can be attached to fences and netherbrick fences, but require
		//permission at the block location for the player.
		
		if((clickedBlock.getType()==Material.FENCE || clickedBlock.getType()==Material.NETHER_FENCE) &&
				inhand!=null && inhand.getId() ==420){
			
			if(wc.getLeadUsageRules().Allowed(clickedBlock.getLocation(), player).Denied()){
				event.setCancelled(true);
				return;
			}
			
		}
		
		
		
		//apply rules for putting out fires (requires build permission)
		if(event.getAction()==Action.LEFT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getRelative(event.getBlockFace()).getType() == Material.FIRE)
		{
			if(!wc.getFireExtinguishing().Allowed(clickedBlock.getLocation(), player).Denied()){
				event.setCancelled(true);
				return;
			}
		
		}
		
		else if(event.getClickedBlock()!=null && event.getClickedBlock().getType()!=Material.AIR && event.getItem()!=null && 
				wc.getAdministrationTool()==event.getItem().getType()){
		
			if(player.hasPermission("GriefPrevention.Administration")){
				//if shifting, add to all worlds...
				if(event.getAction()==Action.LEFT_CLICK_BLOCK){
					if (player.isSneaking()) {
					GriefPrevention.instance.Configuration.AddContainerID(player, player.getWorld(), new MaterialInfo(event.getClickedBlock().getType()));
					event.setCancelled(true);
				} else {
					GriefPrevention.instance.Configuration.AddContainerID(player, null, new MaterialInfo(event.getClickedBlock().getType()));
					event.setCancelled(true);
				}
				
			}
			}
			
		}
		if(ContainerMaterials==null){
			ContainerMaterials = new HashSet<Material>();
			ContainerMaterials.add(Material.WORKBENCH);
			ContainerMaterials.add(Material.ENDER_CHEST);
			ContainerMaterials.add(Material.ANVIL);
			ContainerMaterials.add(Material.BREWING_STAND);
			ContainerMaterials.add(Material.ENCHANTMENT_TABLE);
			ContainerMaterials.add(Material.CAKE_BLOCK);
			ContainerMaterials.add(Material.JUKEBOX);
			ContainerMaterials.add(Material.DISPENSER);
			ContainerMaterials.add(Material.DROPPER);
			ContainerMaterials.add(Material.HOPPER);
			
		}
		//apply rules for containers and crafting blocks
		if(	(event.getAction() == Action.RIGHT_CLICK_BLOCK && 
				(clickedBlock.getState() instanceof InventoryHolder ||
						ContainerMaterials.contains(clickedBlock.getType()) || 
						
						wc.getModsContainerTrustIds().contains(new MaterialInfo(clickedBlock.getTypeId(), clickedBlock.getData(), null)))
						))
		{		
			
			//block container access when they cannot see it.
			/*if(!(clickedBlock==player.getTargetBlock(null, 100))){
				event.setCancelled(true);
				GriefPrevention.AddLogEntry("Cancelled non-visible Target container access.");
			}*/
			
			//block container use while under siege, so players can't hide items from attackers
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoContainers);
				event.setCancelled(true);
				return;
			}
			
			//special Chest looting behaviour.
			Claim cc = this.dataStore.getClaimAt(clickedBlock.getLocation(),true,null);
			//if doorsOpen...
			if(cc!=null && cc.doorsOpen){
				if((cc.LootedChests++)<=wc.getSeigeLootChests()){
					//tell the player how many more chests they can loot.
					player.sendMessage(ChatColor.YELLOW + " You may loot " + (wc.getSeigeLootChests()-cc.LootedChests) + " more chests");
					
					
					return;
				}
				
				
				
			}
			
			
			//block container use during pvp combat, same reason
			if(playerData.inPvpCombat() && wc.getPvPBlockContainers())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PvPNoContainers);
				event.setCancelled(true);
				return;
			}
			
			//otherwise check permissions for the claim the player is in
			if(wc.getContainersRules().Allowed(clickedBlock.getLocation(), player,true).Denied()){
					//message will be sent by the above. noContainersReason won't be used anymore
				    //this will require some thought :/
					event.setCancelled(true);
					//GriefPrevention.sendMessage(player, TextMode.Err, noContainersReason);
					return;
				}
			
			
			//if the event hasn't been cancelled, then the player is allowed to use the container
			//so drop any pvp protection
			if(playerData.pvpImmune)
			{
				playerData.pvpImmune = false;
				GriefPrevention.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
			}
		}
		//apply rules for applicable element.
		ClaimBehaviourData useRule = null;
		if(clickedBlockType==Material.WOODEN_DOOR){
			useRule = wc.getWoodenDoors();
		}else if(clickedBlockType==Material.TRAP_DOOR){
			useRule = wc.getTrapDoors();
		}else if(clickedBlockType==Material.FENCE_GATE){
			useRule = wc.getFenceGates();
		}else if(clickedBlockType==Material.STONE_BUTTON){
			useRule = wc.getStoneButton();
		}else if(clickedBlockType ==Material.WOOD_BUTTON){
			useRule = wc.getWoodenButton();
		}else if(clickedBlockType == Material.WOOD_PLATE){
			useRule = wc.getWoodPressurePlates();
		}else if(clickedBlockType ==Material.STONE_PLATE){
			useRule = wc.getStonePressurePlates();
		} else if(clickedBlockType == Material.LEVER){
			useRule = wc.getLevers();
		}
		
		
		if(useRule!=null){
			
			//stone and wood pressure plates must exceed the timeout to show a message,
			boolean doshowmessage= true;
			Calendar fiveseccal= Calendar.getInstance();
			fiveseccal.add(Calendar.SECOND,-1);
			Date laststepped = fiveseccal.getTime();
			
			if(useRule==wc.getWoodPressurePlates() || useRule==wc.getStonePressurePlates()){
				Date prevstepped = playerData.getLastSteppedOn(clickedBlock.getLocation());
				doshowmessage = prevstepped==null || prevstepped.before(laststepped);
			}
			//System.out.println("doshowmessage=" + doshowmessage);
			playerData.setLastSteppedOn(clickedBlock.getLocation());
			
			if(useRule.Allowed(clickedBlock.getLocation(), player,doshowmessage).Denied())
			{
				event.setCancelled(true);
				return;
			}
			
			
			
		}
		/*//older code here for now...
		//otherwise apply rules for doors, if configured that way
		else if((wc.getClaimsLockWoodenDoors() && clickedBlockType == Material.WOODEN_DOOR) ||
				(wc.getClaimsLockTrapDoors() && clickedBlockType == Material.TRAP_DOOR) ||
				(wc.getClaimsLockFenceGates() && clickedBlockType == Material.FENCE_GATE))
		{
			Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
			if(claim != null)
			{
				playerData.lastClaim = claim;
				
				String noAccessReason = claim.allowAccess(player);
				if(noAccessReason != null)
				{
					
					event.setCancelled(true);
					GriefPrevention.sendMessage(player, TextMode.Err, noAccessReason);
					return;
				}
			}	
		}
		
		//otherwise apply rules for buttons and switches
		else if(wc.getClaimsPreventButtonsSwitches() && (clickedBlockType == null || clickedBlockType == Material.STONE_BUTTON || clickedBlockType == Material.WOOD_BUTTON || clickedBlockType == Material.LEVER || wc.getModsAccessTrustIds().Contains(new MaterialInfo(clickedBlock.getTypeId(), clickedBlock.getData(), null))))
		{
			
			Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
			if(claim != null)
			{
				playerData.lastClaim = claim;
				
				String noAccessReason = claim.allowAccess(player);
				if(noAccessReason != null)
				{
					
					event.setCancelled(true);
					GriefPrevention.sendMessage(player, TextMode.Err, noAccessReason);
					return;
				}
			}			
		}
		*/
		//apply rule for players trampling tilled soil back to dirt (never allow it)
		//NOTE: that this event applies only to players.  monsters and animals can still trample.
		else if(event.getAction() == Action.PHYSICAL && clickedBlockType == Material.SOIL)
		{
			ClaimBehaviourData.ClaimAllowanceConstants trampleresult = wc.getPlayerTrampleRules().Allowed(event.getPlayer().getLocation(),event.getPlayer());
			if(trampleresult==ClaimAllowanceConstants.Allow_Forced) return; //Force it to be allowed.
			if(trampleresult.Denied()){
			event.setCancelled(true);
			return;
			}
		}
		
		//apply rule for note blocks and repeaters
		else if(clickedBlockType == Material.NOTE_BLOCK || 
				clickedBlockType == Material.DIODE_BLOCK_ON || 
				clickedBlockType == Material.DIODE_BLOCK_OFF ||
				clickedBlockType == Material.REDSTONE_COMPARATOR_OFF ||
				clickedBlockType == Material.REDSTONE_COMPARATOR_ON)
		{
			ClaimAllowanceConstants tweakallow = wc.getBlockTweakRules().Allowed(clickedBlock.getLocation(), event.getPlayer());
			if(tweakallow == ClaimAllowanceConstants.Allow_Forced) return;
			else if(tweakallow.Denied()){
				event.setCancelled(true);
				return;
			}
		}
		
		//otherwise handle right click (shovel, stick, bonemeal)
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
				if(wc.getBonemealGrassRules().Allowed(event.getClickedBlock().getLocation(), event.getPlayer()).Denied()){
				    event.setCancelled(true);
				}
				return;
			}
			
			else if(materialInHand ==  Material.BOAT)
			{
				Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
				if(claim != null)
				{
					String noAccessReason = claim.allowAccess(player);
					if(noAccessReason != null)
					{
						GriefPrevention.sendMessage(player, TextMode.Err, noAccessReason);
						event.setCancelled(true);
					}
				}
				
				return;
			}
			
			//if it's a spawn egg, minecart, or boat, and this is a creative world, apply special rules
			else if((materialInHand == Material.MONSTER_EGG || materialInHand == Material.MINECART || materialInHand == Material.POWERED_MINECART || materialInHand == Material.STORAGE_MINECART
					|| materialInHand == Material.HOPPER_MINECART || materialInHand == Material.EXPLOSIVE_MINECART || materialInHand == Material.BOAT) && GriefPrevention.instance.creativeRulesApply(clickedBlock.getLocation()))
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
			
			//if he's investigating a claim			
			else if(materialInHand == wc.getClaimsInvestigationTool())
			{
				
				
				//air indicates too far away
				if(clickedBlockType == Material.AIR)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.TooFarAway);
					return;
				}
				
				//if shift is pressed, show Surrounding Claims and break.
				
				Claim[] claimsshow = null;
				if(player.isSneaking() && player.hasPermission("GriefPrevention.InvestigateArea")){
				//initialize to all claims in the given radius of the location.
					int useradius = wc.getConfigShowSurroundingsRadius();
					//by radius we mean size.
					//initialize the starting and ending X,Z locations to search
					int StartX = player.getLocation().getBlockX() - useradius;
					int EndX = player.getLocation().getBlockX() + useradius;
					int StartZ = player.getLocation().getBlockZ() - useradius;
					int EndZ = player.getLocation().getBlockZ() + useradius;
					Set<Claim> buildlist = new HashSet<Claim>();
					for (int X=StartX;X<EndX;X++){
						for(int Z=StartZ;Z<EndZ;Z++){
							
							Claim grabclaim = dataStore.getClaimAt(new Location(player.getWorld(),X,clickedBlock.getLocation().getBlockY(),Z),false,null);
							if(grabclaim!=null && !buildlist.contains(grabclaim))
							{
								buildlist.add(grabclaim);
							}
							
							
						}
					}
					claimsshow = new Claim[buildlist.size()];
					buildlist.toArray(claimsshow);
					
					
					
				}
				else {
				claimsshow = new Claim[]{this.dataStore.getClaimAt(clickedBlock.getLocation(), false /*ignore height*/, playerData.lastClaim)};
				}
				//no claim case
				if(claimsshow == null || claimsshow.length==0 || claimsshow[0]==null)
				{
					Visualization.Revert(player);
					GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockNotClaimed);
					
				}
				
				//claim case
				else
				{
					if( claimsshow.length==1) Visualization.Revert(player);
					else {
						//Showing X Claims within X Blocks of (X,Z).
						String messagedisplay = "Showing " + claimsshow.length + " Claims within " + 
						wc.getConfigShowSurroundingsRadius() + " blocks of " + GriefPrevention.getfriendlyLocationString(clickedBlock.getLocation())	;
						
						//get the unique owners of the claims.
						Set<String> Owners = new HashSet<String>();
						for(Claim investigate:claimsshow){
							if(!Owners.contains(investigate.getOwnerName()))
								Owners.add(investigate.getOwnerName());
						}
						StringBuffer buildnames = new StringBuffer();
						for(String loopname:Owners){
							buildnames.append(loopname).append(",");
						}
						String shownames = buildnames.toString().substring(0,buildnames.length()-1);
						
						
						
						GriefPrevention.sendMessage(player, TextMode.Info, messagedisplay);
						GriefPrevention.sendMessage(player, TextMode.Instr, "Owned by players:" + shownames);
						
						
					}
					Visualization.Revert(player);
					
					
					for(Claim claim:claimsshow){
						playerData.lastClaim = claim;
						
						
						//visualize boundary
						Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());
						//clear visualized claims if there is only one item.
						Visualization.Apply(player, visualization,claimsshow.length==1);
						if(claimsshow.length==1){ //only show specific claim information when looking at a single claim
							//if can resize this claim, tell about the boundaries
							GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockClaimed, claim.getOwnerName());
							if(claim.allowEdit(player) == null)
							{
								GriefPrevention.sendMessage(player, TextMode.Info, "  " + claim.getWidth() + "x" + claim.getHeight() + "=" + claim.getArea());
							}
							
							//if deleteclaims permission, tell about the player's offline time
							if(!claim.isAdminClaim() && player.hasPermission("griefprevention.deleteclaims"))
							{
								PlayerData otherPlayerData = this.dataStore.getPlayerData(claim.getOwnerName());
								Date lastLogin = otherPlayerData.lastLogin;
								Date now = new Date();
								long daysElapsed = (now.getTime() - lastLogin.getTime()) / (1000 * 60 * 60 * 24); 
								
								GriefPrevention.sendMessage(player, TextMode.Info, Messages.PlayerOfflineTime, String.valueOf(daysElapsed));
								
								//drop the data we just loaded, if the player isn't online
								if(GriefPrevention.instance.getServer().getPlayerExact(claim.getOwnerName()) == null)
									this.dataStore.clearCachedPlayerData(claim.getOwnerName());
							}
						}
					}
				}
				
				return;
			}
			
			//if it's a golden shovel
			else if(materialInHand != wc.getClaimsModificationTool()) return;
			
			//disable golden shovel while under siege
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoShovel);
				event.setCancelled(true);
				return;
			}
			
			//can't use the shovel from too far away
			if(clickedBlockType == Material.AIR)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.TooFarAway);
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
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.BlockClaimed, claim.getOwnerName());
					Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());
					Visualization.Apply(player, visualization);
					
					return;
				}
				
				//figure out which chunk to repair
				Chunk chunk = player.getWorld().getChunkAt(clickedBlock.getLocation());
				
				//start the repair process
				
				//set boundaries for processing
				int miny = clickedBlock.getY();
				
				//if not in aggressive mode, extend the selection down to a little below sea level
				if(!(playerData.shovelMode == ShovelMode.RestoreNatureAggressive))
				{
					if(miny > GriefPrevention.instance.getSeaLevel(chunk.getWorld()) - 10)
					{
						miny = GriefPrevention.instance.getSeaLevel(chunk.getWorld()) - 10;
					}
				}
				
				GriefPrevention.instance.restoreChunk(chunk, miny, playerData.shovelMode == ShovelMode.RestoreNatureAggressive, 0, player);
				
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
					allowedFillBlocks.add(Material.GRASS);
					allowedFillBlocks.add(Material.DIRT);
					allowedFillBlocks.add(Material.STONE);
					allowedFillBlocks.add(Material.SAND);
					allowedFillBlocks.add(Material.SANDSTONE);
					allowedFillBlocks.add(Material.ICE);
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
						
						//default fill block is initially the first from the allowed fill blocks list above
						Material defaultFiller = allowedFillBlocks.get(0);
						
						//prefer to use the block the player clicked on, if it's an acceptable fill block
						if(allowedFillBlocks.contains(centerBlock.getType()))
						{
							defaultFiller = centerBlock.getType();
						}
						
						//if the player clicks on water, try to sink through the water to find something underneath that's useful for a filler
						else if(centerBlock.getType() == Material.WATER || centerBlock.getType() == Material.STATIONARY_WATER)
						{
							Block block = centerBlock.getWorld().getBlockAt(centerBlock.getLocation());
							while(!allowedFillBlocks.contains(block.getType()) && block.getY() > centerBlock.getY() - 10)
							{
								block = block.getRelative(BlockFace.DOWN);
							}
							if(allowedFillBlocks.contains(block.getType()))
							{
								defaultFiller = block.getType();
							}
						}
						
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
							
							//only replace air, spilling water, snow, long grass
							if(block.getType() == Material.AIR || block.getType() == Material.SNOW || (block.getType() == Material.STATIONARY_WATER && block.getData() != 0) || block.getType() == Material.LONG_GRASS)
							{							
								//if the top level, always use the default filler picked above
								if(y == maxHeight)
								{
									block.setType(defaultFiller);
								}
								
								//otherwise look to neighbors for an appropriate fill block
								else
								{
									Block eastBlock = block.getRelative(BlockFace.EAST);
									Block westBlock = block.getRelative(BlockFace.WEST);
									Block northBlock = block.getRelative(BlockFace.NORTH);
									Block southBlock = block.getRelative(BlockFace.SOUTH);
									
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
									
									//if all else fails, use the default filler selected above
									else
									{
										block.setType(defaultFiller);
									}
								}
							}
						}
					}
				}
				
				return;
			}
			
			//if the player doesn't have claims permission, don't do anything
			if(wc.getCreateClaimRequiresPermission() && !player.hasPermission("griefprevention.createclaims"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoCreateClaimPermission);
				return;
			}
			if(playerData.claimResizing==null){
				//see if the player has clicked inside one of their claims.
				Claim checkclaim = GriefPrevention.instance.dataStore.getClaimAt(clickedBlock.getLocation(),true, null);
				//is there even a claim here?
				if(checkclaim!=null){
					//there is a claim; make sure it belongs to this player.
					String cannotedit=checkclaim.allowEdit(player);
					if(cannotedit==null && GriefPrevention.instance.config_autosubclaims){
						//it DOES belong to them.
						//automatically switch to advanced claims mode, and show a message.
						playerData.claimSubdividing = checkclaim;
						playerData.shovelMode=ShovelMode.Subdivide;
						//TODO: Raise StartClaimSubdivideEvent
						GriefPrevention.sendMessage(player, TextMode.Info, "Entering Claim subdivide mode.");
						return;
					} else if(cannotedit!=null){
						GriefPrevention.sendMessage(player,TextMode.Info,cannotedit);
						return;
						//do nothing.
					}
					else {
						
						GriefPrevention.sendMessage(player, TextMode.Instr,Messages.SubdivisionDemo);
					}
				}
				
				
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
				newy2 = clickedBlock.getY() - wc.getClaimsExtendIntoGroundDistance();
				
				//for top level claims, apply size rules and claim blocks requirement
				if(playerData.claimResizing.parent == null)
				{				
					//measure new claim, apply size rules
					int newWidth = (Math.abs(newx1 - newx2) + 1);
					int newHeight = (Math.abs(newz1 - newz2) + 1);
							
					if(!playerData.claimResizing.isAdminClaim() && (newWidth < wc.getMinClaimSize() || newHeight < wc.getMinClaimSize()))
					{
						GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeClaimTooSmall, String.valueOf(wc.getMinClaimSize()));
						return;
					}
					
					//make sure player has enough blocks to make up the difference
					if(!playerData.claimResizing.isAdminClaim() && player.getName().equals(playerData.claimResizing.getOwnerName()))
					{
						int newArea =  newWidth * newHeight;
						int blocksRemainingAfter = playerData.getRemainingClaimBlocks() + playerData.claimResizing.getArea() - newArea;
						
						if(blocksRemainingAfter < 0)
						{
							GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeNeedMoreBlocks, String.valueOf(Math.abs(blocksRemainingAfter)));
							return;
						}
						else if(wc.getClaims_maxBlocks()>0){ //or if there is a maximum block setting on the world...
							int worldblocksafter = playerData.getTotalClaimBlocksinWorld(player.getWorld())+playerData.claimResizing.getArea() - newArea;
							if(worldblocksafter < 0){
								GriefPrevention.sendMessage(player, TextMode.Err, Messages.InsufficientWorldBlocks, String.valueOf(Math.abs(worldblocksafter)));
								return;
							}
						}
					}
				}
				
				//special rules for making a top-level claim smaller.  to check this, verifying the old claim's corners are inside the new claim's boundaries.
				//rule1: in creative mode, top-level claims can't be moved or resized smaller.
				//rule2: in any mode, shrinking a claim removes any surface fluids
				Claim oldClaim = playerData.claimResizing;
				boolean smaller = false;
				if(oldClaim.parent == null)
				{
					//temporary claim instance, just for checking contains()
					Claim newClaim = new Claim(
							new Location(oldClaim.getLesserBoundaryCorner().getWorld(), newx1, newy1, newz1), 
							new Location(oldClaim.getLesserBoundaryCorner().getWorld(), newx2, newy2, newz2),
							"", new String[]{}, new String[]{}, new String[]{}, new String[]{}, null, false);
					
					//if the new claim is smaller
					if(!newClaim.contains(oldClaim.getLesserBoundaryCorner(), true, false) || !newClaim.contains(oldClaim.getGreaterBoundaryCorner(), true, false))
					{
						smaller = true;
						
						//enforce creative mode rule
						if(!wc.getAllowUnclaim())
						{
							GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoCreativeUnClaim);
							return;
						}
						
						//remove surface fluids about to be unclaimed
						oldClaim.removeSurfaceFluids(newClaim);
					}
				}
				
				//ask the datastore to try and resize the claim, this checks for conflicts with other claims
				CreateClaimResult result = GriefPrevention.instance.dataStore.resizeClaim(playerData.claimResizing, newx1, newx2, newy1, newy2, newz1, newz2,player);
				
				if(result.succeeded == CreateClaimResult.Result.Success)
				{
					
					//inform and show the player
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClaimResizeSuccess, String.valueOf(playerData.getRemainingClaimBlocks()));
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());
					Visualization.Apply(player, visualization);
					
					//if resizing someone else's claim, make a log entry
					if(!playerData.claimResizing.ownerName.equals(playerName))
					{
						GriefPrevention.AddLogEntry(playerName + " resized " + playerData.claimResizing.getOwnerName() + "'s claim at " + GriefPrevention.getfriendlyLocationString(playerData.claimResizing.lesserBoundaryCorner) + ".");
					}
					
					//if in a creative mode world and shrinking an existing claim, restore any unclaimed area
					if(smaller && wc.getAutoRestoreUnclaimed() && GriefPrevention.instance.creativeRulesApply(oldClaim.getLesserBoundaryCorner()))
					{
						GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
						GriefPrevention.instance.restoreClaim(oldClaim, 20L * 60 * 2);  //2 minutes
						GriefPrevention.AddLogEntry(player.getName() + " shrank a claim @ " + GriefPrevention.getfriendlyLocationString(playerData.claimResizing.getLesserBoundaryCorner()));
					}
					
					//clean up
					playerData.claimResizing = null;
					playerData.lastShovelLocation = null;
				}else if(result.succeeded == CreateClaimResult.Result.ClaimOverlap)
				{
					//inform player
					if(playerData.claimResizing.parent!=null && playerData.claimResizing.parent == result.claim){
					    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeFailOutsideParent);
					}
					else {
					    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlap);
					}
					
					//show the player the conflicting claim
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());
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
						//TODO: Raise ClaimResizeBegin Event here
						GriefPrevention.sendMessage(player, TextMode.Instr, Messages.ResizeStart);
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
								GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlapSubdivision);							
							}
						
							//otherwise start a new subdivision
							else
							{
								//RaiseCreateSubdivisionStart
								GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionStart);
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
									playerData.lastShovelLocation.getBlockY() - wc.getClaimsExtendIntoGroundDistance(), clickedBlock.getY() - wc.getClaimsExtendIntoGroundDistance(), 
									playerData.lastShovelLocation.getBlockZ(), clickedBlock.getZ(), 
									"--subdivision--",  //owner name is not used for subdivisions
									playerData.claimSubdividing,
									null, false,player,true);
							
							//if it didn't succeed, tell the player why
							if(result.succeeded == CreateClaimResult.Result.ClaimOverlap)
							{
								GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateSubdivisionOverlap);
																				
								Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());
								Visualization.Apply(player, visualization);
								
								return;
							}else if(result.succeeded == CreateClaimResult.Result.Canceled) {
								//It was canceled by a plugin, just return, as the plugin should put out a 
								//custom error message.
								return;
							}
							
							//otherwise, advise him on the /trust command and show him his new subdivision
							else
							{					
								GriefPrevention.sendMessage(player, TextMode.Success, Messages.SubdivisionSuccess);
								Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());
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
						GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlap);
						Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());
						Visualization.Apply(player, visualization);
					}
				}
				
				//otherwise tell the player he can't claim here because it's someone else's claim, and show him the claim
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapOtherPlayer, claim.getOwnerName());
					Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());
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
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimsDisabledWorld);
					return;
				}
				else if(wc.getClaimsPerPlayerLimit() > 0 &&
					!(player.hasPermission("griefprevention.ignoreclaimslimit") )){
					
					//get the number of claims the player has in this world.
					if(wc.getClaimsPerPlayerLimit() <= playerData.getWorldClaims(clickedBlock.getWorld()).size()){
					
					
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerClaimLimit,String.valueOf(wc.getClaimsPerPlayerLimit()));
					return;
					}
				}
					
				
				
				//remember it, and start him on the new claim
				playerData.lastShovelLocation = clickedBlock.getLocation();
				GriefPrevention.sendMessage(player, TextMode.Instr, Messages.ClaimStart);
				//TODO: raise ClaimCreateStartEvent
				//show him where he's working
				Visualization visualization = Visualization.FromClaim(new Claim(clickedBlock.getLocation(), clickedBlock.getLocation(), "", new String[]{}, new String[]{}, new String[]{}, new String[]{}, null, false), clickedBlock.getY(), VisualizationType.RestoreNature, player.getLocation());
				Visualization.Apply(player, visualization);
				
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
				
				if(playerData.shovelMode != ShovelMode.Admin && (newClaimWidth < wc.getMinClaimSize() || newClaimHeight < wc.getMinClaimSize()))
				{
					
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.NewClaimTooSmall, String.valueOf(wc.getMinClaimSize()));
					return;
				}
				
				//if not an administrative claim, verify the player has enough claim blocks for this new claim
				if(playerData.shovelMode != ShovelMode.Admin)
				{					
					int newClaimArea = newClaimWidth * newClaimHeight;
					
					if(wc.getClaims_maxBlocks()>0){
					
						int currentworldclaim = playerData.getTotalClaimBlocksinWorld(player.getWorld());
						int diff = currentworldclaim+newClaimArea-wc.getClaims_maxBlocks();
						if(diff <0){
							GriefPrevention.sendMessage(player,TextMode.Err,Messages.InsufficientWorldBlocks,Math.abs(diff));
						}
						
						
					}
					
					int remainingBlocks = playerData.getRemainingClaimBlocks();
					if(newClaimArea > remainingBlocks)
					{
						
						
						 
						if(player.isSneaking() &&  newClaimArea-remainingBlocks>wc.getInsufficientSneakResetBound() && wc.getInsufficientSneakResetBound() > 0){
							GriefPrevention.sendMessage(player, TextMode.Instr, "First Point Abandoned!");
							playerData.lastShovelLocation= null;
							Visualization.Revert(player);
						}
						else
						{
						GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimInsufficientBlocks, String.valueOf(newClaimArea - remainingBlocks));
						GriefPrevention.sendMessage(player, TextMode.Instr, Messages.AbandonClaimAdvertisement);
						}
						
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
						lastShovelLocation.getBlockY() - wc.getClaimsExtendIntoGroundDistance(), clickedBlock.getY() - wc.getClaimsExtendIntoGroundDistance(), 
						lastShovelLocation.getBlockZ(), clickedBlock.getZ(), 
						playerName,
						null, null, false,player,true);
				
				//if it didn't succeed, tell the player why
				if(result.succeeded == CreateClaimResult.Result.ClaimOverlap)
				{
					//if the claim it overlaps is owned by the player...
					
					if(result.claim.getOwnerName().equalsIgnoreCase(playerName)){
						//owned by the player.
						//make sure our larger claim entirely contains
						//the smaller one.
						
						if((Claim.Contains(lastShovelLocation, clickedBlock.getLocation(), result.claim.getLesserBoundaryCorner(), true) &&
								(Claim.Contains(lastShovelLocation, clickedBlock.getLocation(), result.claim.getGreaterBoundaryCorner(), true)))){
						//Claim tempclaim = new Claim();
						//tempclaim.lesserBoundaryCorner=lastShovelLocation;
						//tempclaim.greaterBoundaryCorner=clickedBlock.getLocation();
						
							//it contains it
							//resize the other claim
							
								result.claim.setLocation(lastShovelLocation, clickedBlock.getLocation());
							
								//msg, and show visualization.
								GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClaimResizeSuccess,String.valueOf(playerData.getRemainingClaimBlocks()));
								//show message if world-based claim block limit is in effect for this world.
								if(wc.getClaims_maxBlocks()>0){
									int worldblocksremaining = playerData.getRemainingClaimBlocks(player.getWorld());
									GriefPrevention.sendMessage(player, TextMode.Success, Messages.RemainingBlocksWorld,worldblocksremaining);
								}
								
								
								Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());
								Visualization.Apply(player, visualization);
								return;
							
						}
						
						
						
					}
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapShort);
					
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());
					Visualization.Apply(player, visualization);
					
					return;
				}else if(result.succeeded == CreateClaimResult.Result.Canceled) {
					//A plugin canceled the event.
					return;
				}
				
				//otherwise, advise him on the /trust command and show him his new claim
				else
				{					
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.CreateClaimSuccess);
					
					if(wc.getClaims_maxBlocks()>0){
						int worldblocksremaining = playerData.getRemainingClaimBlocks(player.getWorld());
						GriefPrevention.sendMessage(player, TextMode.Success, Messages.RemainingBlocksWorld,worldblocksremaining);
					}
					//if there is also a claim # limit, show the number they can still make.
					if(wc.getClaimsPerPlayerLimit()>0 && !player.hasPermission("griefprevention.ignoreclaimslimit")){
						int numclaims = playerData.getWorldClaims(player.getWorld()).size();
						int remaining = wc.getClaimsPerPlayerLimit()-numclaims;
						System.out.println("Sending notification that Player has " + remaining + " Claims left in this world.");
						GriefPrevention.sendMessage(player, TextMode.Success, Messages.RemainingClaimsWorld,String.valueOf(remaining));
						
					}
					
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());
					Visualization.Apply(player, visualization);
					playerData.lastShovelLocation = null;
				}
			}
		}
	}	
}
