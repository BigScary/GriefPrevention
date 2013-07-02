package me.ryanhamshire.GriefPrevention;

import org.bukkit.Location;
import org.bukkit.Material;

//Stores information about a block that was broken.
//Containers and other Tile Entitious blocks cannot be broken,
//but other siegable blocks can be added here when they are broken to be reset when
//the siege ends.
public class BrokenBlockInfo {
	private Material material;
	private byte data;
	private Location location;
	public Material getBlockMaterial(){ return material;}
	public byte getData(){ return data;}
	public Location getLocation(){ return location;}
	
	public static boolean canBreak(Location atpos){
		//if the block has a BlockState, then it cannot be broken. Otherwise, return true.
		return atpos.getBlock().getState()==null;
		//optionally we could store away the State, too...
		
	}
	public BrokenBlockInfo(Location atLocation){
		//called to init before the block at the given location is actually removed.
		material = atLocation.getBlock().getType();
		data = atLocation.getBlock().getData();
		location = atLocation;
	}
	public void reset(){
		//simply reset the block at our location to the cached state.
		location.getBlock().setType(material);
		location.getBlock().setData(data);
	}
	
}
