package me.ryanhamshire.GriefPrevention.CommandHandling;


import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PermNodes;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
import java.util.Random;

public class ViewBook  extends GriefPreventionCommand {
    public static Random r = new Random();

    private static final int MaxBookCount = 15;

    public static Queue<BookViewData> RecentBooks = new ArrayDeque<BookViewData>();

    public static BookViewData AddRecentBook(ItemStack SourceBook){
        BookViewData bcd = new BookViewData(SourceBook);
        RecentBooks.add(bcd);
        while(RecentBooks.size() > MaxBookCount){
            RecentBooks.poll();
        }
        return bcd;
    }
    public static Queue<BookViewData> getRecentBooks(){ return RecentBooks;}

    @Override
    public String[] getLabels() {
        return new String[]{"ViewBook"};
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) return false;
        Player invoker = (Player)sender;
        if(!invoker.hasPermission(PermNodes.EavesDropPermission)){
            GriefPrevention.sendMessage(invoker,TextMode.Err,"The Viewbook command requires eavesdropping permission.");
            return true;
        }
        if(args.length==0){

            for(BookViewData bvd:RecentBooks){
               String usemessage = "ID:" + String.valueOf(bvd.getEavesDropID()) + " Title:" + ((BookMeta)(bvd.getBookItem().getItemMeta())).getTitle();
               GriefPrevention.sendMessage(invoker, TextMode.Info,usemessage);
            }
            GriefPrevention.sendMessage(invoker,TextMode.Info, "use /ViewBook <id> to receive a copy of the specified book.");
            return true;
        }
        else {
            try {int acquiredID = Integer.parseInt(args[0]);
            BookViewData founddata = null;
            for(BookViewData bvd:RecentBooks){
                if(bvd.getEavesDropID()==acquiredID){
                   founddata = bvd;
                   break;
                }
            }
            if(founddata==null){
                GriefPrevention.sendMessage(invoker,TextMode.Err,"EavesDrop ID not found.");
            }
                else {
                ItemStack cloned = founddata.getBookItem().clone();
                invoker.getWorld().dropItemNaturally(invoker.getLocation(),cloned);
                GriefPrevention.sendMessage(invoker,TextMode.Info,"You've been given a copy of the specified book.");
            }
                return true;
            }
            catch(Exception exx){
                return false;
            }

        }



    }
}
