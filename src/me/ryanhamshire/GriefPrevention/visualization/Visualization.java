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

package me.ryanhamshire.GriefPrevention.visualization;

import java.util.ArrayList;
import java.util.List;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

//represents a visualization sent to a player
//FEATURE: to show players visually where claim boundaries are, we send them fake block change packets
//the result is that those players see new blocks, but the world hasn't been changed.  other players can't see the new blocks, either.
public class Visualization {
	// sends a visualization to a player
	public static void Apply(Player player, Visualization visualization) {
		Apply(player, visualization, true);
	}

	public static void Apply(Player player, Visualization visualization, boolean CancelCurrent) {
		PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getName());

		// if he has any current visualization, clear it first
		if (playerData.ActiveVisualizations.size() > 0 && CancelCurrent) {
			Visualization.Revert(player);
		}

		// if he's online, create a task to send him the visualization in about
		// half a second
		if (player.isOnline()) {
			GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, new VisualizationApplicationTask(player, playerData, visualization), 10L);
		}
	}

	// convenience method to build a visualization from a claim
	// visualizationType determines the style (gold blocks, silver, red,
	// diamond, etc)
	public static Visualization FromClaim(Claim claim, int height, VisualizationType visualizationType, Location locality) {
		// visualize only top level claims
		if (claim.parent != null) {
			return FromClaim(claim.parent, height, visualizationType, locality);
		}

		Visualization visualization = new Visualization();

		// add subdivisions first
		for (int i = 0; i < claim.children.size(); i++) {
			visualization.addClaimElements(claim.children.get(i), height, VisualizationType.Subdivision, locality);
		}

		// add top level last so that it takes precedence (it shows on top when
		// the child claim boundaries overlap with its boundaries)
		visualization.addClaimElements(claim, height, visualizationType, locality);

		return visualization;
	}

	// finds a block the player can probably see. this is how visualizations
	// "cling" to the ground or ceiling
	private static Location getVisibleLocation(World world, int x, int y, int z) {
		Block block = world.getBlockAt(x, y, z);
		BlockFace direction = (isTransparent(block)) ? BlockFace.DOWN : BlockFace.UP;

		while (block.getY() >= 1 && block.getY() < world.getMaxHeight() - 1 && (!isTransparent(block.getRelative(BlockFace.UP)) || isTransparent(block))) {
			block = block.getRelative(direction);
		}

		return block.getLocation();
	}
    private static List<Location> getVisibleLocations(World world,int x,int y,int z){
        ArrayList<Location> results = new ArrayList<Location>();
        boolean LastSolid=false;
        for(int currY=0;currY<world.getHighestBlockYAt(x,z)+2;currY++){

            Block blockatpos = world.getBlockAt(x,currY,z);
            boolean currsolid = !isTransparent(blockatpos);
            Location appearpos = new Location(world,x,currY-1,z);
            if(currsolid!=LastSolid){
                if(!currsolid)
                    results.add(appearpos);
                else
                    results.add(blockatpos.getLocation());
            }

            LastSolid=currsolid;
        }
        return results;
    }
	// helper method for above. allows visualization blocks to sit underneath
	// partly transparent blocks like grass and fence
    private static List<Material> TransparentMaterials= null;
	private static boolean isTransparent(Block block) {
        if(TransparentMaterials==null){
            List<Material> BuildList  = new ArrayList<Material>();
            BuildList.add(Material.AIR);
            BuildList.add(Material.LONG_GRASS);
            BuildList.add(Material.FENCE);
            BuildList.add(Material.NETHER_FENCE);
            BuildList.add(Material.CHEST);
            BuildList.add(Material.TRAPPED_CHEST);
            BuildList.add(Material.TRAP_DOOR);
            BuildList.add(Material.WOODEN_DOOR);
            BuildList.add(Material.IRON_DOOR);
            BuildList.add(Material.ENDER_CHEST);
            BuildList.add(Material.IRON_FENCE);
            BuildList.add(Material.THIN_GLASS);
            BuildList.add(Material.YELLOW_FLOWER);
            BuildList.add(Material.RED_ROSE);
            BuildList.add(Material.SNOW);
            if(GriefPrevention.isMCVersionorLater(GriefPrevention.MinecraftVersions.MC16)){
                BuildList.add(Material.FLOWER_POT);
            }
            if(GriefPrevention.isMCVersionorLater(GriefPrevention.MinecraftVersions.MC17)){
              BuildList.add(Material.STAINED_GLASS);
                BuildList.add(Material.STAINED_GLASS_PANE);
                BuildList.add(Material.DOUBLE_PLANT);
            }
            TransparentMaterials=BuildList;
        }
		WorldConfig applicableWorld = GriefPrevention.instance.getWorldCfg(block.getWorld());
		if (applicableWorld.getModsContainerTrustIds() != null && applicableWorld.getModsContainerTrustIds().contains(block.getType()))
			return true;
		if (applicableWorld.getModsAccessTrustIds() != null && applicableWorld.getModsAccessTrustIds().contains(block.getType()))
			return true;

        return TransparentMaterials.contains(block.getType());

	}

	// reverts a visualization by sending another block change list, this time
	// with the real world block values
	public static void Revert(Player player) {
		PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getName());

		// Visualization visualization = playerData.currentVisualization;

		if (playerData.ActiveVisualizations.size() > 0) {
			if (player.isOnline()) {
				for (Visualization visualization : playerData.ActiveVisualizations) {
					for (int i = 0; i < visualization.elements.size(); i++) {
						VisualizationElement element = visualization.elements.get(i);
						Block block = element.location.getBlock();
						player.sendBlockChange(element.location, block.getType(), block.getData());
					}
				}
			}

			playerData.ActiveVisualizations.clear();
		}
	}

	public ArrayList<VisualizationElement> elements = new ArrayList<VisualizationElement>();

    private void cosmeticCleanup()
    {

        ArrayList<VisualizationElement> AdditionalElements = new ArrayList<VisualizationElement>();
        if(elements==null) return;
        for(int i=0;i<elements.size();i++){
            VisualizationElement currelement = elements.get(i);
            //check the block above; if it's a cosmetic block, create a cosmetic "air" block and add it to this visualization instance.
            Location abovespot = new Location(currelement.location.getWorld(),currelement.location.getX(),currelement.location.getY()+1,currelement.location.getZ());
            if(abovespot.getBlock().getType()==Material.SNOW){

                AdditionalElements.add(new VisualizationElement(new Location(abovespot.getWorld(), abovespot.getX(), abovespot.getY(), abovespot.getZ()), Material.AIR, (byte) 0));
            }

        }

        for(int i=0;i<AdditionalElements.size();i++){
            elements.add(AdditionalElements.get(i));
        }

    }
	// adds a claim's visualization to the current visualization
	// handy for combining several visualizations together, as when
	// visualization a top level claim with several subdivisions inside
	// locality is a performance consideration. only create visualization blocks
	// for around 100 blocks of the locality
	private void addClaimElements(Claim claim, int height, VisualizationType visualizationType, Location locality) {
		Location smallXsmallZ = claim.getLesserBoundaryCorner();
		Location bigXbigZ = claim.getGreaterBoundaryCorner();
		World world = smallXsmallZ.getWorld();

		int smallx = smallXsmallZ.getBlockX();
		int smallz = smallXsmallZ.getBlockZ();
		int bigx = bigXbigZ.getBlockX();
		int bigz = bigXbigZ.getBlockZ();

		Material cornerMaterial;
		Material accentMaterial;

		if (visualizationType == VisualizationType.Claim) {
			cornerMaterial = Material.GLOWSTONE;
			accentMaterial = claim.isAdminClaim()?Material.EMERALD_BLOCK: Material.GOLD_BLOCK;
		}

		else if (visualizationType == VisualizationType.Subdivision) {
			cornerMaterial = Material.IRON_BLOCK;
			accentMaterial = Material.WOOL;
		}

		else if (visualizationType == VisualizationType.RestoreNature) {
			cornerMaterial = Material.DIAMOND_BLOCK;
			accentMaterial = Material.DIAMOND_BLOCK;
		}

		else {
			cornerMaterial = Material.GLOWING_REDSTONE_ORE;
			accentMaterial = Material.NETHERRACK;
		}

		// bottom left corner
        for(Location addposition:getVisibleLocations(world,smallx,height,smallz))
		    this.elements.add(new VisualizationElement(addposition, cornerMaterial, (byte) 0));

        for(Location addposition:getVisibleLocations(world,smallx+1,height,smallz))
		    this.elements.add(new VisualizationElement(addposition, accentMaterial, (byte) 0));

        for(Location addposition:getVisibleLocations(world, smallx, height, smallz + 1))
		    this.elements.add(new VisualizationElement(addposition, accentMaterial, (byte) 0));

		// bottom right corner
        for(Location addposition:getVisibleLocations(world, bigx, height, smallz))
		    this.elements.add(new VisualizationElement(addposition, cornerMaterial, (byte) 0));
        for(Location addposition:getVisibleLocations(world, bigx - 1, height, smallz))
		    this.elements.add(new VisualizationElement(addposition, accentMaterial, (byte) 0));
        for(Location addposition:getVisibleLocations(world, bigx, height, smallz + 1))
		    this.elements.add(new VisualizationElement(addposition, accentMaterial, (byte) 0));

		// top right corner
        for(Location addposition:getVisibleLocations(world, bigx, height, bigz))
		    this.elements.add(new VisualizationElement(addposition, cornerMaterial, (byte) 0));
		for(Location addposition:getVisibleLocations(world, bigx - 1, height, bigz))
            this.elements.add(new VisualizationElement(addposition, accentMaterial, (byte) 0));
        for(Location addposition:getVisibleLocations(world, bigx, height, bigz - 1))
		    this.elements.add(new VisualizationElement(addposition, accentMaterial, (byte) 0));

		// top left corner

        for(Location addposition:getVisibleLocations(world, smallx, height, bigz))
		    this.elements.add(new VisualizationElement(addposition, cornerMaterial, (byte) 0));

        for(Location addposition:getVisibleLocations(world, smallx + 1, height, bigz))
		    this.elements.add(new VisualizationElement(addposition, accentMaterial, (byte) 0));
		for(Location addposition:getVisibleLocations(world, smallx, height, bigz - 1))
            this.elements.add(new VisualizationElement(addposition, accentMaterial, (byte) 0));

        cosmeticCleanup();
		// locality
		int minx = locality.getBlockX() - 100;
		int minz = locality.getBlockZ() - 100;
		int maxx = locality.getBlockX() + 100;
		int maxz = locality.getBlockZ() + 100;

		// top line
		for (int x = smallx + 10; x < bigx - 10; x += 10) {
			if (x > minx && x < maxx)
                for(Location addlocation:getVisibleLocations(world, x, height, bigz))
				    this.elements.add(new VisualizationElement(addlocation, accentMaterial, (byte) 0));
		}

		// bottom line
		for (int x = smallx + 10; x < bigx - 10; x += 10) {
			if (x > minx && x < maxx)
                for(Location addlocation:getVisibleLocations(world, x, height, smallz))
				    this.elements.add(new VisualizationElement(addlocation, accentMaterial, (byte) 0));
		}

		// left line
		for (int z = smallz + 10; z < bigz - 10; z += 10) {
			if (z > minz && z < maxz)
                for(Location addlocation:getVisibleLocations(world, smallx, height, z))
				    this.elements.add(new VisualizationElement(addlocation, accentMaterial, (byte) 0));
		}

		// right line
		for (int z = smallz + 10; z < bigz - 10; z += 10) {
			if (z > minz && z < maxz)
                for(Location addlocation:getVisibleLocations(world, bigx, height, z))
				    this.elements.add(new VisualizationElement(addlocation, accentMaterial, (byte) 0));
		}
	}
}
