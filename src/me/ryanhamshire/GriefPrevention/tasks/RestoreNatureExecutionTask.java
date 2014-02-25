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

import me.ryanhamshire.GriefPrevention.BlockSnapshot;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;
import me.ryanhamshire.GriefPrevention.visualization.VisualizationType;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;

//this main thread task takes the output from the RestoreNatureProcessingTask\
//and updates the world accordingly
class RestoreNatureExecutionTask implements Runnable {
	private Location greaterCorner;

	private Location lesserCorner;
	// boundaries for changes
	private int miny;
	// player who should be notified about the result (will see a visualization
	// when the restoration is complete)
	private Player player;

	// results from processing thread
	// will be applied to the world
	private BlockSnapshot[][][] snapshots;

	public RestoreNatureExecutionTask(BlockSnapshot[][][] snapshots, int miny, Location lesserCorner, Location greaterCorner, Player player) {

		this.snapshots = snapshots;
		this.miny = miny;
		this.lesserCorner = lesserCorner;
		this.greaterCorner = greaterCorner;
		this.player = player;
	}

	public void run() {
		// apply changes to the world, but ONLY to unclaimed blocks
		// note that the edge of the results is not applied (the 1-block-wide
		// band around the outside of the chunk)
		// those data were sent to the processing thread for referernce
		// purposes, but aren't part of the area selected for restoration
		Claim cachedClaim = null;
		for (int x = 1; x < this.snapshots.length - 1; x++) {
			for (int z = 1; z < this.snapshots[0][0].length - 1; z++) {
				for (int y = this.miny; y < this.snapshots[0].length; y++) {
					BlockSnapshot blockUpdate = this.snapshots[x][y][z];
					Block currentBlock = blockUpdate.location.getBlock();
					if (blockUpdate.typeId != currentBlock.getTypeId() || blockUpdate.data != currentBlock.getData()) {
						Claim claim = GriefPrevention.instance.dataStore.getClaimAt(blockUpdate.location, false);
						if (claim != null) {
							cachedClaim = claim;
							break;
						}

						currentBlock.setTypeId(blockUpdate.typeId);
						currentBlock.setData(blockUpdate.data);
					}
				}
			}
		}

		// clean up any entities in the chunk, ensure no players are suffocated
		Chunk chunk = this.lesserCorner.getChunk();
		Entity[] entities = chunk.getEntities();
		for (int i = 0; i < entities.length; i++) {
			Entity entity = entities[i];
			if (!(entity instanceof Player || entity instanceof Animals)) {
				// hanging entities (paintings, item frames) are protected when
				// they're in land claims
				if (!(entity instanceof Hanging) || GriefPrevention.instance.dataStore.getClaimAt(entity.getLocation(), false) == null) {
					// everything else is removed
					entity.remove();
				}
			}

			// for players, always ensure there's air where the player is
			// standing
			else {
				Block feetBlock = entity.getLocation().getBlock();
				feetBlock.setType(Material.AIR);
				feetBlock.getRelative(BlockFace.UP).setType(Material.AIR);
			}
		}

		// show visualization to player who started the restoration
		if (player != null) {
			Claim claim = new Claim(lesserCorner, greaterCorner, "", new String[] {}, new String[] {}, new String[] {}, new String[] {}, null, false);
			Visualization visualization = Visualization.FromClaim(claim, player.getLocation().getBlockY(), VisualizationType.RestoreNature, player.getLocation());
			Visualization.Apply(player, visualization);
		}
	}
}
