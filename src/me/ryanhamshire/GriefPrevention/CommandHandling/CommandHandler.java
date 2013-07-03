package me.ryanhamshire.GriefPrevention.CommandHandling;
import java.util.ArrayList;
//import java.util.Calendar;
import java.util.List;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.Debugger;
import me.ryanhamshire.GriefPrevention.Debugger.DebugLevel;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.MaterialInfo;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.ShovelMode;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.tasks.PlayerRescueTask;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;



public class CommandHandler implements CommandExecutor {


	private List<GriefPreventionCommand> GPCommands = new ArrayList<GriefPreventionCommand>(); 

	
	public CommandHandler(){
		
		//registers appropriate Commands. Each
		//command get's it's own happy class in the CommandHandling package.
		GPCommands.add(new GpHelpCommand());
		GPCommands.add(new AbandonClaimCommand());
		GPCommands.add(new AbandonAllClaimsCommand());
		GPCommands.add(new IgnoreClaimsCommand());
		GPCommands.add(new ClaimInfoCommand());
		GPCommands.add(new CleanClaimCommand());
		GPCommands.add(new SetClaimBlocksCommand());
		GPCommands.add(new SetBlockBankCommand());
		GPCommands.add(new ClearManagersCommand());
		GPCommands.add(new GPReloadCommand());
		GPCommands.add(new TransferClaimBlocksCommand());
		GPCommands.add(new RestoreNatureCommands());
		GPCommands.add(new TrustCommands());
		GPCommands.add(new LockClaimCommands());
		GPCommands.add(new GiveClaimCommand());
		GPCommands.add(new TransferClaimCommand());
		GPCommands.add(new TrustListCommand());
		GPCommands.add(new UntrustCommand());
		GPCommands.add(new BuySellClaimBlocks());
		GPCommands.add(new ClaimModeCommands());
		GPCommands.add(new DeleteClaimCommand());
		GPCommands.add(new ClaimExplosionsCommand());
		GPCommands.add(new ClaimsListCommand());
		GPCommands.add(new DeathBlowCommand());
		GPCommands.add(new AdjustBonusClaimBlocksCommand());
		GPCommands.add(new TrappedCommand());
		GPCommands.add(new SiegeCommand());
		GPCommands.add(new AbandonClaimCommand());
		GPCommands.add(new AbandonAllClaimsCommand());
		for(GriefPreventionCommand iterate:GPCommands){
			String[] gotlabels = iterate.getLabels();
			if(gotlabels==null){
				System.out.println("ERROR: GriefPrevention command named " + iterate.getClass().getName() + " returned null for labels!");
				continue;
			}
			for(String addcmd:gotlabels){
				PluginCommand pc = GriefPrevention.instance.getCommand(addcmd);
				try {
				Debugger.Write("Attaching Command \"" + addcmd + "\" to command class " + iterate.getClass().getName() , DebugLevel.Informational);
				pc.setExecutor(iterate);
				
				}
				catch(Exception exx){
					System.out.println("Exception adding command:" + addcmd);
					System.out.println("This is likely a mistake in plugin.yml, please notify the GriefPrevention maintainers :)");
					exx.printStackTrace();
				}
			}

		}
	
	}
	

	

	/**
	 * transfers a number of claim blocks from a source player to a  target player.
	 * @param Source Source player name. 
	 * @param string Target Player name.
	 * @return number of claim blocks transferred.
	 */
	static synchronized int transferClaimBlocks(String Source, String Target,int DesiredAmount) {
		// TODO Auto-generated method stub
		GriefPrevention inst = GriefPrevention.instance;
		//transfer claim blocks from source to target, return number of claim blocks transferred.
		PlayerData playerData = inst.dataStore.getPlayerData(Source);
		PlayerData receiverData = inst.dataStore.getPlayerData(Target);
		if(playerData!=null && receiverData!=null){
		    int xferamount = Math.min(playerData.accruedClaimBlocks,DesiredAmount);
		    playerData.accruedClaimBlocks-=xferamount;
		    receiverData.accruedClaimBlocks+=xferamount;
		    return xferamount;
		}
		return 0;
		
		
	}
	public boolean onCommand(CommandSender sender, Command cmd,
			String label, String[] args) {
		// TODO Auto-generated method stub
		
		GriefPrevention inst = GriefPrevention.instance;
		DataStore dataStore = inst.dataStore;
		Player player = null;
		WorldConfig wc = null;
		if (sender instanceof Player) 
		{
			player = (Player) sender;
			wc = inst.getWorldCfg(player.getWorld());
		}
		
		
		return false; 

	}
	private String getfriendlyLocationString(Location lesserBoundaryCorner) {
		// TODO Auto-generated method stub
		return GriefPrevention.instance.getfriendlyLocationString(lesserBoundaryCorner);
	}

	
	
	
}
