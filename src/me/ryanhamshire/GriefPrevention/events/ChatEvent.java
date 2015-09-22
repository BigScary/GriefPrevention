package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.ChatMode;
import org.bukkit.entity.Player;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatEvent extends Event implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();
    private final AsyncPlayerChatEvent asyncChatEvent;
    private final ChatMode chatMode;

    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    public ChatEvent(AsyncPlayerChatEvent asyncChatEvent, ChatMode chatMode)
    {
        this.asyncChatEvent = asyncChatEvent;
        this.chatMode = chatMode;
    }

    public AsyncPlayerChatEvent getAsyncChatEvent()
    {
        return this.asyncChatEvent;
    }
    
    public ChatMode getChatMode()
    {
        return this.chatMode;
    }
    
    public String getMessage()
    {
        return this.asyncChatEvent.getMessage();
    }
    
    public Player getPlayer()
    {
        return this.asyncChatEvent.getPlayer();
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }
    
    @Override
    public boolean isCancelled()
    {
        return this.asyncChatEvent.isCancelled();
    }
    
    @Override
    public void setCancelled(boolean cancelled)
    {
        this.asyncChatEvent.setCancelled(cancelled);
    }
}