package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetBlockBankCommand extends GriefPreventionCommand {

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
        //requires exactly two parameters, the other player's name and the adjustment
        if(args.length != 2) return false;
        GriefPrevention inst = GriefPrevention.instance;
        Player player = (sender instanceof Player)?(Player)sender:null;
        //find the specified player
        OfflinePlayer targetPlayer = inst.resolvePlayer(args[0]);
        if(targetPlayer == null)
        {
                GriefPrevention.sendMessage(player, TextMode.Err, "Player \"" + args[0] + "\" not found.");
                return true;
        }

        //parse the adjustment amount
        int adjustment;
        try
        {
                adjustment = Integer.parseInt(args[1]);
        }
        catch(NumberFormatException numberFormatException)
        {
                return false;  //causes usage to be displayed
        }
        //give blocks to player
        PlayerData playerData = inst.dataStore.getPlayerData(targetPlayer.getName());
        playerData.accruedClaimBlocks += adjustment;
        inst.dataStore.savePlayerData(targetPlayer.getName(), playerData);

        GriefPrevention.sendMessage(player, TextMode.Success, "Adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".  New total bonus blocks: " + playerData.bonusClaimBlocks + ".");
        GriefPrevention.AddLogEntry(player.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by "  + adjustment + ".");

        return true;  
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"setblockbank"};
	}
	

}
