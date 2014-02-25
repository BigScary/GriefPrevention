package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Debugger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Created with IntelliJ IDEA.
 * User: BC_Programming
 * Date: 10/6/13
 * Time: 2:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class GPDebugCommand extends GriefPreventionCommand {

    @Override
    public String[] getLabels() {
        return new String[]{"gpdebug"};
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length ==0) return false;
        if(args.length==1) return false;
        if(args.length==2){

            if(args[0].equalsIgnoreCase("level")){

                String grabarg = args[1];
                Debugger.DebugLevel getlevel = Debugger.DebugLevel.valueOf(grabarg);
                Debugger.setDebugLevel(getlevel);
                sender.sendMessage("Debug Level set to '" + getlevel.name() + "'");
                return true;

            }



        }

        return false;
    }

}
