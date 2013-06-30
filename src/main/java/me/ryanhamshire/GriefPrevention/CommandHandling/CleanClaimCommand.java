package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.MaterialInfo;
import me.ryanhamshire.GriefPrevention.PlayerData;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CleanClaimCommand extends GriefPreventionCommand{
	private void HandleClaimClean(Claim c,MaterialInfo source,MaterialInfo target,Player player){
		Location lesser = c.getLesserBoundaryCorner();
		Location upper = c.getGreaterBoundaryCorner();
		//System.out.println("HandleClaimClean:" + source.typeID + " to " + target.typeID);
		
		for(int x =lesser.getBlockX();x<=upper.getBlockX();x++){
			for(int y = 0;y<=255;y++){
				for(int z = lesser.getBlockZ();z<=upper.getBlockZ();z++){
					Location createloc =  new Location(lesser.getWorld(),x,y,z);
					Block acquired = lesser.getWorld().getBlockAt(createloc);
					if(acquired.getTypeId() == source.typeID && acquired.getData() == source.data){
						acquired.setTypeIdAndData(target.typeID, target.data, true);
						
					}
					
					
					
				}
			}
		}
		
		
		
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if(!(sender instanceof Player)) return false;
		Player player = (Player)sender;
		GriefPrevention inst = GriefPrevention.instance;
		DataStore dataStore = inst.dataStore;
		// TODO Auto-generated method stub
		//source is first arg; target is second arg.
		player.sendMessage("cleanclaim command..." + args.length);
		if(args.length==0){
			return true;
		}
		MaterialInfo source = MaterialInfo.fromString(args[0]);
	    if(source==null){
	    	Material attemptparse = Material.valueOf(args[0]); 
	    	if(attemptparse!=null){
	    		source = new MaterialInfo(attemptparse.getId(),(byte)0,args[0]);
	    	}
	    	else
	    	{
	    		player.sendMessage("Failed to parse Source Material," + args[0]);
	    		return true;
	    	}
	    	
	    }
	    MaterialInfo target = new MaterialInfo(Material.AIR.getId(),(byte)0,"Air");
	    if(args.length >1){
	    	target = MaterialInfo.fromString(args[1]);
	    	if(target==null){
	    		Material attemptparse = Material.valueOf(args[1]);
	    		if(attemptparse!=null){
	    			target = new MaterialInfo(attemptparse.getId(),(byte)0,args[1]);
	    		}
	    		else {
	    			player.sendMessage("Failed to parse Target Material," + args[1]);
	    		}
	    	}
	    
	    }
	    //System.out.println(source.typeID + " " +target.typeID);
	    PlayerData pd = dataStore.getPlayerData(player.getName());
	    Claim retrieveclaim = dataStore.getClaimAt(player.getLocation(), true, null);
	    if(retrieveclaim!=null){
		    if(pd.ignoreClaims || retrieveclaim.ownerName.equalsIgnoreCase(player.getName())){
		    	HandleClaimClean(retrieveclaim,source,target,player);
		    	return true;
		    }
	    }
		return false;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"cleanclaim"};
	}

}
