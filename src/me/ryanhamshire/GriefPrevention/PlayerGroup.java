package me.ryanhamshire.GriefPrevention;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * represents a Player Group.
 * @author BC_Programming
 * Player groups are defined in the configuration file. This represents the contents of one group, and provides
 * methods for matching the items within a group with the names.
 */
public class PlayerGroup {

	
	private List<String> PlayerNames = new ArrayList<String>(); //names of players in this group.
	private String GroupName;
	/**
	 * retrieves the current list of PlayerNames in this group.
	 * @return
	 */
	public List<String> getPlayerNames() { return new ArrayList<String>(PlayerNames);}
	/**
	 * retrieves the name of this Group.
	 * @return
	 */
	public String getGroupName(){ return GroupName;}
	
	/**
	 * determines if the given player is in this PlayerGroup.
	 * @param PlayerName Name if the player to check.
	 * @return true of PlayerName is in this Group. false otherwise.
	 */
	public boolean MatchPlayer(String PlayerName){
		
		//determines if this player is in this group.
		for(String iterateplayer:PlayerNames){
			if(iterateplayer.equalsIgnoreCase(PlayerName))
				return true;
		}
		return false;
		
		
	}
	/* Groups:
  Names:[Donator,HalfOp]:
    Donator: [Chicken,Waffle]
    HalfOp:  [Choodles,Smeagle]*/
	/**
	 * reads Groups from the given Configuration file and Source Node.
	 * @param Source Config to read from.
	 * @param SourceNode Source node to read from within the configuration.
	 * @return
	 */
	public static List<PlayerGroup> getGroups(FileConfiguration Source,String SourceNode){
		//sourcenode will be the node: normally, GriefPrevention.Groups.
		//System.out.println("Attempting to read groups from" + SourceNode);
		ArrayList<PlayerGroup> results = new ArrayList<PlayerGroup>();
		ConfigurationSection csec = Source.getConfigurationSection(SourceNode + ".Names");
		if(csec==null){
			//no groups.
			return new ArrayList<PlayerGroup>();
		}
		Set<String> GroupNames = csec.getKeys(false);
				
		System.out.println("Found " + GroupNames.size() + " Groups" );
		for(String iterategroup:GroupNames){
			//create Group Name.
			String GroupPath = SourceNode + "." + iterategroup;
			PlayerGroup makegroup = new PlayerGroup(Source,GroupPath);
			if(makegroup.PlayerNames.size() > 0){
				results.add(makegroup);
			}
			
		}
		return results;
	}
	/**
	 * Saves this PlayerGroup to the given configuration file at the specified node.
	 * @param Target Configuration file to save to.
	 * @param TargetNode Node to save to in the given configuration file.
	 */
	public void Save(FileConfiguration Target, String TargetNode){
		

		Target.set(TargetNode, PlayerNames);
		
		
	}
	
	private PlayerGroup(FileConfiguration Source,String SourceNode){
		//System.out.println("reading group from " + SourceNode);
		//Source is the Config to load from.
		//Node is the first node. An example of the Groups:
		
		/*

		 
		 */
		//SourceNode will be "GriefPrevention.Groups.Donator" when loading the Donator Group.
		
		//First: parse out the Group name. This is everything past the last period.
		
		String getgroupname = SourceNode.substring(SourceNode.lastIndexOf('.')+1);
		GroupName = getgroupname;
		//now we want the list at this Node.
		PlayerNames = new ArrayList<String>();
		for(String iteratename:Source.getStringList(SourceNode)){
			System.out.println("Group-" + SourceNode + " " + iteratename);
			PlayerNames.add(iteratename);
			
		}
		
	}
	
	
}
