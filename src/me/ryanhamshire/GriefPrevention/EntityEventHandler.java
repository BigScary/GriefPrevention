/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.GriefPrevention;

import java.util.Calendar;
import java.util.List;

import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData;
import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData.ClaimAllowanceConstants;
import me.ryanhamshire.GriefPrevention.Configuration.PlacementRules.BasicPermissionConstants;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.minecart.ExplosiveMinecart;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.util.Vector;

//import com.gmail.nossr50.mcMMO;
//import com.gmail.nossr50.runnables.skills.BleedTimerTask;

//handles events related to entities
class EntityEventHandler implements Listener
{
	//convenience reference for the singleton datastore
	private DataStore dataStore;
	
	public EntityEventHandler(DataStore dataStore)
	{
		this.dataStore = dataStore;
	}
	private Claim ChangeBlockClaimCache = null;
	//don't allow endermen to change blocks
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityChangeBlock(EntityChangeBlockEvent event)
	{
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		
		
		if(event.getEntityType() == EntityType.ENDERMAN)
		{
			
			//if the enderman is holding something, this is a placement.
			Enderman theenderman = (Enderman)event.getEntity();
			if(theenderman.getCarriedMaterial()==null || theenderman.getCarriedMaterial().getItemType()==Material.AIR){
				//carrying nothing, so picking up. This could be reversed depending on when this event is
				//actually called...
				ClaimAllowanceConstants cac = wc.getEndermanPickupRules().Allowed(event.getBlock().getLocation(), null);
				if(cac.Allowed()) return;
				
				//DENY!
				event.setCancelled(true);
				
			}
			else {
				//otherwise, putting it down.
				ClaimAllowanceConstants cac = wc.getEndermanPlacementRules().Allowed(event.getBlock().getLocation(), null);
				if(cac.Allowed()) return;
				event.setCancelled(true);
			}
			
			
		}
		
		else if(event.getEntityType() == EntityType.SILVERFISH)
		{
			if(wc.getSilverfishBreakRules().Allowed(event.getBlock().getLocation(), null).Denied())
				event.setCancelled(true);
		}
		
		
		
		//don't allow the wither to break blocks, when the wither is determined, too expensive to constantly check for claimed blocks
		else if(event.getEntityType() == EntityType.WITHER && wc.getClaimsEnabled())
		{
			
			event.setCancelled(wc.getWitherEatBehaviour().Allowed(event.getEntity().getLocation(),null).Denied());
		}
	}
	
	//don't allow zombies to break down doors
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onZombieBreakDoor(EntityBreakDoorEvent event)
	{		
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		if(!wc.getZombieDoorBreaking().Allowed(event.getEntity().getLocation(), null).Allowed()) 
			event.setCancelled(true);
	}
	@EventHandler
	public void onShootBow(EntityShootBowEvent event){
		//if shot by a player, cache it for onProjectileHit.
		
	}
	
	//@EventHandler
	/*public void onProjectileHit(ProjectileHitEvent event){
		//currently we only deal with arrows.
		Player Shooter=null;
		
		
		Projectile proj = event.getEntity();
		if(proj.getShooter() instanceof Player) Shooter = (Player)(proj.getShooter());
		if(event.getEntityType()==EntityType.ARROW){
		    //get the block at the position of the arrow.
			Location projectilelocation = proj.getLocation();
			Vector vel = proj.getVelocity();
		    Block atpos = event.getEntity().getWorld().getBlockAt(projectilelocation);
		    Block secondblock;
		    Location secondpos = projectilelocation.add(new Vector(0,0,0).subtract(vel));
		    secondblock = proj.getWorld().getBlockAt(secondpos);
		    
		    
			if((atpos !=null && atpos.getType()==Material.WOOD_BUTTON) || 
					(secondblock!=null && secondblock.getType()==Material.WOOD_BUTTON)){
                WorldConfig wc = GriefPrevention.instance.getWorldCfg(proj.getWorld());
                if(wc.getArrowWoodenButtonRules().Allowed(proj.getLocation(), Shooter).Denied()){
                	proj.remove();
                	
                }
                
			}
			
			
			
		}
	}
	*/
	
