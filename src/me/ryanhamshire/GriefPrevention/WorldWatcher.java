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
   //we need to watch world unload and load events; the latter, mostly.
   //this is because when a world is unloaded and loaded again, it will be another world instance.
   //this is an issue because the claims generally "index" themselves based on their location, and
	//they will be holding a Location reference to a world that does not exist anymore. Thus we need to update
	//all loaded claims. Hopefully the Location object references
	//will still be valid for unloaded worlds at least in terms of their coordinates.
	@EventHandler
	public void WorldLoad(WorldLoadEvent event){
		if(event.getWorld()==null) return;
		
		if(WorldLoadDelegates.containsKey(event.getWorld().getName())){
			Runnable r=null;
			while(null!=(r= WorldLoadDelegates.get(event.getWorld()).poll())){
				r.run();
			}
			
			
			
		}
		
		if(storedloaded.contains(event.getWorld())){
			System.out.println("WorldLoad: World named " + event.getWorld().getName() + " Already exists!");
		}
		else {
			storedloaded.add(event.getWorld());
		}
		System.out.println("stored Worlds:");
		for(World iterate:storedloaded){
			System.out.println("world:" + iterate.getName());
		}
		
		
		String searchWorld  = event.getWorld().getName();
		System.out.println("World Load detected:" + event.getWorld().getName() + " applying reference fix-ups...");
		if(GriefPrevention.instance ==null || GriefPrevention.instance.dataStore == null) return;
		if(!GriefPrevention.instance.dataStore.getClaimArray().claimworldmap.contains(searchWorld)) return;
		ArrayList<Claim> grablist = GriefPrevention.instance.dataStore.getClaimArray().claimworldmap.get(searchWorld);
		//we need to fix-up All the claims.
		for(Claim fixit:grablist){
			System.out.println("fixed up claim owned by:" + fixit.ownerName + " in world:" + event.getWorld().getName());
			fixit.lesserBoundaryCorner = Correspond(fixit.lesserBoundaryCorner,event.getWorld());
			fixit.greaterBoundaryCorner = Correspond(fixit.greaterBoundaryCorner,event.getWorld());
		}
		//that should do it, simple enough.
	}
	
	
	
	
}
