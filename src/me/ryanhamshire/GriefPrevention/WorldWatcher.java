package me.ryanhamshire.GriefPrevention;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import me.ryanhamshire.GriefPrevention.Debugger.DebugLevel;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.tasks.WorldClaimCleanupTask;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldWatcher implements Listener {

	// public HashMap<String,List<UnloadedClaimData>> DeferredLoad = new
	// HashMap<String,List<UnloadedClaimData>>();
	public Set<World> LoadedWorlds = new HashSet<World>();
	HashSet<World> storedloaded = new HashSet<World>(); // temp debugger...

	public Map<String, WorldClaimCleanupTask> WorldClaimTasks = new HashMap<String, WorldClaimCleanupTask>();

	public HashMap<String, Queue<Runnable>> WorldLoadDelegates = new HashMap<String, Queue<Runnable>>();

	public void AddDelegate(String worldname, Runnable addthis) {
		if (!WorldLoadDelegates.containsKey(worldname)) {
			WorldLoadDelegates.put(worldname, new ArrayBlockingQueue<Runnable>(40));

		}
		WorldLoadDelegates.get(worldname).add(addthis);
	}

	private Location Correspond(Location Source, World Target) {
		// System.out.println("Creating corresponding location, Source is in " +
		// Source.getWorld().getName() + " And target is in " + Target.getName()
		// + " Equal:" + Source.getWorld().equals(Target));
		return new Location(Target, Source.getX(), Source.getY(), Source.getZ());
	}

	@EventHandler
	public void WorldLoad(WorldLoadEvent event) {

        if (event.getWorld() == null)
			return;
        Debugger.Write("WorldLoaded:" + event.getWorld().getName(),DebugLevel.Verbose);
		if (WorldLoadDelegates.containsKey(event.getWorld().getName())) {
			int randelegates = 0;
			Runnable r = null;
			while (null != (r = WorldLoadDelegates.get(event.getWorld()).poll())) {
				r.run();
				randelegates++;
			}
			if (randelegates > 0) {
				Debugger.Write("Ran " + randelegates + " WorldLoad Delegates.",DebugLevel.Verbose);
			}
		}

		GriefPrevention.instance.dataStore.WorldLoaded(event.getWorld());
		// that should do it, simple enough.
		LoadedWorlds.add(event.getWorld());
		// start the cleanup listener for this world.
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getWorld());
		WorldClaimCleanupTask createdTask = new WorldClaimCleanupTask(event.getWorld().getName());
		WorldClaimTasks.put(event.getWorld().getName(), createdTask);
		if (wc.getClaimCleanupEnabled()) {
            //ten minute cleanup interval.
			int taskCookie = Bukkit.getScheduler().scheduleSyncRepeatingTask(GriefPrevention.instance, createdTask, 60*5*20, 60*5*10);
			createdTask.setTaskCookie(taskCookie);
		}



	}

	@EventHandler
	public void WorldUnload(WorldUnloadEvent event) {
		if (LoadedWorlds.contains(event.getWorld())) {
			// stop world claim cleanup.
            Debugger.Write("WorldUnload:" + event.getWorld().getName(),DebugLevel.Verbose);
			WorldClaimCleanupTask wtask = WorldClaimTasks.remove(event.getWorld().getName());
			// stop the task.
            if(wtask!=null) Bukkit.getScheduler().cancelTask(wtask.getTaskCookie());
			// that oughta do it.

			GriefPrevention.instance.dataStore.WorldUnloaded(event.getWorld());
			LoadedWorlds.remove(event.getWorld());

		}
	}

    public void clear() {
        LoadedWorlds.clear();
        //cancel all claim cleanup Tasks.
        for(WorldClaimCleanupTask cleaner :this.WorldClaimTasks.values()){
            Bukkit.getScheduler().cancelTask(cleaner.getTaskCookie());
        }
        WorldClaimTasks.clear();

    }
}