	//don't allow entities to trample crops
	/**
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityInteract(EntityInteractEvent event)
	{
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		if(event.getEntity() instanceof Arrow)
		{
			Arrow proj = (Arrow)event.getEntity();
			Player source = null;
			if(proj.getShooter() instanceof Player)
				source = (Player)proj.getShooter();
				
			if(event.getBlock().getType()==Material.WOOD_BUTTON){
				if(wc.getArrowWoodenButtonRules().Allowed(event.getBlock().getLocation(), source).Denied()){
					event.setCancelled(true);
					//remove the arrow also.
					proj.remove();
					//send them a message.
					
					return;
				}
				
				
			}
			else if(event.getBlock().getType() == Material.WOOD_PLATE){
				
				if(wc.getArrowWoodenTouchPlateRules().Allowed(event.getBlock().getLocation(), source).Denied()){
					event.setCancelled(true);
					proj.remove();
					return;
				}
			}
			
			
		}
		if(event.getBlock().getType()==Material.STONE_PLATE){
			Player grabplayer = event.getEntity() instanceof Player?(Player)event.getEntity():null;
			if(wc.getStonePressurePlates().Allowed(event.getBlock().getLocation(), grabplayer).Denied()){
				event.setCancelled(true);
			
				return;
			}
		}
		
		if(!wc.creaturesTrampleCrops() && event.getBlock().getType() == Material.SOIL)
		{
			event.setCancelled(true);
		}
	}
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent explodeEvent)
	{		
		
		//System.out.println("EntityExplode:" + explodeEvent.getEntity().getClass().getName());
		List<Block> blocks = explodeEvent.blockList();
		Location location = explodeEvent.getLocation();
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(location.getWorld());
		
		Claim claimatEntity = GriefPrevention.instance.dataStore.getClaimAt(location, true);
		//quickest exit: if we are inside a claim and allowExplosions is false, break.
		//if(claimatEntity!=null && claimatEntity.areExplosivesAllowed) return;
		
	    //logic: we have Creeper and TNT Explosions currently. each one has special configuration options.
		//make sure that we are allowed to explode, first.
		Entity explodingEntity = explodeEvent.getEntity();
		boolean isCreeper = explodingEntity!=null && explodingEntity instanceof Creeper;
		boolean isTNT = explodingEntity !=null && 
				(explodingEntity instanceof TNTPrimed || explodingEntity instanceof ExplosiveMinecart);
		
		boolean isWither = explodingEntity !=null && (
				explodingEntity instanceof WitherSkull || explodingEntity instanceof Wither);
		
		
		ClaimBehaviourData preExplodeCheck = null;
		
		if(isCreeper){
			preExplodeCheck = wc.getCreeperExplosionBehaviour();
		}
		else if(isWither)
			preExplodeCheck = wc.getWitherExplosionBehaviour();
		else if(isTNT)
			preExplodeCheck = wc.getTNTExplosionBehaviour();
		else
			preExplodeCheck = wc.getOtherExplosionBehaviour();
		
		
		if(preExplodeCheck.Allowed(explodeEvent.getLocation(), null).Denied()) {
			explodeEvent.setCancelled(true);
			return;
		}
		
		
		ClaimBehaviourData usebehaviour = null;
		if(isCreeper) 
			usebehaviour = wc.getCreeperExplosionBlockDamageBehaviour();
		else if(isWither) 
			usebehaviour = wc.getWitherExplosionBlockDamageBehaviour();
		else if(isTNT) 
			usebehaviour = wc.getTNTExplosionBlockDamageBehaviour();
		else 
			usebehaviour = wc.getOtherExplosionBlockDamageBehaviour();
		Claim claimpos = null;
		////go through each block that was affected...
		for(int i=0;i<blocks.size();i++){
			Block block = blocks.get(i);
			//if(wc.getModsExplodableIds().contains(new MaterialInfo(block.getTypeId(), block.getData(), null))) continue;
			if(block.getX()==explodeEvent.getLocation().getBlockX() &&
					block.getY()==explodeEvent.getLocation().getBlockY() &&
					block.getZ()==explodeEvent.getLocation().getBlockZ()) continue;
			//creative rules stop all explosions, regardless of the other settings.
			if(wc.getCreativeRules() ||  (usebehaviour!=null && usebehaviour.Allowed(block.getLocation(),null).Denied())){
				//if not allowed. remove it...
				blocks.remove(i--);
			}
			else {
				//it is allowed, however, if it is on a claim only allow if explosions are enabled for that claim.
				claimpos = GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(), false);
				if(claimpos!=null && !claimpos.areExplosivesAllowed){
					blocks.remove(i--);
				}
				else if(block.getType() == Material.LOG)
				{
					GriefPrevention.instance.handleLogBroken(block);
				}
				
				
			}

		}
		
		
		
		
		
		
		
		
		
	}
	/*
	//when an entity explodes...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent explodeEvent)
	{		
		
		List<Block> blocks = explodeEvent.blockList();
		Location location = explodeEvent.getLocation();
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(location.getWorld());
		//FEATURE: explosions don't destroy blocks when they explode near or above sea level in standard worlds
		boolean isCreeper = (explodeEvent.getEntity() !=null && explodeEvent.getEntity() instanceof Creeper);
		boolean isWither = (explodeEvent.getEntity() !=null && explodeEvent.getEntity() instanceof Wither);
		boolean isTNT = (explodeEvent.getEntity()!=null && explodeEvent.getEntity() instanceof TNTPrimed);
		isTNT |= (explodeEvent.getEntity()!=null && explodeEvent.getEntity() instanceof ExplosiveMinecart);
		boolean CreeperCapable = wc.getCreeperExplosionBehaviour().Allowed(explodeEvent.getEntity().getLocation());
		boolean TNTCapable = wc.getTNTExplosionBehaviour().Allowed(explodeEvent.getEntity().getLocation());
		
		
		
		if(isCreeper){
			if(CreeperCapable){
				
				Claim cacheclaim = null;
				//iterate through the explosion blocklist and remove blocks that are not inside a claim.
				for(int i=0;i<blocks.size();i++){
					Block block = blocks.get(i);
					if(wc.mods_explodableIds().Contains(new MaterialInfo(block.getTypeId(), block.getData(), null))) continue;
					
					cacheclaim = GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(), false, cacheclaim);
					if(cacheclaim==null){
						blocks.remove(i--);
					}
					
				}
				
			}
		}
		
		if( location.getWorld().getEnvironment() == Environment.NORMAL && 
				wc.claims_enabled() && 
				((isCreeper && CreeperCapable) || 
						(isTNT && TNTCapable)))
		{
			for(int i = 0; i < blocks.size(); i++)
			{
				Block block = blocks.get(i);
				if(wc.mods_explodableIds().Contains(new MaterialInfo(block.getTypeId(), block.getData(), null))) continue;
				
				if(block.getLocation().getBlockY() > GriefPrevention.instance.getSeaLevel(location.getWorld()) - 7)
				{
					blocks.remove(i--);
				}
			}			
		}
		
		//special rule for creative worlds: explosions don't destroy anything
		if(GriefPrevention.instance.creativeRulesApply(explodeEvent.getLocation()))
		{
			for(int i = 0; i < blocks.size(); i++)
			{
				Block block = blocks.get(i);
				if(wc.mods_explodableIds().Contains(new MaterialInfo(block.getTypeId(), block.getData(), null))) continue;
				
				blocks.remove(i--);
			}
		}
		
		//FEATURE: explosions don't damage claimed blocks	
		Claim claim = null;
		for(int i = 0; i < blocks.size(); i++)  //for each destroyed block
		{
			Block block = blocks.get(i);
			if(block.getType() == Material.AIR) continue;  //if it's air, we don't care
			
			if(wc.mods_explodableIds().Contains(new MaterialInfo(block.getTypeId(), block.getData(), null))) continue;
			
			claim = this.dataStore.getClaimAt(block.getLocation(), false, claim); 
			//if the block is claimed, remove it from the list of destroyed blocks
			if(claim != null && !claim.areExplosivesAllowed)
			{
				blocks.remove(i--);
			}
			
			//if the block is not claimed and is a log, trigger the anti-tree-top code
			else if(block.getType() == Material.LOG)
			{
				GriefPrevention.instance.handleLogBroken(block);
			}
		}
	}
	*/
	//when an item spawns...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onItemSpawn(ItemSpawnEvent event)
	{
		//precheck: always allow Droppers to drop items when triggered.
		//We do this by seeing of there is a Dropper within a few blocks of the spawned item.
		
		
		
		Block centerblock = event.getEntity().getLocation().getBlock();
        for(int testx=-1;testx<=1;testx++){
        	for(int testy=-1;testy<=1;testy++){
        		for(int testz=-1;testz<=1;testz++){
        		Block grabblock = event.getEntity().getWorld().
        				getBlockAt(centerblock.getX() + testx,
        			centerblock.getY()+testy,
        			centerblock.getZ()+testz);
        		if(grabblock.getType().equals(Material.DROPPER)){
        			return;
        		}
        		}
        	}
        			
        }

		
		
		
		//if in a creative world, cancel the event (don't drop items on the ground)
		if(GriefPrevention.instance.creativeRulesApply(event.getLocation()))
		{
			event.setCancelled(true);
		}
		else {
			WorldConfig wc=  GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
			if(event.getEntity().getItemStack().getType()==Material.WRITTEN_BOOK){
				
				if(wc.getEavesDropBookDrop()){
					
					Player DroppingPlayer = GriefPrevention.instance.getNearestPlayer(event.getEntity().getLocation());
					StringBuffer buildMessage = new StringBuffer();
					
					BookMeta bm = (BookMeta) event.getEntity().getItemStack().getItemMeta();
					
					//bm.getTitle()
					buildMessage.append("Book Dropped by " + DroppingPlayer.getName() + " Titled:" + bm.getTitle());
					
					GriefPrevention.instance.sendEavesDropMessage(DroppingPlayer, buildMessage.toString());
					
				}
				
				
			}
		}
	}
	
