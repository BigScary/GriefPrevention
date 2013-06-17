package me.ryanhamshire.GriefPrevention;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldWatcher implements Listener {
	
	
	//public HashMap<String,List<UnloadedClaimData>> DeferredLoad = new HashMap<String,List<UnloadedClaimData>>();
	
	
	private Location Correspond(Location Source,World Target){
		System.out.println("Creating corresponding location, Source is in " + Source.getWorld().getName() + " And target is in " + Target.getName() + " Equal:" + Source.getWorld().equals(Target));
		return new Location(Target,Source.getX(),Source.getY(),Source.getZ());
	}
	public HashMap<String,Queue<Runnable>> WorldLoadDelegates = new HashMap<String,Queue<Runnable>>();
	
	public void AddDelegate(String worldname,Runnable addthis){
		if(!WorldLoadDelegates.containsKey(worldname)){
			WorldLoadDelegates.put(worldname, new ArrayBlockingQueue<Runnable>(40));
			
		}
		WorldLoadDelegates.get(worldname).add(addthis);
	}
	
	HashSet<World> storedloaded = new HashSet<World>(); //temp debugger...
	
	@EventHandler
	public void WorldUnload(WorldUnloadEvent event){
		GriefPrevention.instance.dataStore.WorldUnloaded(event.getWorld());
	}
	

	@EventHandler
	public void WorldLoad(WorldLoadEvent event){
		if(event.getWorld()==null) return;
		
		if(WorldLoadDelegates.containsKey(event.getWorld().getName())){
			Runnable r=null;
			while(null!=(r= WorldLoadDelegates.get(event.getWorld()).poll())){
				r.run();
			}
		}
		
		GriefPrevention.instance.dataStore.WorldLoaded(event.getWorld());
		//that should do it, simple enough.
	}
	
	
	
	
}
