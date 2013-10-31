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

public class IgnoreCommand extends GriefPreventionCommand {

    @Override
    public String[] getLabels() {
        // TODO Auto-generated method stub
        return new String[] { "ignore" };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //ignore <player>

        if(sender instanceof Player){
            Player p = (Player)sender;
            PlayerData pdata = GriefPrevention.instance.dataStore.getPlayerData(p.getName());
            if(args.length==0){
                 //show current ignore list.
                GriefPrevention.sendMessage(p,TextMode.Info,"Players you are ignoring:");
                 List<String> ignoring = pdata.getIgnoreList();
                 if(ignoring.size()==0){
                     GriefPrevention.sendMessage(p,TextMode.Info,"You are not ignoring anybody.");
                 }
                 else {

                     for(String str:ignoring){
                        GriefPrevention.sendMessage(p,TextMode.Info,str);

                     }
                 }
                return true;
            }   else {
                //retrieve the player they want to ignore.
            Player ignoredplayer = Bukkit.getPlayer(args[0]);
            if(ignoredplayer==null){
                GriefPrevention.sendMessage(p,TextMode.Err,Messages.PlayerNotFound);
                return true;
            }
            boolean nowignored = pdata.ToggleIgnored(ignoredplayer);
            Messages usemessage = nowignored?Messages.StartIgnorePlayer:Messages.StopIgnorePlayer;
            GriefPrevention.sendMessage(p,TextMode.Info,usemessage,ignoredplayer.getName());
            return true;

            }
        }
        return false;
    }

}
