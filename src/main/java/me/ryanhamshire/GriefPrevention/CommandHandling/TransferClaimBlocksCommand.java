package me.ryanhamshire.GriefPrevention.CommandHandling;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class TransferClaimBlocksCommand extends GriefPreventionCommand{

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		if(args.length<3){
			return false;
		}
		String sourcename = args[0];
		String targetname = args[1];
		int desiredxfer = 0;
		try {desiredxfer = Integer.parseInt(args[2]);}
		catch(NumberFormatException exx){
			return false;
		}
		CommandHandler.transferClaimBlocks(sourcename, targetname, desiredxfer);
		
		return true;
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"transferclaimblocks"};
	}
	

}
