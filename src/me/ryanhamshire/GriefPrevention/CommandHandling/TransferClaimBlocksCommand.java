package me.ryanhamshire.GriefPrevention.CommandHandling;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TransferClaimBlocksCommand extends GriefPreventionCommand {

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[] { "transferclaimblocks" };
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// TODO Auto-generated method stub
		if (args.length < 3) {
			return false;
		}
		if(sender instanceof Player){
			if(!EnsurePermission((Player)sender, label)) return true;
		}
		String sourcename = args[0];
		String targetname = args[1];
		int desiredxfer = 0;
		try {
			desiredxfer = Integer.parseInt(args[2]);
		} catch (NumberFormatException exx) {
			return false;
		}
		CommandHandler.transferClaimBlocks(sourcename, targetname, desiredxfer);

		return true;
	}

}
