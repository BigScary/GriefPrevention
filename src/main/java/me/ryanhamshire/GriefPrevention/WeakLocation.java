package me.ryanhamshire.GriefPrevention;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class WeakLocation{

	private String WorldName;
	public String getWorldName(){ return WorldName;}
	private double X,Y,Z;
	public double getX(){ return X;}
	public double getY(){ return Y;}
	public double getZ(){ return Z;}
	public WeakLocation(String pWorldName,double pX,double pY,double pZ){
		WorldName=pWorldName;
		X=pX;
		Y=pY;
		Z=pZ;
	}

	public Location getValue(){
		
		World grabworld = Bukkit.getWorld(WorldName);
		if(grabworld==null) return null;
		
		
		return new Location(grabworld,X,Y,Z);
		
		
		
	}
}