	//when an experience bottle explodes...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onExpBottle(ExpBottleEvent event)
	{
		//if in a creative world, cancel the event (don't drop exp on the ground)
		if(GriefPrevention.instance.creativeRulesApply(event.getEntity().getLocation()))
		{
			event.setExperience(0);
		}
	}
	
	//when a creature spawns...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntitySpawn(CreatureSpawnEvent event)
	{
		
		LivingEntity entity = event.getEntity();
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(entity.getWorld());
		//these rules apply only to creative worlds
		
		
		//chicken eggs and breeding could potentially make a mess in the wilderness, once griefers get involved
		SpawnReason reason = event.getSpawnReason();

		if(reason == SpawnReason.BUILD_WITHER){
			//can we build a wither?
			if(wc.getWitherSpawnBehaviour().Allowed(entity.getLocation(), null).Denied()){
				event.setCancelled(true);
				//spawn what was used to make the wither (four soul sand and three skull items).
				//ItemStack SoulSand = new ItemStack(Material.SOUL_SAND,4);
				//ItemStack WitherSkulls = new ItemStack(Material.SKULL_ITEM,3,(short) 1);
				//entity.getWorld().dropItem(entity.getLocation(), SoulSand);
				//entity.getWorld().dropItem(entity.getLocation(),WitherSkulls);
				return;
			}
		}
		else if(reason == SpawnReason.BUILD_SNOWMAN){
			//can we build a snowman?
			if(wc.getSnowGolemSpawnBehaviour().Allowed(entity.getLocation(),null).Denied()){
				event.setCancelled(true);
				//spawn what was used to make the snowman. we'll spawn 8 snowball and a pumpkin.
				//ItemStack Snowballs = new ItemStack(Material.SNOW_BLOCK,2);
				//ItemStack Pumpkin = new ItemStack(Material.PUMPKIN,1);
				//entity.getWorld().dropItem(entity.getLocation(),Snowballs);
				//entity.getWorld().dropItem(entity.getLocation(),Pumpkin);
				return;
			}
		}
		else if(reason == SpawnReason.BUILD_IRONGOLEM){
			
			if(wc.getIronGolemSpawnBehaviour().Allowed(entity.getLocation(), null).Denied()){
				event.setCancelled(true);
				//ItemStack IronBlocks = new ItemStack(Material.IRON_BLOCK,3);
				//ItemStack Pumpkin = new ItemStack(Material.PUMPKIN,1);
			    //entity.getWorld().dropItem(entity.getLocation(),IronBlocks);
			    //entity.getWorld().dropItem(entity.getLocation(),Pumpkin);
			    return;
			    
			}
			
			
		}
		
		if(!GriefPrevention.instance.creativeRulesApply(entity.getLocation())) return;

		//otherwise, just apply the limit on total entities per claim 
		Claim claim = this.dataStore.getClaimAt(event.getLocation(), false);
		if(claim!=null && claim.allowMoreEntities() != null)
		{
			event.setCancelled(true);
			return;
		}
		if(reason != SpawnReason.SPAWNER_EGG && reason != SpawnReason.BUILD_IRONGOLEM && reason != SpawnReason.BUILD_SNOWMAN)
		{
			event.setCancelled(true);
			return;
		}
	}
	
