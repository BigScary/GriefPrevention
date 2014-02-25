package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SoftMuteCommand extends GriefPreventionCommand {

    @Override
    public String[] getLabels() {
        // TODO Auto-generated method stub
        return new String[] { "softmute" };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //softmute <player>
        //does NOT require a player to issue the command, but it does require
        //a player name (at least one arg).
        Player p=null;
        if(sender instanceof Player)
            p = (Player)sender;
        if(args.length==0) return false; //not enough args.


        String grabplayername = args[0];
        Player acquired = Bukkit.getPlayer(grabplayername);
        if(acquired==null){
            GriefPrevention.sendMessage(p,TextMode.Err,Messages.PlayerNotFound);
        }
        PlayerData pd = GriefPrevention.instance.dataStore.getPlayerData(acquired.getName());
        boolean toggleresult = pd.ToggleSoftMute();
        Messages usemsg = toggleresult?Messages.PlayerSoftMuted:Messages.PlayerUnSoftMuted;
        GriefPrevention.sendMessage(p,TextMode.Info,usemsg,acquired.getName());
        return true;

    }

}

