package me.ryanhamshire.GriefPrevention.CommandHandling;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GpHelpCommand extends GriefPreventionCommand {
	private static final String[] HelpIndex = new String[] { 
		ChatColor.AQUA + "-----=GriefPrevention Help Index=------",
                         "use /gphelp [topic] to view each topic." ,
ChatColor.YELLOW +       "Topics: Claims,Trust"};


private static final String[] ClaimsHelp = new String[] {
		ChatColor.AQUA + "-----=GriefPrevention Claims=------" ,
      ChatColor.YELLOW + " GriefPrevention uses Claims to allow you to claim areas and prevent " ,
      ChatColor.YELLOW + "other players from messing with your stuff without your permission.",
      ChatColor.YELLOW + "Claims are created by either placing your first Chest or by using the",
      ChatColor.YELLOW + "Claim creation tool, which is by default a Golden Shovel.",
      ChatColor.YELLOW + "You can resize your claims by using a Golden Shovel on a corner, or",
      ChatColor.YELLOW + "by defining a new claim that encompasses it. The original claim",
      ChatColor.YELLOW + "Will be resized. you can use trust commands to give other players abilities",
      ChatColor.YELLOW + "In your claim. See /gphelp trust for more information"};

private static final String[] TrustHelp = new String[] {
	ChatColor.AQUA +     "------=GriefPrevention Trust=------",
	ChatColor.YELLOW +   "You can control who can do things in your claims by using the trust commands",
	ChatColor.YELLOW +   "/accesstrust can be used to allow a player to interact with items in your claim",
	ChatColor.YELLOW +   "/containertrust can be used to allow a player to interact with your chests.",
	ChatColor.YELLOW +   "/trust allows players to build on your claim.",
	ChatColor.YELLOW +   "Each trust builds upon the previous one in this list; containertrust includes accesstrust",
	ChatColor.YELLOW +   "and build trust includes container trust and access trust."};
	                     
	
	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[]{"gphelp"};
	}
	private void handleHelp(CommandSender p,String Topic){
		if(p==null) return;
		String[] uselines;
		if(Topic.equalsIgnoreCase("claims"))
			uselines = ClaimsHelp;
		else if(Topic.equalsIgnoreCase("trust"))
			uselines = TrustHelp;
		else
			uselines = HelpIndex;
			
			
		for(String iterate:uselines){
		    p.sendMessage(iterate);	
		}
			
			
		
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		if(sender != null){
			
			String topic="index";
			if(args.length>0) topic = args[0];
				handleHelp(sender,topic);
			
				
			}
		
		return true;
	}
	
	
	
	
	
	

}
