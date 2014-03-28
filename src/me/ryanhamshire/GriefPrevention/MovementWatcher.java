package me.ryanhamshire.GriefPrevention;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import me.ryanhamshire.GriefPrevention.events.ClaimEnterEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimExitEvent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementWatcher implements Listener {

	private class LastMoveData {
		private boolean Forced = false;
		private Claim LastClaim;
		private long lasttick;
		private Player player;

		public LastMoveData(Claim cClaim, Player pPlayer, int pTick, boolean pForced) {
			player = pPlayer;
			lasttick = pTick;
			LastClaim = cClaim;
			Forced = pForced;
		}

		public boolean getForced() {
			return Forced;
		}

		public Claim getLastClaim() {
			return LastClaim;
		}

		public long getLastTick() {
			return lasttick;
		}

		public Player getPlayer() {
			return player;
		}

		public void setLastClaim(Claim pValue) {
			LastClaim = pValue;
		}

		public void setLastTick() {
			lasttick = new Date().getTime();
		}

		public void setLastTick(long newvalue) {
			lasttick = newvalue;
		}
	}

	private static final int CallCountMod = 10;
	private static final int mscalldiff = 750; // called for different players
												// 4000 ms apart.
	private int CallCount = 0;
	java.util.concurrent.ConcurrentHashMap<String, LastMoveData> MoveData = new ConcurrentHashMap<String, LastMoveData>();

	public MovementWatcher() {
		GriefPrevention.AddLogEntry("MovementWatcher Created.");
	}

	@EventHandler
	public void PlayerJoin(PlayerJoinEvent event) {
		// if present in hashmap remove it.
		if (MoveData.containsKey(event.getPlayer().getName())) {
			MoveData.remove(event.getPlayer().getName());
		}
	}

	@EventHandler
	public void PlayerMove(PlayerMoveEvent pme) {
		// we only handle every 5 actual PlayerMove Events.
		if (CallCount++ > CallCountMod) {
			LastMoveData Acquired = null;
			CallCount = 0;
			boolean doHandle = false;
			// grab the data for this player from the hashMap.
			String pName = pme.getPlayer().getName();
			if (!MoveData.containsKey(pName)) {
				// if not present, add it in and force it to be handled.
				doHandle = true;
				MoveData.put(pName, Acquired = (new LastMoveData(null, pme.getPlayer(), 0, true)));
			} else {
				// grab the item from the HashMap and make the appropriate
				// comparisons.
				// we only want to handle the events every 4 seconds or so for a
				// given player, to reduce load.
				Acquired = MoveData.get(pName);
				long currtick = new Date().getTime();
				long tickdiff = currtick - Acquired.getLastTick();
				if (tickdiff > mscalldiff) {
					doHandle = true;
				}
			}

			if (doHandle) {

				// get the player's current location.
				Claim CurrClaim = GriefPrevention.instance.dataStore.getClaimAt(pme.getTo(), true);
				// if forced to handle it or CurrClaim is different from the
				// previous one...
				if (Acquired.Forced || Acquired.LastClaim != CurrClaim) {

					Acquired.Forced = false;
					// if the previous one is not null, fire an exit event.
					if (Acquired.LastClaim != null) {
						// System.out.println("firing claim exit.");
						// if it is not null we are leaving a claim, so fire the
						// exit event.
						ClaimExitEvent cex = new ClaimExitEvent(Acquired.getLastClaim(), pme.getPlayer());
						Bukkit.getPluginManager().callEvent(cex);
					}

					// if the new claim is not null, then fire an enter event.
					if (CurrClaim != null) {
						// System.out.println("firing claim enter.");
						ClaimEnterEvent cee = new ClaimEnterEvent(CurrClaim, pme.getPlayer());
						Bukkit.getPluginManager().callEvent(cee);

					}
					// now, refresh the stored instance data.
					Acquired.setLastClaim(CurrClaim);
					Acquired.setLastTick();

				}

			}

		}

	}

}
