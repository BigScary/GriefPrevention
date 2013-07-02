package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaimInfoCommand extends GriefPreventionCommand{

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		//show information about a claim.
		if(!(sender instanceof Player)) return false;
		GriefPrevention inst = GriefPrevention.instance;
		Player player = (Player)sender;
		Claim claimatpos = null;
		if(args.length ==0)
			claimatpos = inst.dataStore.getClaimAt(player.getLocation(),true,null);
		else {
			int claimid = Integer.parseInt(args[0]);
			claimatpos = inst.dataStore.getClaim(claimid);
			if(claimatpos==null){
			    GriefPrevention.sendMessage(player, TextMode.Err, "Invalid Claim ID:" + claimid);
			    return true;
			}
		}
		if(claimatpos==null){
			GriefPrevention.sendMessage(player,TextMode.Err, "There is no Claim here!");
			GriefPrevention.sendMessage(player,TextMode.Err, "Make sure you are inside a claim.");
			
			return true;
		}
		else {
			//there is a claim, show all sorts of pointless info about it.
			//we do not show trust, since that can be shown with /trustlist.
			//first, get the upper and lower boundary.
			//see that it has Children.
			if(claimatpos.children.size()>0){
				
			}
			
			
			String lowerboundary = GriefPrevention.getfriendlyLocationString(claimatpos.getLesserBoundaryCorner());
			String upperboundary = GriefPrevention.getfriendlyLocationString(claimatpos.getGreaterBoundaryCorner()) ;
			String SizeString = "(" +String.valueOf(claimatpos.getWidth()) + "," + String.valueOf(claimatpos.getHeight()) + ")";
			String ClaimOwner = claimatpos.getOwnerName();
			GriefPrevention.sendMessage(player,TextMode.Info, "ID:" + claimatpos.getID());
			GriefPrevention.sendMessage(player,TextMode.Info, "Position:" + lowerboundary + "-" + upperboundary);
			GriefPrevention.sendMessage(player,TextMode.Info,"Size:" + SizeString);
			GriefPrevention.sendMessage(player, TextMode.Info, "Owner:" + ClaimOwner);
			String parentid = claimatpos.parent==null?"(no parent)":String.valueOf(claimatpos.parent.getID());
			GriefPrevention.sendMessage(player,TextMode.Info, "Parent ID:" + parentid);
			String childinfo = "";
			//if no subclaims, set childinfo to indicate as such.
			if(claimatpos.children.size() ==0){
				childinfo = "No subclaims.";
			}
			else { //otherwise, we need to get the childclaim info by iterating through each child claim.
				childinfo = claimatpos.children.size() + " (";
				
				for(Claim childclaim:claimatpos.children){
				    childinfo+=String.valueOf(childclaim.getSubClaimID()) + ",";	
				}
				//remove the last character since it is a comma we do not want.
				childinfo = childinfo.substring(0,childinfo.length()-1);
				
				//tada
			}
			GriefPrevention.sendMessage(player,TextMode.Info,"Subclaims:" + childinfo);
			
			return true;
		}
		
		
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"claiminfo"};
	}
	

}
