package me.ryanhamshire.GriefPrevention.tasks;

import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.Debugger;
import me.ryanhamshire.GriefPrevention.Debugger.DebugLevel;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;

import org.bukkit.Chunk;
import org.bukkit.World;

public class WorldClaimCleanupTask implements Runnable {
	private String CleanupWorldName;
	private boolean flInitialized = false;
	// Claim Cleanup has/will be refactored to working
	// per world rather than globally.
	// This decision was made because the previous logic was too unpredictable.
	private int nextClaimIndex = 0;
	private int TaskCookie = 0;
	private WorldConfig wc = null;
    public int lastcleaned = 0;

	public WorldClaimCleanupTask(String WorldCleanup) {
		CleanupWorldName = WorldCleanup;
		wc = GriefPrevention.instance.getWorldCfg(WorldCleanup);
		Debugger.Write("WorldClaimCleanupTask started for world:" + WorldCleanup, DebugLevel.Verbose);
	}

	public int getTaskCookie() {
		return TaskCookie;
	}

	public void run() {

		// retrieve the claims mapped to our world.
		List<Claim> WorldClaims = GriefPrevention.instance.dataStore.getClaimArray().getWorldClaims(CleanupWorldName);
		// if the list is null or empty, we have no work to do, so break out.

		if (WorldClaims == null || WorldClaims.size() == 0)
			return;

		Debugger.Write("Claim Cleanup Running for World:" + CleanupWorldName, DebugLevel.Verbose);
		if (!flInitialized) {
			Random randomNumberGenerator = new Random();
			this.nextClaimIndex = randomNumberGenerator.nextInt(GriefPrevention.instance.dataStore.getClaimsSize());
			flInitialized = true;
		}
		nextClaimIndex = nextClaimIndex % (WorldClaims.size()) - 1;

		// decide which claim to check next
		Claim claim = WorldClaims.get(++this.nextClaimIndex);

		// skip administrative claims
		if (claim.isAdminClaim())
			return;

		if (wc == null)
			return;
		// track whether we do any important work which would require cleanup
		// afterward
		boolean cleanupChunks = false;

		// get data for the player, especially last login timestamp
		PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(claim.getOwnerName());

		// determine area of the default chest claim
		int areaOfDefaultClaim = 0;
		if (wc.getAutomaticClaimsForNewPlayerRadius() >= 0) {
			areaOfDefaultClaim = (int) Math.pow(wc.getAutomaticClaimsForNewPlayerRadius() * 2 + 1, 2);
		}

		// if he's been gone at least a week, if he has ONLY the new player
		// claim, it will be removed
		Calendar sevenDaysAgo = Calendar.getInstance();
		if (sevenDaysAgo == null)
			return;
		sevenDaysAgo.add(Calendar.DATE, -wc.getChestClaimExpirationDays());

		boolean newPlayerClaimsExpired = sevenDaysAgo.getTime().after(playerData.lastLogin);

		// if only one claim, and the player hasn't played in a week
		if (newPlayerClaimsExpired && playerData.claims.size() == 1) {

			// if that's a chest claim and those are set to expire
			if (claim.getArea() <= areaOfDefaultClaim && newPlayerClaimsExpired && wc.getChestClaimExpirationDays() > 0) {
				Debugger.Write("Deleting Chest Claim owned by " + claim.getOwnerName() + " last login:" + playerData.lastLogin.toString(), DebugLevel.Verbose);
				claim.removeSurfaceFluids(null);
				GriefPrevention.instance.dataStore.deleteClaim(claim);
                lastcleaned++;
				if (playerData.claims.size() == 0) {
					GriefPrevention.instance.dataStore.deletePlayerData(playerData.playerName);
				}

				cleanupChunks = true;

				// if configured to do so, restore the land to natural
				if (wc.getClaimsAutoNatureRestoration()) {
					GriefPrevention.instance.restoreClaim(claim, 0);
				}

				GriefPrevention.AddLogEntry(" " + claim.getOwnerName() + "'s new player claim expired.");
			}
		}

		// if configured to always remove claims after some inactivity period
		// without exceptions...
		else if (wc.getClaimsExpirationDays() > 0) {
			Calendar earliestPermissibleLastLogin = Calendar.getInstance();
			earliestPermissibleLastLogin.add(Calendar.DATE, -wc.getClaimsExpirationDays());

			if (earliestPermissibleLastLogin.getTime().after(playerData.lastLogin)) {
				// make a copy of this player's claim list
				Vector<Claim> claims = new Vector<Claim>();
				for (int i = 0; i < playerData.claims.size(); i++) {
					claims.add(playerData.claims.get(i));
				}

				// delete them
				GriefPrevention.instance.dataStore.deleteClaimsForPlayer(claim.getOwnerName(), true, false);
				GriefPrevention.AddLogEntry(" All of " + claim.getOwnerName() + "'s claims have expired. Removing all but the locked claims.");
				GriefPrevention.instance.dataStore.deletePlayerData(playerData.playerName);

				for (int i = 0; i < claims.size(); i++) {
					// if configured to do so, restore the land to natural
					if (wc.getClaimsAutoNatureRestoration()) {
						GriefPrevention.instance.restoreClaim(claims.get(i), 0);
						cleanupChunks = true;
					}
				}
			}
		}

		else if (wc.getUnusedClaimExpirationDays() > 0) {
			// if the player has been gone two weeks, scan claim content to
			// assess player investment
			Calendar earliestAllowedLoginDate = Calendar.getInstance();
			earliestAllowedLoginDate.add(Calendar.DATE, -wc.getUnusedClaimExpirationDays());
			boolean needsInvestmentScan = earliestAllowedLoginDate.getTime().after(playerData.lastLogin);
			boolean creativerules = GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner());
			boolean sizelimitreached = (creativerules && claim.getWidth() > wc.getClaimCleanupMaximumSize());

			// avoid scanning large claims, locked claims, and administrative
			// claims
			if (claim.isAdminClaim() || claim.neverdelete || sizelimitreached)
				return;

			// if creative mode or the claim owner has been away a long enough
			// time, scan the claim content
			if (needsInvestmentScan || creativerules) {
				int minInvestment;
				minInvestment = wc.getClaimCleanupMaxInvestmentScore();
				// if minInvestment is 0, assume no limitation and force the
				// following conditions to clear the claim.
				long investmentScore = minInvestment == 0 ? Long.MAX_VALUE : claim.getPlayerInvestmentScore();
				cleanupChunks = true;
				boolean removeClaim = false;

				// in creative mode, a build which is almost entirely lava above
				// sea level will be automatically removed, even if the owner is
				// an active player
				// lava above the surface deducts 10 points per block from the
				// investment score
				// so 500 blocks of lava without anything built to offset all
				// that potential mess would be cleaned up automatically
				if (GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()) && investmentScore < -5000) {
					Debugger.Write("Creative Rules World, InvestmentScore of " + investmentScore + " is below -5000", DebugLevel.Verbose);
					removeClaim = true;
				}

				// otherwise, the only way to get a claim automatically removed
				// based on build investment is to be away for two weeks AND not
				// build much of anything
				else if (needsInvestmentScan && investmentScore < minInvestment) {
					Debugger.Write("Investment Score (" + investmentScore + " does not meet threshold " + minInvestment, DebugLevel.Verbose);
					removeClaim = true;
				}

				if (removeClaim) {
					GriefPrevention.instance.dataStore.deleteClaim(claim);
					GriefPrevention.AddLogEntry("Removed " + claim.getOwnerName() + "'s unused claim @ " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));

					// if configured to do so, restore the claim area to natural
					// state
					if (wc.getClaimsAutoNatureRestoration()) {
						GriefPrevention.instance.restoreClaim(claim, 0);
					}
				}
			}
		}

		// toss that player data out of the cache, it's probably not needed in
		// memory right now
		if (!GriefPrevention.instance.getServer().getOfflinePlayer(claim.getOwnerName()).isOnline()) {
			GriefPrevention.instance.dataStore.clearCachedPlayerData(claim.getOwnerName());
		}

		// since we're potentially loading a lot of chunks to scan parts of the
		// world where there are no players currently playing, be mindful of
		// memory usage
		if (cleanupChunks) {
			World world = claim.getLesserBoundaryCorner().getWorld();
			Chunk[] chunks = world.getLoadedChunks();
			for (int i = chunks.length - 1; i > 0; i--) {
				Chunk chunk = chunks[i];
				chunk.unload(true, true);
			}
		}

	}

	public void setTaskCookie(int value) {
		TaskCookie = value;
	}
}
