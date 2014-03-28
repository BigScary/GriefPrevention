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

import java.util.ArrayList;
import java.util.List;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;

//FEATURE: creative mode worlds get a regular entity cleanup

//this main thread task revisits the location of a partially chopped tree from several minutes ago
//if any part of the tree is still there and nothing else has been built in its place, remove the remaining parts
public class EntityCleanupTask implements Runnable {
	// where to start cleaning in the list of entities
	private double percentageStart;

	public EntityCleanupTask(double percentageStart) {
		this.percentageStart = percentageStart;
	}

	public void run() {
		List<WorldConfig> gotworlds = new ArrayList<WorldConfig>(GriefPrevention.instance.Configuration.getWorldConfigs().values());

		List<WorldConfig> worlds = new ArrayList<WorldConfig>();
		for (WorldConfig w : gotworlds) {
			if (w.getEntityCleanupEnabled()) {
				gotworlds.add(w);
			}
		}

		for (WorldConfig worldconfiguration : worlds) {
			World world = Bukkit.getWorld(worldconfiguration.getWorldName());
			if (world == null) {
				continue;
			}

			List<Entity> entities = world.getEntities();

			// starting and stopping point. each execution of the task scans 10%
			// of the server's (loaded) entities
			int j = (int) (entities.size() * this.percentageStart);
			int k = (int) (entities.size() * (this.percentageStart + .1));
			Claim cachedClaim = null;
			for (; j < entities.size() && j < k; j++) {
				Entity entity = entities.get(j);

				boolean remove = false;
				if (entity instanceof Boat) // boats must be occupied
				{
					Boat boat = (Boat) entity;
					if (boat.isEmpty())
						remove = true;
				}

				else if (entity instanceof Vehicle) {
					Vehicle vehicle = (Vehicle) entity;

					// minecarts in motion must be occupied by a player
					if (vehicle.getVelocity().lengthSquared() != 0) {
						if (vehicle.isEmpty() || !(vehicle.getPassenger() instanceof Player)) {
							remove = true;
						}
					}

					// stationary carts must be on rails
					else {
						Material material = world.getBlockAt(vehicle.getLocation()).getType();
						if (material != Material.RAILS && material != Material.POWERED_RAIL && material != Material.DETECTOR_RAIL) {
							remove = true;
						}
					}
				}

				// all non-player entities must be in claims
				else if (!(entity instanceof Player)) {
					Claim claim = GriefPrevention.instance.dataStore.getClaimAt(entity.getLocation(), false);
					if (claim != null) {
						cachedClaim = claim;
					} else {
						remove = true;
					}
				}

				if (remove) {
					entity.remove();
				}
			}
		}

		// starting and stopping point. each execution of the task scans 5% of
		// the server's claims
		Long[] claims = GriefPrevention.instance.dataStore.getClaimIds();
		int j = (int) (claims.length * this.percentageStart);
		int k = (int) (claims.length * (this.percentageStart + .05));
		for (; j < claims.length && j < k; j++) {
			Claim claim = GriefPrevention.instance.dataStore.getClaim(claims[j]);

			// if it's a creative mode claim
			if (GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner())) {
				// check its entity count and remove any extras
				claim.allowMoreEntities();
			}
		}

		// schedule the next run of this task, in 3 minutes (20L is
		// approximately 1 second)
		double nextRunPercentageStart = this.percentageStart + .05;
		if (nextRunPercentageStart > .99) {
			nextRunPercentageStart = 0;
			// System.gc(); //clean up every hour
		}

		EntityCleanupTask task = new EntityCleanupTask(nextRunPercentageStart);
		GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 20L * 60 * 1);
	}
}
