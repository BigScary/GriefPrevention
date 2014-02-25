package me.ryanhamshire.GriefPrevention.CommandHandling;


import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.tasks.WorldClaimCleanupTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CleanClaimsCommand extends GriefPreventionCommand {

    @Override
    public String[] getLabels() {
        return new String[]{"CleanClaims"};
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        for(World w: Bukkit.getWorlds()){
                   int CleanedinWorld = 0;
                   WorldClaimCleanupTask wcc = new WorldClaimCleanupTask(w.getName());
                    List<Claim> WorldClaims = GriefPrevention.instance.dataStore.getClaimArray().getWorldClaims(w);
                    for(int i=0;i<WorldClaims.size();i++){
                        wcc.run();
                        CleanedinWorld = wcc.lastcleaned;
                        Player p = sender instanceof Player?(Player)sender:null;

                        GriefPrevention.sendMessage(p,TextMode.Info,"Cleaned up " + String.valueOf(CleanedinWorld) + " Claims in " + w.getName());
                    }

        }
        return true;
    }
}
