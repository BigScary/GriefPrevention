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

import java.util.*;

import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData;
import me.ryanhamshire.GriefPrevention.Configuration.ClaimBehaviourData.ClaimAllowanceConstants;
import me.ryanhamshire.GriefPrevention.Configuration.SiegeableData;
import me.ryanhamshire.GriefPrevention.Configuration.WorldConfig;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.meta.BookMeta;

//import com.gmail.nossr50.mcMMO;
//import com.gmail.nossr50.runnables.skills.BleedTimerTask;

//handles events related to entities
class EntityEventHandler implements Listener {
	private Claim ChangeBlockClaimCache = null;

	// convenience reference for the singleton datastore

    private DataStore getDataStore(){ return GriefPrevention.instance.dataStore;}

	public EntityEventHandler() {

	}

	private void CancelMMO(LivingEntity e) {
		/*
		 * if(GriefPrevention.MinecraftMMO!=null){ BleedTimerTask.remove(e); }
		 */

	}

	// don't allow endermen to change blocks
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		if(!wc.Enabled()) return;
		if (event.getEntityType() == EntityType.ENDERMAN) {

			// if the enderman is holding something, this is a placement.
			Enderman theenderman = (Enderman) event.getEntity();
			if (theenderman.getCarriedMaterial() == null || theenderman.getCarriedMaterial().getItemType() == Material.AIR) {
				// carrying nothing, so picking up. This could be reversed
				// depending on when this event is
				// actually called...
				ClaimAllowanceConstants cac = wc.getEndermanPickupRules().Allowed(event.getBlock().getLocation(), null);
				if (cac.Allowed())
					return;

				// DENY!
				event.setCancelled(true);
				return;
			} else {
				// otherwise, putting it down.
				ClaimAllowanceConstants cac = wc.getEndermanPlacementRules().Allowed(event.getBlock().getLocation(), null);
				if (cac.Allowed())
					return;
				event.setCancelled(true);
				return;
			}

		}

		else if (event.getEntityType() == EntityType.SILVERFISH) {

			if (wc.getSilverfishBreakRules().Allowed(event.getEntity().getLocation().getBlock().getLocation(), null).Denied())
				event.setCancelled(true);
			return;
		}