	//when an entity dies...
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event)
	{
		LivingEntity entity = event.getEntity();
		
		//special rule for creative worlds: killed entities don't drop items or experience orbs
		if(GriefPrevention.instance.creativeRulesApply(entity.getLocation()))
		{
			event.setDroppedExp(0);
			event.getDrops().clear();			
		}
		
		//FEATURE: when a player is involved in a siege (attacker or defender role)
		//his death will end the siege
		
		if(!(entity instanceof Player)) return;  //only tracking players
		
		Player player = (Player)entity;
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(player.getWorld());
		//if involved in a siege
		if(playerData.siegeData != null)
		{
			//don't drop items as usual, they will be sent to the siege winner
			if(wc.getSiegeAutoTransfer()) event.getDrops().clear();
			
			//end it, with the dieing player being the loser
			this.dataStore.endSiege(playerData.siegeData, null, player.getName(), true /*ended due to death*/);
		}
		
		//if it is an ocelot or wolf, and the owner is under seige, 
		//inform the owner of the casualty on the line of battle.
		
		if(entity instanceof Wolf || entity instanceof Ocelot){
			
			if(entity instanceof Tameable){
				Tameable tamed = (Tameable)entity;
				if(tamed.isTamed()){
					String ownername = tamed.getOwner().getName();
					PlayerData ownerdata = GriefPrevention.instance.dataStore.getPlayerData(ownername);
					if(ownerdata!=null){
						if(ownerdata.siegeData!=null){
							//if the owner is the Defender...
							if(ownerdata.siegeData.defender == tamed.getOwner()){
								//inform them of the loss to their great cause.
								if(tamed.getOwner() instanceof Player){
									String tamedname = "";
									if(tamed instanceof Wolf) tamedname = "Wolf"; else tamedname="Ocelot";
									
									Player theplayer = (Player)tamed.getOwner();
									GriefPrevention.sendMessage(theplayer, TextMode.Info, Messages.TamedDeathDefend,tamedname);
									//theplayer.sendMessage(arg0)
								}
								
								
								
							}
							
							
						}
					}
					
					
					
				}
				
				
			}
			
			
			
		}
		
		
	}
	
	//when an entity picks up an item
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityPickup(EntityChangeBlockEvent event)
	{
		//FEATURE: endermen don't steal claimed blocks
		
		//if its an enderman
		if(event.getEntity() instanceof Enderman)
		{
			//and the block is claimed
			if(this.dataStore.getClaimAt(event.getBlock().getLocation(), false) != null)
			{
				//he doesn't get to steal it
				event.setCancelled(true);
			}
		}
	}
	
	//when a painting is broken
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onHangingBreak(HangingBreakEvent event)
    {
        //FEATURE: claimed paintings are protected from breakage
		
		//only allow players to break paintings, not anything else (like water and explosions)
		if(!(event instanceof HangingBreakByEntityEvent))
    	{
        	event.setCancelled(true);
        	return;
    	}
        
        HangingBreakByEntityEvent entityEvent = (HangingBreakByEntityEvent)event;
        
        //who is removing it?
		Entity remover = entityEvent.getRemover();
        
		//again, making sure the breaker is a player
		if(!(remover instanceof Player))
        {
        	event.setCancelled(true);
        	return;
        }
		
		//if the player doesn't have build permission, don't allow the breakage
		Player playerRemover = (Player)entityEvent.getRemover();
        String noBuildReason = GriefPrevention.instance.allowBuild(playerRemover, event.getEntity().getLocation());
        if(noBuildReason != null)
        {
        	event.setCancelled(true);
        	GriefPrevention.sendMessage(playerRemover, TextMode.Err, noBuildReason);
        }
    }
	
	//when a painting is placed...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPaintingPlace(HangingPlaceEvent event)
	{
		//FEATURE: similar to above, placing a painting requires build permission in the claim
	
		//if the player doesn't have permission, don't allow the placement
		String noBuildReason = GriefPrevention.instance.allowBuild(event.getPlayer(), event.getEntity().getLocation());
        if(noBuildReason != null)
        {
        	event.setCancelled(true);
        	GriefPrevention.sendMessage(event.getPlayer(), TextMode.Err, noBuildReason);
			return;
        }
		
		//otherwise, apply entity-count limitations for creative worlds
		else if(GriefPrevention.instance.creativeRulesApply(event.getEntity().getLocation()))
		{
			PlayerData playerData = this.dataStore.getPlayerData(event.getPlayer().getName());
			Claim claim = this.dataStore.getClaimAt(event.getBlock().getLocation(), false);
			if(claim == null) return;
			
			String noEntitiesReason = claim.allowMoreEntities();
			if(noEntitiesReason != null)
			{
				GriefPrevention.sendMessage(event.getPlayer(), TextMode.Err, noEntitiesReason);
				event.setCancelled(true);
				return;
			}
		}
	}
	private void CancelMMO(LivingEntity e){
		/*if(GriefPrevention.MinecraftMMO!=null){
			BleedTimerTask.remove(e);
		}
		*/
		
	}
	//when an entity is damaged
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntityDamage (EntityDamageEvent event)
	{
		//environmental damage
		if(event.getEntity() instanceof Hanging){ //hanging objects are not destroyed by explosions inside claims.
			Claim claimatpos = dataStore.getClaimAt(event.getEntity().getLocation(), false);
			if(claimatpos!=null){
				if(!claimatpos.areExplosivesAllowed){
					event.setCancelled(true);
				}
			}
			
		}
		
		
		if(!(event instanceof EntityDamageByEntityEvent)) return;
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		//monsters are never protected
		if(event.getEntity() instanceof Monster) return;
		
		EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
		
		//determine which player is attacking, if any
		Player attacker = null;
		Arrow arrow = null;
		Entity damageSource = subEvent.getDamager();
		if(damageSource instanceof Player)
		{
			attacker = (Player)damageSource;
		}
		else if(damageSource instanceof Arrow)
		{
			arrow = (Arrow)damageSource;
			if(arrow.getShooter() instanceof Player)
			{
				attacker = (Player)arrow.getShooter();
			}
		}
		else if(damageSource instanceof ThrownPotion)
		{
			ThrownPotion potion = (ThrownPotion)damageSource;
			if(potion.getShooter() instanceof Player)
			{
				attacker = (Player)potion.getShooter();
			}
		}
		else if(damageSource instanceof Snowball){
			Snowball sball = (Snowball)damageSource;
			if(sball.getShooter() instanceof Player){
				attacker = (Player)sball.getShooter();
			}
		}
		else if(damageSource instanceof Egg){
			Egg segg = (Egg)damageSource;
			if(segg.getShooter() instanceof Player){
				attacker = (Player)segg.getShooter();
			}
		}
		else if(damageSource instanceof Fireball){
			Fireball fball = (Fireball)damageSource;
			if(fball.getShooter() instanceof Player){
				attacker = (Player)fball.getShooter();
			}
		}
		else if(damageSource instanceof WitherSkull){
			WitherSkull wskull = (WitherSkull)damageSource;
			if(wskull.getShooter() instanceof Player){
				attacker = (Player)wskull.getShooter();
			}
		}
		
		//if the attacker is a player and defender is a player (pvp combat)
		if(attacker != null && event.getEntity() instanceof Player && event.getEntity().getWorld().getPVP())
		{
			//FEATURE: prevent pvp in the first minute after spawn, and prevent pvp when one or both players have no inventory
			
			//doesn't apply when the attacker has the no pvp immunity permission
			//this rule is here to allow server owners to have a world with no spawn camp protection by assigning permissions based on the player's world
			if(attacker.hasPermission("griefprevention.nopvpimmunity")) return;
			
			Player defender = (Player)(event.getEntity());
			
			PlayerData defenderData = this.dataStore.getPlayerData(((Player)event.getEntity()).getName());
			PlayerData attackerData = this.dataStore.getPlayerData(attacker.getName());
			if(defender instanceof Player && attacker instanceof Player)
				if(!defender.isOnline() || !attacker.isOnline()) return;
			//otherwise if protecting spawning players
			if(wc.getProtectFreshSpawns())
			{
				if(defenderData.pvpImmune)
				{
					event.setCancelled(true);
					CancelMMO((LivingEntity)event.getEntity());
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.ThatPlayerPvPImmune);
					return;
				}
				
				if(attackerData.pvpImmune)
				{
					event.setCancelled(true);
					CancelMMO((LivingEntity)event.getEntity());
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
					return;
				}		
			}
			
			//FEATURE: prevent players from engaging in PvP combat inside land claims (when it's disabled)
			if(wc.getPvPNoCombatinPlayerClaims() || wc.getNoPvPCombatinAdminClaims())
			{
				Claim attackerClaim = this.dataStore.getClaimAt(attacker.getLocation(), false);
				if(	attackerClaim != null && 
					(attackerClaim.isAdminClaim() && wc.getNoPvPCombatinAdminClaims() ||
					!attackerClaim.isAdminClaim() && wc.getPvPNoCombatinPlayerClaims()))
				{
					attackerData.lastClaim = attackerClaim;
					event.setCancelled(true);
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
					return;
				}
				
				Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false);
				if( defenderClaim != null &&
					(defenderClaim.isAdminClaim() && wc.getNoPvPCombatinAdminClaims() ||
					!defenderClaim.isAdminClaim() && wc.getPvPNoCombatinPlayerClaims()))
				{
					defenderData.lastClaim = defenderClaim;
					event.setCancelled(true);
					CancelMMO((LivingEntity)event.getEntity());
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.PlayerInPvPSafeZone);
					return;
				}
			}
			
			//FEATURE: prevent players who very recently participated in pvp combat from hiding inventory to protect it from looting
			//FEATURE: prevent players who are in pvp combat from logging out to avoid being defeated
			
			long now = Calendar.getInstance().getTimeInMillis();
			defenderData.lastPvpTimestamp = now;
			defenderData.lastPvpPlayer = attacker.getName();
			attackerData.lastPvpTimestamp = now;
			attackerData.lastPvpPlayer = defender.getName();			
		}
		
		//FEATURE: protect claimed animals, boats, minecarts
		//NOTE: animals can be lead with wheat, vehicles can be pushed around.
		//so unless precautions are taken by the owner, a resourceful thief might find ways to steal anyway
		
		//if theft protection is enabled
		if(event instanceof EntityDamageByEntityEvent)
		{
			//if the entity is an non-monster creature (remember monsters disqualified above), or a vehicle
			//
			if (subEvent.getEntity() instanceof Creature)
			{
				
				
				
				Claim cachedClaim = null;
				PlayerData playerData = null;
				if(attacker != null)
				{
					playerData = this.dataStore.getPlayerData(attacker.getName());
					cachedClaim = playerData.lastClaim;
				}
				
				Claim claim = this.dataStore.getClaimAt(event.getEntity().getLocation(), false);
				
				//if it's claimed
				if(claim != null)
				{
					//if damaged by anything other than a player (exception villagers injured by zombies in admin claims), cancel the event
					//why exception?  so admins can set up a village which can't be CHANGED by players, but must be "protected" by players.
					//Additional exception added: cactus, lava, and drowning of entities happens.
					if(attacker == null)
					{
						//exception case
						if(event.getEntity() instanceof Villager && damageSource instanceof Monster && claim.isAdminClaim())
						{
							return;
						}
						else if(event.getCause().equals(DamageCause.CONTACT) || event.getCause().equals(DamageCause.DROWNING)){
								return;
						}
						//all other cases
						else
						{
							event.setCancelled(true);
							CancelMMO((LivingEntity)event.getEntity());
							
						}						
					}
					
					//otherwise the player damaging the entity must have permission,
					//or, for wolves and ocelots, apply special logic for sieges.
					else
					{		
						if(event.getEntityType()==EntityType.WOLF || event.getEntityType()==EntityType.OCELOT){
							//get the claim at this position...
							Claim mobclaim = GriefPrevention.instance.dataStore.getClaimAt(event.getEntity().getLocation(),true);
							//is this claim under siege?
							if(mobclaim!=null && mobclaim.siegeData!=null){
								
								SiegeData sd = mobclaim.siegeData;
								//get the defending player.
								Player defender = sd.defender;
								//if the player attacking this entity is within 15 blocks, don't cancel.
								if(attacker.getLocation().distance(defender.getLocation()) < wc.getSiegeTamedAnimalDistance()){
									event.setCancelled(false);
									CancelMMO((LivingEntity)event.getEntity());
									return;
								}
								
								
							}
							
							
							
						}
						
						
						String noContainersReason = claim.allowContainers(attacker);
						if(noContainersReason != null)
						{
							event.setCancelled(true);
							CancelMMO((LivingEntity)event.getEntity());
							//kill the arrow to avoid infinite bounce between crowded together animals
							if(arrow != null) arrow.remove();
							
							GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.NoDamageClaimedEntity, claim.getOwnerName());
						}
						
						//cache claim for later
						if(playerData != null)
						{
							playerData.lastClaim = claim;
						}						
					}
				}
			}
		}
	}
	
	//when a vehicle is damaged
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onVehicleDamage (VehicleDamageEvent event)
	{
		WorldConfig wc = new WorldConfig(event.getVehicle().getWorld());
		
		//all of this is anti theft code
				
		
		//determine which player is attacking, if any
		Player attacker = null;
		Entity damageSource = event.getAttacker();
		if(damageSource instanceof Player)
		{
			attacker = (Player)damageSource;
		}
		else if(damageSource instanceof Arrow)
		{
			Arrow arrow = (Arrow)damageSource;
			if(arrow.getShooter() instanceof Player)
			{
				attacker = (Player)arrow.getShooter();
			}
		}
		else if(damageSource instanceof ThrownPotion)
		{
			ThrownPotion potion = (ThrownPotion)damageSource;
			if(potion.getShooter() instanceof Player)
			{
				attacker = (Player)potion.getShooter();
			}
		}
		//if Damage source is unspecified and we allow environmental damage, don't cancel the event.
		else if(damageSource ==null && wc.getEnvironmentalVehicleDamage().Allowed(event.getVehicle().getLocation(),attacker,false).Allowed()){
			return;
		}
		//NOTE: vehicles can be pushed around.
		//so unless precautions are taken by the owner, a resourceful thief might find ways to steal anyway
		Claim cachedClaim = null;
		PlayerData playerData = null;
		if(attacker != null)
		{
			playerData = this.dataStore.getPlayerData(attacker.getName());
			cachedClaim = playerData.lastClaim;
		}
		
		Claim claim = this.dataStore.getClaimAt(event.getVehicle().getLocation(), false);
		
		//if it's claimed
		if(claim != null)
		{
			//if damaged by anything other than a player, or a cactus,
			if(attacker == null)
			{
				event.setCancelled(true);
			}
			
			//otherwise the player damaging the entity must have permission
			else
			{		
				String noContainersReason = claim.allowContainers(attacker);
				if(noContainersReason != null)
				{
					event.setCancelled(true);
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.NoDamageClaimedEntity, claim.getOwnerName());
				}
				
				//cache claim for later
				if(playerData != null)
				{
					playerData.lastClaim = claim;
				}						
			}
		}
	}
}
