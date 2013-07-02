package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TransferClaimCommand extends GriefPreventionCommand {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		//can take two parameters. Source Player and target player.
		Player player = (sender instanceof Player)?(Player)sender:null;
		GriefPrevention inst = GriefPrevention.instance;
		
		if(player==null) return false;
		if(args.length == 0){
			if(!player.hasPermission("GriefPrevention.adminclaims")){
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoAdminClaimsPermission);
				return true;
			}
			else {
				
				Claim claim = inst.dataStore.getClaimAt(player.getLocation(), true, null);
				if(claim == null)
				{
					GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferClaimMissing);
					return true;
				}
				else {
					//make it an admin claim.
					try {
					inst.dataStore.changeClaimOwner(claim,"");
					GriefPrevention.sendMessage(player,TextMode.Instr,"This claim is now an Admin claim.");
					return true;
					}
					catch(Exception exx){
						GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferTopLevel);
						return true;
					}
				}
				
				
			}
		}
		//one arg requires "GriefPrevention.transferclaims" or "GriefPrevention.adminclaims" permission.
		//two args requires the latter.
		if(args.length >0)
		//check additional permission
		if(!(player.hasPermission("griefprevention.adminclaims") ))
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoAdminClaimsPermission);
			return true;
		}
		else if(!player.hasPermission("griefprevention.transferclaims")){
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.TransferClaimPermission);
			return true;
		}

		//which claim is the user in?
		Claim claim = inst.dataStore.getClaimAt(player.getLocation(), true, null);
		if(claim == null)
		{
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferClaimMissing);
			return true;
		}
		
		OfflinePlayer targetPlayer = inst.resolvePlayer(args[0]);
		if(targetPlayer == null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
			return true;
		}
		
		//change ownership
		try
		{
			inst.dataStore.changeClaimOwner(claim, targetPlayer.getName());
		}
		catch(Exception e)
		{
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferTopLevel);
			return true;
		}
		
		//confirm
		GriefPrevention.sendMessage(player, TextMode.Success, Messages.TransferSuccess);
		GriefPrevention.AddLogEntry(player.getName() + " transferred a claim at " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " to " + targetPlayer.getName() + ".");
		
		return true;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"transferclaim"};
	}

	
}
