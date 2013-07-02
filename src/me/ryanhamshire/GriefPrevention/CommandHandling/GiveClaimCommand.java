package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiveClaimCommand extends GriefPreventionCommand {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		//gives a claim to another player. get the source player first.
		if(args.length==0) return false;
		Player source = (sender instanceof Player)?(Player)sender:null;
		Player target = Bukkit.getPlayer(args[0]);
		if(sender==null) return false;
		DataStore dataStore = GriefPrevention.instance.dataStore;
		if(target==null){
			GriefPrevention.sendMessage(source,TextMode.Err, Messages.PlayerNotFound,args[0]);
			return true;
		}
		//if it's not null, make sure they have either have giveclaim permission or adminclaims permission.
		
		if(!(source.hasPermission("griefprevention.giveclaims") || source.hasPermission("griefprevention.adminclaims"))){
		
			//find the claim at the players location.
			Claim claimtogive = dataStore.getClaimAt(source.getLocation(), true, null);
			//if the owner is not the source, they have to have adminclaims permission too.
			if(!claimtogive.getOwnerName().equalsIgnoreCase(source.getName())){
				//if they don't have adminclaims permission, deny it.
				if(!source.hasPermission("griefprevention.adminclaims")){
					GriefPrevention.sendMessage(source, TextMode.Err, Messages.NoAdminClaimsPermission);
					return true;
				}
			}
			//transfer ownership.
			claimtogive.ownerName = target.getName();

			String originalOwner = claimtogive.getOwnerName();
			try {dataStore.changeClaimOwner(claimtogive, target.getName());
			//message both players.
			GriefPrevention.sendMessage(source, TextMode.Success, Messages.GiveSuccessSender,originalOwner,target.getName());
			if(target!=null && target.isOnline()){
				GriefPrevention.sendMessage(target,TextMode.Success,Messages.GiveSuccessTarget,originalOwner);
			}
			}
			catch(Exception exx){
				GriefPrevention.sendMessage(source, TextMode.Err, "Failed to transfer Claim.");
			}
			return true;
			
			
		}
		return false;
		
		
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"giveclaim"};
	}

}
