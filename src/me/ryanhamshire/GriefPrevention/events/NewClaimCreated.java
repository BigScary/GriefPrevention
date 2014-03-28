package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * NewClaimCreated will be fired in the same situations as ClaimBeforeCreateEvent.
 * refer to ClaimBeforeCreateEvent for details on what is applicable and best-case usage of
 * this event.
 *
 * Deprecated because of semantic considerations. NewClaimCreated's name implicates a certain validity
 * too the Claim instance that it contains when the event is fired that is not present; for example, Claims
 * will either not have an ID or their ID will not yet actually be a valid ID used by the dataStore. As such, any actions taken
 * by the Event handler that create a claim or even resize a claim will cause problems. Again, refer to ClaimBeforeCreateEvent
 * for some of those semantic considerations.
 *
 * I've re-added this particular class for compatibility reasons.
 */
@Deprecated

public class NewClaimCreated extends ClaimEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    Claim claim;

    public NewClaimCreated(Claim claim) {
        super(claim);

    }



    @Override
    public boolean isCancelled() {

        return cancelled;
    }

    @Override
    public void setCancelled(boolean iscancelled) {
        cancelled = iscancelled;
    }
}
