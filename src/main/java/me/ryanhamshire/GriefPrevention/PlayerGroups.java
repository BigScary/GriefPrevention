package me.ryanhamshire.GriefPrevention;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

public class PlayerGroups {

	private HashMap<String,PlayerGroup> PlayerGroups = new HashMap<String,PlayerGroup>();
	/**
	 * returns a copy of the PlayerGroups of this instance.
	 * @return
	 */
	public List<PlayerGroup> getPlayerGroups(){ return new ArrayList<PlayerGroup>(PlayerGroups.values());}
	public boolean GroupExists(String testName){
		return getGroupByName(testName) !=null;
	}
	/**
	 * retrieves a a specified group, or null of the name does not match any group.
	 * @param groupname
	 * @return
	 */
	public PlayerGroup getGroupByName(String groupname){
		String capgroup = groupname.toUpperCase();
		if(!PlayerGroups.containsKey(capgroup))
			return null;
		return PlayerGroups.get(capgroup);
	}
/*
  Groups:
  Names:[Donator,HalfOp]:
  - Donator: [Chicken,Waffle]
  - HalfOp:  [Choodles,Smeagle]
 * 
 * 
 */
	/**
	 * Initializes this PlayerGroups collection based on data in the given configuration file at the specified node.
	 * @param Source
	 * @param SourceNode
	 */
	public PlayerGroups(FileConfiguration Source,String SourceNode){
		List<PlayerGroup> checklist = PlayerGroup.getGroups(Source, SourceNode);
		for(PlayerGroup iterate:checklist){
			PlayerGroups.put(iterate.getGroupName().toUpperCase(), iterate);
		}
	}
	/**
	 * Saves this PlayerGroups list to a FileConfiguration.
	 * @param Target
	 * @param TargetNode
	 */
	void Save(FileConfiguration Target,String TargetNode){
		ArrayList<String> groupnames = new ArrayList<String>();
		for(PlayerGroup pg:PlayerGroups.values()){
			groupnames.add(pg.getGroupName());
		}
		Target.set(TargetNode + ".Names", groupnames);
		for(PlayerGroup iterate:PlayerGroups.values()){
			String usenode = TargetNode + "." + iterate.getGroupName();
			iterate.Save(Target, usenode);
		}
		
		
	}
	
	/**
	 * returns the PlayerGroup(s) this player belongs to. 
	 * @param PlayerName
	 * @return
	 */
	public List<PlayerGroup> getGroupsForPlayer(String PlayerName){
		ArrayList<PlayerGroup> makelist = new ArrayList<PlayerGroup>();
		for(PlayerGroup iterate:PlayerGroups.values()){
			if(iterate.MatchPlayer(PlayerName)){
				makelist.add(iterate);
			}
		}
		
		return makelist;
	}
	
	
	
}