		// don't allow the wither to break blocks, when the wither is
		// determined, too expensive to constantly check for claimed blocks
		else if (event.getEntityType() == EntityType.WITHER && wc.getClaimsEnabled()) {

			event.setCancelled(wc.getWitherEatBehaviour().Allowed(event.getEntity().getLocation(), null).Denied());
			return;
		}
	}

	// when an entity is damaged
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntityDamage(EntityDamageEvent event) {
        Debugger.Write("onEntityDamage- " + event.getClass().getName() + "  instance:" + event.getEntity().getClass().getName(), Debugger.DebugLevel.Verbose);

		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		if (!wc.Enabled())
			return;

        //PvP damage. since we have logic inside for PvP stuff, we want to disable ALL of that logic if PvP is not enabled.


            if(event instanceof EntityDamageByEntityEvent){

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
                Entity Damager = subEvent.getDamager();
                if(Damager!=null){
                    Debugger.Write("Damager instance:" + subEvent.getDamager().getClass().getName(), Debugger.DebugLevel.Verbose);
                }

            if(!wc.getPvPEnabled()){
                if(Damager instanceof Player && event.getEntity() instanceof Player){
                    return;
                }
            }
                    //tweak: use class data to determine if entities were Pixelmon, and if so, allow them
                //to damage one another.
                if(Damager.getClass().getName().endsWith("EntityPixelmon") &&
                        subEvent.getEntity().getClass().getName().endsWith("EntityPixelmon"))
                {

                    return;
                }


            }


		// environmental damage
        Claim claimatpos = getDataStore().getClaimAt(event.getEntity().getLocation(), false);
		if (event.getEntity() instanceof Hanging) { // hanging objects are not
													// destroyed by explosions
													// inside claims.


			if (claimatpos != null) {
				if (!claimatpos.areExplosivesAllowed) {
					event.setCancelled(true);
				}
			}


		}

		if (!(event instanceof EntityDamageByEntityEvent))
			return;

		// monsters are never protected
		if (event.getEntity() instanceof Monster)
			return;





		EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;

		// determine which player is attacking, if any
		Player attacker = null;
		Arrow arrow = null;
		Entity damageSource = subEvent.getDamager();
		if (damageSource instanceof Player) {
			attacker = (Player) damageSource;
        }
        else if(damageSource instanceof Fish){
            Fish f = (Fish)damageSource;
            if(f.getShooter() instanceof Player){
                attacker = (Player)f.getShooter();
            }

		} else if (damageSource instanceof Arrow) {
			arrow = (Arrow) damageSource;
			if (arrow.getShooter() instanceof Player) {
				attacker = (Player) arrow.getShooter();
			}
		} else if (damageSource instanceof ThrownPotion) {
			ThrownPotion potion = (ThrownPotion) damageSource;
			if (potion.getShooter() instanceof Player) {
				attacker = (Player) potion.getShooter();
			}
		} else if (damageSource instanceof Snowball) {
			Snowball sball = (Snowball) damageSource;
			if (sball.getShooter() instanceof Player) {
				attacker = (Player) sball.getShooter();
			}
		} else if (damageSource instanceof Egg) {
			Egg segg = (Egg) damageSource;
			if (segg.getShooter() instanceof Player) {
				attacker = (Player) segg.getShooter();
			}
		} else if (damageSource instanceof Fireball) {
			Fireball fball = (Fireball) damageSource;
			if (fball.getShooter() instanceof Player) {
				attacker = (Player) fball.getShooter();
			}
		} else if (damageSource instanceof WitherSkull) {
			WitherSkull wskull = (WitherSkull) damageSource;
			if (wskull.getShooter() instanceof Player) {
				attacker = (Player) wskull.getShooter();
			}
		}
        if(event.getEntity() instanceof Hanging){
            if(wc.getItemFrameRules().Allowed(event.getEntity().getLocation(),attacker).Denied()){
                event.setCancelled(true);
                return;
            }
            else {
                event.setCancelled(false);
                return;
            }
        }
        //horses are protected:
        //In wilderness: Protected from everybody except the owner.
        //In Claim: Protected from everybody except the owner, Unless being ridden, in which case players with trust on that claim
        //can attack the horse.
        //this can be modelled by a rule but people don't like my rules framework *sadface*
        if(attacker!=null && GriefPrevention.instance.isHorse(event.getEntity()) && ((Horse)event.getEntity()).isTamed()){
            Horse h = (Horse)event.getEntity();
            //if the attacker owns the horse, he can abuse it as he sees fit.
            if(h.getOwner()!=null && h.getOwner().getName().equals(attacker.getName()))
            {
                return;
            }
            if(claimatpos==null){

                if(!attacker.getName().equals(h.getOwner().getName())){
                    //deny
                    String owner = h.getOwner()==null?"unknown":h.getOwner().getName();
                    GriefPrevention.sendMessage(attacker,TextMode.Err,Messages.NoDamageClaimedEntity,owner);
                    event.setCancelled(true);
                    return;
                }
            }
            else {
                //inside a claim. More tricky. It's allowed if the attacker has trust on the claim and the player riding the horse doesn't.
                //if there is no rider, it is disallowed.
                //Does the horse have a rider?
                if(h.getPassenger()!=null && h.getPassenger() instanceof Player){
                    Player rider = (Player)(h.getPassenger());
                    //ok, does the attacker have Access Trust on the Claim? And does the attacker NOT
                    //have that access?

                    if(claimatpos.allowAccess(attacker)==null && claimatpos.allowAccess(rider)!=null){
                        //indeed. Horse has a passenger, AND the attacker is in a claim they have trust in- allow it.
                        event.setCancelled(false);
                        return;
                    }
                    else {
                        event.setCancelled(true);
                        GriefPrevention.sendMessage(attacker,TextMode.Err,Messages.NoDamageClaimedEntity,rider.getName());
                        return;
                    }
                }
                else if(h.getPassenger()==null){
                //not allowed. This may cause issues, as players can leave their horses in other peoples claims and cause problems.
                //will be changed based on feedback. Looking at this it's funny because I could just use a rule for all of this. That's too complicated though :P
                    String owner = h.getOwner()==null?"Unknown":h.getOwner().getName();
                    GriefPrevention.sendMessage(attacker,TextMode.Err,Messages.NoDamageClaimedEntity,owner);
                    event.setCancelled(true);
                    return;
                }
            }
        }




		// if the attacker is a player and defender is a player (pvp combat)
		if (attacker != null && event.getEntity() instanceof Player && event.getEntity().getWorld().getPVP()) {
			// FEATURE: prevent pvp in the first minute after spawn, and prevent
			// pvp when one or both players have no inventory
            Debugger.Write("PVP Damage detected between " + ((Player)event.getEntity()).getName() + " And " + attacker.getName(),Debugger.DebugLevel.Verbose);
			// doesn't apply when the attacker has the no pvp immunity
			// permission
			// this rule is here to allow server owners to have a world with no
			// spawn camp protection by assigning permissions based on the
			// player's world
			if (attacker.hasPermission(PermNodes.NoPvPImmunityPermission)){
                Debugger.Write("PVP Damage: Attacker (" + attacker.getName() + ") has " + PermNodes.NoPvPImmunityPermission + " Permission.",Debugger.DebugLevel.Verbose);
				return;
            }

			Player defender = (Player) (event.getEntity());

			PlayerData defenderData = this.getDataStore().getPlayerData(defender.getName());
			PlayerData attackerData = this.getDataStore().getPlayerData(attacker.getName());
			if (defender instanceof Player && attacker instanceof Player)
				if (!defender.isOnline() || !attacker.isOnline())
					return;
			// otherwise if protecting spawning players
			if (wc.getSpawnProtectEnabled()) {

                Debugger.Write("Spawn Protection Enabled...", Debugger.DebugLevel.Verbose);
                Debugger.Write("Defender PvPImmune=" + defenderData.pvpImmune, Debugger.DebugLevel.Verbose);
                Debugger.Write("Attacker PvPImmune=" + attackerData.pvpImmune,Debugger.DebugLevel.Verbose);
                if(!defenderData.pvpImmune && attackerData.pvpImmune && wc.getSpawnProtectDisableonInstigate()){
                //disable the attacker's pvp immunity.
                    Debugger.Write("Disabling PVP immunity for attacking player," + attacker.getName(), Debugger.DebugLevel.Verbose);

                    attackerData.pvpImmune=false;
                }

				else{
                    if (defenderData.pvpImmune) {
                        Debugger.Write("Defender is immune. Cancelling.",Debugger.DebugLevel.Verbose);
                        event.setCancelled(true);
                        CancelMMO((LivingEntity) event.getEntity());
                        GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.ThatPlayerPvPImmune);
                        return;
                    }


				if (attackerData.pvpImmune) {
					event.setCancelled(true);
                    Debugger.Write("Attacker is immune. Cancelling.",Debugger.DebugLevel.Verbose);
					CancelMMO((LivingEntity) event.getEntity());
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
					return;
				}
            }
			}

			// FEATURE: prevent players from engaging in PvP combat inside land
			// claims (when it's disabled)
			if (wc.getPvPNoCombatinPlayerClaims() || wc.getNoPvPCombatinAdminClaims()) {
				Claim attackerClaim = this.getDataStore().getClaimAt(attacker.getLocation(), false);
				if (attackerClaim != null && (attackerClaim.isAdminClaim() && wc.getNoPvPCombatinAdminClaims() || !attackerClaim.isAdminClaim() && wc.getPvPNoCombatinPlayerClaims())) {
					attackerData.lastClaim = attackerClaim;
					event.setCancelled(true);
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
					return;
				}

				Claim defenderClaim = this.getDataStore().getClaimAt(defender.getLocation(), false);
				if (defenderClaim != null && (defenderClaim.isAdminClaim() && wc.getNoPvPCombatinAdminClaims() || !defenderClaim.isAdminClaim() && wc.getPvPNoCombatinPlayerClaims())) {
					defenderData.lastClaim = defenderClaim;
					event.setCancelled(true);
					CancelMMO((LivingEntity) event.getEntity());
					GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.PlayerInPvPSafeZone);
					return;
				}
			}

			// FEATURE: prevent players who very recently participated in pvp
			// combat from hiding inventory to protect it from looting
			// FEATURE: prevent players who are in pvp combat from logging out
			// to avoid being defeated

			long now = Calendar.getInstance().getTimeInMillis();
			defenderData.lastPvpTimestamp = now;
			defenderData.lastPvpPlayer = attacker.getName();
			attackerData.lastPvpTimestamp = now;
			attackerData.lastPvpPlayer = defender.getName();

		}

		// FEATURE: protect claimed animals, boats, minecarts
		// NOTE: animals can be lead with wheat, vehicles can be pushed around.
		// so unless precautions are taken by the owner, a resourceful thief
		// might find ways to steal anyway

		// if theft protection is enabled
		//
			if (subEvent.getEntity() instanceof Creature) {

				Claim cachedClaim = null;
				PlayerData playerData = null;
				if (attacker != null) {
					playerData = this.getDataStore().getPlayerData(attacker.getName());
					cachedClaim = playerData.lastClaim;
				}

				Claim claim = this.getDataStore().getClaimAt(event.getEntity().getLocation(), false);

				// if it's claimed
				if (claim != null) {

					// if damaged by anything other than a player (exception
					// villagers injured by zombies in admin claims), cancel the
					// event
					// why exception? so admins can set up a village which can't
					// be CHANGED by players, but must be "protected" by
					// players.
					// Additional exception added: cactus, lava, and drowning of
					// entities happens.

					if (attacker == null) {
						// exception case
						if (event.getEntity() instanceof Villager && damageSource instanceof Monster && claim.isAdminClaim()) {
							return;
						} else if (event.getCause().equals(DamageCause.CONTACT) || event.getCause().equals(DamageCause.DROWNING)) {
							return;
						}
						// all other cases
						else {
							event.setCancelled(true);
							CancelMMO((LivingEntity) event.getEntity());

						}
					}

					// otherwise the player damaging the entity must have
					// permission,
					// or, for wolves and ocelots, apply special logic for
					// sieges.
					else {
						if (event.getEntityType() == EntityType.WOLF || event.getEntityType() == EntityType.OCELOT) {
							// get the claim at this position...
							Claim mobclaim = GriefPrevention.instance.dataStore.getClaimAt(event.getEntity().getLocation(), true);
							// is this claim under siege?
							if (mobclaim != null && mobclaim.siegeData != null) {

								SiegeData sd = mobclaim.siegeData;
								// get the defending player.
								Player defender = sd.defender;
								// if the player attacking this entity is within
								// 15 blocks, don't cancel.
								if (attacker.getLocation().distance(defender.getLocation()) < wc.getSiegeTamedAnimalDistance()) {
									event.setCancelled(false);
									CancelMMO((LivingEntity) event.getEntity());
									return;
								}

							}

						}

						String noContainersReason = claim.allowContainers(attacker);
						if (noContainersReason != null) {
							event.setCancelled(true);
							CancelMMO((LivingEntity) event.getEntity());
							// kill the arrow to avoid infinite bounce between
							// crowded together animals
							if (arrow != null)
								arrow.remove();

							String Claimowner = claim.getOwnerName();
							if(Claimowner==null || Claimowner.length()==0) Claimowner="Administrator";
							GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.NoDamageClaimedEntity, claim.getOwnerName());
						}

						// cache claim for later
						if (playerData != null) {
							playerData.lastClaim = claim;
						}
					}
				}
			}

	}

	// @EventHandler
	/*
	 * public void onProjectileHit(ProjectileHitEvent event){ //currently we
	 * only deal with arrows. Player Shooter=null;
	 * 
	 * 
	 * Projectile proj = event.getEntity(); if(proj.getShooter() instanceof
	 * Player) Shooter = (Player)(proj.getShooter());
	 * if(event.getEntityType()==EntityType.ARROW){ //get the block at the
	 * position of the arrow. Location projectilelocation = proj.getLocation();
	 * Vector vel = proj.getVelocity(); Block atpos =
	 * event.getEntity().getWorld().getBlockAt(projectilelocation); Block
	 * secondblock; Location secondpos = projectilelocation.add(new
	 * Vector(0,0,0).subtract(vel)); secondblock =
	 * proj.getWorld().getBlockAt(secondpos);
	 * 
	 * 
	 * if((atpos !=null && atpos.getType()==Material.WOOD_BUTTON) ||
	 * (secondblock!=null && secondblock.getType()==Material.WOOD_BUTTON)){
	 * WorldConfig wc = GriefPrevention.instance.getWorldCfg(proj.getWorld());
	 * if(wc.getArrowWoodenButtonRules().Allowed(proj.getLocation(),
	 * Shooter).Denied()){ proj.remove();
	 * 
	 * }
	 * 
	 * }
	 * 
	 * 
	 * 
	 * } }
	 */

	// when an entity dies...
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		if (!wc.Enabled())
			return;
		LivingEntity entity = event.getEntity();

		// special rule for creative worlds: killed entities don't drop items or
		// experience orbs
		if (GriefPrevention.instance.creativeRulesApply(entity.getLocation())) {
			event.setDroppedExp(0);
			event.getDrops().clear();
		}

		// FEATURE: when a player is involved in a siege (attacker or defender
		// role)
		// his death will end the siege

		if (entity instanceof Player) {

			Player player = (Player) entity;
			PlayerData playerData = this.getDataStore().getPlayerData(player.getName());

			// if involved in a siege
			if (playerData.siegeData != null) {
				// don't drop items as usual, they will be sent to the siege
				// winner
				if (wc.getSiegeAutoTransfer())
					event.getDrops().clear();

				// end it, with the dieing player being the loser
				this.getDataStore().endSiege(playerData.siegeData, null, player.getName(), true /*
																							 * ended
																							 * due
																							 * to
																							 * death
																							 */);
				return;
			}
		}
		// if it is an ocelot or wolf, and the owner is under Siege,
		// inform the owner of the casualty on the line of battle.

		else if (entity instanceof Wolf || entity instanceof Ocelot) {

			if (entity instanceof Tameable) {
				Tameable tamed = (Tameable) entity;
				if (tamed.isTamed()) {
					String ownername = tamed.getOwner().getName();
					PlayerData ownerdata = GriefPrevention.instance.dataStore.getPlayerData(ownername);
					if (ownerdata != null) {
						if (ownerdata.siegeData != null) {
							// if the owner is the Defender...
							if (ownerdata.siegeData.defender == tamed.getOwner()) {
								// inform them of the loss to their great cause.
								if (tamed.getOwner() instanceof Player) {
									String tamedname = "";
									if (tamed instanceof Wolf)
										tamedname = "Wolf";
									else
										tamedname = "Ocelot";

									Player theplayer = (Player) tamed.getOwner();
									GriefPrevention.sendMessage(theplayer, TextMode.Info, Messages.TamedDeathDefend, tamedname);
									// theplayer.sendMessage(arg0)
								}

							}

						}
					}

				}

			}

		}

	}

    private Set<Entity> HandledEntities = new HashSet<Entity>();
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent explodeEvent) {



		// System.out.println("EntityExplode:" +
		// explodeEvent.getEntity().getClass().getName());
		List<Block> blocks = explodeEvent.blockList();
		Location location = explodeEvent.getLocation();
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(location.getWorld());
		if (!wc.Enabled())
			return;
		Claim claimatEntity = GriefPrevention.instance.dataStore.getClaimAt(location, true);
		// quickest exit: if we are inside a claim and allowExplosions is false,
		// break.
		// if(claimatEntity!=null && claimatEntity.areExplosivesAllowed) return;

		// logic: we have Creeper and TNT Explosions currently. each one has
		// special configuration options.
		// make sure that we are allowed to explode, first.
		Entity explodingEntity = explodeEvent.getEntity();


		boolean isCreeper = explodingEntity != null && explodingEntity instanceof Creeper;
		boolean isTNT = explodingEntity != null && (explodingEntity instanceof TNTPrimed || explodingEntity instanceof ExplosiveMinecart);

		boolean isWither = explodingEntity != null && (explodingEntity instanceof WitherSkull || explodingEntity instanceof Wither);

		ClaimBehaviourData preExplodeCheck = null;

		if (isCreeper) {
			preExplodeCheck = wc.getCreeperExplosionBehaviour();
		} else if (isWither)
			preExplodeCheck = wc.getWitherExplosionBehaviour();
		else if (isTNT)
			preExplodeCheck = wc.getTNTExplosionBehaviour();
		else
			preExplodeCheck = wc.getOtherExplosionBehaviour();

		if (preExplodeCheck.Allowed(explodeEvent.getLocation(), null).Denied()) {
			//Debugger.Write("Explosion cancelled.", DebugLevel.Verbose);
			explodeEvent.setCancelled(true);
            explodeEvent.blockList().clear();
			return;
		}

		ClaimBehaviourData usebehaviour = null;
		if (isCreeper)
			usebehaviour = wc.getCreeperExplosionBlockDamageBehaviour();
		else if (isWither)
			usebehaviour = wc.getWitherExplosionBlockDamageBehaviour();
		else if (isTNT){
			usebehaviour = wc.getTNTExplosionBlockDamageBehaviour();
            /*if(wc.getTNTCoalesceBehaviour().Allowed(explodeEvent.getLocation(), null).Allowed()
                    && ! HandledEntities.contains(explodeEvent.getEntity())
                    ){
                  //try to coalesce nearby TNTPrimed Entities.
                 //count nearby TNTPrimed and add 25% more power for each.
                //we only do this if the entity being kerploded isn't a "handled" entity.

                 float powerfactor = this.getCoalescedPower(explodeEvent.getEntity(),5,true);
                 //generate a new TNTPrimed with a lower fuse time in this position, and set it to have a higher yield.
                TNTPrimed newTNT = (TNTPrimed)(explodeEvent.getEntity().getWorld().spawnEntity(explodeEvent.getEntity().getLocation(),EntityType.PRIMED_TNT));
                HandledEntities.add(newTNT);
                newTNT.setFuseTicks(2);
                float newYield= newTNT.getYield()*powerfactor;
                newTNT.setYield(newYield);

            } else if(HandledEntities.contains(explodeEvent.getEntity())){
                HandledEntities.remove(explodeEvent.getEntity());
            } */
		}
		else
			usebehaviour = wc.getOtherExplosionBlockDamageBehaviour();
		Claim claimpos = GriefPrevention.instance.dataStore.getClaimAt(explodeEvent.getLocation(),true);
		// //go through each block that was affected...
		for (int i = 0; i < blocks.size(); i++)
        {



			Block block = blocks.get(i);
            Claim explodepos = GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(),false);
            if(explodepos!=null && explodepos.siegeData!=null){
                //under siege...
                Debugger.Write("Explosion Block in claim under siege.", Debugger.DebugLevel.Verbose);


                    float gotpower=0;
                //-1 is returned if it is not in that list.
                    if((i>0) && (-1==(gotpower=SiegeableData.getListPower(wc.getTNTSiegeBlocks(),block.getType())) ||
                    (gotpower > explodeEvent.getYield()))){
                        Debugger.Write("cancelling:" + block.getType().name() + "Power=" + gotpower + " Yield:" + explodeEvent.getYield() + " i=" + i, Debugger.DebugLevel.Verbose);
                        //getListPower will return the power of the specified Material, or -1 if the material is
                        //not in the list. if it's not in the list, remove it; if the retrieved power is greater than the explosion events
                        //explosion yield, remove it.

                        //remove it.
                       blocks.remove(i--);

                        String usekey = GriefPrevention.getfriendlyLocationString(block.getLocation());
                        // if it already contains an entry, the block was broken
                        // during this siege
                        // and replaced with another block that is being broken
                        // again.
                        if (explodepos.siegeData.SiegedBlocks.containsKey(usekey)) {

                        } else {
                            // otherwise, we have to add it to the siege blocks
                            // list.
                            explodepos.siegeData.SiegedBlocks.put(usekey, new BrokenBlockInfo(block.getLocation()));
                            // replace it manually
                            block.setType(Material.AIR);



                        }
                        continue;


                    }




                }

			// if(wc.getModsExplodableIds().contains(new
			// MaterialInfo(block.getTypeId(), block.getData(), null)))
			// continue;
			if (explodeEvent.getEntity()==null &&  block.getX() == explodeEvent.getLocation().getBlockX() && block.getY() == explodeEvent.getLocation().getBlockY() && block.getZ() == explodeEvent.getLocation().getBlockZ())
            {

				continue;

            }
			// creative rules stop all explosions, regardless of the other
			// settings.
			if (wc.getDenyAllExplosions() || (usebehaviour != null && usebehaviour.Allowed(block.getLocation(), null).Denied())) {
				// if not allowed. remove it...
				blocks.remove(i--);
                continue;
			} else {
				// it is allowed, however, if it is on a claim only allow if
				// explosions are enabled for that claim.
				claimpos = GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(), false);
				if ( i>0 && claimpos != null && !claimpos.areExplosivesAllowed) {
					blocks.remove(i--);
				} else if (block.getType() == Material.LOG) {
					GriefPrevention.instance.handleLogBroken(block);
				}

			}


        //now, check if we are in a claim, if so and if that claim is under siege, add all blocks still in the blocks list to
        //the revert list of that claim.
        if(claimpos!=null){
            if(claimpos.siegeData!=null){
                //claim is under siege.
                if(wc.getSiegeBlockRevert()){
                    for(Block iterate:blocks){
                        String usekey = GriefPrevention.getfriendlyLocationString(iterate.getLocation());
                        claimpos.siegeData.SiegedBlocks.put(usekey, new BrokenBlockInfo(iterate.getLocation()));
                    }
                }


            }
        }
	}
    }
	// don't allow entities to trample crops
	/**
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onEntityInteract(EntityInteractEvent event) {
        Debugger.Write("onEntityInteract, instance:" + event.getEntity().getClass().getName(), Debugger.DebugLevel.Verbose);
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		if (!wc.Enabled())
			return;
		Player grabplayer = null;
		PlayerData pdata = null;

		if (event.getEntity() instanceof Arrow) {
			Arrow proj = (Arrow) event.getEntity();
			Player source = null;
			if (proj.getShooter() instanceof Player)
				source = (Player) proj.getShooter();

			if (event.getBlock().getType() == Material.WOOD_BUTTON) {
				if (wc.getArrowWoodenButtonRules().Allowed(event.getBlock().getLocation(), source).Denied()) {
					event.setCancelled(true);
					// remove the arrow also.
					proj.remove();
					// send them a message.

					return;
				}

			} else if (event.getBlock().getType() == Material.WOOD_PLATE) {

				if (wc.getArrowWoodenTouchPlateRules().Allowed(event.getBlock().getLocation(), source).Denied()) {
					event.setCancelled(true);
					proj.remove();
					return;
				}
			}

		} else {

			grabplayer = event.getEntity() instanceof Player ? (Player) event.getEntity() : null;
			if (grabplayer != null) {
                if(event.getEntity() instanceof Hanging){
                    //can't interact with paintings yet...
                    if(event.getEntity() instanceof ItemFrame){
                          if(wc.getItemFrameRules().Allowed(event.getEntity().getLocation(),grabplayer).Denied()){
                             event.setCancelled(true);
                              return;
                          }
                    }
                 //itemframe or painting.
                }
				if (event.getEntity().getPassenger() != null) {
					if (event.getEntity().getPassenger() instanceof Player) {
						grabplayer = (Player) event.getEntity().getPassenger();
					}
				}

			}
		}
		if (grabplayer != null){
			PlayerInteractEvent pie = new PlayerInteractEvent(grabplayer, Action.PHYSICAL, grabplayer.getItemInHand(), null, null);
			Bukkit.getPluginManager().callEvent(pie);
			if(pie.isCancelled()){ 
				event.setCancelled(true);
				return;
			}
		}
		/*	pdata = GriefPrevention.instance.dataStore.getPlayerData(grabplayer.getName());
		if (event.getBlock().getType() == Material.STONE_PLATE) {
			if (grabplayer != null) {
				pdata.PlateData.put(event.getBlock().getLocation().toString(), pdata.new PressurePlateData(event.getBlock().getLocation()));
			}
			if (wc.getStonePressurePlates().Allowed(event.getBlock().getLocation(), grabplayer).Denied()) {
				event.setCancelled(true);

				return;
			}
		} else if (event.getBlock().getType() == Material.WOOD_PLATE) {

			if (grabplayer != null) {
				pdata.PlateData.put(event.getBlock().getLocation().toString(), pdata.new PressurePlateData(event.getBlock().getLocation()));
			}
			if (wc.getWoodPressurePlates().Allowed(event.getBlock().getLocation(), grabplayer).Denied()) {
				event.setCancelled(true);
				return;
			}
		}*/

		if (!wc.creaturesTrampleCrops() && event.getBlock().getType() == Material.SOIL) {
			event.setCancelled(true);
		}
	}

	// when an entity picks up an item
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityPickup(EntityChangeBlockEvent event) {
		// FEATURE: endermen don't steal claimed blocks
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		if (!wc.Enabled())
			return;
		// if its an enderman
		if (event.getEntity() instanceof Enderman) {
			if (wc.getEndermanPickupRules().Allowed(event.getBlock().getLocation(), null).Denied()) {
				event.setCancelled(true);
			}

		}
	}

	// when a creature spawns...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntitySpawn(CreatureSpawnEvent event) {

		LivingEntity entity = event.getEntity();
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(entity.getWorld());
		// these rules apply only to creative worlds
		if(!wc.Enabled()) return;
		// chicken eggs and breeding could potentially make a mess in the
		// wilderness, once griefers get involved
		SpawnReason reason = event.getSpawnReason();

		if (reason == SpawnReason.BUILD_WITHER) {
			// can we build a wither?
			if (wc.getWitherSpawnBehaviour().Allowed(entity.getLocation(), null).Denied()) {
				event.setCancelled(true);
				// spawn what was used to make the wither (four soul sand and
				// three skull items).
				// ItemStack SoulSand = new ItemStack(Material.SOUL_SAND,4);
				// ItemStack WitherSkulls = new
				// ItemStack(Material.SKULL_ITEM,3,(short) 1);
				// entity.getWorld().dropItem(entity.getLocation(), SoulSand);
				// entity.getWorld().dropItem(entity.getLocation(),WitherSkulls);
				return;
			}
		} else if (reason == SpawnReason.BUILD_SNOWMAN) {
			// can we build a snowman?
			if (wc.getSnowGolemSpawnBehaviour().Allowed(entity.getLocation(), null).Denied()) {
				event.setCancelled(true);
				// spawn what was used to make the snowman. we'll spawn 8
				// snowball and a pumpkin.
				// ItemStack Snowballs = new ItemStack(Material.SNOW_BLOCK,2);
				// ItemStack Pumpkin = new ItemStack(Material.PUMPKIN,1);
				// entity.getWorld().dropItem(entity.getLocation(),Snowballs);
				// entity.getWorld().dropItem(entity.getLocation(),Pumpkin);
				return;
			}
		} else if (reason == SpawnReason.BUILD_IRONGOLEM) {

			if (wc.getIronGolemSpawnBehaviour().Allowed(entity.getLocation(), null).Denied()) {
				event.setCancelled(true);
				// ItemStack IronBlocks = new ItemStack(Material.IRON_BLOCK,3);
				// ItemStack Pumpkin = new ItemStack(Material.PUMPKIN,1);
				// entity.getWorld().dropItem(entity.getLocation(),IronBlocks);
				// entity.getWorld().dropItem(entity.getLocation(),Pumpkin);
				return;

			}

		}

		if (!GriefPrevention.instance.creativeRulesApply(entity.getLocation()))
			return;

		// otherwise, just apply the limit on total entities per claim
		Claim claim = this.getDataStore().getClaimAt(event.getLocation(), false);
		if (claim != null && claim.allowMoreEntities() != null) {
			event.setCancelled(true);
			return;
		}
		if (reason != SpawnReason.SPAWNER_EGG && reason != SpawnReason.BUILD_IRONGOLEM && reason != SpawnReason.BUILD_SNOWMAN) {
			event.setCancelled(true);
			return;
		}
	}

	// when an experience bottle explodes...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onExpBottle(ExpBottleEvent event) {
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		if (!wc.Enabled())
			return;
		// if in a creative world, cancel the event (don't drop exp on the
		// ground)
		if (GriefPrevention.instance.creativeRulesApply(event.getEntity().getLocation())) {
			event.setExperience(0);
		}
	}

	// when a painting is broken
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onHangingBreak(HangingBreakEvent event) {
		// FEATURE: claimed paintings are protected from breakage
        Debugger.Write("onHangingBreak", Debugger.DebugLevel.Verbose);
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		if (!wc.Enabled())
			return;
		// only allow players to break paintings, not anything else (like water
		// and explosions)
		if (!(event instanceof HangingBreakByEntityEvent)) {
			event.setCancelled(true);
			return;
		}

		HangingBreakByEntityEvent entityEvent = (HangingBreakByEntityEvent) event;

		// who is removing it?
		Entity remover = entityEvent.getRemover();




		// again, making sure the breaker is a player
		if (!(remover instanceof Player)) {
			event.setCancelled(true);
			return;
		}

		// if the player doesn't have build permission, don't allow the breakage
		Player playerRemover = (Player) entityEvent.getRemover();
		String noBuildReason = GriefPrevention.instance.allowBuild(playerRemover, event.getEntity().getLocation());
		if (noBuildReason != null) {
			event.setCancelled(true);
			GriefPrevention.sendMessage(playerRemover, TextMode.Err, noBuildReason);
		}
	}

	/*
	 * //when an entity explodes...
	 * 
	 * @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	 * public void onEntityExplode(EntityExplodeEvent explodeEvent) {
	 * 
	 * List<Block> blocks = explodeEvent.blockList(); Location location =
	 * explodeEvent.getLocation(); WorldConfig wc =
	 * GriefPrevention.instance.getWorldCfg(location.getWorld()); //FEATURE:
	 * explosions don't destroy blocks when they explode near or above sea level
	 * in standard worlds boolean isCreeper = (explodeEvent.getEntity() !=null
	 * && explodeEvent.getEntity() instanceof Creeper); boolean isWither =
	 * (explodeEvent.getEntity() !=null && explodeEvent.getEntity() instanceof
	 * Wither); boolean isTNT = (explodeEvent.getEntity()!=null &&
	 * explodeEvent.getEntity() instanceof TNTPrimed); isTNT |=
	 * (explodeEvent.getEntity()!=null && explodeEvent.getEntity() instanceof
	 * ExplosiveMinecart); boolean CreeperCapable =
	 * wc.getCreeperExplosionBehaviour
	 * ().Allowed(explodeEvent.getEntity().getLocation()); boolean TNTCapable =
	 * wc
	 * .getTNTExplosionBehaviour().Allowed(explodeEvent.getEntity().getLocation
	 * ());
	 * 
	 * 
	 * 
	 * if(isCreeper){ if(CreeperCapable){
	 * 
	 * Claim cacheclaim = null; //iterate through the explosion blocklist and
	 * remove blocks that are not inside a claim. for(int
	 * i=0;i<blocks.size();i++){ Block block = blocks.get(i);
	 * if(wc.mods_explodableIds().Contains(new MaterialInfo(block.getTypeId(),
	 * block.getData(), null))) continue;
	 * 
	 * cacheclaim =
	 * GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(), false,
	 * cacheclaim); if(cacheclaim==null){ blocks.remove(i--); }
	 * 
	 * }
	 * 
	 * } }
	 * 
	 * if( location.getWorld().getEnvironment() == Environment.NORMAL &&
	 * wc.claims_enabled() && ((isCreeper && CreeperCapable) || (isTNT &&
	 * TNTCapable))) { for(int i = 0; i < blocks.size(); i++) { Block block =
	 * blocks.get(i); if(wc.mods_explodableIds().Contains(new
	 * MaterialInfo(block.getTypeId(), block.getData(), null))) continue;
	 * 
	 * if(block.getLocation().getBlockY() >
	 * GriefPrevention.instance.getSeaLevel(location.getWorld()) - 7) {
	 * blocks.remove(i--); } } }
	 * 
	 * //special rule for creative worlds: explosions don't destroy anything
	 * if(GriefPrevention
	 * .instance.creativeRulesApply(explodeEvent.getLocation())) { for(int i =
	 * 0; i < blocks.size(); i++) { Block block = blocks.get(i);
	 * if(wc.mods_explodableIds().Contains(new MaterialInfo(block.getTypeId(),
	 * block.getData(), null))) continue;
	 * 
	 * blocks.remove(i--); } }
	 * 
	 * //FEATURE: explosions don't damage claimed blocks Claim claim = null;
	 * for(int i = 0; i < blocks.size(); i++) //for each destroyed block { Block
	 * block = blocks.get(i); if(block.getType() == Material.AIR) continue; //if
	 * it's air, we don't care
	 * 
	 * if(wc.mods_explodableIds().Contains(new MaterialInfo(block.getTypeId(),
	 * block.getData(), null))) continue;
	 * 
	 * claim = this.getDataStore().getClaimAt(block.getLocation(), false, claim);
	 * //if the block is claimed, remove it from the list of destroyed blocks
	 * if(claim != null && !claim.areExplosivesAllowed) { blocks.remove(i--); }
	 * 
	 * //if the block is not claimed and is a log, trigger the anti-tree-top
	 * code else if(block.getType() == Material.LOG) {
	 * GriefPrevention.instance.handleLogBroken(block); } } }
	 */
	// when an item spawns...
	@EventHandler(priority = EventPriority.LOWEST)
	public void onItemSpawn(ItemSpawnEvent event) {
		// precheck: always allow Droppers to drop items when triggered.
		// We do this by seeing of there is a Dropper within a few blocks of the
		// spawned item.

		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getLocation().getWorld());
		if (!wc.Enabled())
			return;

		Block centerblock = event.getEntity().getLocation().getBlock();
		for (int testx = -1; testx <= 1; testx++) {
			for (int testy = -1; testy <= 1; testy++) {
				for (int testz = -1; testz <= 1; testz++) {
					Block grabblock = event.getEntity().getWorld().getBlockAt(centerblock.getX() + testx, centerblock.getY() + testy, centerblock.getZ() + testz);
					if (grabblock.getType().equals(Material.DROPPER)) {
						return;
					}
				}
			}

		}

		// if in a creative world, cancel the event (don't drop items on the
		// ground)
		if (!wc.getAllowItemSpawn()) {
			event.setCancelled(true);
		} else {

			if (event.getEntity().getItemStack().getType() == Material.WRITTEN_BOOK) {

				if (wc.getEavesDropBookDrop()) {

					Player DroppingPlayer = GriefPrevention.instance.getNearestPlayer(event.getEntity().getLocation());
					StringBuffer buildMessage = new StringBuffer();

					BookMeta bm = (BookMeta) event.getEntity().getItemStack().getItemMeta();

					// bm.getTitle()
					buildMessage.append("Book Dropped by " + DroppingPlayer.getName() + " Titled:" + bm.getTitle());

					GriefPrevention.sendEavesDropMessage(DroppingPlayer, buildMessage.toString());

				}

			}
		}
	}
    private Location getAffectedLocation(HangingPlaceEvent ev){
        if(ev.getBlock()==null) return null;
        Location uselocation = ev.getBlock().getLocation();
        BlockFace bf = ev.getBlockFace();
        int xoffset=bf.getModX(),yoffset=bf.getModY(),zoffset=bf.getModZ();
        //south: Z+ North Z-
        //east: X+ West X-
        //Up: Y+ Down Y-
        return new Location(ev.getBlock().getWorld(),uselocation.getX()+xoffset,uselocation.getY()+yoffset,uselocation.getZ()+zoffset);
    }
	// when a painting is placed...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPaintingPlace(HangingPlaceEvent event) {
		// FEATURE: similar to above, placing a painting requires build
		// permission in the claim
        WorldConfig wc=  GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
        Location placedpos = getAffectedLocation(event);


        Debugger.Write("onpaintingPlace:" + event.getEntity().getClass().getName(),Debugger.DebugLevel.Verbose);




		if (!wc.Enabled())
			return;

		// if the player doesn't have permission, don't allow the placement
		String noBuildReason = GriefPrevention.instance.allowBuild(event.getPlayer(), placedpos);
		if (noBuildReason != null) {
			event.setCancelled(true);
			GriefPrevention.sendMessage(event.getPlayer(), TextMode.Err, noBuildReason);
			return;
		}

		// otherwise, apply entity-count limitations for creative worlds
		else if (GriefPrevention.instance.creativeRulesApply(placedpos)) {
			PlayerData playerData = this.getDataStore().getPlayerData(event.getPlayer().getName());
			Claim claim = this.getDataStore().getClaimAt(placedpos, false);
			if (claim == null)
				return;

			String noEntitiesReason = claim.allowMoreEntities();
			if (noEntitiesReason != null) {
				GriefPrevention.sendMessage(event.getPlayer(), TextMode.Err, noEntitiesReason);
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler
	public void onShootBow(EntityShootBowEvent event) {
		// if shot by a player, cache it for onProjectileHit.

	}

	// when a vehicle is damaged
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onVehicleDamage(VehicleDamageEvent event) {
		
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getVehicle().getWorld());
		
		if (!wc.Enabled())
			return;
		// all of this is anti theft code
                
		// determine which player is attacking, if any
		Player attacker = null;
		Entity damageSource = event.getAttacker();
		if (damageSource instanceof Player) {
			attacker = (Player) damageSource;
		} else if (damageSource instanceof Arrow) {
			Arrow arrow = (Arrow) damageSource;
			if (arrow.getShooter() instanceof Player) {
				attacker = (Player) arrow.getShooter();
			}
		} else if (damageSource instanceof ThrownPotion) {
			ThrownPotion potion = (ThrownPotion) damageSource;
			if (potion.getShooter() instanceof Player) {
				attacker = (Player) potion.getShooter();
			}
		}
		// if Damage source is unspecified and we allow environmental damage,
		// don't cancel the event.
		if(attacker==null && wc.getEnvironmentalVehicleDamage().Allowed(event.getVehicle().getLocation(), null,false).Denied()){
			
			event.setCancelled(true);
			return;
		}else if (attacker!=null && wc.getVehicleDamage().Allowed(event.getVehicle().getLocation(), attacker, true).Denied()) {
			event.setCancelled(true);
			return;
		}
		
		// NOTE: vehicles can be pushed around.
		// so unless precautions are taken by the owner, a resourceful thief
		// might find ways to steal anyway
	


		
	}

	// don't allow zombies to break down doors
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onZombieBreakDoor(EntityBreakDoorEvent event) {
		WorldConfig wc = GriefPrevention.instance.getWorldCfg(event.getEntity().getWorld());
		if (!wc.Enabled())
			return;
		if (!wc.getZombieDoorBreaking().Allowed(event.getEntity().getLocation(), null).Allowed())
			event.setCancelled(true);
	}
}
