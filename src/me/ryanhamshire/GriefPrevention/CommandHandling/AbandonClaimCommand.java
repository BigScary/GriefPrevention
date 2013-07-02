package me.ryanhamshire.GriefPrevention.CommandHandling;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;
import me.ryanhamshire.GriefPrevention.visualization.Visualization;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AbandonClaimCommand extends GriefPreventionCommand {
	private boolean abandonClaimHandler(Player player, boolean deleteTopLevelClaim) 
	{
		GriefPrevention inst = GriefPrevention.instance;
		DataStore dataStore = inst.dataStore;
		PlayerData playerData = dataStore.getPlayerData(player.getName());
		
		WorldConfig wc = inst.getWorldCfg(player.getWorld());
		
		//which claim is being abandoned?
		Claim claim = inst.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
		if(claim == null)
		{
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.AbandonClaimMissing);
			return true;
		}
		int claimarea = claim.getArea();
		//retrieve (1-abandonclaimration)*totalarea to get amount to subtract from the accrued claim blocks
		//after we delete the claim.
		int costoverhead =(int)Math.floor((double)claimarea*(1-wc.getClaimsAbandonReturnRatio()));
		//System.out.println("costoverhead:" + costoverhead);
		
		
		//verify ownership
		if(claim.allowEdit(player) != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
		}
		
		//don't allow abandon of claims if not configured to allow.
		else if(!wc.getAllowUnclaim() )
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoCreativeUnClaim);
		}
		
		//warn if has children and we're not explicitly deleting a top level claim
		else if(claim.children.size() > 0 && !deleteTopLevelClaim)
		{
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.DeleteTopLevelClaim);
			return true;
		}
		
		//if the claim is locked, let's warn the player and give them a chance to back out
		else if(!playerData.warnedAboutMajorDeletion && claim.neverdelete)
		{			
			GriefPrevention.sendMessage(player, TextMode.Warn, Messages.ConfirmAbandonLockedClaim);
			playerData.warnedAboutMajorDeletion = true;
		}
		//if auto-restoration is enabled,
		else if(!playerData.warnedAboutMajorDeletion && wc.getClaimsAbandonNatureRestoration())
				{
			GriefPrevention.sendMessage(player,TextMode.Warn,Messages.AbandonClaimRestoreWarning);
			playerData.warnedAboutMajorDeletion=true;
		}
		else if(!playerData.warnedAboutMajorDeletion && costoverhead!=claimarea){
			playerData.warnedAboutMajorDeletion=true;
			GriefPrevention.sendMessage(player,TextMode.Warn,Messages.AbandonCostWarning,String.valueOf(costoverhead));
		}
		//if the claim has lots of surface water or some surface lava, warn the player it will be cleaned up
		else if(!playerData.warnedAboutMajorDeletion && claim.hasSurfaceFluids() && claim.parent == null)
		{			
			GriefPrevention.sendMessage(player, TextMode.Warn, Messages.ConfirmFluidRemoval);
			playerData.warnedAboutMajorDeletion = true;
		}
		
		else
		{
			//delete it
			//Only do water/lava cleanup when it's a top level claim.
			if(claim.parent == null) {
				claim.removeSurfaceFluids(null);
			}
			//retrieve area of this claim...
			
			
			if(!inst.dataStore.deleteClaim(claim,player)){
				//cancelled!
				//assume the event called will show an appropriate message...
				return false;
			}
			
			//if in a creative mode world, restore the claim area
			//CHANGE: option is now determined by configuration options.
			//if we are in a creative world and the creative Abandon Nature restore option is enabled,
			//or if we are in a survival world and the creative Abandon Nature restore option is enabled,
			//then perform the restoration.
			if((wc.getClaimsAbandonNatureRestoration())){
			
				GriefPrevention.AddLogEntry(player.getName() + " abandoned a claim @ " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
				GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
				GriefPrevention.instance.restoreClaim(claim, 20L * 60 * 2);
			}
			//remove the interest cost, and message the player.
			if(costoverhead > 0){
			    playerData.accruedClaimBlocks-=costoverhead;
				//
			    GriefPrevention.sendMessage(player, TextMode.Warn, Messages.AbandonCost,0,String.valueOf(costoverhead));
			}
			int remainingBlocks = playerData.getRemainingClaimBlocks();
			//tell the player how many claim blocks he has left
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.AbandonSuccess, 0,String.valueOf(remainingBlocks));
			
			//revert any current visualization
			Visualization.Revert(player);
			
			playerData.warnedAboutMajorDeletion = false;
			}
		
		
		return true;
		
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		// TODO Auto-generated method stub
		if(sender==null) return false;
		if(!(sender instanceof Player)) return false;
		Player p =(Player)sender;
		boolean dotoplevel = label.equalsIgnoreCase("abandontoplevelclaim");
		return this.abandonClaimHandler(p, false);
	}

	@Override
	public String[] getLabels() {
		// TODO Auto-generated method stub
		return new String[] {"abandonclaim","abandontoplevelclaim"};
	}
	

}
